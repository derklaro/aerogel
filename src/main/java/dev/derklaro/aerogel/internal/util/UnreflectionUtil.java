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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.jetbrains.annotations.NotNull;

public final class UnreflectionUtil {

  private UnreflectionUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull MethodHandle unreflectMethod(
    @NotNull Method method,
    @NotNull MethodHandles.Lookup baseLookup
  ) {
    return tryUnreflect(method, baseLookup::unreflect);
  }

  public static @NotNull MethodHandle unreflectFieldSetter(
    @NotNull Field field,
    @NotNull MethodHandles.Lookup baseLookup
  ) {
    return tryUnreflect(field, baseLookup::unreflectSetter);
  }

  public static @NotNull MethodHandle unreflectConstructor(
    @NotNull Constructor<?> constructor,
    @NotNull MethodHandles.Lookup baseLookup
  ) {
    return tryUnreflect(constructor, baseLookup::unreflectConstructor);
  }

  private static @NotNull <A extends AccessibleObject> MethodHandle tryUnreflect(
    @NotNull A target,
    @NotNull Unreflector<A> unreflector
  ) {
    try {
      // try normal unreflection
      return unreflector.unreflect(target);
    } catch (Exception ignored) {
    }

    try {
      // try forcing our way in
      target.setAccessible(true);
      return unreflector.unreflect(target);
    } catch (Exception exception) {
      throw new IllegalStateException(
        "Unable to access " + target + ". Provide a Lookup that has access during binding building");
    } finally {
      target.setAccessible(false);
    }
  }

  @FunctionalInterface
  private interface Unreflector<A extends AccessibleObject> {

    @NotNull
    MethodHandle unreflect(@NotNull A target) throws Exception;
  }
}
