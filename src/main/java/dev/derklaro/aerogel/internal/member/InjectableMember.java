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
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextScope;
import dev.derklaro.aerogel.internal.provider.ParameterProviderFactory;
import dev.derklaro.aerogel.internal.util.MethodHandleUtil;
import dev.derklaro.aerogel.internal.util.UnreflectionUtil;
import jakarta.inject.Provider;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.jetbrains.annotations.NotNull;

abstract class InjectableMember {

  private static @NotNull Provider<?> resolveProviderForKey(@NotNull Injector injector, @NotNull BindingKey<?> key) {
    // try to get an overridden provider from the current injection scope first before
    // trying to get a provider from the injector. that way overrides are preserved
    // when doing member injection rather than being replaced by the usual binding
    InjectionContextScope currentScope = InjectionContextProvider.provider().currentScope();
    if (currentScope != null) {
      Provider<?> overridden = currentScope.context().findOverriddenProvider(key);
      if (overridden != null) {
        return overridden;
      }
    }

    return injector.provider(key);
  }

  @NotNull
  public abstract MemberInjectionExecutor provideInjectionExecutor(
    @NotNull Injector injector,
    @NotNull MethodHandles.Lookup lookup
  ) throws Exception;

  static final class InjectableField extends InjectableMember {

    private final Field field;
    private final boolean isStatic;
    private final MemberInjectionTracker tracker;

    private final BindingKey<?> key;

    public InjectableField(@NotNull Field field) {
      this.field = field;
      this.isStatic = Modifier.isStatic(field.getModifiers());
      this.tracker = this.isStatic ? new MemberInjectionTracker() : null;

      this.key = BindingKey.of(field.getGenericType()).selectQualifier(field.getAnnotations());
    }

    @Override
    public @NotNull MemberInjectionExecutor provideInjectionExecutor(
      @NotNull Injector injector,
      @NotNull MethodHandles.Lookup lookup
    ) {
      // resolve the setter method handle for the field
      MethodHandle setter = UnreflectionUtil.unreflectFieldSetter(this.field, lookup);

      // resolve the provider for the field and generify the setter method handle
      MethodHandle genericSetter = MethodHandleUtil.generifyFieldSetter(setter, this.isStatic);
      return constructedInstance -> {
        if ((!this.isStatic && constructedInstance != null) || (this.isStatic && this.tracker.markInjected())) {
          Provider<?> fieldProvider = InjectableMember.resolveProviderForKey(injector, key);
          Object fieldValue = fieldProvider.get();
          genericSetter.invokeExact(constructedInstance, fieldValue);
        }
      };
    }
  }

  static final class InjectableMethod extends InjectableMember {

    private static final Object[] NO_PARAMS = new Object[0];

    private final Method method;
    private final boolean isStatic;
    private final MemberInjectionTracker tracker;

    private final BindingKey<?>[] paramKeys;

    public InjectableMethod(@NotNull Method method) {
      this.method = method;
      this.isStatic = Modifier.isStatic(method.getModifiers());
      this.tracker = this.isStatic ? new MemberInjectionTracker() : null;

      this.paramKeys = ParameterProviderFactory.resolveParameterKeys(method.getParameters());
    }

    @Override
    public @NotNull MemberInjectionExecutor provideInjectionExecutor(
      @NotNull Injector injector,
      @NotNull MethodHandles.Lookup lookup
    ) {
      // find the method in the target class
      MethodHandle invoker = UnreflectionUtil.unreflectMethod(this.method, lookup);

      // resolve the providers for the parameters & generify the invoke method handle
      MethodHandle genericInvoker = MethodHandleUtil.generifyMethodInvoker(invoker, this.isStatic, true);
      return constructedInstance -> {
        if ((!this.isStatic && constructedInstance != null) || (this.isStatic && this.tracker.markInjected())) {
          Object[] params = this.resolveParameterValues(injector);
          genericInvoker.invokeExact(constructedInstance, params);
        }
      };
    }

    private @NotNull Object[] resolveParameterValues(@NotNull Injector injector) {
      // no providers, no parameter values
      int paramKeyCount = this.paramKeys.length;
      if (paramKeyCount == 0) {
        return NO_PARAMS;
      }

      // resolve the instances for each target parameter
      Object[] paramInstances = new Object[paramKeyCount];
      for (int keyIndex = 0; keyIndex < paramKeyCount; keyIndex++) {
        BindingKey<?> key = this.paramKeys[keyIndex];
        Provider<?> paramProvider = InjectableMember.resolveProviderForKey(injector, key);
        paramInstances[keyIndex] = paramProvider.get();
      }

      return paramInstances;
    }
  }
}
