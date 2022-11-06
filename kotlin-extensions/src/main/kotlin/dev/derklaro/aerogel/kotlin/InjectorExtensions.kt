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

import dev.derklaro.aerogel.AerogelException
import dev.derklaro.aerogel.BindingHolder
import dev.derklaro.aerogel.Injector
import dev.derklaro.aerogel.MemberInjector

/**
 * Creates or gets the instance of the specified generic type [T], the instance may be null if null was bound.
 *
 * @throws AerogelException if no binding is present and no runtime binding can be created.
 * @see Injector.instance
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> Injector.instance(): T? = this.instance(element<T>())

/**
 * Creates or gets the binding of the specified generic type [T].
 *
 * @throws AerogelException if no binding is present and no runtime binding can be created.
 * @see Injector.binding
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> Injector.binding(): BindingHolder = this.binding(element<T>())

/**
 * Get the stored binding of the generic type [T] in the current injector or null if not present.
 *
 * @see Injector.fastBinding
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> Injector.fastBinding(): BindingHolder? = this.fastBinding(element<T>())

/**
 * Retrieves the binding in the current injector and all parent injector of the generic type [T] or null if none of the
 * injectors can provide a binding.
 *
 * @see Injector.bindingOrNull
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> Injector.bindingOrNull(): BindingHolder? = this.bindingOrNull(element<T>())

/**
 * Get or creates a member injector for the generic type [T].
 *
 * @see Injector.memberInjector
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> Injector.memberInjector(): MemberInjector = this.memberInjector(T::class.java)

/**
 * Gets the member injector of the generic type [T] which is stored in the current injector or null if not present.
 *
 * @see Injector.fastMemberInjector
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> Injector.fastMemberInjector(): MemberInjector? = this.fastMemberInjector(T::class.java)
