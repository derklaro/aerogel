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
import dev.derklaro.aerogel.AnnotationPredicate;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A factory to generate instances of annotations.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.*")
public final class AnnotationFactory {

  private AnnotationFactory() {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates a new annotation proxy which overrides all methods of the given annotation type and returns either the
   * value from the given override map or the default value of the method (if present).
   *
   * @param annotationType   the annotation type to generate a proxy for.
   * @param overriddenValues the overridden values, must include at least an override for all non-defaulted methods.
   * @param <A>              the annotation type to generate a proxy for.
   * @return a proxy for the given annotation type.
   * @throws NullPointerException if the given annotation type or overridden values map is null.
   * @throws AerogelException     if a method with no default value has no override.
   */
  public static @NotNull <A extends Annotation> A generateAnnotation(
    @NotNull Class<A> annotationType,
    @NotNull Map<String, Object> overriddenValues
  ) {
    // copy the overridden values map to ensure that we have write
    // access and no further changes are made to the given map
    Map<String, Object> overriddenCopy = new HashMap<>(overriddenValues);

    // get the base data from the annotation class
    Method[] declaredMethods = AnnotationUtil.resolveMethods(annotationType);
    Map<String, Object> defaultValues = AnnotationUtil.resolveDefaults(annotationType);

    // ensure that all non-optional values are provided
    for (Method method : declaredMethods) {
      // ensure that either a default is present or the value is given in the override map
      String name = method.getName();
      if (!defaultValues.containsKey(name) && !overriddenCopy.containsKey(name)) {
        throw AerogelException.forMessage("Value for method " + name + " is missing when using @"
          + annotationType.getCanonicalName() + " as the value is not optional");
      }
    }

    // merge the overridden and defaults map
    defaultValues.forEach(overriddenCopy::putIfAbsent);
    return doGenerateAnnotation(annotationType, overriddenCopy);
  }

  /**
   * Generates a proxy instance for the given annotation type which returns the method values from the given override
   * map.
   *
   * @param annotationType   the type to generate the proxy for.
   * @param annotationValues the values to return for the annotation methods.
   * @param <A>              the annotation type to generate the proxy for.
   * @return a proxy for the given annotation type.
   * @throws NullPointerException if the given annotation type or overridden values map is null.
   */
  private static @NotNull <A extends Annotation> A doGenerateAnnotation(
    @NotNull Class<A> annotationType,
    @NotNull Map<String, Object> annotationValues
  ) {
    // generate the proxy
    Object createdProxy = Proxy.newProxyInstance(
      annotationType.getClassLoader(),
      new Class[]{annotationType},
      new AnnotationInvocationHandler<>(annotationType, annotationValues));

    // cast the proxy instance
    return annotationType.cast(createdProxy);
  }

  /**
   * An implementation of the {@code hashCode()} method as defined by {@link Annotation#hashCode()}.
   *
   * @param values the values of the annotation to include in the hash.
   * @return the hash of an annotation based on the given values.
   * @throws NullPointerException if the given annotation values map is null.
   */
  private static int hashCode(@NotNull Map<String, Object> values) {
    int result = 0;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      // we use deepHashCode as the given value might be an array - we want the hash of each entry
      int valHashCode = Arrays.deepHashCode(new Object[]{entry.getValue()});
      result += (127 * entry.getKey().hashCode()) ^ valHashCode;
    }
    return result;
  }

  /**
   * An implementation of the {@code toString()} method as defined by {@link Annotation#toString()}.
   *
   * @param type   the annotation type.
   * @param values the values of the annotation to include in the string.
   * @return the stringified value based on the annotation type and values.
   * @throws NullPointerException if the given annotation type or value map is null.
   */
  private static @NotNull String toString(@NotNull Class<?> type, @NotNull Map<String, Object> values) {
    // build the prefix of the toString value
    String prefix = String.format("@%s", type.getName());

    // append all values
    StringJoiner valueJoiner = new StringJoiner(", ", "(", ")");
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      // deepToString as some values might be arrays - we want to catch them as well
      String stringifiedValue = Arrays.deepToString(new Object[]{entry.getValue()});

      // remove the brackets added by deepToString and append to the builder
      String val = stringifiedValue.substring(1, stringifiedValue.length() - 1);
      valueJoiner.add(String.format("%s=%s", entry.getKey(), val));
    }

    // append the closing bracket and convert to string
    return prefix + valueJoiner;
  }

  /**
   * An invocation handler for annotations which overrides all methods of {@link Annotation} and gets all other
   * annotation specific methods from a map.
   *
   * @param <A> the annotation type.
   * @author Pasqual K.
   * @since 2.0
   */
  private static final class AnnotationInvocationHandler<A extends Annotation> implements InvocationHandler {

    private final Class<A> annotationType;
    private final Map<String, Object> annotationValues;

    private AnnotationPredicate predicate; // lazy initialized when needed

    /**
     * Constructs a new invocation handler for an annotation.
     *
     * @param annotationType   the type of the annotation.
     * @param annotationValues the values for all annotation methods.
     */
    public AnnotationInvocationHandler(
      @NotNull Class<A> annotationType,
      @NotNull Map<String, Object> annotationValues
    ) {
      this.annotationType = annotationType;
      this.annotationValues = annotationValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Object invoke(@NotNull Object proxy, @NotNull Method method, @NotNull Object[] args) {
      String invokedMethod = method.getName();
      switch (invokedMethod) {
        // base annotation methods
        case "annotationType":
          return this.annotationType;
        case "hashCode":
          return AnnotationFactory.hashCode(this.annotationValues);
        case "toString":
          return AnnotationFactory.toString(this.annotationType, this.annotationValues);
        case "equals":
          // construct the tester if not given
          AnnotationPredicate tester = this.predicate;
          if (tester == null) {
            this.predicate = tester = DefaultAnnotationPredicate.forAnnotation(proxy);
          }
          return tester.test(args[0]);
        // non inherited methods
        default:
          return this.annotationValues.get(invokedMethod);
      }
    }
  }
}
