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

package dev.derklaro.aerogel.util;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.ElementMatcher;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.KnownValue;
import dev.derklaro.aerogel.ScopeProvider;
import dev.derklaro.aerogel.internal.provider.BaseContextualProvider;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A collection of default scopes.
 *
 * @since 2.0
 */
@API(status = API.Status.STABLE, since = "2.0")
public final class Scopes {

  /**
   * A scope which holds the constructed value during the complete injector lifecycle.
   */
  public static final ScopeProvider SINGLETON = SingletonContextualProvider::new;

  private Scopes() {
    throw new UnsupportedOperationException();
  }

  /**
   * A contextual provider which holds the constructed value during the complete injector lifecycle.
   *
   * @since 2.0
   */
  @API(status = API.Status.INTERNAL, since = "2.0")
  private static final class SingletonContextualProvider extends BaseContextualProvider<Object> {

    private final ContextualProvider<Object> downstream;
    private final AtomicReference<KnownValue> reference = new AtomicReference<>();

    /**
     * Constructs a new singleton contextual provider instance.
     *
     * @param constructingType the type constructed by the given downstream provider.
     * @param elementMatcher   a matcher for all elements tracked by the upstream provider.
     * @param downstream       the provider which is wrapped by this scope.
     */
    public SingletonContextualProvider(
      @NotNull Type constructingType,
      @NotNull ElementMatcher elementMatcher,
      @NotNull ContextualProvider<Object> downstream
    ) {
      super(downstream.injector(), constructingType, elementMatcher);
      this.downstream = downstream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nullable Object get(@NotNull InjectionContext context) throws AerogelException {
      // check if the value was already constructed
      KnownValue knownValue = this.reference.get();
      if (knownValue != null) {
        return knownValue;
      }

      // construct the value using the downstream provider & wrap it in a value store request
      Object value = this.downstream.get(context);
      KnownValue firstOccurrence = KnownValue.of(value);
      if (this.reference.compareAndSet(null, firstOccurrence.asSecondOccurrence())) {
        return firstOccurrence;
      }

      // the value was provided while we were doing our stuff, return the old value
      knownValue = this.reference.get();
      return knownValue;
    }
  }
}
