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
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class to make working with method handles internally easier.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0")
public final class MethodHandleUtil {

  private static final MethodType GEN_F_SET = MethodType.methodType(void.class, Object.class, Object.class);
  private static final MethodType GEN_C_INVOKE = MethodType.genericMethodType(0, true);
  private static final MethodType GEN_M_INVOKE = MethodType.genericMethodType(1, true);
  private static final MethodType GEN_M_INVOKE_NORETURN = GEN_M_INVOKE.changeReturnType(void.class);

  private MethodHandleUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull MethodHandle generifyFieldSetter(@NotNull MethodHandle setter, boolean staticField) {
    if (staticField) {
      // drop the first argument for static setters, as the instance value is not required
      setter = MethodHandles.dropArguments(setter, 0, Object.class);
    }

    // converts the handle to a generic form: (Object instance, Object value): void
    return setter.asType(GEN_F_SET).asFixedArity();
  }

  public static @NotNull MethodHandle generifyMethodInvoker(
    @NotNull MethodHandle invoker,
    boolean staticMethod,
    boolean dropReturn
  ) {
    // convert the handle to a spreader of an object array (for the parameters)
    int paramCount = invoker.type().parameterCount() - (staticMethod ? 0 : 1);
    invoker = invoker.asSpreader(Object[].class, paramCount);

    // on static methods the leading instance argument must be dropped
    if (staticMethod) {
      invoker = MethodHandles.dropArguments(invoker, 0, Object.class);
    }

    // convert the handle to the final generic form, either with or without return:
    //   dropped return: (Object instance, Object[] params): void
    //   with return: (Object instance, Object[] params): Object
    if (dropReturn) {
      invoker = invoker.asType(GEN_M_INVOKE_NORETURN);
    } else {
      invoker = invoker.asType(GEN_M_INVOKE);
    }

    return invoker.asFixedArity();
  }

  public static @NotNull MethodHandle generifyConstructorInvoker(@NotNull MethodHandle invoker) {
    int paramCount = invoker.type().parameterCount();
    return invoker
      .asSpreader(Object[].class, paramCount)
      .asType(GEN_C_INVOKE)
      .asFixedArity();
  }
}
