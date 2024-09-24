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

import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextScope;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

@API(status = API.Status.INTERNAL, since = "3.0")
public final class ParameterProviderFactory implements ProviderFactory<Object[]> {

  private static final Object[] NO_PARAMS = new Object[0];
  private static final BindingKey<?>[] NO_KEYS = new BindingKey<?>[0];

  private final BindingKey<?>[] keys;

  private ParameterProviderFactory(@NotNull BindingKey<?>[] keys) {
    this.keys = keys;
  }

  public static @NotNull ParameterProviderFactory fromMethod(@NotNull Method method) {
    return fromParameters(method.getParameters());
  }

  public static @NotNull ParameterProviderFactory fromConstructor(@NotNull Constructor<?> constructor) {
    return fromParameters(constructor.getParameters());
  }

  public static @NotNull ParameterProviderFactory fromParameters(@NotNull Parameter[] parameters) {
    BindingKey<?>[] paramKeys = resolveParameterKeys(parameters);
    return new ParameterProviderFactory(paramKeys);
  }

  public static @NotNull BindingKey<?>[] resolveParameterKeys(@NotNull Parameter[] parameters) {
    // if there are no parameter types, there is just nothing we need to inject
    int paramCount = parameters.length;
    if (paramCount == 0) {
      return NO_KEYS;
    }

    // resolve the binding keys for each executable parameter
    BindingKey<?>[] keys = new BindingKey<?>[paramCount];
    for (int index = 0; index < paramCount; index++) {
      Parameter parameter = parameters[index];
      keys[index] = BindingKey.of(parameter.getParameterizedType()).selectQualifier(parameter.getAnnotations());
    }
    return keys;
  }

  @Override
  public @NotNull ProviderWithContext<Object[]> constructProvider() {
    return new ParameterProvider(this.keys);
  }

  private static final class ParameterProvider implements ProviderWithContext<Object[]> {

    private final BindingKey<?>[] keys;

    public ParameterProvider(@NotNull BindingKey<?>[] keys) {
      this.keys = keys;
    }

    @Override
    public @NotNull Object[] get(@NotNull InjectionContext context) {
      // no providers, no parameter values
      int paramKeyCount = this.keys.length;
      if (paramKeyCount == 0) {
        return NO_PARAMS;
      }

      // resolve the instances for each target parameter
      Object[] paramInstances = new Object[paramKeyCount];
      for (int keyIndex = 0; keyIndex < paramKeyCount; keyIndex++) {
        BindingKey<?> key = this.keys[keyIndex];
        InstalledBinding<?> binding = context.binding(key);
        InjectionContextScope scope = context.enterSubcontextScope(binding);
        Object paramInstance = scope.executeScoped(() -> scope.context().resolveInstance());
        paramInstances[keyIndex] = paramInstance;
      }

      return paramInstances;
    }

    @Override
    public @NotNull String toString() {
      return "Params(" + Arrays.toString(this.keys) + ")";
    }
  }
}
