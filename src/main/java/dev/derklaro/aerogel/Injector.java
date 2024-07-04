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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.injector.InjectorBuilderImpl;
import dev.derklaro.aerogel.internal.injector.InjectorImpl;
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
 * An injector is the main entry point for dependency injection with aerogel. Each injector holds a set of bindings,
 * which are used to resolve class instances during injection. An injector doesn't need bindings for all types that
 * should be injected. If a binding is needed for a type without an explicit binding, a new binding is created on the
 * fly.
 * <p>
 * Injectors can be used in a tree structure by creating a child from an injector. Each injector can have an unlimited
 * amount of child injectors. The power of child injectors comes from the fact that each of them can access the bindings
 * from all parents, but also have their on bindings. Therefore, specific overrides can be made in a child injector,
 * that do not reflect into the parent injector(s). However, as explained before, changes made to one of the parent
 * injectors will reflect into the child injectors as they are able to retrieve the newly registered value.
 * <p>
 * Take the following example how the relation between child and parent injectors work:
 * <pre>
 * {@code
 * void main() {
 *   var parentInjector = Injector.newInjector();
 *   var childInjector = parentInjector.createChildInjector();
 *
 *   // installs a binding for a string into the parent injector
 *   var helloWorldBinding = parentInjector.createBindingBuilder()
 *     .bind(String.class)
 *     .toInstance("Hello World");
 *   injector.install(helloWorldBinding);
 *
 *   // retrieve the value for the string from the child injector, which
 *   // will be the exact value registered in the parent injector - the child
 *   // gets the value from the parent as no local override is registered
 *   // in the child injector
 *   var stringValue = childInjector.instance(String.class);
 *   assertEquals("Hello World", stringValue);
 * }
 * }
 * </pre>
 * <p>
 * On the other hand there are targeted injectors. This type of injector has specific bindings present that were set
 * during construction once and can never change. When an attempt is made to register something in that type of
 * injector, the call is actually delegated to the parent injector until an injector in the tree is found that is not
 * targeted. So, as a general rule of thumb: a targeted injector is an injector that can return bindings, but has no
 * registered bindings itself. Jit bindings created from the injector will have access to the bindings specifically
 * registered in the targeted injector.
 * <p>
 * As previously stated, everything that can be registered into an injector is shared with all child injector
 * automatically, but can be overridden on a per-child basis. There is one exception from this: the injector options
 * that are supplied to the root injector are always used for all child injectors and cannot be overridden. Options for
 * an injector can be defined using an injector builder which can be obtained by {@link #builder()}.
 *
 * @author Pasqual Koschmieder
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface Injector {

  @Contract(pure = true)
  static @NotNull Injector newInjector() {
    return new InjectorImpl();
  }

  @Contract(pure = true)
  static @NotNull InjectorBuilder builder() {
    return new InjectorBuilderImpl();
  }

  @NotNull
  Optional<Injector> parentInjector();

  @NotNull
  @Contract(" -> new")
  Injector createChildInjector();

  @NotNull
  @Contract(" -> new")
  TargetedInjectorBuilder createTargetedInjectorBuilder();

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
