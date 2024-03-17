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

package dev.derklaro.aerogel.internal.annotation;

import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import java.lang.annotation.Annotation;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InjectAnnotationUtil {

  private InjectAnnotationUtil() {
    throw new UnsupportedOperationException();
  }

  public static boolean validQualifierAnnotation(@NotNull Class<? extends Annotation> annotationType) {
    RetentionPolicy retention = AnnotationUtil.extractRetention(annotationType);
    return retention == RetentionPolicy.RUNTIME && annotationType.isAnnotationPresent(Qualifier.class);
  }

  public static boolean validScopeAnnotation(@NotNull Class<? extends Annotation> annotationType) {
    boolean hasProperties = AnnotationUtil.hasProperties(annotationType);
    RetentionPolicy retention = AnnotationUtil.extractRetention(annotationType);
    return !hasProperties
      && retention == RetentionPolicy.RUNTIME
      && annotationType.isAnnotationPresent(Scope.class);
  }

  public static void checkValidQualifierAnnotation(@NotNull Class<? extends Annotation> annotationType) {
    boolean validQualifier = validQualifierAnnotation(annotationType);
    if (!validQualifier) {
      throw new IllegalStateException("@" + annotationType.getName() + " is not a valid qualifier annotation");
    }
  }

  public static void checkValidScopeAnnotation(@NotNull Class<? extends Annotation> annotationType) {
    boolean validScope = validScopeAnnotation(annotationType);
    if (!validScope) {
      throw new IllegalStateException("@" + annotationType.getName() + " is not a valid scope annotation");
    }
  }

  public static @Nullable Annotation findQualifierAnnotation(@NotNull Annotation[] annotations) {
    Annotation qualifierAnnotation = null;
    for (Annotation annotation : annotations) {
      Class<? extends Annotation> annotationType = annotation.annotationType();
      if (validQualifierAnnotation(annotationType)) {
        if (qualifierAnnotation != null) {
          throw new IllegalStateException("Detected duplicate qualifier annotation: " + Arrays.toString(annotations));
        } else {
          qualifierAnnotation = annotation;
        }
      }
    }

    return qualifierAnnotation;
  }

  public static @Nullable Class<? extends Annotation> findScopeAnnotation(@NotNull Annotation[] annotations) {
    Class<? extends Annotation> scopeAnnotation = null;
    for (Annotation annotation : annotations) {
      Class<? extends Annotation> annotationType = annotation.annotationType();
      if (validScopeAnnotation(annotationType)) {
        if (scopeAnnotation != null) {
          throw new IllegalStateException("Detected duplicate scope annotation: " + Arrays.toString(annotations));
        } else {
          scopeAnnotation = annotationType;
        }
      }
    }

    return scopeAnnotation;
  }
}
