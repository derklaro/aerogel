/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
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

package dev.derklaro.aerogel.internal.reflect;

import dev.derklaro.aerogel.AerogelException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.StringJoiner;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods to simplify working with generic types.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.*")
public final class TypeUtil {

  private TypeUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Extracts the raw type of the given {@code type}. For example a parameterized type like {@code List<String>} will
   * result in {@code List}.
   *
   * @param type the type to extract the raw type of.
   * @return the raw type of the parameterized type.
   * @throws AerogelException if the type is a type variable or wildcard type.
   */
  public static @NotNull Class<?> rawType(@NotNull Type type) {
    if (type instanceof Class<?>) {
      // the given type is a normal class
      return (Class<?>) type;
    } else if (type instanceof GenericArrayType) {
      // unbox the component type, create an array of that type and use it's class
      Class<?> componentType = rawType(((GenericArrayType) type).getGenericComponentType());
      return Array.newInstance(componentType, 0).getClass();
    } else if (type instanceof ParameterizedType) {
      // the raw type is always of type class - the internet is not sure why exactly this is a type
      return rawType(((ParameterizedType) type).getRawType());
    } else if (type instanceof TypeVariable<?>) {
      // get the raw type from the first bound, if present
      TypeVariable<?> typeVariable = (TypeVariable<?>) type;
      Type[] bounds = typeVariable.getBounds(); // prevent cloning twice
      return bounds.length == 0 ? Object.class : rawType(bounds[0]);
    } else if (type instanceof WildcardType) {
      // get the type variable from the lower bounds, if not present use the first upper bound
      WildcardType wildcardType = (WildcardType) type;
      Type[] lowerBounds = wildcardType.getLowerBounds(); // prevent cloning twice
      return rawType(lowerBounds.length > 0 ? lowerBounds[0] : wildcardType.getUpperBounds()[0]);
    }

    // every other type implementation can not be exactly found - ignored them
    throw AerogelException.forMessage("Unsupported type " + type + " to erase");
  }

  /**
   * Constructs a pretty string of the given type which isn't cluttered with package information of the given type but
   * will handle all default generic types and output a precise representation of the given type.
   *
   * @param type the type to build a string representation of.
   * @return a string representation of the given type.
   */
  public static @NotNull String toPrettyString(@NotNull Type type) {
    if (type instanceof Class<?>) {
      Class<?> clazz = (Class<?>) type;

      // check if the class is enclosed
      Class<?> enclosingClass = clazz.getEnclosingClass();
      if (enclosingClass != null) {
        String prettyEnclosingClass = toPrettyString(enclosingClass);
        return prettyEnclosingClass + '$' + clazz.getSimpleName();
      } else {
        // top level class
        return clazz.getSimpleName();
      }
    } else if (type instanceof ParameterizedType) {
      StringBuilder infoBuilder = new StringBuilder();
      ParameterizedType parameterizedType = (ParameterizedType) type;

      // append the info about the raw type
      String rawPretty = toPrettyString(parameterizedType.getRawType());
      infoBuilder.append(rawPretty);

      // append the type arguments
      Type[] typeArguments = parameterizedType.getActualTypeArguments();
      if (typeArguments.length > 0) {
        StringJoiner argumentJoiner = new StringJoiner(", ", "<", ">");
        for (Type typeArgument : typeArguments) {
          String argumentPretty = toPrettyString(typeArgument);
          argumentJoiner.add(argumentPretty);
        }
        infoBuilder.append(argumentJoiner);
      }
      return infoBuilder.toString();
    } else if (type instanceof WildcardType) {
      WildcardType wildcardType = (WildcardType) type;

      // check the lower bounds of the wildcard type
      Type[] lowerBounds = wildcardType.getLowerBounds();
      if (lowerBounds.length > 0) {
        String prettyBound = toPrettyString(lowerBounds[0]);
        return "? super " + prettyBound;
      }

      // check the upper bounds of the type
      Type[] upperBounds = wildcardType.getUpperBounds();
      if (upperBounds.length == 0 || upperBounds[0] == Object.class) {
        return "?";
      } else {
        String prettyBound = toPrettyString(upperBounds[0]);
        return "? extends " + prettyBound;
      }
    } else if (type instanceof GenericArrayType) {
      GenericArrayType genericArrayType = (GenericArrayType) type;

      // get the info about the component type
      String prettyComponentType = toPrettyString(genericArrayType.getGenericComponentType());
      return prettyComponentType + "[]";
    } else if (type instanceof TypeVariable<?>) {
      TypeVariable<?> typeVariable = (TypeVariable<?>) type;

      // get the raw type of the bounds
      Type[] bounds = typeVariable.getBounds();
      String prettyBound = toPrettyString(bounds.length == 0 ? Object.class : bounds[0]);

      // append the name of the variable as well
      return prettyBound + " " + typeVariable.getName();
    } else {
      // unknown type, just return the toString value
      return type.toString();
    }
  }
}
