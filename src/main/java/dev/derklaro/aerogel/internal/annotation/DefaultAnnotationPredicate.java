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

package dev.derklaro.aerogel.internal.annotation;

import dev.derklaro.aerogel.AnnotationPredicate;
import dev.derklaro.aerogel.internal.utility.Preconditions;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of an annotation predicate.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.*")
public final class DefaultAnnotationPredicate implements AnnotationPredicate {

  private final Method[] annotationMethods;
  private final Map<String, Object> annotationValues;
  private final Class<? extends Annotation> annotationType;

  private String toStringCached; // lazy initialized

  /**
   * Constructs a new annotation predicate instance.
   *
   * @param annotationMethods the methods of the annotation type.
   * @param annotationValues  the resolved values of the annotation.
   * @param annotationType    the annotation type.
   */
  private DefaultAnnotationPredicate(
    @NotNull Method[] annotationMethods,
    @NotNull Map<String, Object> annotationValues,
    @NotNull Class<? extends Annotation> annotationType
  ) {
    this.annotationMethods = annotationMethods;
    this.annotationValues = annotationValues;
    this.annotationType = annotationType;
  }

  /**
   * Creates a new annotation predicate instance for the given annotation instance.
   *
   * @param instance the annotation instance.
   * @return a predicate for the given annotation instance.
   * @throws NullPointerException                  if the given annotation instance is null.
   * @throws dev.derklaro.aerogel.AerogelException if the given instance is not an annotation.
   */
  @Contract(pure = true)
  public static @NotNull AnnotationPredicate forAnnotation(@NotNull Object instance) {
    Preconditions.checkArgument(instance instanceof Annotation, "instance is not an annotation");
    Annotation annotation = (Annotation) instance;

    // get the annotation data
    Class<? extends Annotation> type = annotation.annotationType();
    Method[] declaredMethods = AnnotationUtil.resolveMethods(type);
    Map<String, Object> annotationValues = AnnotationUtil.resolveAnnotationValues(annotation);

    // build the predicate
    return new DefaultAnnotationPredicate(declaredMethods, annotationValues, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean test(@NotNull Object annotation) {
    // check for the type
    if (!this.annotationType.isInstance(annotation)) {
      return false;
    }

    // check if the annotation has any field values
    if (this.annotationValues.isEmpty()) {
      // can return true, the type matching check was already done and there is nothing more to check
      return true;
    }

    // check each value
    for (Method method : this.annotationMethods) {
      Object val = this.annotationValues.get(method.getName());
      Object valOfOther = AnnotationUtil.resolveValue(method, annotation);

      // we use deep equals here as both values might be an array, and we need to check each array element as well
      if (!Arrays.deepEquals(new Object[]{val}, new Object[]{valOfOther})) {
        return false;
      }
    }

    // everything matches
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Class<? extends Annotation> annotationType() {
    return this.annotationType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    DefaultAnnotationPredicate that = (DefaultAnnotationPredicate) o;
    return this.annotationValues.equals(that.annotationValues) && this.annotationType.equals(that.annotationType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.annotationType, this.annotationValues);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    // check if we already computed the toString value
    if (this.toStringCached != null) {
      return this.toStringCached;
    }

    // construct the toString value
    StringBuilder builder = new StringBuilder("@").append(this.annotationType.getSimpleName()).append('(');

    // only append the entries if there are any
    Set<Map.Entry<String, Object>> entries = this.annotationValues.entrySet();
    if (!entries.isEmpty()) {
      for (Map.Entry<String, Object> entry : entries) {
        builder.append(entry.getKey()).append(" = ").append(entry.getValue()).append(", ");
      }

      // remove the trailing comma
      int builderLength = builder.length();
      builder.delete(builderLength - 2, builderLength);
    }

    // close the opening bracket and set the cached value
    this.toStringCached = builder.append(')').toString();
    return this.toStringCached;
  }
}
