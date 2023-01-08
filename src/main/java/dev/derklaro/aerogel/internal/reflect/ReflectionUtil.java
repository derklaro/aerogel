/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility class to work with java reflections.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal.*")
public final class ReflectionUtil {

  private ReflectionUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the generic super type of the given {@link ParameterizedType}.
   *
   * @param type the type to get the generic super type of.
   * @return the generic super type of the {@link ParameterizedType} or null if the type is not a parameterized type.
   */
  public static @NotNull Type genericSuperType(@NotNull Type type) {
    return type instanceof ParameterizedType ? ((ParameterizedType) type).getActualTypeArguments()[0] : type;
  }

  /**
   * Checks if the member is package private (has no specific access modifier)
   *
   * @param member the member to check.
   * @return {@code true} if the member is package private, {@code false} otherwise.
   */
  public static boolean isPackagePrivate(@NotNull Member member) {
    int mod = member.getModifiers();
    return !Modifier.isPublic(mod) && !Modifier.isProtected(mod) && !Modifier.isPrivate(mod);
  }

  /**
   * Creates a short unique string for the member based on the member's visibility.
   *
   * @param member the member to create the summary for.
   * @return the short created visibility summary for the member.
   */
  public static @NotNull String shortVisibilitySummary(@NotNull Member member) {
    // check if the member is package private
    // in this case we need to make the visibility summary unique by suffixing with the package name as the method
    // cannot be overridden by any type outside that package
    if (isPackagePrivate(member)) {
      return "package-private" + member.getDeclaringClass().getPackage().getName();
    }
    // check if the method is private
    // in this case we need to make the visibility summary unique by suffixing with the declaring class name as the
    // method cannot be "overridden" by any type outside the declaring class
    if (Modifier.isPrivate(member.getModifiers())) {
      return "private" + member.getDeclaringClass().getName();
    }
    // the method is either public or protected - it can be overridden by any inheritance
    return "public";
  }

  /**
   * Check if the given field is uninitialized. A non-primitive field is uninitialized if it's value is null, a
   * primitive field is considered uninitialized if it's value is equal to the default's field value.
   *
   * @param field  the field to check.
   * @param holder the holder of the field if the field is not an instance field.
   * @return true if the field is uninitialized, false otherwise.
   * @throws IllegalAccessException if JLAC is enforced and the field is inaccessible.
   */
  public static boolean isUninitialized(@NotNull Field field, @Nullable Object holder) throws IllegalAccessException {
    // get the current field value
    Object currentValue = field.get(holder);
    // check if the field type is primitive - check for the default value then
    if (isPrimitive(field.getGenericType())) {
      return currentValue != null && currentValue.equals(Primitives.defaultValue(field.getType()));
    } else {
      // for non-primitive fields the value must just be null
      return currentValue == null;
    }
  }

  /**
   * Checks if the given {@code type} is primitive.
   *
   * @param type the type to check.
   * @return true if the field is primitive, false otherwise.
   */
  public static boolean isPrimitive(@NotNull Type type) {
    return type instanceof Class<?> && ((Class<?>) type).isPrimitive();
  }

  /**
   * Builds a class tree of the extended classes of the given {@code startingPoint}.
   *
   * @param startingPoint the starting point from which the tree should get created.
   * @return a full tree of all extended classes of starting from the given {@code startingPoint}.
   */
  public static @NotNull List<Class<?>> hierarchyTree(@NotNull Class<?> startingPoint) {
    List<Class<?>> result = new LinkedList<>();
    // add all super classes of the class
    Class<?> currentClass = startingPoint;
    do {
      // if this gets called for a type without super (f. ex. a primitive type) then the current class might be null
      if (currentClass == null) {
        break;
      }

      result.add(currentClass);
    } while ((currentClass = currentClass.getSuperclass()) != Object.class);
    // return the result
    return result;
  }
}
