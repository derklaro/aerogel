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

import org.jetbrains.annotations.NotNull;

/**
 * Represents a not yet constructed binding which can be bound to any constructor either by invoking {@link
 * #construct(Injector)} directly using the {@link Injector} to which the binding should get bound or by using {@link
 * Injector#install(BindingConstructor)} or {@link Injector#install(Iterable)}. See {@link Bindings} for some
 * pre-defined ways to bind a value.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@FunctionalInterface
public interface BindingConstructor {

  /**
   * Constructs this binding and installs it to the injector. The construction can result in an {@link AerogelException}
   * but should not result in any other exception.
   *
   * @param injector the injector which is currently installing the binding.
   * @return the constructed binding holder based on the given {@code injector}.
   * @throws AerogelException if the construction failed.
   */
  @NotNull BindingHolder construct(@NotNull Injector injector) throws AerogelException;
}
