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

package dev.derklaro.aerogel.auto.internal.util;

import java.io.DataInput;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AutoDecodingUtil {

  private AutoDecodingUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull Class<?>[] decodeTypes(
    @NotNull DataInput dataInput,
    @Nullable ClassLoader loader
  ) throws IOException {
    int typeCount = dataInput.readInt();
    Class<?>[] types = new Class<?>[typeCount];
    for (int i = 0; i < typeCount; i++) {
      types[i] = decodeType(dataInput, loader);
    }

    return types;
  }

  public static @NotNull Class<?> decodeType(
    @NotNull DataInput dataInput,
    @Nullable ClassLoader loader
  ) throws IOException {
    String className = dataInput.readUTF();
    switch (className) {
      // primitive types (cannot be resolved via Class.forName)
      case "int":
        return int.class;
      case "byte":
        return byte.class;
      case "char":
        return char.class;
      case "long":
        return long.class;
      case "short":
        return short.class;
      case "float":
        return float.class;
      case "double":
        return double.class;
      case "boolean":
        return boolean.class;
      // reference type or primitive array, resolve via Class.forName
      // as it was already encoded into the correct format when writing
      default:
        try {
          return Class.forName(className, false, loader);
        } catch (ClassNotFoundException exception) {
          throw new IllegalStateException("Unable to locate class " + className + " using loader " + loader);
        }
    }
  }
}
