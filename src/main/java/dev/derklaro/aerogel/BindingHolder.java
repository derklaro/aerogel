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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a binding for a type. {@link #get()} and {@link #get(InjectionContext)} are responsible to execute the
 * construction of the type in any way. Any binding can be injected into member if the member type is a provider. This
 * is the reason why this class represents a provider as well.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public interface BindingHolder extends Provider<Object> {

  /**
   * Get the injector for which this binding was constructed.
   *
   * @return the injector for which this binding was constructed.
   */
  @NotNull Injector injector();

  /**
   * Get the types of the element which are injectable by this binding. The type can for example be an interface whilst
   * {@link #binding()} represents the implementation of it.
   *
   * @return the type of the element which is injectable by this binding.
   */
  @NotNull Element[] types();

  /**
   * Get the type of the element which gets injected when this binding is requested.
   *
   * @return the type of the element which gets injected when this binding is requested.
   */
  @NotNull Element binding();

  /**
   * Get the value of this binding based on the information provided by the given {@code context}.
   *
   * @param context the context in which the injection is about to happen.
   * @param <T>     the result type which this method should return. This should always be a subtype of {@link #types()}
   *                and {@link #binding()}.
   * @return the constructed value of this binding or null if the construction ended up with null.
   * @throws Throwable if any exception occurs during the construction of the underlying type.
   */
  @Nullable <T> T get(@NotNull InjectionContext context) throws Throwable;
}
