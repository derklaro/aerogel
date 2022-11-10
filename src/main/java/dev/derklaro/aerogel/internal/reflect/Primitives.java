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

package dev.derklaro.aerogel.internal.reflect;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.internal.utility.Preconditions;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility class for working with java's primitive types.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class Primitives {

  /**
   * Holds all primitive to its wrapper class associations. For example {@code int} is associated with {@code Integer}.
   */
  private static final Map<String, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>(9);
  /**
   * Holds the default values for every primitive data type except for void as defined <a
   * href="https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html">here</a>.
   */
  private static final Map<String, Object> PRIMITIVE_DEFAULT_VALUES = new HashMap<>(8);

  static {
    // primitive type -> wrapper type
    PRIMITIVE_TO_WRAPPER.put(mask(void.class), Void.class);
    PRIMITIVE_TO_WRAPPER.put(mask(byte.class), Byte.class);
    PRIMITIVE_TO_WRAPPER.put(mask(long.class), Long.class);
    PRIMITIVE_TO_WRAPPER.put(mask(short.class), Short.class);
    PRIMITIVE_TO_WRAPPER.put(mask(int.class), Integer.class);
    PRIMITIVE_TO_WRAPPER.put(mask(float.class), Float.class);
    PRIMITIVE_TO_WRAPPER.put(mask(double.class), Double.class);
    PRIMITIVE_TO_WRAPPER.put(mask(char.class), Character.class);
    PRIMITIVE_TO_WRAPPER.put(mask(boolean.class), Boolean.class);
    // primitive type -> default value
    PRIMITIVE_DEFAULT_VALUES.put(mask(int.class), 0);
    PRIMITIVE_DEFAULT_VALUES.put(mask(char.class), '\0');
    PRIMITIVE_DEFAULT_VALUES.put(mask(boolean.class), false);
    PRIMITIVE_DEFAULT_VALUES.put(mask(byte.class), (byte) 0);
    PRIMITIVE_DEFAULT_VALUES.put(mask(long.class), (long) 0);
    PRIMITIVE_DEFAULT_VALUES.put(mask(short.class), (short) 0);
    PRIMITIVE_DEFAULT_VALUES.put(mask(float.class), (float) 0);
    PRIMITIVE_DEFAULT_VALUES.put(mask(double.class), (double) 0);
  }

  private Primitives() {
    throw new UnsupportedOperationException();
  }

  /**
   * Maks the given class by returning the name of it. This way of accessing the value from a map is faster as Class
   * does not override hashCode() which leads to a native call, while string does override the method.
   *
   * @param clazz the class to mask.
   * @return the masked value of the class.
   * @since 2.0
   */
  private static @NotNull String mask(@NotNull Class<?> clazz) {
    return clazz.getName();
  }

  /**
   * Checks if the given {@code type} is assignable to the {@code boxed} object.
   *
   * @param type  the type to check for.
   * @param boxed the boxed instance to check if compatible to the given {@code type}.
   * @return true if {@code boxed} is an instance of {@code type}, false otherwise.
   */
  public static boolean isNotPrimitiveOrIsAssignable(@NotNull Type type, @Nullable Object boxed) {
    // check if the type is a class and primitive
    if (type instanceof Class<?> && ((Class<?>) type).isPrimitive()) {
      return boxed != null && isOfBoxedType((Class<?>) type, boxed);
    }
    // not a primitive type - check if the boxed type is null (in this case there is no check possible)
    if (boxed == null) {
      return true;
    }
    // get the raw super type of the given type
    Class<?> rawType = ReflectionUtil.rawType(type);
    // check if the raw type equals to the given boxed type class
    return rawType.isAssignableFrom(boxed.getClass());
  }

  /**
   * Checks if the given {@code boxed} instance is a boxed type of the given {@code primitive} class.
   *
   * @param primitive the primitive class to check for.
   * @param boxed     the boxed instance to check.
   * @return true if the boxed instance is the boxed type of the primitive class.
   */
  public static boolean isOfBoxedType(@NotNull Class<?> primitive, @Nullable Object boxed) {
    // get the boxed type the given type should be assignable from
    Class<?> box = PRIMITIVE_TO_WRAPPER.get(mask(primitive));
    // either the box is null or the type assignable to it
    return box != null && boxed != null && box.isAssignableFrom(boxed.getClass());
  }

  /**
   * Gets the default value of the given {@code type}.
   *
   * @param type the type to get.
   * @param <T>  the wildcard type of the class to get.
   * @return the default initialization value of the associated primitive type.
   * @throws AerogelException if the given {@code type} is not primitive.
   */
  @SuppressWarnings("unchecked")
  public static <T> @NotNull T defaultValue(@NotNull Class<T> type) {
    Preconditions.checkArgument(type.isPrimitive(), "type " + type + " is not primitive");
    return (T) PRIMITIVE_DEFAULT_VALUES.get(mask(type));
  }
}
