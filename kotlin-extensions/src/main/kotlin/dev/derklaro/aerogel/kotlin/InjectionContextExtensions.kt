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

import dev.derklaro.aerogel.InjectionContext
import dev.derklaro.aerogel.InjectionContext.Builder
import dev.derklaro.aerogel.Injector

/**
 * Tries to get or construct an instance of the given type [T], the instance may be null if null was bound.
 *
 * @see InjectionContext.findInstance
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> InjectionContext.findInstance(injector: Injector): T? = this.findInstance(element<T>(), injector)

/**
 * Used to indicate that the construction of [result] typed [T] was done successfully. The resulting instance may be
 * null in case null was bound to it.
 *
 * @see InjectionContext.storeValue
 * @author Pasqual K.
 * @since 2.0
 */
inline fun <reified T> InjectionContext.storeValue(result: Any?) {
  this.storeValue(element<T>(), result)
}

/**
 * Used to indicate that the construction of [result] typed [T] was done successfully. The resulting instance may be
 * null in case null was bound to it. [doMemberInjection] is used to check if members should be injected into the given
 * result object.
 *
 * @see InjectionContext.postConstruct
 * @author Pasqual K.
 * @since 2.0
 */
inline fun <reified T> InjectionContext.postConstruct(
  result: Any?,
  injector: Injector,
  doMemberInjection: Boolean = true
) {
  this.postConstruct(element<T>(), injector, result, doMemberInjection)
}

/**
 * Overrides the given generic element [T] with the provided [value].
 *
 * @see Builder.override
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> Builder.override(value: T?): Builder = this.override(element<T>(), value)
