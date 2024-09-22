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

package dev.derklaro.aerogel.binding;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.ScopeApplier;
import dev.derklaro.aerogel.binding.key.BindingKey;
import java.util.List;
import java.util.Optional;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A binding that is installed to an injector and ready to provide instances for injection requests. Installed bindings
 * are constructed when an {@link UninstalledBinding} is being installed in an {@link Injector}.
 *
 * @param <T> the type of values handled by this binding.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface InstalledBinding<T> {

  /**
   * Get the main binding key of this binding, that is, the key that was used when the binding build process was
   * initially started.
   *
   * @return the main binding key of this binding.
   */
  @NotNull
  BindingKey<? extends T> mainKey();

  /**
   * Get the keys of this binding. Elements that are matching one of the keys (for example constructor parameters) can
   * be provided by this binding.
   *
   * @return the keys of this binding.
   */
  @NotNull
  @Unmodifiable
  List<BindingKey<? extends T>> keys();

  /**
   * Get if the given key can be provided by this binding.
   *
   * @param key the key to test.
   * @return true if the key can be provided by this binding, false otherwise.
   */
  boolean supportsKey(@NotNull BindingKey<?> key);

  /**
   * Get the injector into which this binding was installed.
   *
   * @return the injector into which this binding was installed.
   */
  @NotNull
  Injector installedInjector();

  /**
   * Get the advanced options that were supplied to the binding.
   *
   * @return the advanced options that were supplied to the binding.
   */
  @NotNull
  BindingOptions options();

  /**
   * Get the scope that was applied to this binding.
   *
   * @return the scope that was applied to this binding.
   */
  @NotNull
  Optional<ScopeApplier> scope();

  /**
   * Get a provider that can provide a value for this binding, but also takes the current injection context into
   * account. Mainly exposed for internal use, but can be useful in some external cases as well.
   *
   * @return a provider that can provide a value for this binding and takes the current injection context into account.
   */
  @NotNull
  @API(status = API.Status.MAINTAINED, since = "3.0")
  ProviderWithContext<T> providerWithContext();

  /**
   * Get this binding without the connection to a specific injector.
   *
   * @return this binding without the connection to a specific injector.
   */
  @NotNull
  @Contract(" -> new")
  UninstalledBinding<T> asUninstalled();
}
