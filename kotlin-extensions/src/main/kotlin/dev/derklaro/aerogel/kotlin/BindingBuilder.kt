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

import dev.derklaro.aerogel.binding.BindingBuilder

/**
 * Binds the given type as one type that is handled by the final call to one of building methods. This methods will
 * not check for any scope annotations on the given type, use {@link #bindFully(Type)} for that instead.
 *
 * @see BindingBuilder.bind
 * @author Pasqual K.
 * @since 2.0
 */
inline fun <reified T> BindingBuilder.bind() = bind(T::class.java)

/**
 * Binds the given type as one type that is handled by the final call to one of building methods. This method will
 * apply all scopes present on the given type to this builder as well.
 *
 * <p>Note that while this method accepts a generic type, you can only pass one of:
 * <ol>
 *   <li>Classes
 *   <li>Generic Array Types (where the component type matches this list as well)
 *   <li>Parameterized Types (where the raw type matches this list as well)
 * </ol>
 *
 * @see BindingBuilder.bindFully
 * @author Pasqual K.
 * @since 2.0
 */
inline fun <reified T> BindingBuilder.bindFully() = bindFully(T::class.java)
