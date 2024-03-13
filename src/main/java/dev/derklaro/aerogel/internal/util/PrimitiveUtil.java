/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2024 Pasqual K. and contributors
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

import org.jetbrains.annotations.NotNull;

public final class PrimitiveUtil {

  // valueOf for other primitive types are cached, double & float will always produce a new instance
  private static final Float FLOAT_DEFAULT = 0F;
  private static final Double DOUBLE_DEFAULT = 0D;

  private PrimitiveUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the default value of the given {@code type}.
   *
   * @param type the type to get.
   * @param <T>  the wildcard type of the class to get.
   * @return the default initialization value of the associated primitive type.
   */
  @SuppressWarnings("unchecked")
  public static <T> @NotNull T defaultValue(@NotNull Class<T> type) {
    if (type == boolean.class) {
      return (T) Boolean.FALSE;
    } else if (type == char.class) {
      return (T) Character.valueOf('\0');
    } else if (type == byte.class) {
      return (T) Byte.valueOf((byte) 0);
    } else if (type == short.class) {
      return (T) Short.valueOf((short) 0);
    } else if (type == int.class) {
      return (T) Integer.valueOf(0);
    } else if (type == long.class) {
      return (T) Long.valueOf(0L);
    } else if (type == float.class) {
      return (T) FLOAT_DEFAULT;
    } else if (type == double.class) {
      return (T) DOUBLE_DEFAULT;
    } else {
      // cannot reach
      throw new AssertionError();
    }
  }
}
