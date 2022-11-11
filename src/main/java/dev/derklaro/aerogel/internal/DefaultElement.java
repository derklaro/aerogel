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

package dev.derklaro.aerogel.internal;

import dev.derklaro.aerogel.AnnotationPredicate;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.internal.utility.ToStringHelper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of an element. See {@link Element#forType(Type)}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class DefaultElement implements Element {

  private final Type componentType;
  private final List<AnnotationPredicate<?>> annotationPredicates;

  private String requiredName;

  /**
   * Constructs a new default element type instance.
   *
   * @param componentType the type of the element.
   */
  public DefaultElement(@NotNull Type componentType) {
    this.componentType = componentType;
    this.annotationPredicates = new LinkedList<>();
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
  @UnmodifiableView
  public @NotNull Collection<AnnotationPredicate<?>> requiredAnnotations() {
    return Collections.unmodifiableCollection(this.annotationPredicates);
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
  public @NotNull Element requireAnnotation(@NotNull Annotation annotation) {
    Objects.requireNonNull(annotation, "annotation");
    this.annotationPredicates.add(AnnotationPredicateFactory.construct(annotation));
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element requireAnnotation(@NotNull Class<? extends Annotation> annotationType) {
    Objects.requireNonNull(annotationType, "type");
    this.annotationPredicates.add(AnnotationPredicateFactory.construct(annotationType));
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element requireAnnotation(@NotNull AnnotationPredicate<?> predicate) {
    Objects.requireNonNull(predicate, "predicate");
    this.annotationPredicates.add(predicate);
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasSpecialRequirements() {
    return this.requiredName != null || !this.annotationPredicates.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String toString() {
    return ToStringHelper.create(this)
      .putField("componentType", this.componentType)
      .putField("requiredName", this.requiredName)
      .putCollection("requiredAnnotations", this.annotationPredicates)
      .toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.requiredName, this.componentType, this.annotationPredicates);
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
    if (this.annotationPredicates.isEmpty() && that.annotationPredicates.isEmpty()) {
      return true;
    }
    // or not the same size
    if (this.annotationPredicates.size() != that.annotationPredicates.size()) {
      return false;
    }
    // check every comparer
    for (int i = 0; i < this.annotationPredicates.size(); i++) {
      if (!this.annotationPredicates.get(i).equals(that.annotationPredicates.get(i))) {
        return false;
      }
    }
    // every annotation comparer equals as well
    return true;
  }
}
