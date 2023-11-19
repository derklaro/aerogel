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

package dev.derklaro.aerogel.internal.context;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ElementMatcher;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.context.InjectionContext;
import dev.derklaro.aerogel.context.LazyContextualProvider;
import dev.derklaro.aerogel.internal.util.Preconditions;
import java.lang.reflect.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A default implementation of a lazy contextual provider.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0", consumers = "dev.derklaro.aerogel.internal.context")
final class DefaultLazyContextualProvider implements LazyContextualProvider {

  private final Injector boundInjector;

  private final Object boundInstance;
  private final ElementMatcher elementMatcher;

  /**
   * Constructs a new lazy contextual provider instance.
   *
   * @param boundInjector  the injector to which this provider is bound, can be null.
   * @param boundInstance  the instance to which this provider is bound, can be null.
   * @param elementMatcher the matcher for the elements that can be injected from this provider.
   */
  public DefaultLazyContextualProvider(
    @Nullable Injector boundInjector,
    @Nullable Object boundInstance,
    @NotNull ElementMatcher elementMatcher
  ) {
    this.boundInjector = boundInjector;
    this.boundInstance = boundInstance;
    this.elementMatcher = elementMatcher;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector injector() {
    Preconditions.checkArgument(this.boundInjector != null, "no injector was bound to lazy provider yet");
    return this.boundInjector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Type constructingType() {
    throw new UnsupportedOperationException("unsupported on lazy provider");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull ElementMatcher elementMatcher() {
    return this.elementMatcher;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object get(@NotNull InjectionContext context) throws AerogelException {
    return this.boundInstance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object get() throws AerogelException {
    return this.boundInstance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean injectorBound() {
    return this.boundInjector != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull LazyContextualProvider withInjector(@NotNull Injector injector) {
    if (injector == this.boundInjector) {
      return this;
    } else {
      return new DefaultLazyContextualProvider(injector, this.boundInstance, this.elementMatcher);
    }
  }
}
