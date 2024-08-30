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

package dev.derklaro.aerogel.internal.provider;

import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.internal.ConstructionException;
import dev.derklaro.aerogel.internal.PassThroughException;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import dev.derklaro.aerogel.internal.util.MethodHandleUtil;
import dev.derklaro.aerogel.internal.util.UnreflectionUtil;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FactoryMethodProviderFactory<T> implements ProviderFactory<T> {

  private final Method origMethod;
  private final MethodHandle methodHandle;
  private final ParameterProviderFactory parameterProvider;

  public FactoryMethodProviderFactory(Method origMethod, MethodHandle methodHandle,
    ParameterProviderFactory parameterProvider) {
    this.origMethod = origMethod;
    this.methodHandle = methodHandle;
    this.parameterProvider = parameterProvider;
  }

  public static @NotNull <T> FactoryMethodProviderFactory<T> fromMethod(
    @NotNull Method factoryMethod,
    @NotNull MethodHandles.Lookup lookup
  ) {
    MethodHandle directMethodHandle = UnreflectionUtil.unreflectMethod(factoryMethod, lookup);
    MethodHandle genericMethodHandle = MethodHandleUtil.generifyMethodInvoker(directMethodHandle, true, false);
    ParameterProviderFactory parameterProviderFactory = ParameterProviderFactory.fromMethod(factoryMethod);
    return new FactoryMethodProviderFactory<>(factoryMethod, genericMethodHandle, parameterProviderFactory);
  }

  @Override
  public @NotNull ProviderWithContext<T> constructProvider() {
    ProviderWithContext<Object[]> paramProvider = this.parameterProvider.constructProvider();
    return new FactoryMethodProvider<>(this.origMethod, this.methodHandle, paramProvider);
  }

  private static final class FactoryMethodProvider<T> implements ProviderWithContext<T> {

    private final Method origMethod;
    private final MethodHandle methodHandle;
    private final ProviderWithContext<Object[]> paramProvider;

    public FactoryMethodProvider(
      @NotNull Method origMethod,
      @NotNull MethodHandle methodHandle,
      @NotNull ProviderWithContext<Object[]> paramProvider
    ) {
      this.origMethod = origMethod;
      this.methodHandle = methodHandle;
      this.paramProvider = paramProvider;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable T get(@NotNull InjectionContext context) {
      try {
        Object[] paramValues = this.paramProvider.get(context);
        Object constructedInstance = this.methodHandle.invokeExact(paramValues);
        context.requestMemberInjectionSameBinding(constructedInstance);
        return (T) constructedInstance;
      } catch (PassThroughException exception) {
        // internal marker exception, pass back to the caller
        throw exception;
      } catch (Throwable throwable) {
        throw ConstructionException.of(this.origMethod, throwable);
      }
    }

    @Override
    public @NotNull String toString() {
      String simpleMethodDescriptor = this.origMethod.getDeclaringClass() + "." + this.origMethod.getName();
      return "Factory(" + simpleMethodDescriptor + "(" + this.paramProvider + "))";
    }
  }
}
