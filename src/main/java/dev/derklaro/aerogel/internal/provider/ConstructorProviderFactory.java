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

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.internal.ConstructionException;
import dev.derklaro.aerogel.internal.PassthroughException;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import dev.derklaro.aerogel.internal.util.MethodHandleUtil;
import dev.derklaro.aerogel.internal.util.RecordUtil;
import dev.derklaro.aerogel.internal.util.UnreflectionUtil;
import jakarta.inject.Inject;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@API(status = API.Status.INTERNAL, since = "3.0")
public final class ConstructorProviderFactory<T> implements ProviderFactory<T> {

  private final Constructor<?> origConstructor;
  private final MethodHandle constructorHandle;
  private final ParameterProviderFactory parameterProvider;

  private ConstructorProviderFactory(
    @NotNull Constructor<?> origConstructor,
    @NotNull MethodHandle constructorHandle,
    @NotNull ParameterProviderFactory parameterProvider
  ) {
    this.origConstructor = origConstructor;
    this.constructorHandle = constructorHandle;
    this.parameterProvider = parameterProvider;
  }

  public static @NotNull <T> ConstructorProviderFactory<T> fromConstructor(
    @NotNull Constructor<? extends T> constructor,
    @NotNull MethodHandles.Lookup lookup
  ) {
    MethodHandle directConstructorHandle = UnreflectionUtil.unreflectConstructor(constructor, lookup);
    MethodHandle genericConstructorHandle = MethodHandleUtil.generifyConstructorInvoker(directConstructorHandle);
    ParameterProviderFactory parameterProviderFactory = ParameterProviderFactory.fromConstructor(constructor);
    return new ConstructorProviderFactory<>(constructor, genericConstructorHandle, parameterProviderFactory);
  }

  public static @NotNull <T> ConstructorProviderFactory<T> fromClass(
    @NotNull Class<? extends T> clazz,
    @NotNull MethodHandles.Lookup lookup
  ) {
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();

    // 1. try: find a constructor that is explicitly annotated with @Inject
    Constructor<?> match = null;
    for (Constructor<?> constructor : constructors) {
      if (constructor.isAnnotationPresent(Inject.class)) {
        if (match != null) {
          // 2 constructors are annotated with @Inject
          throw new IllegalArgumentException(
            "Duplicate injectable constructor in class " + clazz + ": 1. " + match + "; 2. " + constructor);
        }

        match = constructor;
      }
    }

    // 2. try: if there is no match yet, try the all args constructor from a record class
    if (match == null) {
      Class<?>[] recordComponentTypes = RecordUtil.recordComponentTypes(clazz);
      if (recordComponentTypes != null) {
        try {
          match = clazz.getDeclaredConstructor(recordComponentTypes);
        } catch (NoSuchMethodException ignored) {
          // this should usually not happen as a constructor with all record components
          // must always exist.
        }
      }
    }

    // 3. try: use the constructor which doesn't take any arguments
    if (match == null) {
      try {
        match = clazz.getDeclaredConstructor();
      } catch (NoSuchMethodException ignored) {
      }
    }

    // still no match found, cannot inject the target class
    if (match == null) {
      throw new IllegalArgumentException("No injectable constructor in class " + clazz);
    }

    //noinspection unchecked
    return fromConstructor((Constructor<T>) match, lookup);
  }

  @Override
  public @NotNull ProviderWithContext<T> constructProvider(@NotNull Injector injector) {
    ProviderWithContext<Object[]> paramProvider = this.parameterProvider.constructProvider(injector);
    return new ConstructorProvider<>(this.origConstructor, this.constructorHandle, paramProvider);
  }

  private static final class ConstructorProvider<T> implements ProviderWithContext<T> {

    private final Constructor<?> origConstructor;
    private final MethodHandle constructorHandle;
    private final ProviderWithContext<Object[]> paramProvider;

    public ConstructorProvider(
      @NotNull Constructor<?> origConstructor,
      @NotNull MethodHandle constructorHandle,
      @NotNull ProviderWithContext<Object[]> paramProvider
    ) {
      this.origConstructor = origConstructor;
      this.constructorHandle = constructorHandle;
      this.paramProvider = paramProvider;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable T get(@NotNull InjectionContext context) {
      try {
        Object[] paramValues = this.paramProvider.get(context);
        Object constructedInstance = this.constructorHandle.invokeExact(paramValues);
        context.requestMemberInjectionSameBinding(constructedInstance);
        return (T) constructedInstance;
      } catch (PassthroughException exception) {
        // internal marker exception, pass back to the caller
        throw exception;
      } catch (Throwable throwable) {
        throw ConstructionException.of(this.origConstructor, throwable);
      }
    }

    @Override
    public @NotNull String toString() {
      return "Constructor(" + this.origConstructor.getDeclaringClass() + "(" + this.paramProvider + "))";
    }
  }
}
