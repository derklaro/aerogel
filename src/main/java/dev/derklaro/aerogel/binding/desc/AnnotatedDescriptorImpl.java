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

package dev.derklaro.aerogel.binding.desc;

import dev.derklaro.aerogel.internal.annotation.AnnotationDesc;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class AnnotatedDescriptorImpl implements AnnotatedDescriptor {

  private final Map<Class<? extends Annotation>, Annotation> annotations;

  protected AnnotatedDescriptorImpl(@NotNull AnnotatedElement annotatedElement) {
    Annotation[] declaredAnnotations = annotatedElement.getDeclaredAnnotations();
    if (declaredAnnotations.length == 0) {
      this.annotations = Collections.emptyMap();
    } else {
      Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();
      for (Annotation annotation : declaredAnnotations) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        AnnotationDesc annotationDesc = AnnotationDesc.of(annotationType);
        if (annotationDesc != null) {
          annotations.put(annotationType, annotation);
        }
      }

      this.annotations = Collections.unmodifiableMap(annotations);
    }
  }

  @Override
  public @NotNull Collection<Annotation> annotations() {
    return this.annotations.values();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Annotation> @Nullable T annotation(@NotNull Class<T> annotationType) {
    return (T) this.annotations.get(annotationType);
  }

  @Override
  public boolean annotationPresent(@NotNull Class<? extends Annotation> annotationType) {
    return this.annotations.containsKey(annotationType);
  }
}
