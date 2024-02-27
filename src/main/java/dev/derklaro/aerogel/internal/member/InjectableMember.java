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

package dev.derklaro.aerogel.internal.member;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.util.MethodHandleUtil;
import jakarta.inject.Provider;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import org.jetbrains.annotations.NotNull;

interface InjectableMember {

  boolean staticMember();

  @NotNull
  MemberInjectionExecutor provideInjectionExecutor(
    @NotNull Injector injector,
    @NotNull MethodHandles.Lookup lookup
  ) throws Exception;

  class InjectableField implements InjectableMember {

    private final BindingKey<?> key;

    private final String name;
    private final boolean isStatic;

    private final Class<?> type;
    private final Class<?> refClass;

    public InjectableField(@NotNull Field field) {
      this.name = field.getName();
      this.isStatic = Modifier.isStatic(field.getModifiers());

      this.type = field.getType();
      this.refClass = field.getDeclaringClass();

      this.key = BindingKey.of(field.getGenericType()).selectQualifier(field.getAnnotations());
    }

    @Override
    public boolean staticMember() {
      return this.isStatic;
    }

    @Override
    public @NotNull MemberInjectionExecutor provideInjectionExecutor(
      @NotNull Injector injector,
      @NotNull MethodHandles.Lookup lookup
    ) throws Exception {
      // resolve the setter method handle for the field
      MethodHandle setter;
      if (this.isStatic) {
        setter = lookup.findStaticSetter(this.refClass, this.name, this.type);
      } else {
        setter = lookup.findSetter(this.refClass, this.name, this.type);
      }

      // resolve the provider for the field and generify the setter method handle
      Provider<?> fieldProvider = injector.binding(this.key).provider();
      MethodHandle genericSetter = MethodHandleUtil.generifyFieldSetter(setter, this.isStatic);
      return constructedInstance -> {
        if (constructedInstance != null || this.isStatic) {
          Object fieldValue = fieldProvider.get();
          genericSetter.invokeExact(constructedInstance, fieldValue);
        }
      };
    }
  }

  class InjectableMethod implements InjectableMember {

    private static final Object[] NO_PARAMS = new Object[0];
    private static final BindingKey<?>[] NO_KEYS = new BindingKey<?>[0];
    private static final Provider<?>[] NO_PROVIDER = new Provider<?>[0];

    private final BindingKey<?>[] parameterKeys;

    private final String name;
    private final boolean isStatic;

    private final Class<?> refClass;
    private final MethodType methodType;

    public InjectableMethod(@NotNull Method method) {
      this.name = method.getName();
      this.isStatic = Modifier.isStatic(method.getModifiers());

      this.refClass = method.getDeclaringClass();
      this.methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());

      Parameter[] parameters = method.getParameters();
      this.parameterKeys = this.resolveBindingKeys(parameters);
    }

    @Override
    public boolean staticMember() {
      return this.isStatic;
    }

    @Override
    public @NotNull MemberInjectionExecutor provideInjectionExecutor(
      @NotNull Injector injector,
      @NotNull MethodHandles.Lookup lookup
    ) throws Exception {
      // find the method in the target class
      MethodHandle invoker;
      if (this.isStatic) {
        invoker = lookup.findStatic(this.refClass, this.name, this.methodType);
      } else {
        invoker = lookup.findVirtual(this.refClass, this.name, this.methodType);
      }

      // resolve the providers for the parameters & generify the invoke method handle
      Provider<?>[] paramProviders = this.resolveParameterProviders(injector);
      MethodHandle genericInvoker = MethodHandleUtil.generifyMethodInvoker(invoker, this.isStatic, true);
      return constructedInstance -> {
        if (constructedInstance != null || this.isStatic) {
          Object[] params = this.resolveParameterValues(paramProviders);
          genericInvoker.invokeExact(constructedInstance, params);
        }
      };
    }

    private @NotNull BindingKey<?>[] resolveBindingKeys(@NotNull Parameter[] parameters) {
      int paramCount = parameters.length;
      if (paramCount == 0) {
        return NO_KEYS;
      }

      BindingKey<?>[] keys = new BindingKey<?>[paramCount];
      for (int index = 0; index < paramCount; index++) {
        Parameter parameter = parameters[index];
        keys[index] = BindingKey.of(parameter.getParameterizedType()).selectQualifier(parameter.getAnnotations());
      }
      return keys;
    }

    private @NotNull Provider<?>[] resolveParameterProviders(@NotNull Injector injector) {
      int paramCount = this.parameterKeys.length;
      if (paramCount == 0) {
        return NO_PROVIDER;
      }

      Provider<?>[] providers = new Provider<?>[paramCount];
      for (int index = 0; index < paramCount; index++) {
        BindingKey<?> key = this.parameterKeys[index];
        providers[index] = injector.binding(key).provider();
      }
      return providers;
    }

    private @NotNull Object[] resolveParameterValues(@NotNull Provider<?>[] providers) {
      int providerCount = providers.length;
      if (providerCount == 0) {
        return NO_PARAMS;
      }

      Object[] params = new Object[providerCount];
      for (int index = 0; index < providerCount; index++) {
        Provider<?> provider = providers[index];
        params[index] = provider.get();
      }
      return params;
    }
  }
}
