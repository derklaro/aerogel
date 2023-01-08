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
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import java.lang.reflect.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A contextual provider which gets the injector instance lazily injected into it. For internal use during constructing
 * of instances only!
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.context")
final class LazyContextualProvider implements ContextualProvider<Object> {

  private final Object boundInstance;

  private final Type constructingType;
  private final Element[] trackedElements;

  Injector injector;

  /**
   * Constructs a new contextual provider which can get the injector lazily set.
   *
   * @param boundInstance the instance this provider should return.
   * @param boundElement  the element to which this provider is bound.
   */
  public LazyContextualProvider(
    @Nullable Object boundInstance,
    @NotNull Element boundElement
  ) {
    this.boundInstance = boundInstance;
    this.constructingType = boundElement.componentType();
    this.trackedElements = new Element[]{boundElement};
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
  public @NotNull ContextualProvider<Object> asUpstreamContext() {
    throw new UnsupportedOperationException("unsupported on this provider");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext.Builder createContextBuilder() {
    throw new UnsupportedOperationException("unsupported on this provider");
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
  public @Nullable Object get(@NotNull InjectionContext context) throws AerogelException {
    return this.boundInstance;
  }
}
