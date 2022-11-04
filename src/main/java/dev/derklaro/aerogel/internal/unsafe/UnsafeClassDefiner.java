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

import dev.derklaro.aerogel.AerogelException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

/**
 * A class defining method for legacy jvm implementations (Java 7 - 14) which is deprecated since Java 15 in honor of
 * the Lookup class defining method. This method uses the {@code defineAnonymousClass} class provided by the jvm
 * internal {@code Unsafe} class.
 *
 * @author Pasqual K.
 * @since 1.0
 */
final class UnsafeClassDefiner implements ClassDefiner {

  private static final MethodHandle DEFINE_HANDLE;

  static {
    MethodHandle defineHandle;

    try {
      // Unsafe.defineAnonymousClass(HostClass, ByteCode, CpPatches)
      defineHandle = MethodHandles.lookup().findVirtual(
        Unsafe.class,
        "defineAnonymousClass",
        MethodType.methodType(Class.class, Class.class, byte[].class, Object[].class));
    } catch (Exception exception) {
      // unable to retrieve
      defineHandle = null;
    }

    // assign the handle
    DEFINE_HANDLE = defineHandle;
  }

  /**
   * Checks if the {@code defineAnonymousClass} is available and this defining method can be used.
   *
   * @return if the {@code defineAnonymousClass} is available and this defining method can be used.
   */
  public static boolean isAvailable() {
    return UnsafeAccess.isAvailable() && DEFINE_HANDLE != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Class<?> defineClass(@NotNull String name, @NotNull Class<?> parent, byte[] bytecode) {
    try {
      // Use unsafe to define the class
      return (Class<?>) DEFINE_HANDLE.invokeExact(UnsafeAccess.U, parent, bytecode, null);
    } catch (Throwable throwable) {
      throw AerogelException.forMessagedException("Unable to define class " + name, throwable);
    }
  }
}
