/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
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

package aerogel.internal.context;

import aerogel.Element;
import aerogel.InjectionContext;
import aerogel.InjectionContext.Builder;
import aerogel.Injector;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A default {@link InjectionContext.Builder} implementation.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class DefaultInjectionContextBuilder implements Builder {

  private final Map<Element, Object> overriddenElements = new ConcurrentHashMap<>();

  private Injector parentInjector;

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext.Builder injector(@NotNull Injector injector) {
    this.parentInjector = Objects.requireNonNull(injector, "Injector must be non-null");
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull <T> InjectionContext.Builder override(@NotNull Element element, @Nullable T instance) {
    this.overriddenElements.put(
      Objects.requireNonNull(element, "Element must be non-null"),
      instance == null ? InjectionContext.NIL : instance);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext build() {
    // ensure that the necessary information are provided
    Objects.requireNonNull(this.parentInjector, "Parent injector must be given before building");
    // create the instance
    return new DefaultInjectionContext(this.parentInjector, this.overriddenElements);
  }
}
