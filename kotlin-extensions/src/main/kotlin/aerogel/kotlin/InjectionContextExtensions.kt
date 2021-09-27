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

package aerogel.kotlin

import aerogel.InjectionContext
import aerogel.InjectionContext.Builder

/**
 * Tries to get or construct an instance of the given type [T], the instance may be null if null was bound.
 *
 * @see InjectionContext.findInstance
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> InjectionContext.findInstance(): T? = this.findInstance(element<T>())

/**
 * Used to indicate that the construction of [result] typed [T] was done successfully. The resulting instance may be
 * null in case null was bound to it. [injectMembers] is used to determine if all members in [result] should be injected.
 *
 * @see InjectionContext.constructDone
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> InjectionContext.constructDone(result: Any?, injectMembers: Boolean = true) {
  this.constructDone(element<T>(), result, injectMembers)
}

/**
 * Overrides the given generic element [T] with the provided [value].
 *
 * @see Builder.override
 * @author Pasqual K.
 * @since 1.0
 */
inline fun <reified T> Builder.override(value: T?): Builder = this.override(element<T>(), value)
