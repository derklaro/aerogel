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

package aerogel.internal.jakarta;

import aerogel.Inject;
import aerogel.Name;
import aerogel.Provider;
import aerogel.Qualifier;
import aerogel.Singleton;
import jakarta.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility class to support all jakarta inject-api annotations.
 *
 * @author Pasqual K.
 * @since 1.0
 */
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
   * Checks if the given {@code element} is annotated as {@code Singleton}.
   *
   * @param element the element to check.
   * @return true if the element is annotated as {@code Singleton}.
   */
  public static boolean isSingleton(@NotNull AnnotatedElement element) {
    return element.isAnnotationPresent(Singleton.class) || element.isAnnotationPresent(jakarta.inject.Singleton.class);
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
   * Checks if the given annotation is not a {@code Name} annotation.
   *
   * @param annotation the annotation to check.
   * @return true if the annotation is not a naming annotation.
   */
  public static boolean isNotNameAnnotation(@NotNull Annotation annotation) {
    Class<? extends Annotation> type = annotation.annotationType();
    return !type.equals(Name.class) && !type.equals(Named.class);
  }

  /**
   * Gets the requested name of the given {@code element}.
   *
   * @param element the element to get the name of.
   * @return the requested name of the element or null if no name is requested.
   */
  public static @Nullable String nameOf(@NotNull AnnotatedElement element) {
    // get the name from our annotation first
    Name name = element.getAnnotation(Name.class);
    if (name != null) {
      return name.value();
    }
    // check jakarta
    Named named = element.getAnnotation(Named.class);
    return named == null ? null : named.value();
  }

  /**
   * Checks if the given {@code type} is a provider type.
   *
   * @param type the type to check.
   * @return true if the given type is a provider type.
   */
  public static boolean isProvider(@NotNull Class<?> type) {
    return type == Provider.class || type == jakarta.inject.Provider.class;
  }

  /**
   * Check if the given {@code type} is a jakarta provider and therefore an aerogel injector must be wrapped to work.
   *
   * @param type the type to check.
   * @return true if the given type is a jakarta provider.
   */
  public static boolean needsProviderWrapping(@NotNull Class<?> type) {
    return type == jakarta.inject.Provider.class;
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
