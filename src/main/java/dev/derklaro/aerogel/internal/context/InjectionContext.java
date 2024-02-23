/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dev.derklaro.aerogel.internal.context;

import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import dev.derklaro.aerogel.internal.proxy.InjectionTimeProxy;
import dev.derklaro.aerogel.internal.proxy.ProxyMapping;
import dev.derklaro.aerogel.internal.util.Preconditions;
import io.leangen.geantyref.GenericTypeReflector;
import jakarta.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Pasqual Koschmieder
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0")
public final class InjectionContext {

  private static final MemberInjectionRequest[] EMPTY_MEMBER_REQUEST_ARRAY = new MemberInjectionRequest[0];

  /**
   * This context is ready to execute construction.
   */
  private static final int STATE_READY = 0;
  /**
   * This context is currently constructing a value.
   */
  private static final int STATE_CONSTRUCTING = 1;
  /**
   * This context was proxied and calls to {@link #resolveInstance()} will return a proxy (with or without delegate).
   */
  private static final int STATE_PROXIED = 2;
  /**
   * This context was delegated and call to {@link #resolveInstance()} will return the value known to
   * {@link #delegate}.
   */
  private static final int STATE_DELEGATED = 3;

  /**
   * Stores the direct reference to the root context. This prevents the need to travel up the full tree in order to get
   * the root context.
   */
  private final InjectionContext root;
  /**
   * The binding instance that requested the construction of the element.
   */
  private final InstalledBinding<?> binding;

  /**
   * The context provider that tracks this scope, null if no provider is responsible for this scope.
   */
  private final InjectionContextProvider contextProvider;

  /**
   * All proxies that were created somewhere in the tree. Note that this collection is only writeable in a root context,
   * in all other cases modifying the collection will result in an exception.
   */
  private final List<ContextualProxy> knownProxies;
  /**
   * Holds all mappings of overridden instances for the current context tree, note that this list is only present on the
   * root context, in all other cases modifying the map will result in an exception.
   */
  private final Map<BindingKey<?>, Provider<?>> overrides;
  /**
   * Holds all member injection requests that were made somewhere in the tree. Note that this collection is only
   * writeable in a root context, in all other cases modifying the collection will result in an exception.
   */
  private final Set<MemberInjectionRequest> requestedMemberInjections;

  /**
   * Set to true on the root context of a construction tree if finishConstruction was called.
   */
  private boolean obsolete = false;
  /**
   * Indicates that this context is just a virtual context and should be skipped for any operation. While the leaf is
   * present in the tree, any checks made based on virtual nodes might be faulty as they are inserted to break circular
   * references, and therefore and the root leaf which caused the problem.
   */
  private boolean virtual = false;

  /**
   * The context prior to this one, null if this context is the root context.
   */
  private InjectionContext prev;
  /**
   * The context which was created based on this context, null if this is the tail context.
   */
  private InjectionContext next;

  /**
   * The current state of this context, see the static state fields.
   */
  private int state = STATE_READY;
  /**
   * The current constructed reference of this context.
   */
  private Object delegate;

  /**
   * The created proxy of this context, null if no proxy was created for this context yet.
   */
  private ProxyMapping createdProxy;
  /**
   * The constructions which are waiting to be resumed by this context once the underlying value was constructed.
   */
  private Queue<InjectionContext> waitingConstructions;
  /**
   * The added listeners for construction finish.
   */
  private Queue<BiConsumer<InjectionContext, Object>> constructionFinishListeners;

  /**
   * Constructs a new root injection context.
   *
   * @param binding         the binding that requested this context.
   * @param overrides       the overridden instances whose delegates are present immediately.
   * @param contextProvider the context provider that is tracking the context.
   */
  public InjectionContext(
    @NotNull InstalledBinding<?> binding,
    @NotNull Map<BindingKey<?>, Provider<?>> overrides,
    @Nullable InjectionContextProvider contextProvider
  ) {
    this.binding = binding;
    this.contextProvider = contextProvider;

    this.root = this;
    this.knownProxies = new ArrayList<>();
    this.overrides = new HashMap<>(overrides); // TODO: should this map be immutable?
    this.requestedMemberInjections = new LinkedHashSet<>();
  }

  /**
   * Constructs a new sub injection context of the given root context.
   *
   * @param root    the root context.
   * @param binding the binding that requested this context.
   */
  private InjectionContext(
    @NotNull InjectionContext root,
    @NotNull InstalledBinding<?> binding
  ) {
    this.root = root;
    this.binding = binding;

    // just use the empty variants as we're not the root
    this.overrides = null;
    this.contextProvider = null;
    this.knownProxies = Collections.emptyList();
    this.requestedMemberInjections = Collections.emptySet();
  }

  public @NotNull Provider<?> resolveProvider(@NotNull BindingKey<?> key) {
    Provider<?> overridden = this.findOverriddenProvider(key);
    if (overridden != null) {
      return overridden;
    }

    InstalledBinding<?> binding = this.binding.injector().binding(key);
    return binding.provider();
  }

  public @NotNull InjectionContext copyAsRoot(
    @NotNull InstalledBinding<?> binding,
    @NotNull Map<BindingKey<?>, Provider<?>> overrides,
    @Nullable InjectionContextProvider contextProvider
  ) {
    InjectionContextProvider ctxProvider = contextProvider == null ? this.contextProvider : contextProvider;

    InjectionContext context;
    if (this.overrides == null || this.overrides.isEmpty()) {
      // if this context has no overrides just return a new context using the given overrides
      context = new InjectionContext(binding, overrides, ctxProvider);
    } else {
      // copy this injection context into a new root context, preserving the given overrides
      Map<BindingKey<?>, Provider<?>> overriddenProviders = new HashMap<>(overrides);
      overriddenProviders.putAll(this.overrides);
      context = new InjectionContext(binding, overriddenProviders, ctxProvider);
    }

    // check if the root context has an overridden value available if the associated element is known
    // delegate the newly created context directly to the overridden value
    Provider<?> overridden = this.findOverriddenProvider(binding.key());
    if (overridden != null) {
      context.state = STATE_DELEGATED;
      context.delegate = overridden.get();
    }

    return context;
  }

  public @NotNull InjectionContext enterSubcontext(@NotNull InstalledBinding<?> binding) {
    // check if the root context has an overridden value available if the associated element is known
    Provider<?> overridden = this.findOverriddenProvider(binding.key());
    if (overridden != null) {
      // create a sub context which just returns the given instance
      InjectionContext subcontext = new InjectionContext(this.root, binding);
      subcontext.state = STATE_DELEGATED;
      subcontext.delegate = overridden.get();
      return subcontext.init(this);
    }

    // find the known context leaf in the current tree
    InjectionContext knownLeaf = this.findCreatedLeaf(binding);
    if (knownLeaf != null) {
      // if the known leaf is delegated or proxied, we can just return the leaf as the value is immediately present,
      // and therefore we cannot run into any circular issues
      int leafState = knownLeaf.state;
      if (leafState == STATE_PROXIED || leafState == STATE_DELEGATED) {
        return knownLeaf;
      }

      // this is a circular call, check if we can proxy this leaf first
      Class<?> ourRawType = GenericTypeReflector.erase(this.binding.key().type());
      if (ourRawType.isInterface()) {
        // yes, this is proxyable
        if (this.createdProxy == null) {
          // check if there is a re-usable proxy
          ContextualProxy createdProxy = this.findReusableProxy(this.binding);
          if (createdProxy != null) {
            this.setProxy(createdProxy);
          } else {
            // construct the proxy & it's remove listener
            ProxyMapping proxyMapping = InjectionTimeProxy.makeProxy(ourRawType);
            Runnable proxyRemoveListener = new LeafWaitingConstructionRemoveTask(knownLeaf);

            // register the proxy
            ContextualProxy proxy = new ContextualProxy(proxyRemoveListener, proxyMapping, this.binding);
            this.setProxy(proxy);

            // add a note to the leaf context that it should resume the construction
            Queue<InjectionContext> waitingConstructions = knownLeaf.waitingConstructions;
            if (waitingConstructions == null) {
              knownLeaf.waitingConstructions = waitingConstructions = new LinkedList<>();
            }
            waitingConstructions.add(this);
          }
        }

        // indicate to the caller that we created a proxy
        throw SelfTypeProxiedException.INSTANCE;
      }

      // check if the known leaf node is proxyable
      Class<?> leafRawType = GenericTypeReflector.erase(knownLeaf.binding.key().type());
      if (leafRawType.isInterface()) {
        // create a marker context which holds the proxy for the leaf type
        InjectionContext subcontext = new InjectionContext(this.root, binding);
        subcontext.virtual = true;
        subcontext.state = STATE_PROXIED;
        subcontext.init(this);

        // check if there is a re-usable proxy
        ContextualProxy createdProxy = this.findReusableProxy(binding);
        if (createdProxy != null) {
          subcontext.setProxy(createdProxy);
        } else {
          // add a construction finish listener to the known leaf in order to set the proxy of the sub context
          BiConsumer<InjectionContext, Object> listener = new MarkerConstructionDoneListener(subcontext);
          knownLeaf.addConstructionListener(listener);

          // construct the proxy & it's remove listener
          ProxyMapping proxyMapping = InjectionTimeProxy.makeProxy(leafRawType);
          Runnable proxyRemoveListener = new LeafConstructionListenerRemoveTask(knownLeaf, listener);

          // insert the proxy into the subcontext
          ContextualProxy proxy = new ContextualProxy(proxyRemoveListener, proxyMapping, binding);
          subcontext.setProxy(proxy);
        }

        // return the created context
        return subcontext;
      }

      // this call is cyclic but neither this context nor the leaf that was invoked first can be proxied
      // in this case there is no way to recover, we just fail hard and try to provide a good error message
      StringBuilder treeInfoBuilder = new StringBuilder();

      InjectionContext ctx = this.root;
      do {
        // skip virtual nodes
        if (ctx.virtual) {
          continue;
        }

        // if the context is not the root we append a branch
        if (ctx != this.root) {
          treeInfoBuilder.append(" >--> ");
        }

        // [ <type> (<-- here) ]
        treeInfoBuilder.append("[ ").append(ctx.binding.key().type());
        if (ctx == knownLeaf) {
          treeInfoBuilder.append(" <-- here");
        }
        treeInfoBuilder.append(" ]");
      } while ((ctx = ctx.next) != null);

      // build the full error message and throw the exception
      throw new IllegalStateException(String.format(
        "Detected cyclic dependency while constructing %s. See traverse tree for more info: %s",
        this.root.binding.key().type(),
        treeInfoBuilder.append(" >--> [ ").append(this.binding.key().type()).append(" ]")));
    }

    // nothing special to do, just construct a brand-new sub context
    InjectionContext subcontext = new InjectionContext(this.root, binding);
    return subcontext.init(this);
  }

  public @Nullable Object resolveInstance() {
    int currentState = this.state;
    if (currentState == STATE_READY) {
      // no construction was attempted yet
      this.state = STATE_CONSTRUCTING;
      try {
        // construct the value from the underlying binding
        Provider<?> provider = this.binding.provider();
        Object constructedValue = provider.get();

        // check if the constructed value is a singleton in the current scope
        // in this case the result of this context should be delegated to the value
        if (constructedValue instanceof ScopedSingleton) {
          // mark this context as delegated
          this.state = STATE_DELEGATED;

          // unwrap the inner value
          ScopedSingleton knownValue = (ScopedSingleton) constructedValue;
          this.delegate = constructedValue = ScopedSingleton.unwrap(knownValue);

          // if the result is a known value there is a chance that multiple proxies were created
          // that we should all delegate to the same instance
          List<ContextualProxy> knownProxies = this.root.knownProxies;
          if (!knownProxies.isEmpty()) {
            for (ContextualProxy knownProxy : knownProxies) {
              InstalledBinding<?> proxyBinding = knownProxy.binding;
              if (proxyBinding == this.binding && !knownProxy.removeListenerExecuted) {
                // this provider and the proxy provider were called from the same context
                // this is the indication that the same delegate can be used for both proxies
                knownProxy.setDelegate(constructedValue);

                // remove the waiting construction which depends on the proxy
                knownProxy.executeRemoveListener();
              }
            }
          }
        }

        // finish the construction by calling all added stuff to this leaf
        this.callConstructDoneListeners(constructedValue);
        this.executeWaitingConstructions(constructedValue);

        // normal case, just return the value
        return constructedValue;
      } catch (SelfTypeProxiedException selfProxiedException) {
        // this type was proxied during the call
        return this.createdProxy.proxy();
      } finally {
        // reset the state if no other call changed the field
        if (this.state == STATE_CONSTRUCTING) {
          this.state = STATE_READY;
        }
      }
    }

    if (currentState == STATE_CONSTRUCTING) {
      // circular call, should not happen
      throw new IllegalStateException("Circular call to InjectionContext#resolveInstance()!");
    }

    if (currentState == STATE_PROXIED) {
      // this context was proxied
      return this.createdProxy.proxy();
    }

    if (currentState == STATE_DELEGATED) {
      // we have a delegate that we should return
      return this.delegate;
    }

    // unhandled state
    throw new IllegalStateException("Unable to handle context state: " + currentState);
  }

  public void addConstructionListener(@NotNull BiConsumer<InjectionContext, Object> listener) {
    Queue<BiConsumer<InjectionContext, Object>> constructionFinishListeners = this.constructionFinishListeners;
    if (constructionFinishListeners == null) {
      this.constructionFinishListeners = constructionFinishListeners = new LinkedList<>();
    }

    constructionFinishListeners.add(listener);
  }

  public void requestMemberInjection(@NotNull Class<?> type, @Nullable Object instance) {
    MemberInjectionRequest request = new MemberInjectionRequest(this.binding.injector(), type, instance);
    this.root.requestedMemberInjections.add(request);
  }

  public void finishConstruction() {
    // ensure that we are the root context
    Preconditions.checkArgument(this.root == this, "finishConstruction() call to non-root context");

    // mark this context as obsolete to indicate that the context should no longer
    // be used to resolve instances, however, the overrides should persist, and therefore
    // we don't want to remove the context from the current scope
    this.obsolete = true;

    // pre-validate all created proxies to ensure that they are all delegated
    this.validateAllProxiesAreDelegated();

    // execute member injections
    Set<MemberInjectionRequest> requestedMemberInjections = this.requestedMemberInjections;
    if (!requestedMemberInjections.isEmpty()) {
      // copy over the requested member injections are there might be requests coming in while executing injection
      // remove all requests that we're going to execute this time
      MemberInjectionRequest[] memberInjectionRequests = requestedMemberInjections.toArray(EMPTY_MEMBER_REQUEST_ARRAY);
      requestedMemberInjections.clear();

      // execute the member injections
      for (MemberInjectionRequest injectionRequest : memberInjectionRequests) {
        injectionRequest.executeMemberInjection();
      }

      // check if new member injection requests came in while executing member injection
      if (!requestedMemberInjections.isEmpty()) {
        this.finishConstruction();
      } else {
        // ensure that no proxies without a delegate were created during the last round of member injection
        this.validateAllProxiesAreDelegated();
      }
    }
  }

  private @NotNull InjectionContext init(@NotNull InjectionContext parent) {
    this.prev = parent;
    parent.next = this;
    return this;
  }

  private @Nullable InjectionContext findCreatedLeaf(@NotNull InstalledBinding<?> binding) {
    InjectionContext leaf = this.prev;
    if (leaf != null) {
      do {
        if (leaf.binding == binding) {
          return leaf;
        }
      } while ((leaf = leaf.prev) != null);
    }

    return null;
  }

  private @Nullable ContextualProxy findReusableProxy(@NotNull InstalledBinding<?> binding) {
    List<ContextualProxy> knownProxies = this.root.knownProxies;
    if (!knownProxies.isEmpty()) {
      for (ContextualProxy knownProxy : knownProxies) {
        // a proxy is re-usable if the same provider constructed the proxy + the delegate is present
        if (knownProxy.binding == binding && knownProxy.removeListenerExecuted) {
          return knownProxy;
        }
      }
    }
    return null;
  }

  /**
   * Sets the proxy of this context and marks this context as proxied. The proxy is registered to the root node as well
   * to allow for later delegation validation.
   *
   * @param proxy the proxy to set for this node.
   */
  private void setProxy(@NotNull ContextualProxy proxy) {
    this.state = STATE_PROXIED;
    this.createdProxy = proxy.proxyMapping;
    this.root.knownProxies.add(proxy);
  }

  /**
   * Calls all construction listeners which were added to this context, if any.
   *
   * @param constructedValue the value constructed by this context.
   */
  private void callConstructDoneListeners(@Nullable Object constructedValue) {
    Queue<BiConsumer<InjectionContext, Object>> constructionFinishListeners = this.constructionFinishListeners;
    if (constructionFinishListeners != null) {
      BiConsumer<InjectionContext, Object> listener;
      while ((listener = constructionFinishListeners.poll()) != null) {
        listener.accept(this, constructedValue);
      }
    }
  }

  /**
   * Resumes the construction of all injection contexts that depend on the construction of this context. For the time
   * while the construction process gets resumed, this context gets delegated to the constructed value.
   *
   * @param constructedValue the value constructed by this context.
   */
  private void executeWaitingConstructions(@Nullable Object constructedValue) {
    Queue<InjectionContext> waitingConstructions = this.waitingConstructions;
    if (waitingConstructions != null && !waitingConstructions.isEmpty()) {
      // store the old field values for later reset
      int oldState = this.state;
      Object oldDelegate = this.delegate;

      try {
        // temporary mark this leaf as delegated to allow constructions to access the constructed value
        this.state = STATE_DELEGATED;
        this.delegate = constructedValue;

        // call the waiting constructions
        InjectionContext waitingContext;
        while ((waitingContext = waitingConstructions.poll()) != null) {
          // reset the state of the waiting context to ready in order to force the instance resolve
          waitingContext.state = STATE_READY;

          // resolve the context instance
          Object contextCreatedValue = waitingContext.resolveInstance();

          // set the proxy delegate if needed
          ProxyMapping proxy = waitingContext.createdProxy;
          if (proxy != null && !proxy.isDelegatePresent()) {
            proxy.setDelegate(contextCreatedValue);
          }
        }
      } finally {
        // restore the old state
        this.state = oldState;
        this.delegate = oldDelegate;
      }
    }
  }

  private void validateAllProxiesAreDelegated() {
    // ensure that there are no proxies without a delegate
    List<ContextualProxy> knownProxies = this.knownProxies;
    if (!knownProxies.isEmpty()) {
      int proxiesWithoutDelegate = 0;
      for (ContextualProxy proxy : knownProxies) {
        if (!proxy.proxyMapping.isDelegatePresent()) {
          proxiesWithoutDelegate++;
        }
      }

      // fail hard if there are invalid proxies
      if (proxiesWithoutDelegate > 0) {
        throw new IllegalStateException(
          "Construction finish requested but there were " + proxiesWithoutDelegate + " proxies without a delegate");
      }
    }
  }

  /**
   * Tries to resolve an overridden provider for the given element.
   *
   * @param key the element to get the provider for.
   * @return the overridden provider, or null if no provider override is registered that matches the given element.
   */
  private @Nullable Provider<?> findOverriddenProvider(@NotNull BindingKey<?> key) {
    // overrides are provided to the root injector only
    return this.root.overrides.get(key);
  }

  public boolean obsolete() {
    return this.obsolete;
  }

  /**
   * A task that removes an injection listener from the given tree node.
   *
   * @author Pasqual K.
   * @since 2.0
   */
  private static final class LeafConstructionListenerRemoveTask implements Runnable {

    private final InjectionContext leaf;
    private final BiConsumer<InjectionContext, Object> listener;

    /**
     * Constructs a new remove task instance.
     *
     * @param leaf     the node to remove the listener from.
     * @param listener the listener to remove.
     */
    public LeafConstructionListenerRemoveTask(
      @NotNull InjectionContext leaf,
      @NotNull BiConsumer<InjectionContext, Object> listener
    ) {
      this.leaf = leaf;
      this.listener = listener;
    }

    /**
     * Removes the listener from the given node, if the listener of the node are still present.
     */
    @Override
    public void run() {
      Queue<BiConsumer<InjectionContext, Object>> constructionFinishListeners = this.leaf.constructionFinishListeners;
      if (constructionFinishListeners != null) {
        constructionFinishListeners.remove(this.listener);
      }
    }
  }

  /**
   * Removes the waiting construction of this context from the given context.
   *
   * @author Pasqual K.
   * @since 2.0
   */
  private final class LeafWaitingConstructionRemoveTask implements Runnable {

    private final InjectionContext leaf;

    /**
     * Constructs a new waiting construction remove task.
     *
     * @param leaf the node to remove the waiting construction from.
     */
    public LeafWaitingConstructionRemoveTask(@NotNull InjectionContext leaf) {
      this.leaf = leaf;
    }

    /**
     * Removes the waiting construction of this context from the given leaf.
     */
    @Override
    public void run() {
      Queue<InjectionContext> waitingConstructions = this.leaf.waitingConstructions;
      if (waitingConstructions != null) {
        waitingConstructions.remove(InjectionContext.this);
      }
    }
  }

  /**
   * A listener which is added to a virtual node which sets the delegate of the created proxy for the context and
   * removes the virtual context from the tree.
   *
   * @author Pasqual K.
   * @since 2.0
   */
  private final class MarkerConstructionDoneListener implements BiConsumer<InjectionContext, Object> {

    private final InjectionContext markerContext;

    /**
     * Constructs a new marker construction done listener.
     *
     * @param markerContext the virtual context that was inserted into the tree.
     */
    public MarkerConstructionDoneListener(@NotNull InjectionContext markerContext) {
      this.markerContext = markerContext;
    }

    /**
     * Sets the delegate of the proxy created in the marker context to the given constructed value and removes the
     * marker context from the tree afterwards.
     *
     * @param realContext      the context that constructed the value.
     * @param constructedValue the constructed value of the context.
     */
    @Override
    public void accept(@NotNull InjectionContext realContext, @Nullable Object constructedValue) {
      // set the delegate in the marker context we created
      ProxyMapping proxyMapping = this.markerContext.createdProxy;
      if (!proxyMapping.isDelegatePresent()) {
        proxyMapping.setDelegate(constructedValue);
      }

      // remove the marker context from the tree
      InjectionContext markerNext = this.markerContext.next;
      InjectionContext.this.next = markerNext;
      if (markerNext != null) {
        markerNext.prev = InjectionContext.this;
      }
    }
  }
}
