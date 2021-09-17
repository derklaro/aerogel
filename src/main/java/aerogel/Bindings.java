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

package aerogel;

import static aerogel.internal.utility.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import aerogel.internal.binding.ConstructingBindingHolder;
import aerogel.internal.binding.FactoryBindingHolder;
import aerogel.internal.binding.ImmediateBindingHolder;
import aerogel.internal.jakarta.JakartaBridge;
import aerogel.internal.reflect.Primitives;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Gives access to constructors for some binding types which are pre-defined.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public interface Bindings {

  @Contract(pure = true)
  static @NotNull BindingConstructor fixed(@NotNull Element element, @NotNull Object value) {
    return fixed(element, element, value);
  }

  @Contract(pure = true)
  static @NotNull BindingConstructor fixed(@NotNull Element type, @NotNull Element bound, @NotNull Object value) {
    requireNonNull(type, "Fixed bound type must not be null");
    requireNonNull(bound, "Fixed bound value type must not be null");

    // @todo: check if the given value is assignable to the both types?
    checkArgument(Primitives.isNotPrimitiveOrIsAssignable(type.componentType(), value),
      "Primitive binding must be non-null");

    return injector -> new ImmediateBindingHolder(type, bound, injector, value);
  }

  @Contract(pure = true)
  static @NotNull BindingConstructor constructing(@NotNull Element element) {
    requireNonNull(element, "Bound type must be non-null");
    return injector -> ConstructingBindingHolder.create(injector, element);
  }

  @Contract(pure = true)
  static @NotNull BindingConstructor constructing(@NotNull Element type, @NotNull Element bound) {
    requireNonNull(type, "Fixed bound type must not be null");
    requireNonNull(bound, "Fixed bound value type must not be null");

    return injector -> ConstructingBindingHolder.create(injector, type, bound);
  }

  @Contract(pure = true)
  static @NotNull BindingConstructor factory(@NotNull Method factoryMethod) {
    return factory(Element.get(factoryMethod.getGenericReturnType()), factoryMethod);
  }

  @Contract(pure = true)
  static @NotNull BindingConstructor factory(@NotNull Element type, @NotNull Method factoryMethod) {
    requireNonNull(type, "Factory type must not be null");
    requireNonNull(factoryMethod, "Factory method must not be null");

    checkArgument(factoryMethod.getReturnType() != void.class, "Factory method must not return void");
    checkArgument(Modifier.isStatic(factoryMethod.getModifiers()), "Factory method need has to be static");

    return injector -> {
      // check if the return type is a singleton
      boolean singleton = JakartaBridge.isSingleton(factoryMethod.getReturnType());
      // create a new factory binding
      return new FactoryBindingHolder(
        type,
        Element.get(factoryMethod.getGenericReturnType()),
        injector,
        factoryMethod,
        singleton);
    };
  }
}
