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

import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.ElementMatcher;
import dev.derklaro.aerogel.InjectionContext;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A default {@link InjectionContext.Builder} implementation.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal")
public final class DefaultInjectionContextBuilder implements InjectionContext.Builder {

  private final List<LazyContextualProvider> overriddenInstances = new LinkedList<>();

  private final Type constructingType;
  private final ContextualProvider<?> callingProvider;

  /**
   * Constructs a new injection context builder.
   *
   * @param constructingType the type that should be constructed.
   * @param callingProvider  the provider for which the context should get created.
   */
  public DefaultInjectionContextBuilder(
    @NotNull Type constructingType,
    @NotNull ContextualProvider<?> callingProvider
  ) {
    this.constructingType = constructingType;
    this.callingProvider = callingProvider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> InjectionContext.@NotNull Builder override(@NotNull Type type, @Nullable T instance) {
    return this.override(Element.forType(type), instance);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> InjectionContext.@NotNull Builder override(@NotNull Element element, @Nullable T instance) {
    ElementMatcher elementMatcher = ElementMatcher.matchesOne(element);
    return this.override(elementMatcher, instance);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> InjectionContext.@NotNull Builder override(@NotNull ElementMatcher elementMatcher, @Nullable T instance) {
    this.overriddenInstances.add(new LazyContextualProvider(instance, elementMatcher));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext build() {
    return new DefaultInjectionContext(this.callingProvider, this.constructingType, this.overriddenInstances);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext enterLocal() {
    return InjectionContextProvider.enterRootContext(
      this.callingProvider,
      this.constructingType,
      this.overriddenInstances);
  }
}
