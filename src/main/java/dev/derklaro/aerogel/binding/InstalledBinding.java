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
import jakarta.inject.Provider;
import java.util.Optional;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A binding that is installed to an injector and ready to provide instances for injection requests. Instances should be
 * obtained by using the {@link Provider} returned by {@link #provider()}. Note that instances are always returned based
 * on the current construction context, therefore the value returned by the given provider might vary depending on the
 * context.
 * <p>
 * Installed bindings are constructed when an {@link UninstalledBinding} is being installed in an {@link Injector}.
 *
 * @param <T> the type of values handled by this binding.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface InstalledBinding<T> {

  /**
   * Get the key of this binding. Elements that are matching this key (for example constructor parameters) can be
   * provided by this binding.
   *
   * @return the key of this binding.
   */
  @NotNull
  BindingKey<T> key();

  /**
   * Get the injector to which this binding is bound.
   *
   * @return the injector to which this binding is bound.
   */
  @NotNull
  Injector injector();

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
   * Get a provider that can provide a value for this binding.
   *
   * @return a provider that can provide a value for this binding.
   */
  @NotNull
  Provider<T> provider();

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
