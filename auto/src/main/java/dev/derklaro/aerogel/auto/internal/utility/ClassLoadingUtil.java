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

package dev.derklaro.aerogel.auto.internal.utility;

import dev.derklaro.aerogel.internal.utility.MapUtil;
import java.lang.reflect.Array;
import java.util.Map;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class for loading classes.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.auto.internal")
public final class ClassLoadingUtil {

  private static final Map<String, Class<?>> PRIMITIVE_CLASSES = MapUtil.staticMap(8, map -> {
    map.put("int", int.class);
    map.put("char", char.class);
    map.put("byte", byte.class);
    map.put("long", long.class);
    map.put("short", short.class);
    map.put("float", float.class);
    map.put("double", double.class);
    map.put("boolean", boolean.class);
  });

  private ClassLoadingUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Loads a class by its name using the current thread context class loader. This will not initialize the class.
   * Primitive classes and arrays are supported as well.
   *
   * @param loader the loader to load the class from.
   * @param name   the name of the class to load, may be a primitive type name.
   * @return a class object representing the requested class.
   * @throws ClassNotFoundException if the class can not be located using the thread context class loader.
   */
  public static @NotNull Class<?> loadClass(
    @NotNull ClassLoader loader,
    @NotNull String name
  ) throws ClassNotFoundException {
    // check if the name is a primitive class
    Class<?> primitive = PRIMITIVE_CLASSES.get(name);
    if (primitive != null) {
      return primitive;
    }

    // check if the given type is an array
    if (name.endsWith(TypeUtil.ARRAY_INDICATOR)) {
      // get the component type and create an array from it
      Class<?> componentType = loadClass(loader, name.substring(0, name.length() - TypeUtil.ARRAY_INDICATOR.length()));
      return Array.newInstance(componentType, 0).getClass();
    }

    // try to load the class normally
    return Class.forName(name, false, loader);
  }
}
