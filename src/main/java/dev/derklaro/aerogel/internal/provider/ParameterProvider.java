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
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import jakarta.inject.Provider;
import java.lang.reflect.Parameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ParameterProvider implements Provider<Object[]> {

  private static final Object[] NO_PARAMS = new Object[0];
  private static final BindingKey<?>[] NO_KEYS = new BindingKey<?>[0];

  private final Injector injector;
  private final BindingKey<?>[] keys;

  public ParameterProvider(@NotNull Injector injector, @NotNull BindingKey<?>[] keys) {
    this.injector = injector;
    this.keys = keys;
  }

  public static @NotNull BindingKey<?>[] resolveKeys(@NotNull Parameter[] parameters) {
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
  public @NotNull Object[] get() {
    // no providers, no parameter values
    int paramKeyCount = this.keys.length;
    if (paramKeyCount == 0) {
      return NO_PARAMS;
    }

    // resolve the current injection context
    InjectionContextProvider provider = InjectionContextProvider.provider();
    InjectionContext context = provider.currentContext();

    // resolve the providers for the current context & the parameter values based on them
    Provider<?>[] providers = this.resolveProviders(context);
    return this.resolveProviderValues(providers);
  }

  private @NotNull Provider<?>[] resolveProviders(@Nullable InjectionContext context) {
    int keyCount = this.keys.length;
    Provider<?>[] providers = new Provider<?>[keyCount];
    for (int index = 0; index < keyCount; index++) {
      BindingKey<?> key = this.keys[index];
      providers[index] = this.resolveProvider(key, context);
    }
    return providers;
  }

  private @NotNull Provider<?> resolveProvider(@NotNull BindingKey<?> key, @Nullable InjectionContext context) {
    // try to resolve the provider from the current injection context
    Provider<?> provider = null;
    if (context != null) {
      provider = context.findOverriddenProvider(key);
    }

    // if not in context / no overridden provider is present, try to get the provider from the bound injector
    if (provider == null) {
      provider = this.injector.binding(key).provider();
    }

    return provider;
  }

  private @NotNull Object[] resolveProviderValues(@NotNull Provider<?>[] providers) {
    int providerCount = providers.length;
    Object[] paramValues = new Object[providerCount];
    for (int index = 0; index < providerCount; index++) {
      Provider<?> provider = providers[index];
      paramValues[index] = provider.get();
    }
    return paramValues;
  }
}
