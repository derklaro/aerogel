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

package dev.derklaro.aerogel.type;

import dev.derklaro.aerogel.internal.util.Preconditions;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Objects;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public final class BindingKey<T> {

  private final int hash;

  private final Type type;
  private final AnnotationMatcher annotationMatcher;

  private BindingKey(@NotNull AnnotatedType annotatedType) {
    this.type = annotatedType.getType();

    Annotation[] typeAnnotations = annotatedType.getAnnotations();
    if (typeAnnotations.length == 0) {
      // no annotations, just use the static empty array
      this.annotationMatcher = null;
    } else {
      // convert each annotation to a matcher
      AnnotationMatcher matcher = null;
      for (Annotation annotation : typeAnnotations) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        if (AnnotationUtil.isRuntimeRetained(annotationType) && AnnotationUtil.isQualifierAnnotation(annotationType)) {
          // construct or combine the current annotation matcher
          AnnotationMatcher matcherForAnnotation = AnnotationMatcher.matchingStrategyFor(annotation);
          matcher = combineMatchers(matcher, matcherForAnnotation);
        }
      }

      this.annotationMatcher = matcher;
    }

    this.hash = Objects.hash(this.type, this.annotationMatcher);
  }

  private BindingKey(@NotNull Type type, @Nullable AnnotationMatcher annotationMatcher) {
    this.type = type;
    this.annotationMatcher = annotationMatcher;
    this.hash = Objects.hash(this.type, this.annotationMatcher);
  }

  @Contract(value = "_ -> new", pure = true)
  public static @NotNull <T> BindingKey<T> of(@NotNull AnnotatedType type) {
    AnnotatedType canonicalized = GenericTypeReflector.toCanonical(type);
    return new BindingKey<>(canonicalized);
  }

  @Contract(value = "_ -> new", pure = true)
  public static @NotNull <T> BindingKey<T> of(@NotNull Type type) {
    AnnotatedType annotated = GenericTypeReflector.annotate(type);
    return of(annotated);
  }

  @Contract(value = "_ -> new", pure = true)
  public static @NotNull <T> BindingKey<T> of(@NotNull Class<? extends T> type) {
    AnnotatedType annotated = GenericTypeReflector.annotate(type);
    return of(annotated);
  }

  @Contract(value = "_ -> new", pure = true)
  public static @NotNull <T> BindingKey<T> of(@NotNull TypeToken<T> typeToken) {
    return of(typeToken.getAnnotatedType());
  }

  @CheckReturnValue
  private static @NotNull AnnotationMatcher combineMatchers(
    @Nullable AnnotationMatcher left,
    @NotNull AnnotationMatcher right
  ) {
    if (left == null) {
      return right;
    } else {
      return left.and(right);
    }
  }

  @CheckReturnValue
  public @NotNull BindingKey<T> withoutAnnotations() {
    return new BindingKey<>(this.type, null);
  }

  @CheckReturnValue
  public @NotNull BindingKey<T> withAnnotation(@NotNull Annotation annotation) {
    Preconditions.checkArgument(
      AnnotationUtil.isRuntimeRetained(annotation.annotationType()),
      "Annotation %s is not retained at runtime", annotation.annotationType());
    Preconditions.checkArgument(
      AnnotationUtil.isQualifierAnnotation(annotation.annotationType()),
      "Annotation %s is not a qualifier annotation", annotation.annotationType());

    AnnotationMatcher matcher = AnnotationMatcher.forMatchingInstance(annotation);
    AnnotationMatcher newFullMatcher = combineMatchers(this.annotationMatcher, matcher);
    return new BindingKey<>(this.type, newFullMatcher);
  }

  @CheckReturnValue
  public @NotNull BindingKey<T> withAnnotation(@NotNull Class<? extends Annotation> annotationType) {
    Preconditions.checkArgument(
      AnnotationUtil.isRuntimeRetained(annotationType),
      "Annotation %s is not retained at runtime", annotationType);
    Preconditions.checkArgument(
      AnnotationUtil.isQualifierAnnotation(annotationType),
      "Annotation %s is not a qualifier annotation", annotationType);

    AnnotationMatcher matcher = AnnotationMatcher.forMatchingType(annotationType);
    AnnotationMatcher newFullMatcher = combineMatchers(this.annotationMatcher, matcher);
    return new BindingKey<>(this.type, newFullMatcher);
  }

  @CheckReturnValue
  public @NotNull <R> BindingKey<R> withType(@NotNull AnnotatedType type) {
    AnnotatedType canonicalized = GenericTypeReflector.toCanonical(type);
    return new BindingKey<>(canonicalized.getType(), this.annotationMatcher);
  }

  @CheckReturnValue
  public @NotNull <R> BindingKey<R> withType(@NotNull Type type) {
    AnnotatedType annotated = GenericTypeReflector.annotate(type);
    return this.withType(annotated);
  }

  @CheckReturnValue
  public @NotNull <R> BindingKey<R> withType(@NotNull Class<? extends R> type) {
    AnnotatedType annotated = GenericTypeReflector.annotate(type);
    return this.withType(annotated);
  }

  @CheckReturnValue
  public @NotNull <R> BindingKey<R> withType(@NotNull TypeToken<R> typeToken) {
    return this.withType(typeToken.getAnnotatedType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.hash;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof BindingKey<?>) {
      BindingKey<?> other = (BindingKey<?>) obj;
      return other.type.equals(this.type) && other.annotationMatcher.equals(this.annotationMatcher);
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String toString() {
    return "BindingKey[type=" + this.type + ", annotations=" + this.annotationMatcher + "]";
  }
}
