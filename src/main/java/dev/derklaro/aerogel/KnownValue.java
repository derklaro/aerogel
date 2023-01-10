/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
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

import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A wrapper around a value which should get stored in an injection context when returned by the calling provider. The
 * inner value of the wrapper will be immediately present to subsequent calls. The first occurrence boolean indicates if
 * the wrapped value was newly wrapped or was already wrapped. In case the value is new a member injection request will
 * be recorded unless the underlying value is null.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.STABLE, since = "2.0")
public final class KnownValue {

  private final Object inner;
  private final boolean firstOccurrence;

  /**
   * Constructs a known value with the given inner value.
   *
   * @param inner           the inner value to wrap.
   * @param firstOccurrence if the inner value was constructed for the first time.
   * @see #of(Object)
   */
  private KnownValue(@Nullable Object inner, boolean firstOccurrence) {
    this.inner = inner;
    this.firstOccurrence = firstOccurrence;
  }

  /**
   * Constructs a new value with the given inner value, marked as the first occurrence.
   *
   * @param inner the inner value to wrap.
   * @return a known value wrapping the inner value, marked as the first occurrence.
   * @see #asSecondOccurrence()
   */
  public static @NotNull KnownValue of(@Nullable Object inner) {
    return new KnownValue(inner, true);
  }

  /**
   * Unwraps the inner value stored in this value.
   *
   * @return the inner value of this value.
   */
  public @Nullable Object inner() {
    return this.inner;
  }

  /**
   * Get if the underlying value was constructed for the first time.
   *
   * @return true if the underlying value was constructed for the first time, false otherwise.
   */
  public boolean firstOccurrence() {
    return this.firstOccurrence;
  }

  /**
   * Constructs a new value which contains the same inner value as this one, but marked as the second occurrence.
   *
   * @return a new value with the same inner value, but marked as the second occurrence.
   */
  @Contract(pure = true)
  public @NotNull KnownValue asSecondOccurrence() {
    return new KnownValue(this.inner, false);
  }
}
