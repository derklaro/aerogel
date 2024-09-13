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

package dev.derklaro.aerogel.internal.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A class to support record classes on Java &gt;= 16.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0")
public final class RecordUtil {

  private static final MethodHandle GET_RECORD_COMPONENTS;
  private static final MethodHandle RECORD_COMPONENT_GET_TYPE;

  static {
    MethodHandle getRecordComponents;
    MethodHandle recordComponentGetType;

    try {
      // get the record components method & the RecordComponent type
      Method getRecordComponentsMethod = Class.class.getMethod("getRecordComponents");
      Class<?> recordComponentType = getRecordComponentsMethod.getReturnType().getComponentType();

      // unreflect the getComponentsType & get the getType method
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      getRecordComponents = lookup.unreflect(getRecordComponentsMethod);
      recordComponentGetType = lookup.findVirtual(recordComponentType, "getType", MethodType.methodType(Class.class));
    } catch (NoSuchMethodException | IllegalAccessException exception) {
      // not available
      getRecordComponents = null;
      recordComponentGetType = null;
    }

    // assign the static fields
    GET_RECORD_COMPONENTS = getRecordComponents;
    RECORD_COMPONENT_GET_TYPE = recordComponentGetType;
  }

  /**
   * Get the record component types of a class or null if the given class is not a record.
   *
   * @param clazz the class to get the record components of.
   * @return the record components of the class, null if the class is not a record.
   */
  public static @Nullable Class<?>[] recordComponentTypes(@NotNull Class<?> clazz) {
    if (GET_RECORD_COMPONENTS == null || RECORD_COMPONENT_GET_TYPE == null) {
      return null;
    }

    try {
      // get the record component types; null is returned in case the given class is not a record
      Object[] recordComponents = (Object[]) GET_RECORD_COMPONENTS.invoke(clazz);
      if (recordComponents == null) {
        return null;
      }

      // convert the record components
      Class<?>[] types = new Class[recordComponents.length];
      for (int i = 0; i < recordComponents.length; i++) {
        Class<?> type = (Class<?>) RECORD_COMPONENT_GET_TYPE.invoke(recordComponents[i]);
        types[i] = type;
      }
      return types;
    } catch (Throwable ignored) {
      return null;
    }
  }
}
