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

package aerogel.auto.internal.utility;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

public final class ClassLoadingUtils {

  private static final String ARRAY_SUFFIX = "[]";
  private static final Map<String, Class<?>> PRIMITIVE_CLASSES = new ConcurrentHashMap<>(8);

  static {
    PRIMITIVE_CLASSES.put("int", int.class);
    PRIMITIVE_CLASSES.put("char", char.class);
    PRIMITIVE_CLASSES.put("byte", byte.class);
    PRIMITIVE_CLASSES.put("long", long.class);
    PRIMITIVE_CLASSES.put("short", short.class);
    PRIMITIVE_CLASSES.put("float", float.class);
    PRIMITIVE_CLASSES.put("double", double.class);
    PRIMITIVE_CLASSES.put("boolean", boolean.class);
  }

  private ClassLoadingUtils() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull Class<?> loadClass(@NotNull String name) throws ClassNotFoundException {
    // check if the name is a primitive class
    Class<?> primitive = PRIMITIVE_CLASSES.get(name);
    if (primitive != null) {
      return primitive;
    }
    // check if the class is an array - in this case strip the last [] and get type of that
    if (name.endsWith(ARRAY_SUFFIX)) {
      Class<?> elementClass = loadClass(name.substring(0, name.length() - ARRAY_SUFFIX.length()));
      // create a new array of the element class to get the array type class
      return Array.newInstance(elementClass, 0).getClass();
    }
    // try to load the class normally
    return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
  }
}
