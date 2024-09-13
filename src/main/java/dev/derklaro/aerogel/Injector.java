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
import jakarta.inject.Provider;
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

  /**
   * Constructs a new, empty injector with default settings applied.
   *
   * @return a new, empty injector.
   */
  @Contract(value = " -> new", pure = true)
  static @NotNull Injector newInjector() {
    return new InjectorImpl();
  }

  /**
   * Creates a new builder to customize the creation of the new injector.
   *
   * @return a new injector builder.
   */
  @Contract(value = " -> new", pure = true)
  static @NotNull InjectorBuilder builder() {
    return new InjectorBuilderImpl();
  }

  /**
   * Get the parent injector of this injector. Empty if this injector is a root injector and has no parent.
   *
   * @return the parent injector of this injector.
   */
  @NotNull
  Optional<Injector> parentInjector();

  /**
   * Creates a new child injector of this injector. Child injectors have access to all settings, scopes, bindings etc.
   * of this injector. When, for example, a binding needs to be looked up a child injector first tries to find it in the
   * local registry and then consults the registry of the parent injector to check if the binding is present there. This
   * process is repeated until the root injector is reached.
   *
   * @return a new child injector with this injector as its parent.
   */
  @NotNull
  @Contract(" -> new")
  Injector createChildInjector();

  /**
   * Creates a new targeted injector based on this injector. Targeted injectors have a limited set of bindings that they
   * can access, everything else is resolved from the parent injector.
   *
   * @return a mew builder for a targeted injector.
   */
  @NotNull
  @Contract(" -> new")
  TargetedInjectorBuilder createTargetedInjectorBuilder();

  /**
   * Creates a new binding builder based on the settings of this injector. Note that there is no requirement to install
   * bindings created by a builder from this injector into this injector.
   *
   * @return a new builder for a binding.
   */
  @NotNull
  @Contract(" -> new")
  RootBindingBuilder createBindingBuilder();

  /**
   * Get a member injector for the given member holder class. This method uses the lookup provided to this injector when
   * building it, use {@link #memberInjector(Class, MethodHandles.Lookup)} to provide a specific member injector.
   * Calling this method multiple times on this injector will always return the same member injector as the result is
   * cached to prevent expensive lookups.
   *
   * @param memberHolderClass the class in which the members to inject are located.
   * @param <T>               the type to inject members into.
   * @return a new or cached member injector for the given member holder class.
   * @throws NullPointerException if the given member holder class is null.
   */
  @NotNull
  <T> MemberInjector<T> memberInjector(@NotNull Class<T> memberHolderClass);

  /**
   * Get a member injector for the given member holder class. The given lookup can be used when getting the member
   * injector for the first time to allow access into the class. If the lookup is {@code null} the default lookup
   * provided to this injector will be used instead. Calling this method multiple times on this injector will always
   * return the same member injector even if given different lookups as the result is cached to prevent expensive member
   * lookups.
   *
   * @param memberHolderClass the class in which the members to inject are located.
   * @param lookup            the lookup to use for member access in the target class, can be null.
   * @param <T>               the type to inject members into.
   * @return a new or cached member injector for the given member holder class.
   * @throws NullPointerException if the given member holder class is null.
   */
  @NotNull
  <T> MemberInjector<T> memberInjector(@NotNull Class<T> memberHolderClass, @Nullable MethodHandles.Lookup lookup);

  /**
   * Get an instance of the given type from a binding known to this injector. When possible, a direct method call should
   * be avoided in favor of injecting the required dependencies.
   *
   * @param type the type to get the constructed instance of.
   * @param <T>  the model of the type to get.
   * @return an instance of the given type from a binding known to this injector.
   * @throws NullPointerException     if the given type is null.
   * @throws IllegalStateException    if a JIT binding couldn't be created for the type.
   * @throws IllegalArgumentException if a JIT binding creation fails due to a user configuration issue.
   */
  <T> T instance(@NotNull Class<T> type);

  /**
   * Get an instance of the given type from a binding known to this injector. When possible, a direct method call should
   * be avoided in favor of injecting the required dependencies.
   *
   * @param type the type to get the constructed instance of.
   * @param <T>  the model of the type to get.
   * @return an instance of the given type from a binding known to this injector.
   * @throws NullPointerException     if the given type is null.
   * @throws IllegalStateException    if a JIT binding couldn't be created for the type.
   * @throws IllegalArgumentException if a JIT binding creation fails due to a user configuration issue.
   */
  <T> T instance(@NotNull Type type);

  /**
   * Get an instance of the given type from a binding known to this injector. When possible, a direct method call should
   * be avoided in favor of injecting the required dependencies.
   *
   * @param typeToken the type token of the type to construct.
   * @param <T>       the model of the type to get.
   * @return an instance of the given type from a binding known to this injector.
   * @throws NullPointerException     if the given type token is null.
   * @throws IllegalStateException    if a JIT binding couldn't be created for the type.
   * @throws IllegalArgumentException if a JIT binding creation fails due to a user configuration issue.
   */
  <T> T instance(@NotNull TypeToken<T> typeToken);

  /**
   * Get an instance of the given key from a binding known to this injector. When possible, a direct method call should
   * be avoided in favor of injecting the required dependencies.
   *
   * @param key the binding key to get an instance of.
   * @param <T> the model of the type to get.
   * @return an instance of the given key from a binding known to this injector.
   * @throws NullPointerException     if the given key is null.
   * @throws IllegalStateException    if a JIT binding couldn't be created for the type.
   * @throws IllegalArgumentException if a JIT binding creation fails due to a user configuration issue.
   */
  <T> T instance(@NotNull BindingKey<T> key);

  /**
   * Get a provider for the binding associated with the given key. The provider can be used to obtain instances of the
   * type wrapped in the given binding key.When possible, a direct method call should be avoided in favor of injecting a
   * provider directly.
   *
   * @param key the key of the binding to get a provider for.
   * @param <T> the model of the type to construct using the provider.
   * @return a provider to construct instances of the type wrapped in the given key.
   * @throws NullPointerException     if the given key is null.
   * @throws IllegalStateException    if a JIT binding couldn't be created for the type.
   * @throws IllegalArgumentException if a JIT binding creation fails due to a user configuration issue.
   */
  @NotNull
  <T> Provider<T> provider(@NotNull BindingKey<T> key);

  /**
   * Gets a binding that is known to this injector tree for the given key or tries to create a JIT binding if no such
   * binding was registered before this method call. This method takes the provided JIT rules into account.
   *
   * @param key the key to get the binding of.
   * @param <T> the model of the type to get.
   * @return a registered or jit created binding for the given key.
   * @throws NullPointerException     if the given key is null.
   * @throws IllegalStateException    if a JIT binding couldn't be created for the type.
   * @throws IllegalArgumentException if a JIT binding creation fails due to a user configuration issue.
   */
  @NotNull
  <T> InstalledBinding<T> binding(@NotNull BindingKey<T> key);

  /**
   * Gets a binding that is known to this injector tree for the given key. If no such binding was registered before this
   * method call, an empty optional is returned instead.
   *
   * @param key the key to get the binding of.
   * @param <T> the model of the type to get.
   * @return a registered or jit created binding for the given key.
   * @throws NullPointerException if the given key is null.
   */
  @NotNull
  <T> Optional<InstalledBinding<T>> existingBinding(@NotNull BindingKey<T> key);

  /**
   * Installs the given dynamic binding into this injector, making it available as a binding for all future injection
   * calls. Each binding can only be installed once per injector.
   *
   * @param binding the dynamic binding to install into this injector.
   * @return this injector, for chaining.
   * @throws NullPointerException          if the given binding is null.
   * @throws IllegalArgumentException      if the given binding is already registered in this injector.
   * @throws UnsupportedOperationException if this injector does not support registering bindings after construction.
   */
  @NotNull
  Injector installBinding(@NotNull DynamicBinding binding);

  /**
   * Installs the given uninstalled binding into this injector, making it available as a binding for all future
   * injection calls. There can only be one binding for a binding key registered in an injector.
   *
   * @param binding the binding to install into this injector.
   * @param <T>     the type of values handled by the given binding.
   * @return this injector, for chaining.
   * @throws NullPointerException          if the given binding is null.
   * @throws IllegalArgumentException      if a binding for the key used by the given binding already exists.
   * @throws UnsupportedOperationException if this injector does not support registering bindings after construction.
   */
  @NotNull
  <T> Injector installBinding(@NotNull UninstalledBinding<T> binding);

  /**
   * Get the binding registry of this injector. The registry can be used to get additional insight into registered
   * bindings and allows, for example, to unregister bindings. Note that an injector implementation might return a
   * frozen registry which does not allow for modification.
   *
   * @return the binding registry used by this injector.
   */
  @NotNull
  Registry.WithKeyMapping<BindingKey<?>, InstalledBinding<?>> bindingRegistry();

  /**
   * Get the dynamic binding registry of this injector. The registry can be used to get additional insight into
   * registered bindings and allows, for example, to unregister bindings. Note that an injector implementation might
   * return a frozen registry which does not allow for modification.
   *
   * @return the dynamic binding registry used by this injector.
   */
  @NotNull
  Registry.WithoutKeyMapping<BindingKey<?>, DynamicBinding> dynamicBindingRegistry();

  /**
   * Get the scope registry of this injector. The registry can be used to get additional insight into registered scopes,
   * as well as allowing to register and unregister scopes. Note that an injector implementation might return a frozen
   * registry which does not allow for modification.
   *
   * @return the scope registry used by this injector.
   */
  @NotNull
  Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry();
}
