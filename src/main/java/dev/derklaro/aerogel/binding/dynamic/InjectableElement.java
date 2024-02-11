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

package dev.derklaro.aerogel.binding.dynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a sort of element that can be injected. By default, it can represent a type, field, method, constructor or
 * parameter. Other implementations (for example for newer java versions only) are possible.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface InjectableElement {

  /**
   * Constructs a new injectable element for the given type. If the type is an inner class, the parent will be outer,
   * defining class.
   *
   * @param clazz the class to build the injectable element for.
   * @return a new injectable element representing the given type.
   */
  @Contract("_ -> new")
  static @NotNull InjectableElement fromClass(@NotNull Class<?> clazz) {
    return InjectableElementImpl.fromClass(clazz);
  }

  /**
   * Constructs a new injectable element for the given field. The parent will be the defining class.
   *
   * @param field the field to build the injectable element for.
   * @return a new injectable element representing the given field.
   * @see #fromClass(Class)
   */
  @Contract("_ -> new")
  static @NotNull InjectableElement fromField(@NotNull Field field) {
    return InjectableElementImpl.fromField(field);
  }

  /**
   * Constructs a new injectable element for the given method. The parent will be the defining class.
   *
   * @param method the method to build the injectable element for.
   * @return a new injectable element representing the given method.
   * @see #fromClass(Class)
   */
  @Contract("_ -> new")
  static @NotNull InjectableElement fromMethod(@NotNull Method method) {
    return InjectableElementImpl.fromMethod(method);
  }

  /**
   * Constructs a new injectable element for the given constructor. The parent will be the defining class.
   *
   * @param constructor the constructor to build the injectable element for.
   * @return a new injectable element representing the given constructor.
   * @see #fromClass(Class)
   */
  @Contract("_ -> new")
  static @NotNull InjectableElement fromConstructor(@NotNull Constructor<?> constructor) {
    return InjectableElementImpl.fromConstructor(constructor);
  }

  /**
   * Constructs a new injectable element for the given parameter. The parent will be the defining method or
   * constructor.
   *
   * @param parameter the parameter to build the injectable element for.
   * @return a new injectable element representing the given parameter.
   * @see #fromMethod(Method)
   * @see #fromConstructor(Constructor)
   */
  @Contract("_ -> new")
  static @NotNull InjectableElement fromParameter(@NotNull Parameter parameter) {
    return InjectableElementImpl.fromParameter(parameter);
  }

  /**
   * The parent element of the current element:
   * <ol>
   *  <li>For parameters this is the defining method or constructor.
   *  <li>For fields, methods or constructors this is the defining class.
   *  <li>For classes this is the declaring outer class, if any.
   * </ol>
   *
   * @return the parent element of this element.
   */
  @NotNull
  Optional<InjectableElement> parent();

  /**
   * Get the type of this element.
   *
   * @return the type of this element.
   */
  @NotNull
  InjectableElementType type();

  /**
   * Get the name of this element. For constructors this is always {@code <init>}.
   *
   * @return the name of this element.
   */
  @NotNull
  String name();

  /**
   * Get the generic type of this element with all annotations of this element. The returned type is in a canonical form
   * that implements {@code equals} and {@code hashCode}.
   *
   * @return the annotated type of this element.
   * @see #genericType()
   * @see #annotations()
   */
  @NotNull
  AnnotatedType annotatedType();

  /**
   * Get the generic type of this element:
   * <ol>
   *  <li>For fields or parameters this is the generic type of them.
   *  <li>For methods this is the return type of the method.
   *  <li>For constructors this is always {@code void}.
   *  <li>For classes this is the raw class type.
   * </ol>
   *
   * @return the generic type of this element.
   */
  @NotNull
  Type genericType();

  /**
   * Get the erased type of this element.
   *
   * @return the erased type of this element.
   * @see #genericType()
   */
  @NotNull
  Class<?> rawType();

  /**
   * Get the annotations that are directly present or inherited on this element.
   *
   * @return the annotations that are directly present or inherited on this element.
   */
  @NotNull
  Collection<Annotation> annotations();
}
