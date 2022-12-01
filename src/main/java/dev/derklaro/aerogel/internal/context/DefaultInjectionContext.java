/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 Pasqual K. and contributors
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
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.MemberInjectionSettings;
import dev.derklaro.aerogel.internal.codegen.InjectionTimeProxy;
import dev.derklaro.aerogel.internal.utility.NullMask;
import dev.derklaro.aerogel.internal.utility.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents a default implementation of an {@link InjectionContext}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal")
public final class DefaultInjectionContext implements InjectionContext {

  /**
   * A jvm static member injection setting which injects all members into a class.
   */
  private static final MemberInjectionSettings ALL_MEMBERS = MemberInjectionSettings.builder().build();

  /**
   * An element representing an injection context without any extra properties
   */
  private static final Element INJECTION_CONTEXT_ELEMENT = Element.forType(InjectionContext.class);

  private final Injector injector;
  private final ElementStack trackingStack;
  private final Set<Object> constructedValues;
  private final Map<Element, Object> createdProxies;
  private final Map<Element, Object> overriddenTypes;

  /**
   * The current element which gets constructed by this context.
   */
  private Element currentElement;

  /**
   * Constructs a new injection context.
   *
   * @param injector        the injector which is used for instance lookups.
   * @param overriddenTypes all types which were overridden and are already present.
   */
  public DefaultInjectionContext(@NotNull Injector injector, @NotNull Map<Element, Object> overriddenTypes) {
    this.injector = injector;
    this.trackingStack = new ElementStack();

    this.constructedValues = new HashSet<>();
    this.createdProxies = new HashMap<>();
    this.overriddenTypes = new HashMap<>(overriddenTypes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector injector() {
    return this.injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element currentElement() {
    return this.currentElement;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NotNull Map<Element, Object> overriddenTypes() {
    return Collections.unmodifiableMap(this.overriddenTypes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T findConstructedValue(@NotNull Element element) {
    Object val = this.createdProxies.get(element);
    // we do not return dynamic created proxy types as they should not be used for construction or general use
    return val == null || val instanceof InjectionTimeProxy.InjectionTimeProxied ? null : (T) val;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T findInstance(@NotNull Element element) {
    Objects.requireNonNull(element, "element");
    // check if the type was overridden when creating the context
    if (this.overriddenTypes.containsKey(element)) {
      Object overriddenElement = this.overriddenTypes.get(element);
      return (T) NullMask.unmask(overriddenElement);
    }
    // check if a type was already constructed during the invocation cycle
    if (this.createdProxies.containsKey(element)) {
      return (T) this.createdProxies.get(element);
    }
    // (1.3.0): return the current injection context if the context is requested without any special properties.
    if (INJECTION_CONTEXT_ELEMENT.equals(element)) {
      return (T) this;
    }
    // check if we already tried to construct the element (which is a clear sign for circular dependencies over object
    // construction - we need to try to tackle that)
    if (this.currentElement == null) {
      this.currentElement = element;
    } else if (this.trackingStack.has(element)) {
      // check for an element in the stack which is proxyable if we already travelled over this element
      Element proxyable = this.trackingStack.filter(stackElement -> {
        // check if the component type is a class - only then we can check for a proxyable type
        if (stackElement.componentType() instanceof Class<?>) {
          // only interfaces can be proxied - every other proxy may be unsafe because of required constructor arguments
          return ((Class<?>) stackElement.componentType()).isInterface();
        }
        return false;
      });
      // if there is no proxyable type - break
      if (proxyable == null) {
        throw AerogelException.forMessageWithoutStack(String.format(
          "Unable to construct element %s because there is no type on the path which can be proxied: %s",
          element,
          this.trackingStack.dumpWalkingStack(element)));
      }
      // check if a type is already known - no proxy needed
      if (!this.createdProxies.containsKey(proxyable)) {
        // push a proxy of the type to the stack
        this.createdProxies.put(proxyable, InjectionTimeProxy.makeProxy((Class<?>) proxyable.componentType()));
      }
    }
    // push the element we want to construct to the stack
    this.trackingStack.push(element);
    try {
      // no cached instance yet - fall back to a binding of the injector
      return this.injector.binding(element).get(this);
    } catch (Throwable throwable) {
      throw AerogelException.forMessagedException("Unable to construct " + element + ':', throwable);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean storeValue(@NotNull Element element, @Nullable Object result) {
    Objects.requireNonNull(element, "element");

    // read the current type from the map
    Object proxy = this.createdProxies.get(element);
    if (proxy != null) {
      // mark the proxy as available, in case it's needed during member injection
      ((InjectionTimeProxy.InjectionTimeProxied) proxy).setDelegate(result);
    } else {
      // do not store proxies as they should be stored after creation and never get injected or used by anyone else
      Preconditions.checkArgument(
        !(result instanceof InjectionTimeProxy.InjectionTimeProxied),
        "Unable to store a proxy handler instance");
    }

    // store the result if given, and check if we already encountered the value
    // if we did, there is no need to do member injection on the resulting object again
    if (result != null) {
      try {
        return this.constructedValues.add(result);
      } catch (AerogelException ignored) {
        // we can ignore this exception - it might be caused because the result class tries to call hashCode on a proxy
        // which has no delegate yet available (for example a record might do that). We can skip the check and just
        // assume that member injection was not done yet and should be done again.
        return true;
      }
    }

    // if the given result is null there is no need to do member injection
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void postConstruct(@NotNull Element element, @Nullable Object result, boolean doMemberInjection) {
    // do member injection if requested
    if (doMemberInjection) {
      this.injectMembers(element, result);
    }

    // remove the constructed element from the tracking stack
    this.trackingStack.take(element);

    // the stack is empty, as all injection steps were done successfully and there are no pending injection proxies
    if (this.trackingStack.empty() && !this.hasIncompleteProxy()) {
      this.trackingStack.dry();
      this.createdProxies.clear();
      this.constructedValues.clear();
      this.currentElement = null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void ensureComplete() {
    Preconditions.checkArgument(!this.hasIncompleteProxy(), "Proxy without delegate is still present");
  }

  /**
   * Gets if this stack has created an injection proxy which has no delegate available yet.
   *
   * @return true if there are incomplete proxies, false otherwise.
   * @since 2.0
   */
  private boolean hasIncompleteProxy() {
    for (Object value : this.createdProxies.values()) {
      InjectionTimeProxy.InjectionTimeProxied proxied = (InjectionTimeProxy.InjectionTimeProxied) value;
      if (!proxied.isDelegatePresent()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Inject all members into the given {@code result} if possible.
   *
   * @param element the element which holds the result.
   * @param result  the result instance into which the members should get injected, may be null.
   */
  private void injectMembers(@NotNull Element element, @Nullable Object result) {
    // if we do have an instance we can do the member injection directly
    if (result != null) {
      this.injector.memberInjector(result.getClass()).inject(result, ALL_MEMBERS, this);
    } else if (element.componentType() instanceof Class<?>) {
      // only if the component type is a class we can at least inject the static members
      this.injector.memberInjector((Class<?>) element.componentType()).inject(ALL_MEMBERS, this);
    }
  }
}
