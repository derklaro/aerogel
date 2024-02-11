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

package dev.derklaro.aerogel.binding.key;

import dev.derklaro.aerogel.internal.annotation.AnnotationUtil;
import java.lang.annotation.Annotation;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A matcher for annotations.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
interface AnnotationMatcher {

  /**
   * Constructs a new annotation matcher that validates that the given annotation instance is the same as being
   * matched.
   *
   * @param annotation the annotation to match.
   * @return a new annotation matcher that matches the given annotation instance.
   */
  @Contract(value = "_ -> new", pure = true)
  static @NotNull AnnotationMatcher forMatchingInstance(@NotNull Annotation annotation) {
    return new AnnotationInstanceMatcher(annotation);
  }

  /**
   * Constructs a new annotation matcher that just validates the type of the given annotation.
   *
   * @param annotationType the annoation type to match.
   * @return a new annotation matcher that matches the given annotation type.
   */
  @Contract(value = "_ -> new", pure = true)
  static @NotNull AnnotationMatcher forMatchingType(@NotNull Class<? extends Annotation> annotationType) {
    return new AnnotationTypeMatcher(annotationType);
  }

  /**
   * Constructs an annotation matcher that best matches the given annotation. If the annotation has no properties, this
   * method just returns a matcher for the annotation type. If properties are preset on the annotation, the returned
   * matcher will respect those.
   *
   * @param annotation the annotation to get the best fitting strategy for.
   * @return an annotation matcher to properly match the given annotation.
   */
  @Contract(value = "_ -> new", pure = true)
  static @NotNull AnnotationMatcher matchingStrategyFor(@NotNull Annotation annotation) {
    Class<? extends Annotation> annotationType = annotation.annotationType();
    if (AnnotationUtil.hasProperties(annotationType)) {
      // annotation has properties, include them when matching
      return forMatchingInstance(annotation);
    } else {
      // annotation has no properties, just matching for the type is fine
      return forMatchingType(annotationType);
    }
  }

  /**
   * Validates that the given annotation matches the stategy of this matcher implementation.
   *
   * @param annotation the annoation to validate.
   * @return true if the given annotation matches, false otherwise.
   */
  boolean test(@NotNull Annotation annotation);

  /**
   * {@inheritDoc}
   */
  @Override
  int hashCode();

  /**
   * {@inheritDoc}
   */
  @Override
  boolean equals(@Nullable Object other);

  /**
   * {@inheritDoc}
   */
  @Override
  @NotNull
  String toString();

  /**
   * Annotation matcher implementation that just validates that the type of the annotation matches the given one.
   *
   * @author Pasqual Koschmieder
   * @since 3.0
   */
  @API(status = API.Status.INTERNAL, since = "3.0")
  final class AnnotationTypeMatcher implements AnnotationMatcher {

    private final Class<? extends Annotation> type;

    /**
     * Constructs a new annotation type matcher instance.
     *
     * @param type the type of annotation to match.
     */
    private AnnotationTypeMatcher(@NotNull Class<? extends Annotation> type) {
      this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(@NotNull Annotation annotation) {
      Class<? extends Annotation> annotationType = annotation.annotationType();
      return this.type.equals(annotationType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return this.type.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof AnnotationTypeMatcher) {
        AnnotationTypeMatcher other = (AnnotationTypeMatcher) obj;
        return other.type.equals(this.type);
      }

      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String toString() {
      return "@" + this.type.getName();
    }
  }

  /**
   * Annotation matcher implementation that validates that the given annotation instance matches the
   *
   * @author Pasqual Koschmieder
   * @since 3.0
   */
  @API(status = API.Status.INTERNAL, since = "3.0")
  final class AnnotationInstanceMatcher implements AnnotationMatcher {

    private final Annotation annotation;

    /**
     * Constructs a new annotation instance based matcher.
     *
     * @param annotation the annotation instance to match.
     */
    public AnnotationInstanceMatcher(@NotNull Annotation annotation) {
      this.annotation = annotation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(@NotNull Annotation annotation) {
      return this.annotation.equals(annotation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return this.annotation.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof AnnotationInstanceMatcher) {
        AnnotationInstanceMatcher other = (AnnotationInstanceMatcher) obj;
        return other.annotation.equals(this.annotation);
      }

      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String toString() {
      return this.annotation.toString();
    }
  }
}
