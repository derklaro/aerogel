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

package dev.derklaro.aerogel.internal.utility;

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Qualifier;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import dev.derklaro.aerogel.internal.reflect.ReflectionUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;

/**
 * A helper for class for constructing {@link Element}s.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class ElementHelper {

  /**
   * An empty array of annotations used to collect all annotations in the {@code extractQualifierAnnotations} method.
   */
  private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

  private ElementHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Makes an element for the given {@code field} extracting all necessary information from it.
   *
   * @param field       the field to build the element for.
   * @param annotations the annotations of the field.
   * @return the constructed element for the given field.
   */
  public static @NotNull Element buildElement(@NotNull Field field, @NotNull Annotation[] annotations) {
    // extract the requested name
    String name = JakartaBridge.nameOf(field);
    // extract the needed type
    Type type = JakartaBridge.isProvider(field.getType())
      ? ReflectionUtils.genericSuperType(field.getGenericType())
      : field.getGenericType();
    // extract all annotations
    Annotation[] qualifierAnnotations = extractQualifierAnnotations(annotations);

    // create an element based on the information
    Element element = Element.forType(type).requireName(name);
    for (Annotation annotation : qualifierAnnotations) {
      element = element.requireAnnotation(annotation);
    }
    return element;
  }

  /**
   * Makes an element for the given {@code parameter} extracting all necessary information from it.
   *
   * @param parameter   the parameter to make the element for.
   * @param annotations the annotations of the parameter.
   * @return the constructed element for the given parameter.
   */
  public static @NotNull Element buildElement(@NotNull Parameter parameter, @NotNull Annotation[] annotations) {
    // extract the requested name
    String name = JakartaBridge.nameOf(parameter);
    // extract the needed type
    Type type = JakartaBridge.isProvider(parameter.getType())
      ? ReflectionUtils.genericSuperType(parameter.getParameterizedType())
      : parameter.getParameterizedType();
    // extract all annotations
    Annotation[] qualifierAnnotations = extractQualifierAnnotations(annotations);

    // create an element based on the information
    Element element = Element.forType(type).requireName(name);
    for (Annotation annotation : qualifierAnnotations) {
      element = element.requireAnnotation(annotation);
    }
    return element;
  }

  /**
   * Makes an element for the given {@code method} extracting all necessary information from it.
   *
   * @param method the method to make the element for.
   * @return the constructed element for the given method.
   */
  public static @NotNull Element buildElement(@NotNull Method method) {
    // extract the name of the method
    String name = JakartaBridge.nameOf(method);
    // extract the qualifier annotations
    Annotation[] qualifierAnnotations = extractQualifierAnnotations(method.getDeclaredAnnotations());

    // create an element based on the information
    Element element = Element.forType(method.getGenericReturnType()).requireName(name);
    for (Annotation annotation : qualifierAnnotations) {
      element = element.requireAnnotation(annotation);
    }
    return element;
  }

  /**
   * Makes an element for the given {@code clazz} extracting all necessary information from it.
   *
   * @param clazz the class to build the element for.
   * @return the constructed element for the given class.
   */
  public static @NotNull Element buildElement(@NotNull Class<?> clazz) {
    // extract the name of the class
    String name = JakartaBridge.nameOf(clazz);
    // extract the qualifier annotations
    Annotation[] qualifierAnnotations = extractQualifierAnnotations(clazz.getDeclaredAnnotations());

    // create an element based on the information
    Element element = Element.forType(clazz).requireName(name);
    for (Annotation annotation : qualifierAnnotations) {
      element = element.requireAnnotation(annotation);
    }
    return element;
  }

  /**
   * Extracts all {@link Qualifier} annotations from the given annotation array.
   *
   * @param annotations the annotation array to filter the qualifier annotations from.
   * @return a new array only containing all qualifier annotations which are in the given annotation array.
   */
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
