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

import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.binding.key.BindingKey;
import java.util.List;
import org.apiguardian.api.API;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

/**
 * A scope that can be applied to a provider. A scope can be used to inject a state into a provider. Default bindings
 * are created without a scope, therefore instances created by providers are create-and-forget. Once the current
 * injection context ends, the constructed instance will no longer by known by the injector. A scope can be used to
 * define a longer lifetime for those instances.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@FunctionalInterface
@API(status = API.Status.STABLE, since = "3.0")
public interface ScopeApplier {

  /**
   * Returns a new provider with this scope applied to it. If necessary, the method may obtain an instance from the
   * given provider.
   *
   * @param keys     the keys of the binding that requests the scoping.
   * @param original the original provider to apply this scope to.
   * @param <T>      the type of value returned by the provider.
   * @return a new provider that scopes the given provider.
   */
  @NotNull
  @CheckReturnValue
  <T> ProviderWithContext<T> applyScope(
    @NotNull List<BindingKey<? extends T>> keys,
    @NotNull ProviderWithContext<T> original);
}
