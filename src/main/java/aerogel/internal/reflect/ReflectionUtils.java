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

package aerogel.internal.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReflectionUtils {

  private ReflectionUtils() {
    throw new UnsupportedOperationException();
  }

  public static void ensureInstantiable(@NotNull Type type) {
    // check if the type is a class
    if (!(type instanceof Class<?>)) {
      throw new UnsupportedOperationException(type.getTypeName() + " is not instantiable");
    }
    // cast to the type
    Class<?> clazz = (Class<?>) type;
    // check if the type is instantiable
    if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isPrimitive() || clazz.isArray()) {
      throw new UnsupportedOperationException(clazz + " is not instantiable");
    }
  }

  public static @NotNull Type genericSuperType(@NotNull Type type) {
    return type instanceof ParameterizedType ? ((ParameterizedType) type).getActualTypeArguments()[0] : type;
  }

  public static @NotNull Class<?> rawType(@NotNull Type type) {
    Type superType = genericSuperType(type);
    // check if the type is a normal class
    if (superType instanceof Class<?>) {
      return (Class<?>) superType;
    } else if (superType instanceof GenericArrayType) {
      // unbox the component type, create an array of that type and use it's class
      Class<?> genericType = rawType(((GenericArrayType) superType).getGenericComponentType());
      return Array.newInstance(genericType, 0).getClass();
    } else if (superType instanceof ParameterizedType) {
      // the raw type is always of type class - the internet is not sure why exactly this is a type
      return (Class<?>) ((ParameterizedType) superType).getRawType();
    }
    // every other type implementation can not be exactly found - ignored them
    throw new IllegalArgumentException("Unsupported type " + superType + " to unbox");
  }

  public static boolean isPackagePrivate(@NotNull Member member) {
    int mod = member.getModifiers();
    return !Modifier.isPublic(mod) && !Modifier.isProtected(mod) && !Modifier.isPrivate(mod);
  }

  public static @NotNull String shortVisibilitySummary(@NotNull Member member) {
    // check if the member is package private
    // in this case we need to make the visibility summary unique by suffixing with the package name as the method
    // cannot be overridden by any type outside that package
    if (isPackagePrivate(member)) {
      return "package-private" + member.getDeclaringClass().getPackage().getName();
    }
    // check if the method is private
    // in this case we need to make the visibility summary unique by suffixing with the declaring class name as the
    // method cannot be "overridden" by any type outside the declaring class
    if (Modifier.isPrivate(member.getModifiers())) {
      return "private" + member.getDeclaringClass().getName();
    }
    // the method is either public or protected - it can be overridden by any inheritance
    return "public";
  }

  public static boolean isUninitialized(@NotNull Field field, @Nullable Object holder) throws IllegalAccessException {
    // get the current field value
    Object currentValue = field.get(holder);
    // check if the field type is primitive - check for the default value then
    if (isPrimitive(field.getGenericType())) {
      return currentValue.equals(Primitives.defaultValue(field.getType()));
    } else {
      // for non-primitive fields the value must just be null
      return currentValue == null;
    }
  }

  public static boolean isPrimitive(@NotNull Type type) {
    return type instanceof Class<?> && ((Class<?>) type).isPrimitive();
  }

  public static @NotNull List<Class<?>> hierarchyTree(@NotNull Class<?> startingPoint) {
    List<Class<?>> result = new ArrayList<>();
    // add all super classes of the class
    Class<?> currentClass = startingPoint;
    do {
      result.add(currentClass);
    } while ((currentClass = currentClass.getSuperclass()) != Object.class);
    // return the result
    return result;
  }
}
