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

package dev.derklaro.aerogel.binding.builder;

import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import jakarta.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * The last step of the binding construction process. It allows the caller to target a binding to an implementation, a
 * factory method, a specific constructor or a provider. The returned uninstalled binding can be installed in an
 * injector. After targeting the builder, it can be re-used to target the same binding configuration to a different
 * implementation (changes made to the builder do not reflect into the binding).
 *
 * @param <T> the type of values handled by this binding that is being built.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface TargetableBindingBuilder<T> {

  /**
   * Constructs a new binding that returns the given instance exclusively. When the returned binding is first installed
   * into an injector, the member fields and method are injected. Note: each time the given instance is provided to a
   * builder, the member injection will be executed.
   *
   * @param instance the singleton instance to return when the binding is called.
   * @return a new uninstalled binding that returns the given instance exclusively on resolve requests.
   */
  @NotNull
  UninstalledBinding<T> toInstance(@NotNull T instance);

  /**
   * Constructs a new binding that uses the given factory method to construct the return value. A factory method is a
   * static method whose return type must match the type being bound. If the method is hidden from the injector (for
   * example in a module), make sure to provide a lookup to this builder that is able to access the method. If no scopes
   * and qualifiers are provided to this builder, the lookup uses 2 steps to resolve them:
   * <ol>
   *   <li>Look directly at the method for scope and qualifier annotations.
   *   <li>Look at the raw return type for scope and qualifier annotations.
   * </ol>
   *
   * @param factoryMethod the factory method to call to resolve the binding return value.
   * @return a new uninstalled binding that uses the given factory method to resolve the return value.
   * @throws IllegalArgumentException if the given factory method does not match the constraints described above.
   */
  @NotNull
  UninstalledBinding<T> toFactoryMethod(@NotNull Method factoryMethod);

  /**
   * Constructs a new binding that calls the given provider to construct the return value of the binding. Scopes and
   * qualifiers are only resolved from this builder, or if missing, from the return type of the binding. No methods and
   * fields are injected in the given provider instance. If injection is required, use {@link #toProvider(Class)}
   * instead.
   *
   * @param provider the provider to call to get the return value of the returned binding.
   * @return a new uninstalled binding that calls the given provider to get the return value for the binding.
   */
  @NotNull
  UninstalledBinding<T> toProvider(@NotNull Provider<? extends T> provider);

  /**
   * Constructs a new binding that calls the provider constructed from the given provider type to construct the return
   * value for the binding. The provider implementation is constructed on each construction request, unless scoped
   * otherwise. Method and field injection will be executed accordingly. Scopes and qualifiers are only resolved from
   * this builder, or if missing, from the return type of the binding.
   *
   * @param providerType the provider type to use to construct the return value for the binding.
   * @return a new binding that uses the provider constructed from the given type to get the binding return value.
   * @throws IllegalArgumentException if no injectable constructor is present in the given provider type.
   */
  @NotNull
  UninstalledBinding<T> toProvider(@NotNull Class<? extends Provider<? extends T>> providerType);

  /**
   * Constructs a new binding that calls the given provider to construct the return value of the binding. Scopes and
   * qualifiers are only resolved from this builder, or if missing, from the return type of the binding. No methods and
   * fields are injected in the given provider instance.
   *
   * @param provider the provider to call to get the return value of the returned binding.
   * @return a new uninstalled binding that calls the given provider to get the return value for the binding.
   */
  @NotNull
  UninstalledBinding<T> toProvider(@NotNull ProviderWithContext<? extends T> provider);

  /**
   * Constructs a new binding that calls the given constructor to get the return value of the binding. The given
   * constructor is called everytime unless otherwise scoped. Scopes and qualifiers are resolved in the following
   * order:
   * <ol>
   *   <li>specifically provided to this builder
   *   <li>scopes and qualifiers on the type that is being implemented.
   *   <li>scopes and qualifiers provided to the class that defines the given constructor.
   * </ol>
   *
   * @param constructor the constructor to call to get the return value for the binding.
   * @return a new binding that calls the given constructor to get the return value for the binding.
   * @throws IllegalArgumentException if the given constructor is inaccessible to the injector.
   */
  @NotNull
  UninstalledBinding<T> toConstructor(@NotNull Constructor<? extends T> constructor);

  /**
   * Constructs a new binding that calls the injectable constructor in the given implementation type. Make sure to
   * provide a lookup in case the constructor is inaccessible to the injector, for example if the type is a module. An
   * injectable constructor is resolved in the following order:
   * <ol>
   *   <li>The constructor that is annotated with {@code @Inject}.
   *   <li>On Java 14+: the all-args constructor on record classes.
   *   <li>The constructor that takes no arguments.
   * </ol>
   * Scopes and qualifiers are resolved in the following order:
   * <ol>
   *   <li>specifically provided to this builder
   *   <li>scopes and qualifiers on the type that is being implemented.
   *   <li>scopes and qualifiers provided to the given class.
   * </ol>
   *
   * @param implementationType the type to construct when an injection request is made to the binding.
   * @return a new binding that calls the injectable constructor defined in the given class to get the return value.
   * @throws IllegalArgumentException if no accessible, injectable constructor is present in the given class.
   * @see #toConstructor(Constructor)
   */
  @NotNull
  UninstalledBinding<T> toConstructingClass(@NotNull Class<? extends T> implementationType);

  /**
   * Constructs a new binding that calls the injectable constructor in the class that is currently targeted by this
   * binding builder. The raw target type of the underlying binding key is used to resolve the class which should be
   * constructed.
   *
   * @return a new binding that calls the injectable constructor defined in the target class of this builder.
   * @throws IllegalArgumentException if no accessible, injectable constructor is present in the target class.
   * @see #toConstructingClass(Class)
   */
  @NotNull
  UninstalledBinding<T> toConstructingSelf();
}
