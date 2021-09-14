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
import aerogel.Element;
import aerogel.internal.utility.ToStringHelper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Don't use this class directly, use {@link Element#ofType(Type)} instead.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class DefaultElement implements Element {

  private final Type componentType;
  private final List<AnnotationComparer> annotationComparer;

  private String requiredName;

  /**
   * Constructs a new default element type instance.
   *
   * @param componentType the type of the element.
   */
  public DefaultElement(@NotNull Type componentType) {
    this.componentType = componentType;
    this.annotationComparer = new CopyOnWriteArrayList<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable String requiredName() {
    return this.requiredName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Type componentType() {
    return this.componentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<AnnotationComparer> annotationComparer() {
    return Collections.unmodifiableCollection(this.annotationComparer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element requireName(@Nullable String name) {
    this.requiredName = name;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element requireAnnotations(Annotation @NotNull ... annotations) {
    // create a new annotation comparer for every annotation
    for (Annotation annotation : annotations) {
      this.annotationComparer.add(AnnotationComparerMaker.make(annotation));
    }
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element requireAnnotations(Class<?> @NotNull ... annotationTypes) {
    // create a new annotation comparer for every annotation
    for (Class<?> annotationType : annotationTypes) {
      this.annotationComparer.add(AnnotationComparerMaker.make(annotationType));
    }
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String toString() {
    return ToStringHelper.from(this)
      .putField("requiredName", this.requiredName)
      .putField("componentType", this.componentType)
      .toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.requiredName, this.componentType, this.annotationComparer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@NotNull Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultElement)) {
      return false;
    }
    DefaultElement that = (DefaultElement) o;
    // compare the common fields - type & name
    if (!this.componentType.equals(that.componentType) || !Objects.equals(this.requiredName, that.requiredName)) {
      return false;
    }
    // compare the annotation strategies
    // save an iterator if they are either both empty
    if (this.annotationComparer.isEmpty() && that.annotationComparer.isEmpty()) {
      return true;
    }
    // or not the same size
    if (this.annotationComparer.size() != that.annotationComparer.size()) {
      return false;
    }
    // check every comparer
    for (int i = 0; i < this.annotationComparer.size(); i++) {
      if (!this.annotationComparer.get(i).equals(that.annotationComparer.get(i))) {
        return false;
      }
    }
    // every annotation comparer equals as well
    return true;
  }
}
