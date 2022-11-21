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

package dev.derklaro.aerogel.internal.unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import org.apiguardian.api.API;
import sun.misc.Unsafe;

/**
 * Gives access to the {@code Unsafe} class of the jvm which allows access to previously inaccessible magic.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal.unsafe")
final class UnsafeAccess {

  /**
   * The {@code unsafe} class object if present.
   */
  static final Unsafe U;

  static {
    Unsafe unsafe = null;

    try {
      // get the unsafe class
      Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
      // get the unsafe instance
      unsafe = AccessController.doPrivileged((PrivilegedExceptionAction<Unsafe>) () -> {
        Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        return (Unsafe) theUnsafeField.get(null);
      });
    } catch (Exception ignored) {
    }

    // assign to the static final fields
    U = unsafe;
  }

  private UnsafeAccess() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get if the {@code Unsafe} class and instance were successfully loaded.
   *
   * @return if the {@code Unsafe} class and instance were successfully loaded.
   */
  static boolean isAvailable() {
    return U != null;
  }
}
