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

package dev.derklaro.aerogel.scopedvalue;

import dev.derklaro.aerogel.context.InjectionContext;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextScope;
import java.util.function.Supplier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * A context scope that uses scope value carries to set the scope during injection.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
final class ScopedValueInjectionContextScope implements InjectionContextScope {

  private final InjectionContext context;
  private final ScopedValue.Carrier carrier;
  private final ScopedValue<InjectionContextScope> scopeScopedValue;

  /**
   * Constructs a new injection context scope for the given context and scoped value.
   *
   * @param context          the context that is bound to this scope.
   * @param scopeScopedValue the scoped value for the injection scopes.
   */
  public ScopedValueInjectionContextScope(
    @NotNull InjectionContext context,
    @NotNull ScopedValue<InjectionContextScope> scopeScopedValue
  ) {
    this.context = context;
    this.scopeScopedValue = scopeScopedValue;
    this.carrier = ScopedValue.where(scopeScopedValue, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext context() {
    return this.context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T executeScoped(@NotNull Supplier<T> operation) {
    InjectionContextScope currentScope = this.scopeScopedValue.orElse(null);
    if (currentScope == null || currentScope.context().obsolete()) {
      // there is either no context currently bound or the current bound snapshot is obsolete
      // we're using our own carrier with the mapping to our context for further actions
      return this.carrier.get(operation);
    } else {
      // the current scope is still valid, use that one
      return operation.get();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T forceExecuteScoped(@NotNull Supplier<T> operation) {
    return this.carrier.get(operation);
  }
}
