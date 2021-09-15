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

package aerogel.internal;

import aerogel.AnnotationComparer;
import java.lang.annotation.Annotation;
import org.jetbrains.annotations.NotNull;

/**
 * Internal class which allows the creation of annotation comparer for...
 *
 * <ul>
 *   <li>... annotation type matching.</li>
 *   <li>... annotation instance matching.</li>
 * </ul>
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class AnnotationComparerMaker {

  private AnnotationComparerMaker() {
    throw new UnsupportedOperationException();
  }

  /**
   * Makes a new annotation comparer. The returned comparer will only compare against the type of the given annotation
   * if the annotation has no fields, or it checks if the annotation provided and given do equal.
   *
   * @param forAnnotation the annotation to create an annotation comparer for.
   * @return the created annotation comparer for the given annotation.
   */
  public static @NotNull AnnotationComparer make(@NotNull Annotation forAnnotation) {
    // check if the annotation has no types - we can then fall back to use the type
    if (forAnnotation.annotationType().getDeclaredMethods().length == 0) {
      return new TypeBasedAnnotationComparer(forAnnotation.annotationType());
    } else {
      // use strict comparison
      return new InstanceBasedAnnotationComparer(forAnnotation);
    }
  }

  /**
   * Makes a new annotation comparer which checks if the type of the given and provided annotation are equal.
   *
   * @param forAnnotationType the type of annotation to check for.
   * @return the created annotation comparer for the given annotation type.
   */
  public static @NotNull AnnotationComparer make(@NotNull Class<?> forAnnotationType) {
    return new TypeBasedAnnotationComparer(forAnnotationType);
  }

  /**
   * An annotation comparer only checking if the types of annotation equal.
   *
   * @author Pasqual K.
   * @since 1.0
   */
  private static final class TypeBasedAnnotationComparer implements AnnotationComparer {

    private final Class<?> annotationType;

    /**
     * Creates a new {@link TypeBasedAnnotationComparer} instance.
     *
     * @param annotationType the type of annotation to check for.
     */
    public TypeBasedAnnotationComparer(Class<?> annotationType) {
      this.annotationType = annotationType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean doesEqual(@NotNull Annotation annotation) {
      return this.annotationType.equals(annotation.annotationType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof TypeBasedAnnotationComparer)) {
        return false;
      }
      // check if the annotation type is equal
      return this.annotationType.equals(((TypeBasedAnnotationComparer) o).annotationType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return this.annotationType.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return '@' + this.annotationType.getName();
    }
  }

  /**
   * An annotation checking of the instances of the given annotation equals the provided annotation.
   *
   * @author Pasqual K.
   * @since 1.0
   */
  private static final class InstanceBasedAnnotationComparer implements AnnotationComparer {

    private final Annotation annotation;

    /**
     * Creates a new {@link InstanceBasedAnnotationComparer} instance.
     *
     * @param annotation the annotation to create the annotation comparer for.
     */
    public InstanceBasedAnnotationComparer(Annotation annotation) {
      this.annotation = annotation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean doesEqual(@NotNull Annotation annotation) {
      return this.annotation.equals(annotation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof InstanceBasedAnnotationComparer)) {
        return false;
      }
      // check if the annotation type is equal
      return this.annotation.equals(((InstanceBasedAnnotationComparer) o).annotation);
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
    public String toString() {
      return this.annotation.toString();
    }
  }
}
