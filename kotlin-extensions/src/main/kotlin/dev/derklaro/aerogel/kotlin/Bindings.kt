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

package dev.derklaro.aerogel.kotlin

import dev.derklaro.aerogel.BindingConstructor
import dev.derklaro.aerogel.Bindings

/**
 * Creates a new constructing binding holder for the given generic type [T].
 *
 * @throws NullPointerException     if {@code element} is null.
 * @throws aerogel.AerogelException if the class more or less than one injectable constructors or is not instantiable.
 * @see Bindings.constructing
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> constructing(): BindingConstructor = Bindings.constructing(element<T>())

/**
 * Creates a new binding constructor for the generic type [T] which always return the given [value].
 *
 * @throws aerogel.AerogelException if {@code element} is not assignable to {@code value}.
 * @see Bindings.fixed
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> fixed(value: Any): BindingConstructor = Bindings.fixed(element<T>(), value)
