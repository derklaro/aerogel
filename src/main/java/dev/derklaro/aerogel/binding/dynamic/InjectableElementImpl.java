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

import io.leangen.geantyref.GenericTypeReflector;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of an injectable element.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
final class InjectableElementImpl implements InjectableElement {

  private final InjectableElement parent;
  private final InjectableElementType elementType;

  private final Type type;
  private final String name;
  private final Annotation[] annotations;

  private /* lazy */ Class<?> rawType;
  private /* lazy */ AnnotatedType annotatedType;
  private /* lazy */ List<Annotation> annotationList;

  /**
   * Constructs a new injectable element instance.
   *
   * @param parent      the parent element, null if none.
   * @param elementType the type of element.
   * @param type        the generic type of the element.
   * @param name        the name of the element.
   * @param annotations the annotations on the element.
   */
  public InjectableElementImpl(
    @Nullable InjectableElement parent,
    @NotNull InjectableElementType elementType,
    @NotNull Type type,
    @NotNull String name,
    @NotNull Annotation[] annotations
  ) {
    this.parent = parent;
    this.elementType = elementType;
    this.type = type;
    this.name = name;
    this.annotations = annotations;
  }

  /**
   * Constructs a new injectable element for the given type. If the type is an inner class, the parent will be outer,
   * defining class.
   *
   * @param type the class to build the injectable element for.
   * @return a new injectable element representing the given type.
   */
  @Contract("_ -> new")
  public static @NotNull InjectableElement fromClass(@NotNull Class<?> type) {
    Class<?> declaring = type.getDeclaringClass();
    InjectableElement parent = declaring == null ? null : fromClass(declaring);

    return new InjectableElementImpl(
      parent,
      StandardInjectableElementType.CLASS,
      type,
      type.getName(),
      type.getAnnotations());
  }

  /**
   * Constructs a new injectable element for the given field. The parent will be the defining class.
   *
   * @param field the field to build the injectable element for.
   * @return a new injectable element representing the given field.
   * @see #fromClass(Class)
   */
  @Contract("_ -> new")
  public static @NotNull InjectableElement fromField(@NotNull Field field) {
    InjectableElement parent = fromClass(field.getDeclaringClass());
    return new InjectableElementImpl(
      parent,
      StandardInjectableElementType.FIELD,
      field.getGenericType(),
      field.getName(),
      field.getAnnotations());
  }

  /**
   * Constructs a new injectable element for the given method. The parent will be the defining class.
   *
   * @param method the method to build the injectable element for.
   * @return a new injectable element representing the given method.
   * @see #fromClass(Class)
   */
  @Contract("_ -> new")
  public static @NotNull InjectableElement fromMethod(@NotNull Method method) {
    InjectableElement parent = fromClass(method.getDeclaringClass());
    return new InjectableElementImpl(
      parent,
      StandardInjectableElementType.METHOD,
      method.getGenericReturnType(),
      method.getName(),
      method.getAnnotations());
  }

  /**
   * Constructs a new injectable element for the given constructor. The parent will be the defining class.
   *
   * @param constructor the constructor to build the injectable element for.
   * @return a new injectable element representing the given constructor.
   * @see #fromClass(Class)
   */
  @Contract("_ -> new")
  public static @NotNull InjectableElement fromConstructor(@NotNull Constructor<?> constructor) {
    InjectableElement parent = fromClass(constructor.getDeclaringClass());
    return new InjectableElementImpl(
      parent,
      StandardInjectableElementType.CONSTRUCTOR,
      void.class,
      "<init>",
      constructor.getAnnotations());
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
  public static @NotNull InjectableElement fromParameter(@NotNull Parameter parameter) {
    InjectableElement parent;
    Executable executable = parameter.getDeclaringExecutable();
    if (executable instanceof Method) {
      parent = fromMethod((Method) executable);
    } else if (executable instanceof Constructor<?>) {
      parent = fromConstructor((Constructor<?>) executable);
    } else {
      throw new IllegalArgumentException("Unknown executable type: " + executable.getClass());
    }

    return new InjectableElementImpl(
      parent,
      StandardInjectableElementType.PARAMETER,
      parameter.getParameterizedType(),
      parameter.getName(),
      parameter.getAnnotations());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Optional<InjectableElement> parent() {
    return Optional.ofNullable(this.parent);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectableElementType type() {
    return this.elementType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String name() {
    return this.name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull AnnotatedType annotatedType() {
    if (this.annotatedType == null) {
      this.annotatedType = GenericTypeReflector.annotate(this.type, this.annotations);
    }
    return this.annotatedType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Type genericType() {
    return this.type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Class<?> rawType() {
    if (this.rawType == null) {
      this.rawType = GenericTypeReflector.erase(this.type);
    }
    return this.rawType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<Annotation> annotations() {
    if (this.annotationList == null) {
      this.annotationList = Arrays.asList(this.annotations);
    }
    return this.annotationList;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull <A extends Annotation> Optional<A> annotation(@NotNull Class<A> annotationType) {
    for (Annotation annotation : this.annotations) {
      if (annotation.annotationType().equals(annotationType)) {
        return Optional.of(annotationType.cast(annotation));
      }
    }

    return Optional.empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String toString() {
    return "InjectableElement[type=" + this.elementType + ", name=" + this.name + ", genericType=" + this.type + "]";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InjectableElementImpl that = (InjectableElementImpl) o;
    return Objects.equals(this.parent, that.parent)
      && Objects.equals(this.elementType, that.elementType)
      && Objects.equals(this.type, that.type)
      && Objects.equals(this.name, that.name)
      && Objects.deepEquals(this.annotations, that.annotations);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.parent, this.elementType, this.type, this.name, Arrays.hashCode(this.annotations));
  }
}
