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
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * Collection of utility methods to work with annotations.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
public final class AnnotationUtil {

  /**
   * Checks if the given annotation type has any properties (= any method declared). See JLS chapter 9.6.1 for method
   * declaration restrictions for annotations.
   *
   * @param annotationType the annotation type to check for properties.
   * @return true if the given annotation has properties, false otherwise.
   */
  public static boolean hasProperties(@NotNull Class<? extends Annotation> annotationType) {
    // no other checks needed, no other methods than property declarations are allowed
    // see JLS chapter 9: https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.1
    return annotationType.getDeclaredMethods().length > 0;
  }

  /**
   * Get the retention policy of the given annotation type. If the retention is not explicitly specified via @Retention
   * on the annotation type, the default CLASS retention is returned.
   *
   * @param annotationType the annotation type to get the retention policy of.
   * @return the retention policy of the given annotation.
   * @see Retention
   */
  public static @NotNull RetentionPolicy extractRetention(@NotNull Class<? extends Annotation> annotationType) {
    Retention retention = annotationType.getDeclaredAnnotation(Retention.class);
    return retention != null ? retention.value() : RetentionPolicy.CLASS;
  }

  /**
   * Checks if the given annotation is a qualifier annotation. This is done by checking if the jakarta.Qualifier
   * annotation is present on the annotation type.
   *
   * @param annotationType the annotation type to check for qualifier status.
   * @return true if the given annotation is a qualifier, false otherwise.
   * @see Qualifier
   */
  public static boolean isQualifierAnnotation(@NotNull Class<? extends Annotation> annotationType) {
    return annotationType.isAnnotationPresent(Qualifier.class);
  }
}
