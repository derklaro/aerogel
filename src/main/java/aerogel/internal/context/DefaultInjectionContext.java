/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
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

package aerogel.internal.context;

import static aerogel.internal.utility.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import aerogel.Element;
import aerogel.InjectionContext;
import aerogel.Injector;
import aerogel.internal.codegen.InjectionTimeProxy;
import aerogel.internal.codegen.InjectionTimeProxy.InjectionTimeProxyable;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultInjectionContext implements InjectionContext {

  private final Injector injector;
  private final ElementStack elementStack;
  private final Map<Element, Object> knownTypes;

  public DefaultInjectionContext(@NotNull Injector injector, @NotNull Map<Element, Object> overriddenTypes) {
    this.injector = injector;
    this.elementStack = new ElementStack();
    this.knownTypes = new HashMap<>(overriddenTypes);
  }

  @Override
  public @NotNull Injector injector() {
    return this.injector;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T findInstance(@NotNull Element element) {
    // check if a type was already constructed during the invocation cycle
    if (this.knownTypes.containsKey(element)) {
      return (T) this.knownTypes.get(element);
    }
    // assign the first type we need to construct
    if (this.elementStack.has(element)) {
      // check for an element in the stack which is proxyable if we already travelled over this element
      Element proxyable = this.elementStack.filter(stackElement -> {
        // check if the component type is a class - only then we can check for a proxyable type
        if (stackElement.componentType() instanceof Class<?>) {
          // only interfaces can be proxied - every other proxy may be unsafe because of required constructor arguments
          return ((Class<?>) stackElement.componentType()).isInterface();
        }
        return false;
      });
      // if there is no proxyable type - break
      requireNonNull(proxyable, "No proxyable element in the stack - no circular dependency management possible");
      // check if a type is already known - no proxy needed
      if (!this.knownTypes.containsKey(proxyable)) {
        // push a proxy of the type to the stack
        this.knownTypes.put(proxyable, InjectionTimeProxy.makeProxy((Class<?>) proxyable.componentType()));
      }
    }
    // push the element we want to construct to the stack
    this.elementStack.push(element);
    // no cached instance yet - fall back to a binding of the injector
    return this.injector.binding(element).get(this);
  }

  @Override
  public void constructDone(@NotNull Element element, @Nullable Object result) {
    // read the current type from the map
    Object current = this.knownTypes.get(element);
    // check if there is a need to re-assign the instance
    if (current != null) {
      // if the current known element is proxied assign it to the delegate handler
      if (current instanceof InjectionTimeProxyable) {
        this.injectMembers(element, result); // inject before making the proxy available
        ((InjectionTimeProxyable) current).setDelegate(result);
      }
    } else {
      // store to the known types as there is no reference yet
      checkArgument(!(result instanceof InjectionTimeProxyable), "Unable to store a proxy handler instance");
      this.knownTypes.put(element, result);
      this.injectMembers(element, result);
    }
  }

  private void injectMembers(@NotNull Element element, @Nullable Object result) {
    // if we do have an instance we can do the member injection directly
    if (result != null) {
      this.injector.memberInjector(result.getClass()).inject(result);
    } else if (element.componentType() instanceof Class<?>) {
      // only if the component type is a class we can at least inject the static members
      this.injector.memberInjector((Class<?>) element.componentType()).inject();
    }
  }
}
