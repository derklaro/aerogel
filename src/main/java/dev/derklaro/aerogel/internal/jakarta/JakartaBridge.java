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

package dev.derklaro.aerogel.internal.jakarta;

import dev.derklaro.aerogel.Inject;
import dev.derklaro.aerogel.Provider;
import dev.derklaro.aerogel.Qualifier;
import dev.derklaro.aerogel.Scope;
import dev.derklaro.aerogel.util.Qualifiers;
import jakarta.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class to support all jakarta inject-api annotations.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal")
public final class JakartaBridge {

  private JakartaBridge() {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks if the given {@code element} is annotated as {@code Inject}.
   *
   * @param element the element to check.
   * @return true if the element is annotated as {@code Inject}.
   */
  public static boolean isInjectable(@NotNull AnnotatedElement element) {
    return element.isAnnotationPresent(Inject.class) || element.isAnnotationPresent(jakarta.inject.Inject.class);
  }

  /**
   * Checks if the given element is annotated as {@code Inject} and is marked as optional.
   *
   * @param element the element to check.
   * @return true if the element is optional.
   * @see Inject#optional()
   */
  public static boolean isOptional(@NotNull AnnotatedElement element) {
    Inject inject = element.getAnnotation(Inject.class);
    return inject != null && inject.optional();
  }

  /**
   * Checks if the given {@code annotation} is a qualifier annotation.
   *
   * @param annotation the annotation to check.
   * @return true if the given annotation is a qualifier annotation.
   */
  public static boolean isQualifierAnnotation(@NotNull Annotation annotation) {
    Class<? extends Annotation> type = annotation.annotationType();
    return type.isAnnotationPresent(Qualifier.class) || type.isAnnotationPresent(jakarta.inject.Qualifier.class);
  }

  /**
   * Checks if the given {@code annotation} is a scope annotation.
   *
   * @param annotation the annotation to check.
   * @return true if the given annotation is a scope annotation.
   */
  public static boolean isScopeAnnotation(@NotNull Annotation annotation) {
    Class<? extends Annotation> type = annotation.annotationType();
    return type.isAnnotationPresent(Scope.class) || type.isAnnotationPresent(jakarta.inject.Scope.class);
  }

  /**
   * Translates the qualifier annotations from jakarta.inject to the aerogel associations.
   *
   * @param annotations the annotations to convert.
   * @throws NullPointerException if the given annotation array or one element is null.
   */
  public static void translateQualifierAnnotations(@NotNull Annotation[] annotations) {
    for (int i = 0; i < annotations.length; i++) {
      Annotation annotation = annotations[i];
      // translate the default qualifier annotations of jakarta (if any)
      Class<?> annotationType = annotation.annotationType();
      if (annotationType.equals(Named.class)) {
        Named named = (Named) annotation;
        annotations[i] = Qualifiers.named(named.value());
      }
    }
  }

  /**
   * Checks if the given {@code type} is a provider type.
   *
   * @param type the type to check.
   * @return true if the given type is a provider type.
   */
  public static boolean isProvider(@NotNull Class<?> type) {
    return type.equals(Provider.class) || type.equals(jakarta.inject.Provider.class);
  }

  /**
   * Check if the given {@code type} is a jakarta provider and therefore an aerogel injector must be wrapped to work.
   *
   * @param type the type to check.
   * @return true if the given type is a jakarta provider.
   */
  public static boolean needsProviderWrapping(@NotNull Class<?> type) {
    return type.equals(jakarta.inject.Provider.class);
  }

  /**
   * Wraps the given {@link Provider} into a jakarta provider.
   *
   * @param provider the provider to wrap.
   * @param <T>      the wildcard type of the provider.
   * @return a jakarta provider which calls the given aerogel provider to obtain it's value.
   */
  public static @NotNull <T> jakarta.inject.Provider<T> bridgeJakartaProvider(@NotNull Provider<T> provider) {
    return provider::get;
  }
}
