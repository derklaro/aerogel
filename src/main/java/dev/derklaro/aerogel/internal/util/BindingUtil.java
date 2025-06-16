/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2025 Pasqual K. and contributors
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

import dev.derklaro.aerogel.binding.key.BindingKey;
import jakarta.inject.Provider;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BindingUtil {

  private BindingUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks if the given binding key is a provider.
   *
   * @param key the key to check.
   * @return true if the given binding key represents a provider, false otherwise.
   * @throws NullPointerException if the given key is null.
   */
  public static boolean isProvider(@NotNull BindingKey<?> key) {
    Type type = key.type();
    if (type instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) type;
      return pt.getRawType() == Provider.class;
    }

    return false;
  }

  /**
   * Extracts the component type of the provider represented by the given key. Returns null if the given key is not a
   * provider (which can be used to check if the key is a provider, but {@link #isProvider(BindingKey)} should be
   * preferred as this invocation causes an array copy).
   *
   * @param key the key to possibly extract the provider component type of.
   * @return the component type if the key represents a provider, null otherwise.
   * @throws NullPointerException if the given key is null.
   */
  public static @Nullable Type extractProviderComponentType(@NotNull BindingKey<?> key) {
    Type type = key.type();
    if (type instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) type;
      if (pt.getRawType() == Provider.class) {
        return pt.getActualTypeArguments()[0];
      }
    }

    return null;
  }
}
