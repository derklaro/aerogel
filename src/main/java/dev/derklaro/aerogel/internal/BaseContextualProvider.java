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

package dev.derklaro.aerogel.internal;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.internal.context.util.ContextInstanceResolveHelper;
import java.lang.reflect.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract implementation of a contextual provider which simplifies the use of providers for downstream api
 * consumers. This provider implementation ensures that:
 * <ol>
 *   <li>calls to the {@link #get()} method are always executed within an injection context.
 *   <li>no calls to construct done are made when this provider is wrapped by a downstream provider.
 * </ol>
 *
 * @param <T> the type constructed by this provider.
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.*")
public abstract class BaseContextualProvider<T> implements ContextualProvider<T> {

  protected final Injector injector;
  protected final Type constructingType;
  protected final Element[] trackedElements;

  /**
   * Constructs a new base contextual provider instance.
   *
   * @param injector         the injector associated with this provider.
   * @param constructingType the type constructed by this provider.
   * @param trackedElements  the tracked elements of this provider.
   */
  protected BaseContextualProvider(
    @NotNull Injector injector,
    @NotNull Type constructingType,
    @NotNull Element[] trackedElements
  ) {
    this.injector = injector;
    this.constructingType = constructingType;
    this.trackedElements = trackedElements;
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
  public @NotNull Type constructingType() {
    return this.constructingType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element[] trackedElements() {
    return this.trackedElements;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext.Builder createContextBuilder() {
    return InjectionContext.builder(this.constructingType, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @Nullable T get() throws AerogelException {
    return (T) ContextInstanceResolveHelper.resolveInstance(this.constructingType, this);
  }
}
