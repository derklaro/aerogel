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

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.scope.KnownValue;
import dev.derklaro.aerogel.context.InjectionContext;
import dev.derklaro.aerogel.context.InjectionContextProvider;
import dev.derklaro.aerogel.context.LazyContextualProvider;
import dev.derklaro.aerogel.internal.proxy.InjectionTimeProxy;
import dev.derklaro.aerogel.internal.proxy.ProxyMapping;
import dev.derklaro.aerogel.internal.reflect.TypeUtil;
import dev.derklaro.aerogel.internal.util.Preconditions;
import dev.derklaro.aerogel.member.MemberInjectionType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of an injection context.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.context")
public final class DefaultInjectionContext implements InjectionContext {

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
  private final DefaultInjectionContext root;
  /**
   * The element which this context is constructing.
   */
  private final Type constructingType;
  /**
   * The binding instance that requested the construction of the element.
   */
  private final ContextualProvider<?> callingBinding;

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
  private final LazyContextualProvider[] overrides;
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
  private DefaultInjectionContext prev;
  /**
   * The context which was created based on this context, null if this is the tail context.
   */
  private DefaultInjectionContext next;

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
  private Queue<DefaultInjectionContext> waitingConstructions;
  /**
   * The added listeners for construction finish.
   */
  private Queue<BiConsumer<InjectionContext, Object>> constructionFinishListeners;

  /**
   * Constructs a new root injection context.
   *
   * @param callingBinding   the binding that requested this context.
   * @param constructingType the type constructed by this context.
   * @param overrides        the overridden instances whose delegates are present immediately.
   * @param contextProvider  the context provider that is tracking the context.
   */
  public DefaultInjectionContext(
    @NotNull ContextualProvider<?> callingBinding,
    @NotNull Type constructingType,
    @NotNull List<LazyContextualProvider> overrides,
    @Nullable InjectionContextProvider contextProvider
  ) {
    this(
      callingBinding,
      constructingType,
      overrides.toArray(DefaultInjectionContextBuilder.EMPTY_OVERRIDES),
      contextProvider);
  }

  /**
   * Constructs a new root injection context.
   *
   * @param callingBinding   the binding that requested this context.
   * @param constructingType the type constructed by this context.
   * @param overrides        the overridden instances whose delegates are present immediately.
   * @param contextProvider  the context provider that is tracking the context.
   */
  public DefaultInjectionContext(
    @NotNull ContextualProvider<?> callingBinding,
    @NotNull Type constructingType,
    @NotNull LazyContextualProvider[] overrides,
    @Nullable InjectionContextProvider contextProvider
  ) {
    this.root = this;
    this.callingBinding = callingBinding;
    this.constructingType = constructingType;
    this.contextProvider = contextProvider;

    // initialize to real fields as this is the root context
    this.knownProxies = new ArrayList<>();
    this.requestedMemberInjections = new LinkedHashSet<>();
    this.overrides = overrides;
  }

  /**
   * Constructs a new sub injection context of the given root context.
   *
   * @param root             the root context.
   * @param callingBinding   the binding that requested this context.
   * @param constructingType the type constructed by this context.
   */
  private DefaultInjectionContext(
    @NotNull DefaultInjectionContext root,
    @NotNull ContextualProvider<?> callingBinding,
    @NotNull Type constructingType
  ) {
    this.root = root;
    this.callingBinding = callingBinding;
    this.constructingType = constructingType;
    this.contextProvider = null;

    // just use the empty variants as we're not the root
    this.knownProxies = Collections.emptyList();
    this.requestedMemberInjections = Collections.emptySet();
    this.overrides = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Type constructingType() {
    return this.constructingType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull ContextualProvider<?> callingProvider() {
    return this.callingBinding;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull ContextualProvider<?> resolveProvider(@NotNull Element element) {
    // get the current injector which is associated with the binding that requested this context
    Injector currentInjector = this.callingBinding.injector();

    // check if the element is overridden
    LazyContextualProvider provider = this.findOverriddenProvider(element);
    if (provider != null) {
      return provider.withInjector(currentInjector);
    }

    // not overridden - resolve from the injector
    return currentInjector.binding(element).provider(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InjectionContext next() {
    return this.next;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InjectionContext prev() {
    return this.prev;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext root() {
    return this.root;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext copyAsRoot(
    @NotNull ContextualProvider<?> callingBinding,
    @NotNull Type constructingType,
    @NotNull List<LazyContextualProvider> overrides,
    @Nullable Element associatedElement,
    @Nullable InjectionContextProvider contextProvider
  ) {
    InjectionContextProvider ctxProvider = contextProvider == null ? this.contextProvider : contextProvider;
    LazyContextualProvider[] givenProviders = overrides.toArray(DefaultInjectionContextBuilder.EMPTY_OVERRIDES);

    DefaultInjectionContext context;
    if (this.overrides == null || this.overrides.length == 0) {
      // if this context has no overrides just return a new context using the given overrides
      context = new DefaultInjectionContext(callingBinding, constructingType, givenProviders, ctxProvider);
    } else {
      // copy this injection context into a new root context, preserving the given overrides
      LazyContextualProvider[] ap = Arrays.copyOf(this.overrides, this.overrides.length + givenProviders.length);
      System.arraycopy(givenProviders, 0, ap, this.overrides.length, givenProviders.length);
      context = new DefaultInjectionContext(callingBinding, constructingType, ap, ctxProvider);
    }

    // check if the root context has an overridden value available if the associated element is known
    // delegate the newly created context directly to the overridden value
    if (associatedElement != null) {
      LazyContextualProvider overriddenProvider = this.findOverriddenProvider(associatedElement);
      if (overriddenProvider != null) {
        context.state = STATE_DELEGATED;
        context.delegate = overriddenProvider.get();
      }
    }

    return context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull DefaultInjectionContext enterSubcontext(
    @NotNull Type constructingType,
    @NotNull ContextualProvider<?> callingBinding,
    @Nullable Element associatedElement
  ) {
    // check if the root context has an overridden value available if the associated element is known
    if (associatedElement != null) {
      LazyContextualProvider overriddenProvider = this.findOverriddenProvider(associatedElement);
      if (overriddenProvider != null) {
        // create a sub context which just returns the given instance
        DefaultInjectionContext subcontext = new DefaultInjectionContext(this.root, callingBinding, constructingType);
        subcontext.state = STATE_DELEGATED;
        subcontext.delegate = overriddenProvider.get();
        // mark the context as a child of this context
        return subcontext.init(this);
      }
    }

    // find the known context leaf in the current tree
    DefaultInjectionContext knownLeaf = this.findCreatedLeaf(callingBinding);
    if (knownLeaf != null) {
      // if the known leaf is delegated or proxied, we can just return the leaf as the value is immediately present,
      // and therefore we cannot run into any circular issues
      int leafState = knownLeaf.state;
      if (leafState == STATE_PROXIED || leafState == STATE_DELEGATED) {
        return knownLeaf;
      }

      // this is a circular call, check if we can proxy this leaf first
      Class<?> ourRawType = TypeUtil.rawType(this.constructingType);
      if (ourRawType.isInterface()) {
        // yes, this is proxyable
        if (this.createdProxy == null) {
          // check if there is a re-usable proxy
          ContextualProxy createdProxy = this.findReusableProxy(this.callingBinding);
          if (createdProxy != null) {
            this.setProxy(createdProxy);
          } else {
            // construct the proxy & it's remove listener
            ProxyMapping proxyMapping = InjectionTimeProxy.makeProxy(ourRawType);
            Runnable proxyRemoveListener = new LeafWaitingConstructionRemoveTask(knownLeaf);

            // register the proxy
            ContextualProxy proxy = new ContextualProxy(proxyRemoveListener, proxyMapping, this.callingBinding);
            this.setProxy(proxy);

            // add a note to the leaf context that it should resume the construction
            Queue<DefaultInjectionContext> waitingConstructions = knownLeaf.waitingConstructions;
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
      Class<?> leafRawType = TypeUtil.rawType(knownLeaf.constructingType);
      if (leafRawType.isInterface()) {
        // create a marker context which holds the proxy for the leaf type
        DefaultInjectionContext subcontext = new DefaultInjectionContext(this.root, callingBinding, constructingType);
        subcontext.virtual = true;
        subcontext.state = STATE_PROXIED;
        // mark the context as a child of this context
        subcontext.init(this);

        // check if there is a re-usable proxy
        ContextualProxy createdProxy = this.findReusableProxy(callingBinding);
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
          ContextualProxy proxy = new ContextualProxy(proxyRemoveListener, proxyMapping, callingBinding);
          subcontext.setProxy(proxy);
        }

        // return the created context
        return subcontext;
      }

      // this call is cyclic but neither this context nor the leaf that was invoked first can be proxied
      // in this case there is no way to recover, we just fail hard and try to provide a good error message
      StringBuilder treeInfoBuilder = new StringBuilder();

      DefaultInjectionContext ctx = this.root;
      do {
        // if the context is not the root we append a branch
        if (ctx != this.root) {
          treeInfoBuilder.append(" >--> ");
        }

        // [ <type> (<-- here) ]
        treeInfoBuilder.append("[ ").append(TypeUtil.toPrettyString(ctx.constructingType));
        if (ctx == knownLeaf) {
          treeInfoBuilder.append(" <-- here");
        }
        treeInfoBuilder.append(" ]");
      } while ((ctx = ctx.next) != null);

      // build the full error message and throw the exception
      throw AerogelException.forMessage(String.format(
        "Detected cyclic dependency while constructing %s. See traverse tree for more info: %s",
        this.root.constructingType,
        treeInfoBuilder.append(" >--> [ ").append(TypeUtil.toPrettyString(constructingType)).append(" ]")));
    }

    // nothing special to do, just construct a brand-new sub context
    DefaultInjectionContext subcontext = new DefaultInjectionContext(this.root, callingBinding, constructingType);
    return subcontext.init(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object resolveInstance() {
    int currentState = this.state;
    if (currentState == STATE_READY) {
      // no construction was attempted yet
      this.state = STATE_CONSTRUCTING;
      try {
        // construct the value from the underlying binding
        Object constructedValue = this.callingBinding.get(this);

        // check if the constructed value requests the store of it
        if (constructedValue instanceof KnownValue) {
          // mark this context as delegated
          this.state = STATE_DELEGATED;

          // unwrap the inner value
          KnownValue knownValue = (KnownValue) constructedValue;
          this.delegate = constructedValue = KnownValue.unwrap(knownValue);

          // add an injection request for the value if it is the first occurrence
          if (knownValue.firstOccurrence() && constructedValue != null) {
            this.requestMemberInjection(constructedValue);
          }

          // if the result is a known value there is a chance that multiple proxies were created
          // that we should all delegate to the same instance
          List<ContextualProxy> knownProxies = this.root.knownProxies;
          if (!knownProxies.isEmpty()) {
            for (ContextualProxy knownProxy : knownProxies) {
              ContextualProvider<?> callingProvider = knownProxy.callingProvider;
              if (callingProvider == this.callingBinding && !knownProxy.removeListenerExecuted) {
                // this provider and the proxy provider were called from the same context
                // this is the indication that the same delegate can be used for both proxies
                knownProxy.setDelegate(constructedValue);

                // remove the waiting construction which depends on the proxy
                knownProxy.executeRemoveListener();
              }
            }
          }
        } else if (constructedValue != null) {
          // request member injection for the constructed value
          this.requestMemberInjection(constructedValue);
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
      throw AerogelException.forMessage("Circular call to InjectionContext#resolveInstance()!");
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
    throw AerogelException.forMessage("Unable to handle context state: " + currentState);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addConstructionListener(@NotNull BiConsumer<InjectionContext, Object> listener) {
    Queue<BiConsumer<InjectionContext, Object>> constructionFinishListeners = this.constructionFinishListeners;
    if (constructionFinishListeners == null) {
      this.constructionFinishListeners = constructionFinishListeners = new LinkedList<>();
    }
    constructionFinishListeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void requestMemberInjection(@Nullable Object value) {
    this.requestMemberInjection(value, MemberInjectionType.FLAG_ALL_MEMBERS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void requestMemberInjection(@Nullable Object value, long flag) {
    // get the type of the value, if the value is present we can directly use the given type
    // in all other cases we can fall back to the raw type of the constructing type
    Class<?> valueType;
    if (value != null) {
      valueType = value.getClass();
    } else {
      valueType = TypeUtil.rawType(this.constructingType);
    }

    // construct and store the injection request
    MemberInjectionRequest request = new MemberInjectionRequest(flag, valueType, value, this.callingBinding.injector());
    this.root.requestedMemberInjections.add(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
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

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean obsolete() {
    return this.obsolete;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean virtualContext() {
    return this.virtual;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean rootContext() {
    return this.prev == null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean leafContext() {
    return this.next == null;
  }

  /**
   * Inits this context by marking this context as a next context of the given parent and sets the previous context of
   * this context to the parent.
   *
   * @param parent the parent context to set as the previous context.
   * @return this context.
   */
  private @NotNull DefaultInjectionContext init(@NotNull DefaultInjectionContext parent) {
    this.prev = parent;
    parent.next = this;
    // return this context, for chaining
    return this;
  }

  /**
   * Finds the context in the current subtree which was created for the given provider. This method returns null if no
   * node was created for the given provider in the current subtree.
   *
   * @param leafProvider the provider to find the node of.
   * @return the node which is associated with the given provider, null if no node is known.
   */
  private @Nullable DefaultInjectionContext findCreatedLeaf(@NotNull ContextualProvider<?> leafProvider) {
    // check if a context for the given calling binding was already created
    DefaultInjectionContext leaf = this.prev;
    if (leaf != null) {
      do {
        if (leaf.callingBinding == leafProvider) {
          // found a matching context
          return leaf;
        }
      } while ((leaf = leaf.prev) != null);
    }
    // no such leaf in the tree
    return null;
  }

  /**
   * Finds a proxy instance which is re-usable for the given calling provider in the complete tree.
   *
   * @param callingProvider the provider for which the proxy is needed.
   * @return a proxy which can be re-used for the given provider.
   */
  private @Nullable ContextualProxy findReusableProxy(@NotNull ContextualProvider<?> callingProvider) {
    List<ContextualProxy> knownProxies = this.root.knownProxies;
    if (!knownProxies.isEmpty()) {
      for (ContextualProxy knownProxy : knownProxies) {
        // a proxy is re-usable if the same provider constructed the proxy + the delegate is present
        if (knownProxy.callingProvider == callingProvider && knownProxy.removeListenerExecuted) {
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
    Queue<DefaultInjectionContext> waitingConstructions = this.waitingConstructions;
    if (waitingConstructions != null && !waitingConstructions.isEmpty()) {
      // store the old field values for later reset
      int oldState = this.state;
      Object oldDelegate = this.delegate;

      try {
        // temporary mark this leaf as delegated to allow constructions to access the constructed value
        this.state = STATE_DELEGATED;
        this.delegate = constructedValue;

        // call the waiting constructions
        DefaultInjectionContext waitingContext;
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

  /**
   * Validates that all known proxies have a delegate present.
   *
   * @throws AerogelException if there are proxies without a delegate known in the tree.
   */
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
        throw AerogelException.forMessageWithoutStack(
          "Construction finish requested but there were " + proxiesWithoutDelegate + " proxies without a delegate");
      }
    }
  }

  /**
   * Tries to resolve an overridden provider for the given element.
   *
   * @param element the element to get the provider for.
   * @return the overridden provider, or null if no provider override is registered that matches the given element.
   */
  private @Nullable LazyContextualProvider findOverriddenProvider(@NotNull Element element) {
    // try to resolve a provider which constructs a value that matches one provider
    LazyContextualProvider[] overriddenProviders = this.root.overrides;
    if (overriddenProviders != null) {
      for (LazyContextualProvider overriddenProvider : overriddenProviders) {
        if (overriddenProvider.elementMatcher().test(element)) {
          return overriddenProvider;
        }
      }
    }

    // no matching provider found
    return null;
  }

  /**
   * A task that removes an injection listener from the given tree node.
   *
   * @author Pasqual K.
   * @since 2.0
   */
  private static final class LeafConstructionListenerRemoveTask implements Runnable {

    private final DefaultInjectionContext leaf;
    private final BiConsumer<InjectionContext, Object> listener;

    /**
     * Constructs a new remove task instance.
     *
     * @param leaf     the node to remove the listener from.
     * @param listener the listener to remove.
     */
    public LeafConstructionListenerRemoveTask(
      @NotNull DefaultInjectionContext leaf,
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

    private final DefaultInjectionContext leaf;

    /**
     * Constructs a new waiting construction remove task.
     *
     * @param leaf the node to remove the waiting construction from.
     */
    public LeafWaitingConstructionRemoveTask(@NotNull DefaultInjectionContext leaf) {
      this.leaf = leaf;
    }

    /**
     * Removes the waiting construction of this context from the given leaf.
     */
    @Override
    public void run() {
      Queue<DefaultInjectionContext> waitingConstructions = this.leaf.waitingConstructions;
      if (waitingConstructions != null) {
        waitingConstructions.remove(DefaultInjectionContext.this);
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

    private final DefaultInjectionContext markerContext;

    /**
     * Constructs a new marker construction done listener.
     *
     * @param markerContext the virtual context that was inserted into the tree.
     */
    public MarkerConstructionDoneListener(@NotNull DefaultInjectionContext markerContext) {
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
      DefaultInjectionContext markerNext = this.markerContext.next;
      DefaultInjectionContext.this.next = markerNext;
      if (markerNext != null) {
        markerNext.prev = DefaultInjectionContext.this;
      }
    }
  }
}
