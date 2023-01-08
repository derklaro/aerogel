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

package dev.derklaro.aerogel.internal.utility;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.internal.unsafe.UnsafeMemberAccess;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility class to make working with method handles internally easier.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.*")
public final class MethodHandleUtil {

  private static final MethodHandles.Lookup BASE_LOOKUP = MethodHandles.lookup();

  private MethodHandleUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Invokes the given generic method handle targeting a method, treated as it would be produced by
   * {@link #toGenericMethodHandle(Method)}.
   *
   * @param handle   the handle to invoke.
   * @param instance the instance to use when invoking, null for static methods.
   * @param params   the parameters to supply to the method.
   * @return the signature-polymorphic result of the method invocation.
   * @throws NullPointerException if the given handle or params are null.
   * @throws Throwable            anything thrown by the underlying method.
   */
  public static @Nullable Object invokeMethod(
    @NotNull MethodHandle handle,
    @Nullable Object instance,
    @NotNull Object[] params
  ) throws Throwable {
    return params.length == 0 ? handle.invoke(instance) : handle.invoke(instance, params);
  }

  /**
   * Invokes the given generic method handle targeting a constructor, treated as it would be produced by
   * {@link #toGenericMethodHandle(Constructor)}.
   *
   * @param handle the handle to invoke.
   * @param params the parameters to supply to the constructor.
   * @return the signature-polymorphic constructed instance, produced by the constructor call.
   * @throws NullPointerException if the given handle or params are null.
   * @throws Throwable            anything thrown by the underlying constructor.
   */
  public static @Nullable Object invokeConstructor(
    @NotNull MethodHandle handle,
    @NotNull Object[] params
  ) throws Throwable {
    return params.length == 0 ? handle.invoke() : handle.invoke(params);
  }

  /**
   * Constructs a generic method handle for the given method, using either the signature {@code (Object)Object} for
   * methods which are not taking any arguments (the object arguments represents the instance to call the method on, for
   * static methods that would always be {@code null}), or {@code (Object,Object[])Object} for methods which are taking
   * arguments.
   *
   * @param method the method to construct the generic handle for.
   * @return a generic method handle for the given method.
   * @throws NullPointerException if the given method is null.
   * @throws AerogelException     if this method is unable to convert the given method to a method handle.
   */
  public static @NotNull MethodHandle toGenericMethodHandle(@NotNull Method method) {
    try {
      // unreflect the method - the handle we use to call the method on does not matter, as we ensure that
      // the method is accessible which will force java into using IMPL_LOOKUP which has full access rights
      Method accessible = UnsafeMemberAccess.makeAccessible(method);
      MethodHandle directHandle = BASE_LOOKUP.unreflect(accessible);

      // convert the direct handle to a handle with fixed arity (only accepts a fixed number of arguments)
      MethodHandle targetHandle = directHandle.asFixedArity();

      // convert the direct handle to a generic one
      boolean isStatic = Modifier.isStatic(method.getModifiers());
      int paramCount = targetHandle.type().parameterCount() - (isStatic ? 0 : 1);
      MethodType type = MethodType.genericMethodType(1, paramCount > 0);

      // if the method has arguments we want to spread the argument array
      if (paramCount > 0) {
        targetHandle = targetHandle.asSpreader(Object[].class, paramCount);
      }

      // drop the leading instance argument from the method handle in case the target method is static
      if (isStatic) {
        targetHandle = MethodHandles.dropArguments(targetHandle, 0, Object.class);
      }

      // return the final handle with a full generic signature
      return targetHandle.asType(type);
    } catch (IllegalAccessException exception) {
      throw AerogelException.forMessagedException("Unable to unreflect method " + method.getName(), exception);
    }
  }

  /**
   * Constructs a generic method handle for the given constructor, using either the signature {@code ()Object} for
   * constructors which aren't taking any arguments or {@code (Object[])Object} for constructors which are taking
   * arguments.
   *
   * @param constructor the constructor to construct the generic handle for.
   * @return a generic method handle for the given constructor.
   * @throws NullPointerException if the given constructor is null.
   */
  public static @NotNull MethodHandle toGenericMethodHandle(@NotNull Constructor<?> constructor) {
    try {
      // unreflect the constructor - the handle we use to call the method on does not matter, as we ensure that
      // the method is accessible which will force java into using IMPL_LOOKUP which has full access rights
      Constructor<?> accessible = UnsafeMemberAccess.makeAccessible(constructor);
      MethodHandle directHandle = BASE_LOOKUP.unreflectConstructor(accessible);

      // convert the direct handle to a handle with fixed arity (only accepts a fixed number of arguments)
      MethodHandle targetHandle = directHandle.asFixedArity();

      // convert the direct handle to a generic one
      int paramCount = directHandle.type().parameterCount();
      MethodType type = MethodType.genericMethodType(0, paramCount > 0);

      // if the method has arguments we want to spread the argument array
      if (paramCount > 0) {
        targetHandle = targetHandle.asSpreader(Object[].class, paramCount);
      }

      // return the final handle with a full generic signature
      return targetHandle.asType(type);
    } catch (IllegalAccessException exception) {
      throw AerogelException.forMessagedException("Unable to unreflect constructor " + constructor, exception);
    }
  }

  /**
   * Constructs a generic method handle to get the value of the given field, using either the signature {@code ()Object}
   * for static fields or {@code (Object)Object} for instance fields.
   *
   * @param field the field to construct the generic getter handle for.
   * @return a generic method handle to get the value of the given field.
   * @throws NullPointerException if the given field is null.
   */
  public static @NotNull MethodHandle toGenericGetterMethodHandle(@NotNull Field field) {
    try {
      // unreflect the field - the handle we use to call the method on does not matter, as we ensure that
      // the method is accessible which will force java into using IMPL_LOOKUP which has full access rights
      Field accessible = UnsafeMemberAccess.makeAccessible(field);
      MethodHandle directHandle = BASE_LOOKUP.unreflectGetter(accessible);

      // convert the direct handle to a handle with fixed arity (only accepts a fixed number of arguments)
      MethodHandle targetHandle = directHandle.asFixedArity();

      // convert the direct handle to a generic one
      boolean isStatic = Modifier.isStatic(field.getModifiers());
      MethodType type = isStatic
        ? MethodType.methodType(Object.class)
        : MethodType.methodType(Object.class, Object.class);

      // return the final handle with a full generic signature
      return targetHandle.asType(type);
    } catch (IllegalAccessException exception) {
      throw AerogelException.forMessagedException("Unable to unreflect getter for field " + field.getName(), exception);
    }
  }

  /**
   * Constructs a generic method handle to set the value of the given field, using either the signature
   * {@code (Object)void} for static fields or {@code (Object,Object)void} for instance fields.
   *
   * @param field the field to construct the generic setter handle for.
   * @return a generic method handle to set the value of the given field.
   * @throws NullPointerException if the given field is null.
   */
  public static @NotNull MethodHandle toGenericSetterMethodHandle(@NotNull Field field) {
    try {
      // unreflect the field - the handle we use to call the method on does not matter, as we ensure that
      // the method is accessible which will force java into using IMPL_LOOKUP which has full access rights
      Field accessible = UnsafeMemberAccess.makeAccessible(field);
      MethodHandle directHandle = BASE_LOOKUP.unreflectSetter(accessible);

      // convert the direct handle to a handle with fixed arity (only accepts a fixed number of arguments)
      MethodHandle targetHandle = directHandle.asFixedArity();

      // convert the direct handle to a generic one
      boolean isStatic = Modifier.isStatic(field.getModifiers());
      MethodType type = isStatic
        ? MethodType.methodType(void.class, Object.class)
        : MethodType.methodType(void.class, Object.class, Object.class);

      // return the final handle with a full generic signature
      return targetHandle.asType(type);
    } catch (IllegalAccessException exception) {
      throw AerogelException.forMessagedException("Unable to unreflect getter for field " + field.getName(), exception);
    }
  }
}
