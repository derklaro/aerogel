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
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import java.util.Optional;
import org.apiguardian.api.API;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A binding that is aware of the target binding key, but is not yet installed to an injector.
 * <p>
 * Instances of this type of binding are returned by a {@link RootBindingBuilder} when finishing the build process. Note
 * that scopes are applied when installing the binding.
 *
 * @param <T> the type of values handled by this binding.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface UninstalledBinding<T> {

  /**
   * Get the key of this binding. Elements that are matching this key (for example constructor parameters) can be
   * provided by this binding.
   *
   * @return the key of this binding.
   */
  @NotNull
  BindingKey<T> key();

  /**
   * Get the scope that will be applied to the binding on creation.
   *
   * @return the scope that will be applied to the binding on creation.
   */
  @NotNull
  Optional<ScopeApplier> scope();

  /**
   * Get a new uninstalled binding using the given scope. Can be null to indicate that no scope should be applied.
   *
   * @param scope the scope to apply to the new binding.
   * @return a new binding that has the same characteristics as this binding, but uses the given scope.
   */
  @NotNull
  @CheckReturnValue
  UninstalledBinding<T> withScope(@Nullable ScopeApplier scope);

  /**
   * Get a binding based on this binding that is bound to the given injector.
   *
   * @param injector the injector to bind this binding to.
   * @return an installed binding that is bound to the given injector.
   */
  @NotNull
  @Contract("_ -> new")
  InstalledBinding<T> prepareForInstallation(@NotNull Injector injector);
}
