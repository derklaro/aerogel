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

package dev.derklaro.aerogel.internal.annotation;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.internal.utility.MapUtil;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A collection of utility methods to work with annotations.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.annotation")
final class AnnotationUtil {

  private static final Map<Class<?>, Method[]> METHOD_CACHE = MapUtil.newConcurrentMap();
  private static final Map<Class<?>, Map<String, Object>> DEFAULT_VALUES_CACHE = MapUtil.newConcurrentMap();

  private AnnotationUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Resolves the methods of the given class, either from the cache or computing the value if not yet cached.
   *
   * @param annotationClass the annotation class to get the methods of.
   * @return the methods of the given class.
   * @throws NullPointerException if the given annotation class is null.
   */
  public static @NotNull Method[] resolveMethods(@NotNull Class<?> annotationClass) {
    return METHOD_CACHE.computeIfAbsent(annotationClass, Class::getDeclaredMethods);
  }

  /**
   * Get the default values of all methods in the annotation class, mapped in the form of: method name to default value.
   * If a method has no default value it is not included in the resulting map. This method returns the default either
   * from the cache or computes the values if not yet cached.
   *
   * @param annotationClass the annotation class to get the method defaults of.
   * @return the default values of all methods in the annotation class.
   * @throws NullPointerException if the given annotation class is null.
   */
  @Unmodifiable
  public static @NotNull Map<String, Object> resolveDefaults(@NotNull Class<? extends Annotation> annotationClass) {
    // get the known value or compute the value
    return DEFAULT_VALUES_CACHE.computeIfAbsent(annotationClass, AnnotationUtil::resolveDefaultsNoCache);
  }

  /**
   * Resolves the values of all annotation methods in the given annotation class for the given annotation instance.
   *
   * @param instance the annotation instance to get the method values of.
   * @return the method values of the given annotation, mapped: method name to method value.
   * @throws NullPointerException if the given annotation instance is null.
   */
  @Unmodifiable
  public static @NotNull Map<String, Object> resolveAnnotationValues(@NotNull Annotation instance) {
    // resolve the class values
    Method[] declaredMethods = resolveMethods(instance.annotationType());

    // resolve the values
    Map<String, Object> annotationValues = new LinkedHashMap<>(declaredMethods.length);
    for (Method method : declaredMethods) {
      Object val = resolveValue(method, instance);
      annotationValues.put(method.getName(), val);
    }

    return Collections.unmodifiableMap(annotationValues);
  }

  /**
   * Get the default values of all methods in the annotation class, mapped in the form of: method name to default value.
   * If a method has no default value it is not included in the resulting map. This method uses no cache.
   *
   * @param annotationClass the annotation class to get the default method values of.
   * @return the default values of all methods in the annotation class.
   * @throws NullPointerException if the given annotation class is null.
   */
  private static @NotNull Map<String, Object> resolveDefaultsNoCache(@NotNull Class<?> annotationClass) {
    // get the default values of each declared method, if possible
    Method[] declaredMethods = resolveMethods(annotationClass);
    if (declaredMethods.length == 0) {
      // no need to do anything
      return Collections.emptyMap();
    }

    if (declaredMethods.length == 1) {
      // only a single member
      Method method = declaredMethods[0];
      Object val = getDefault(method);
      return val == null ? Collections.emptyMap() : Collections.singletonMap(method.getName(), val);
    }

    // build the default values from the given methods
    Map<String, Object> values = new LinkedHashMap<>(declaredMethods.length);
    for (Method method : declaredMethods) {
      Object val = getDefault(method);
      if (val != null) {
        values.put(method.getName(), val);
      }
    }

    return Collections.unmodifiableMap(values);
  }

  /**
   * Get the default value of the given annotation method. This method returns null if the method returns a value of
   * type {@link Class} and no definition can be found for the default value or if the method is not defaulted.
   *
   * @param method the method to get the default value of.
   * @return the default value or null as defined above.
   * @throws NullPointerException if the given method is null.
   */
  static @Nullable Object getDefault(@NotNull Method method) {
    try {
      return method.getDefaultValue();
    } catch (TypeNotPresentException exception) {
      // should normally not happen, just in case
      return null;
    }
  }

  /**
   * Resolves the value of the given method for the given instance.
   *
   * @param method   the method to get the value of.
   * @param instance the instance to call the method on.
   * @return the value of the method.
   * @throws NullPointerException if the given method or instance is null.
   * @throws AerogelException     if an exception occurred while getting the method value.
   */
  static @NotNull Object resolveValue(@NotNull Method method, @NotNull Object instance) {
    try {
      // get the value
      return method.invoke(instance);
    } catch (Exception exception) {
      throw AerogelException.forMessagedException("Cannot resolve annotation values", exception);
    }
  }
}
