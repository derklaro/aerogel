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

package aerogel.internal.reflect;

import aerogel.internal.utility.ExceptionalFunction;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReflectionUtils {

  private ReflectionUtils() {
    throw new UnsupportedOperationException();
  }

  public static void ensureInstantiable(@NotNull Type type) {
    // check if the type is a class
    if (!(type instanceof Class<?>)) {
      throw new UnsupportedOperationException(type.getTypeName() + " is not instantiable");
    }
    // cast to the type
    Class<?> clazz = (Class<?>) type;
    // check if the type is instantiable
    if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isPrimitive() || clazz.isArray()) {
      throw new UnsupportedOperationException(clazz + " is not instantiable");
    }
  }

  public static @NotNull Type genericSuperType(@NotNull Type type) {
    return ((ParameterizedType) type).getActualTypeArguments()[0];
  }

  public static @NotNull Class<?> genericSuperTypeAsClass(@NotNull Type type) {
    Type superType = genericSuperType(type);
    // @todo: this is just shit - unbox the type correctly
    return superType instanceof Class<?> ? (Class<?>) superType : null;
  }

  public static boolean isPrimitive(@NotNull Type type) {
    return type instanceof Class<?> && ((Class<?>) type).isPrimitive();
  }

  public static @NotNull <T extends Member, O> Collection<O> collectMembers(
    @NotNull Class<?> clazz,
    @NotNull Predicate<T> filter,
    @NotNull ExceptionalFunction<Class<?>, T[], ReflectiveOperationException> extractor,
    @NotNull ExceptionalFunction<T, O, IllegalAccessException> then
  ) {
    // result
    Collection<O> result = new ArrayList<>();
    // the class we are working on
    do {
      try {
        for (T t : extractor.apply(clazz)) {
          if (filter.test(t)) {
            result.add(then.apply(t));
          }
        }
      } catch (ReflectiveOperationException exception) {
        throw new RuntimeException("Exception extracting information from " + clazz, exception);
      }
    } while ((clazz = clazz.getSuperclass()) != Object.class);
    // and... done
    return result;
  }
}
