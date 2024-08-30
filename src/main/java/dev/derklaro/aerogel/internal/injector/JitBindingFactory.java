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

package dev.derklaro.aerogel.internal.injector;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.MemberInjector;
import dev.derklaro.aerogel.ProvidedBy;
import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import io.leangen.geantyref.GenericTypeReflector;
import jakarta.inject.Provider;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;

final class JitBindingFactory {

  private final Injector injector;

  public JitBindingFactory(@NotNull Injector injector) {
    this.injector = injector;
  }

  @SuppressWarnings("unchecked")
  private static @NotNull BindingKey<Object> convertKeyUnchecked(@NotNull BindingKey<?> key) {
    return (BindingKey<Object>) key;
  }

  public @NotNull InstalledBinding<?> createJitBinding(@NotNull BindingKey<?> key) {
    // check for special types: Provider & Member injector first
    Type targetType = key.type();
    if (targetType instanceof ParameterizedType) {
      ParameterizedType parameterized = (ParameterizedType) targetType;
      if (parameterized.getRawType().equals(Provider.class)) {
        // target is a provider
        Type componentType = parameterized.getActualTypeArguments()[0];
        BindingKey<?> componentKey = key.withType(componentType);
        InstalledBinding<?> componentBinding = this.injector.binding(componentKey);
        return this.createStaticBinding(key, componentBinding.provider());
      }

      if (parameterized.getRawType().equals(MemberInjector.class)) {
        // target is a member injector
        Type componentType = parameterized.getActualTypeArguments()[0];
        Class<?> rawComponentType = GenericTypeReflector.erase(componentType);
        MemberInjector<?> componentMemberInjector = this.injector.memberInjector(rawComponentType);
        return this.createStaticBinding(key, componentMemberInjector);
      }
    }

    // at this point we actually need to create a binding, which cannot be done when
    // a special binding is requested that is using a qualifier annotation
    if (key.qualifierAnnotationType().isPresent()) {
      throw new IllegalStateException("Unable to create JIT binding for key with qualifier: " + key);
    }

    // allow for injection of the current injector
    Class<?> rawTargetType = GenericTypeReflector.erase(targetType);
    if (rawTargetType.equals(Injector.class)) {
      return this.createStaticBinding(key, this.injector);
    }

    // check for @ProvidedBy
    ProvidedBy providedBy = rawTargetType.getAnnotation(ProvidedBy.class);
    if (providedBy != null) {
      Class<?> implementation = providedBy.value();
      if (targetType.equals(implementation)) {
        throw new IllegalArgumentException("@ProvidedBy: implementation " + implementation + " is the same as target");
      }

      if (!rawTargetType.isAssignableFrom(implementation)) {
        throw new IllegalArgumentException(
          "@ProvidedBy: implementation " + implementation + " is actually not an implementation of " + rawTargetType);
      }

      // get a binding for the implementation and bind construct a binding that
      // delegates to the provider of the implementation binding
      BindingKey<Object> objectKey = convertKeyUnchecked(key);
      UninstalledBinding<?> uninstalledBinding = this.injector.createBindingBuilder()
        .bind(objectKey)
        .toConstructingClass(implementation);
      return uninstalledBinding.prepareForInstallation(this.injector);
    }

    // create a binding that tries to construct the raw target type
    BindingKey<Object> objectKey = convertKeyUnchecked(key);
    UninstalledBinding<?> binding = this.injector.createBindingBuilder()
      .bind(objectKey)
      .toConstructingClass(rawTargetType);
    return binding.prepareForInstallation(this.injector);
  }

  private @NotNull InstalledBinding<?> createStaticBinding(@NotNull BindingKey<?> key, @NotNull Object wrappedValue) {
    BindingKey<Object> objectKey = convertKeyUnchecked(key);
    UninstalledBinding<?> uninstalled = this.injector.createBindingBuilder().bind(objectKey).toInstance(wrappedValue);
    return uninstalled.prepareForInstallation(this.injector);
  }
}
