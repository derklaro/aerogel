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

package dev.derklaro.aerogel.auto.internal.utility;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.auto.Provides;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class for working compile time with annotations
 *
 * @author Pasqual K.
 * @since 1.0.1
 */
@API(status = API.Status.INTERNAL, since = "1.0.1", consumers = "dev.derklaro.aerogel.auto.internal")
public final class TypeUtil {

  /**
   * The identifier for an array, {@code []}.
   */
  public static final String ARRAY_INDICATOR = "[]";

  private TypeUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * A tiny hack which simplifies the value reading of an annotation during compile time. The processor is not allowed
   * to read values of an annotation directly (which is currently used to read the value of the {@link Provides}
   * annotation) which will cause an {@code MirroredTypesException}. In the exception the compiler will then give us
   * access to the mirrored types of the annotation value. There are some other ways around there which are very long
   * and complicated but don't need the exception throwing, but in my opinion this is the best and cleanest way to get
   * around the problem. <a href="https://stackoverflow.com/a/7688029" target="_blank">Thanks Ralph!</a>.
   *
   * @param getter a runnable which gets called and the thrown exception gets catched.
   * @return the type mirrors of the annotation value.
   * @throws AerogelException if the method is not used in the intended way.
   */
  public static @NotNull List<? extends TypeMirror> typesOfAnnotationValue(@NotNull Runnable getter) {
    try {
      getter.run();
      // should not happen - just explode
      throw AerogelException.forMessage("If this method was used as intended or was never touched this is bug");
    } catch (MirroredTypesException exception) {
      return exception.getTypeMirrors();
    }
  }

  /**
   * Extracts the innermost, non-array component type from the given array type, while keeping track of the given array
   * depth.
   *
   * @param arrayType the array type to get the innermost component type from.
   * @return a mapping of the innermost component type and the original array depth.
   */
  public static @NotNull Map.Entry<TypeMirror, Integer> innermostComponentType(@NotNull ArrayType arrayType) {
    int depth = 0;
    // get the innermost component type while keeping track of the walk depth
    TypeMirror result = arrayType;
    while (result.getKind() == TypeKind.ARRAY) {
      depth++;
      result = ((ArrayType) result).getComponentType();
    }
    // pack the result
    return new AbstractMap.SimpleImmutableEntry<>(result, depth);
  }

  /**
   * Converts the given type mirror to a runtime readable class name representation.
   *
   * @param typeMirror the type mirror to convert.
   * @param types      the type utils instance for the current processing environment.
   * @return the given type mirror as a runtime readable class name string.
   */
  public static @NotNull String asRuntimeType(@NotNull TypeMirror typeMirror, @NotNull Types types) {
    // try to convert the given type mirror to an element
    Element element = types.asElement(typeMirror);
    if (element != null) {
      return element.toString();
    }

    // unable to convert, might be a primitive
    try {
      PrimitiveType primitive = types.getPrimitiveType(typeMirror.getKind());
      return primitive.toString();
    } catch (IllegalArgumentException ex) {
      // not primitive
      throw AerogelException.forMessageWithoutStack(String.format(
        "TypeMirror kind %s has no corresponding element and is not primitive",
        typeMirror.getKind()));
    }
  }
}
