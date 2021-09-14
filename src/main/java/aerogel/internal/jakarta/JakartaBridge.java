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

public final class JakartaBridge {

  private JakartaBridge() {
    throw new UnsupportedOperationException();
  }

  public static boolean isInjectable(@NotNull AnnotatedElement element) {
    return element.isAnnotationPresent(Inject.class) || element.isAnnotationPresent(jakarta.inject.Inject.class);
  }

  public static boolean isSingleton(@NotNull AnnotatedElement element) {
    return element.isAnnotationPresent(Singleton.class) || element.isAnnotationPresent(jakarta.inject.Singleton.class);
  }

  public static boolean isQualifierAnnotation(@NotNull Annotation annotation) {
    Class<? extends Annotation> type = annotation.annotationType();
    return type.isAnnotationPresent(Qualifier.class) || type.isAnnotationPresent(jakarta.inject.Qualifier.class);
  }

  public static boolean isNotNameAnnotation(@NotNull Annotation annotation) {
    Class<? extends Annotation> type = annotation.annotationType();
    return !type.equals(Name.class) && !type.equals(Named.class);
  }

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

  public static boolean isProvider(@NotNull Class<?> type) {
    return type == Provider.class || type == jakarta.inject.Provider.class;
  }

  public static boolean needsProviderWrapping(@NotNull Class<?> type) {
    return type == jakarta.inject.Provider.class;
  }

  public static @NotNull <T> jakarta.inject.Provider<T> bridgeJakartaProvider(@NotNull Provider<T> provider) {
    return provider::get;
  }
}
