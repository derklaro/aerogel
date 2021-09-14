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

package aerogel.internal.utility;

import aerogel.Element;
import aerogel.internal.jakarta.JakartaBridge;
import aerogel.internal.reflect.ReflectionUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;

public final class ElementHelper {

  private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

  private ElementHelper() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull Element buildElement(@NotNull Field field) {
    // extract the requested name
    String name = JakartaBridge.nameOf(field);
    // extract the needed type
    Type type = JakartaBridge.isProvider(field.getType())
      ? ReflectionUtils.genericSuperType(field.getGenericType())
      : field.getGenericType();
    // extract all annotations
    Annotation[] qualifierAnnotations = extractQualifierAnnotations(field.getDeclaredAnnotations());
    // create an element based on the information
    return Element.get(type).requireName(name).requireAnnotations(qualifierAnnotations);
  }

  public static @NotNull Element buildElement(@NotNull Parameter parameter) {
    // extract the requested name
    String name = JakartaBridge.nameOf(parameter);
    // extract the needed type
    Type type = JakartaBridge.isProvider(parameter.getType())
      ? ReflectionUtils.genericSuperType(parameter.getParameterizedType())
      : parameter.getParameterizedType();
    // extract all annotations
    Annotation[] qualifierAnnotations = extractQualifierAnnotations(parameter.getDeclaredAnnotations());
    // create an element based on the information
    return Element.get(type).requireName(name).requireAnnotations(qualifierAnnotations);
  }

  public static Annotation @NotNull [] extractQualifierAnnotations(Annotation @NotNull [] annotations) {
    Collection<Annotation> qualifiedAnnotation = new HashSet<>();
    // extract every @Qualifier annotation
    for (Annotation annotation : annotations) {
      if (JakartaBridge.isQualifierAnnotation(annotation) && JakartaBridge.isNotNameAnnotation(annotation)) {
        qualifiedAnnotation.add(annotation);
      }
    }
    // return as an array
    return qualifiedAnnotation.toArray(EMPTY_ANNOTATION_ARRAY);
  }
}
