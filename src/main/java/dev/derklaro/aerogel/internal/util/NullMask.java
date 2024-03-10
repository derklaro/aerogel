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

package dev.derklaro.aerogel.internal.util;

import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility to mask and unmask null values.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class NullMask {

  /**
   * A static object indicating that a value is null.
   */
  private static final Object NULL = new Object();

  private NullMask() {
    throw new UnsupportedOperationException();
  }

  /**
   * Masks the given value with a null representing object if the given value is null. If the given value is present,
   * this method returns the given value.
   *
   * @param unmasked the value to mask if needed.
   * @return a masked value if the given value is null, the same value otherwise.
   */
  public static @NotNull Object mask(@Nullable Object unmasked) {
    return unmasked != null ? unmasked : NULL;
  }

  /**
   * Unmasks the given object, returning null if the given object is masked. If the given object is not masked the given
   * value is returned.
   *
   * @param masked the value to unmask if needed.
   * @return null if the given value is masked, the same value otherwise.
   */
  public static @Nullable Object unmask(@NotNull Object masked) {
    return masked == NULL ? null : masked;
  }

  /**
   * Checks if the given object was masked.
   *
   * @param candidate the candidate to check.
   * @return true if the given object is masked, false otherwise.
   */
  public static boolean masked(@Nullable Object candidate) {
    return candidate == NULL;
  }
}
