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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.InjectorImpl;
import dev.derklaro.aerogel.registry.Registry;
import io.leangen.geantyref.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.Optional;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main part of aerogel. The injector keeps track of all known bindings and shared them with their child injectors.
 * Every injector has always a binding for itself as long as the injector element was not overridden with another
 * biding. In normal cases a developer only interacts once with this injector - to bind all elements he will need later
 * and then to create the main instance of his application. From this point every injection should be done and the main
 * class constructed so that the application can get started. Create a new injector by using {@link #newInjector()}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface Injector {

  @Contract(pure = true)
  static @NotNull Injector newInjector() {
    return new InjectorImpl();
  }

  @NotNull
  Optional<Injector> parentInjector();

  @NotNull
  @Contract(" -> new")
  Injector createChildInjector();

  @NotNull
  @Contract(" -> new")
  RootBindingBuilder createBindingBuilder();

  @NotNull
  <T> MemberInjector<T> memberInjector(@NotNull Class<T> memberHolderClass);

  @NotNull
  <T> MemberInjector<T> memberInjector(@NotNull Class<T> memberHolderClass, @Nullable MethodHandles.Lookup lookup);

  <T> T instance(@NotNull Class<T> type);

  <T> T instance(@NotNull Type type);

  <T> T instance(@NotNull TypeToken<T> typeToken);

  <T> T instance(@NotNull BindingKey<T> key);

  @NotNull
  <T> InstalledBinding<T> binding(@NotNull BindingKey<T> key);

  @NotNull
  <T> Optional<InstalledBinding<T>> existingBinding(@NotNull BindingKey<T> key);

  @NotNull
  Injector installBinding(@NotNull DynamicBinding binding);

  @NotNull
  <T> Injector installBinding(@NotNull UninstalledBinding<T> binding);

  @NotNull
  Registry.WithKeyMapping<BindingKey<?>, InstalledBinding<?>> bindingRegistry();

  @NotNull
  Registry.WithoutKeyMapping<BindingKey<?>, DynamicBinding> dynamicBindingRegistry();

  @NotNull
  Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry();
}
