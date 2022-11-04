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

package dev.derklaro.aerogel;

import static dev.derklaro.aerogel.internal.reflect.Primitives.isNotPrimitiveOrIsAssignable;
import static dev.derklaro.aerogel.internal.utility.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import dev.derklaro.aerogel.internal.binding.ConstructingBindingHolder;
import dev.derklaro.aerogel.internal.binding.FactoryBindingHolder;
import dev.derklaro.aerogel.internal.binding.ImmediateBindingHolder;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import dev.derklaro.aerogel.internal.utility.ElementHelper;
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

  /**
   * A binding constructor which will always return the fixed {@code value} for the given {@code element}.
   *
   * @param element the element to bind the binding to.
   * @param value   the fixed value to return when the element is requested.
   * @return a new binding constructor which will always return the fixed {@code value} for {@code element}.
   * @throws NullPointerException if {@code element} is null.
   * @throws AerogelException     if {@code element} is not assignable to {@code value}.
   */
  @Contract(pure = true)
  static @NotNull BindingConstructor fixed(@NotNull Element element, @NotNull Object value) {
    return fixed(element, element, value);
  }

  /**
   * A binding constructor which will always return the fixed {@code value} for the given {@code bound}.
   *
   * @param type  the element to bind the binding to.
   * @param bound the bound type of the value.
   * @param value the fixed value to return when the element is requested.
   * @return a new binding constructor which will always return the fixed {@code value} for {@code bound}.
   * @throws NullPointerException if {@code type} or {@code bound} is null.
   * @throws AerogelException     if either {@code type} or {@code bound} is not assignable to {@code value}.
   */
  @Contract(pure = true)
  static @NotNull BindingConstructor fixed(@NotNull Element type, @NotNull Element bound, @NotNull Object value) {
    requireNonNull(type, "Fixed bound type must not be null");
    requireNonNull(bound, "Fixed bound value type must not be null");

    checkArgument(isNotPrimitiveOrIsAssignable(type.componentType(), value), "Value is not assignable to type");
    checkArgument(isNotPrimitiveOrIsAssignable(bound.componentType(), value), "Value is not assignable to bound");

    return injector -> new ImmediateBindingHolder(type, bound, injector, value);
  }

  /**
   * Creates a new constructing binding holder - the type will be constructed using either the no-args constructor or
   * the constructor which is annotated as {@literal @}{@code Inject}. The annotated constructor is preferred over the
   * no-args constructor construction. This binding is singleton aware. Parameters of the constructor are taken from the
   * injector.
   *
   * @param element the element of which the binding should be created.
   * @return a new binding constructor which instantiated the given {@code element} to obtain a new instance.
   * @throws NullPointerException if {@code element} is null.
   * @throws AerogelException     if the class more or less than one injectable constructors or is not instantiable.
   */
  @Contract(pure = true)
  static @NotNull BindingConstructor constructing(@NotNull Element element) {
    requireNonNull(element, "Bound type must be non-null");
    return injector -> ConstructingBindingHolder.create(injector, element);
  }

  /**
   * Creates a new constructing binding holder - the type will be constructed using either the no-args constructor or
   * the constructor which is annotated as {@literal @}{@code Inject}. The annotated constructor is preferred over the
   * no-args constructor construction. This binding is singleton aware. Parameters of the constructor are taken from the
   * injector.
   *
   * @param type  the element type.
   * @param bound the element of which the binding should be created.
   * @return a new binding constructor which instantiated the given {@code bound} to obtain a new instance.
   * @throws NullPointerException if {@code type} or {@code bound} is null.
   * @throws AerogelException     if the class more or less than one injectable constructors or is not instantiable.
   */
  @Contract(pure = true)
  static @NotNull BindingConstructor constructing(@NotNull Element type, @NotNull Element bound) {
    requireNonNull(type, "Fixed bound type must not be null");
    requireNonNull(bound, "Fixed bound value type must not be null");

    return injector -> ConstructingBindingHolder.create(injector, type, bound);
  }

  /**
   * Creates a new factory binding holder - the type will be constructed using the return type of this method. A factory
   * method is singleton aware and allowed to return null. Parameters of the method are taken from the injector.
   *
   * @param factoryMethod the method which should be used as the factory method.
   * @return a new binding constructor which calls the given method to create a new instance of the return type.
   * @throws NullPointerException if {@code factoryMethod} is null.
   * @throws AerogelException     if {@code factoryMethod} is not static or returns void.
   */
  @Contract(pure = true)
  static @NotNull BindingConstructor factory(@NotNull Method factoryMethod) {
    requireNonNull(factoryMethod, "Factory method must not be null");
    return factory(ElementHelper.buildElement(factoryMethod), factoryMethod);
  }

  /**
   * Creates a new factory binding holder - the type will be constructed using the return type of this method. A factory
   * method is singleton aware and allowed to return null. Parameters of the method are taken from the injector.
   *
   * @param type          the element type of the binding.
   * @param factoryMethod the method which should be used as the factory method.
   * @return a new binding constructor which calls the given method to create a new instance of the return type.
   * @throws NullPointerException if {@code factoryMethod} is null.
   * @throws AerogelException     if {@code factoryMethod} is not static, returns void or not the same type as {@code
   *                              type}.
   */
  @Contract(pure = true)
  static @NotNull BindingConstructor factory(@NotNull Element type, @NotNull Method factoryMethod) {
    requireNonNull(type, "Factory type must not be null");
    requireNonNull(factoryMethod, "Factory method must not be null");

    checkArgument(factoryMethod.getReturnType() != void.class, "Factory method must not return void");
    checkArgument(Modifier.isStatic(factoryMethod.getModifiers()), "Factory method need has to be static");
    checkArgument(factoryMethod.getGenericReturnType().equals(type.componentType()),
      "Factory method must return element type");

    return injector -> {
      // check if the return type is a singleton
      boolean singleton = JakartaBridge.isSingleton(factoryMethod.getReturnType());
      // create a new factory binding
      return new FactoryBindingHolder(
        type,
        Element.forType(factoryMethod.getGenericReturnType()),
        injector,
        factoryMethod,
        singleton);
    };
  }
}
