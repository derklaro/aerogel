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

package aerogel.internal.unsafe;

import java.lang.reflect.Method;
import org.jetbrains.annotations.NotNull;

final class UnsafeClassDefiner implements ClassDefiner {

  private static final Method DEFINE_ANONYMOUS_CLASS;

  static {
    Method defineAnonymousClass = null;

    if (UnsafeAccess.isAvailable()) {
      try {
        // find the define method
        Method defineAnonymousClassMethod = UnsafeAccess.UNSAFE_CLASS.getDeclaredMethod("defineAnonymousClass",
          Class.class,
          byte[].class,
          Object[].class);
        defineAnonymousClassMethod.setAccessible(true);
        // assign the method
        defineAnonymousClass = defineAnonymousClassMethod;
      } catch (Throwable ignored) {
      }
    }
    // assign the class fields
    DEFINE_ANONYMOUS_CLASS = defineAnonymousClass;
  }

  public static boolean isAvailable() {
    return DEFINE_ANONYMOUS_CLASS != null;
  }

  @Override
  public @NotNull Class<?> defineClass(@NotNull String name, @NotNull Class<?> parent, byte[] bytecode) {
    try {
      // Use unsafe to define the class
      return (Class<?>) DEFINE_ANONYMOUS_CLASS.invoke(UnsafeAccess.THE_UNSAFE_INSTANCE, parent, bytecode, null);
    } catch (Throwable throwable) {
      throw new RuntimeException("Unable to define class " + name, throwable);
    }
  }
}