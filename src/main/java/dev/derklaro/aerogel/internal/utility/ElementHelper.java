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
import dev.derklaro.aerogel.internal.reflect.ReflectionUtil;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A helper for class for constructing {@link Element}s.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel")
public final class ElementHelper {

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
  public static @NotNull Element buildElement(@NotNull Field field, @NotNull Annotation[]... annotations) {
    return buildElement(field.getGenericType(), field.getType(), annotations);
  }

  /**
   * Makes an element for the given {@code parameter} extracting all necessary information from it.
   *
   * @param parameter   the parameter to make the element for.
   * @param annotations the annotations of the parameter.
   * @return the constructed element for the given parameter.
   */
  public static @NotNull Element buildElement(@NotNull Parameter parameter, @NotNull Annotation[]... annotations) {
    return buildElement(parameter.getParameterizedType(), parameter.getType(), annotations);
  }

  /**
   * Makes an element for the given {@code method} extracting all necessary information from it.
   *
   * @param method      the method to make the element for.
   * @param annotations the annotations of the method.
   * @return the constructed element for the given method.
   */
  public static @NotNull Element buildElement(@NotNull Method method, @NotNull Annotation[]... annotations) {
    return buildElement(method.getGenericReturnType(), method.getReturnType(), annotations);
  }

  /**
   * Makes an element for the given {@code clazz} extracting all necessary information from it.
   *
   * @param fullType the full generic type of the resulting element.
   * @param rawType  the raw type of the to extract the annotations from.
   * @return the constructed element for the given class.
   */
  public static @NotNull Element buildElement(@NotNull Type fullType, @NotNull Class<?> rawType) {
    return buildElement(fullType, rawType, rawType.getDeclaredAnnotations());
  }

  /**
   * Makes an element from the provided information.
   *
   * @param fullType    the full generic type of the resulting element.
   * @param rawType     the raw type of the to extract the annotations from.
   * @param annotations the annotations of the method.
   * @return the constructed element for the given class.
   */
  private static @NotNull Element buildElement(
    @NotNull Type fullType,
    @NotNull Class<?> rawType,
    @NotNull Annotation[]... annotations
  ) {
    // begin an element for the given type
    Type type = JakartaBridge.isProvider(rawType) ? ReflectionUtil.genericSuperType(fullType) : fullType;
    Element element = Element.forType(type);

    // extract the qualifier annotations from the raw type & apply all qualifier annotations to the element
    Collection<Annotation> qualifiers = new LinkedList<>();
    for (Annotation[] annotation : annotations) {
      // replace the default qualifier annotations of jakarta
      JakartaBridge.translateQualifierAnnotations(annotation);
      qualifiers.addAll(extractQualifierAnnotations(annotation));
    }

    // require all annotations
    for (Annotation annotation : qualifiers) {
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
  public static Collection<Annotation> extractQualifierAnnotations(@NotNull Annotation[] annotations) {
    Collection<Annotation> qualifiedAnnotations = new LinkedList<>();
    for (Annotation annotation : annotations) {
      if (JakartaBridge.isQualifierAnnotation(annotation)) {
        qualifiedAnnotations.add(annotation);
      }
    }
    return qualifiedAnnotations;
  }
}
