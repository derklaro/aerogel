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

package dev.derklaro.aerogel.internal.binding.defaults;

import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.binding.BindingHolder;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * The default implementation of a binding holder.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.*")
final class DefaultBindingHolder implements BindingHolder {

  private final Injector injector;
  private final Element[] types;
  private final ContextualProvider<Object> provider;

  /**
   * Constructs a new default binding holder instance.
   *
   * @param injector the injector associated with the binding.
   * @param types    the types handled by this holder.
   * @param provider the provider to use to obtain the underlying value.
   */
  public DefaultBindingHolder(
    @NotNull Injector injector,
    @NotNull Element[] types,
    @NotNull ContextualProvider<Object> provider
  ) {
    this.injector = injector;
    this.types = types;
    this.provider = provider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector injector() {
    return this.injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element[] types() {
    return this.types;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull ContextualProvider<Object> provider() {
    return this.provider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext.Builder createContextBuilder() {
    return this.provider.createContextBuilder();
  }
}
