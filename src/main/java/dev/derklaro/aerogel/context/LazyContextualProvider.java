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

package dev.derklaro.aerogel.context;

import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Injector;
import java.lang.reflect.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

/**
 * A provider that gets access to the bound provider lazily.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface LazyContextualProvider extends ContextualProvider<Object> {

  /**
   * Get if this provider was bound to an injector.
   *
   * @return true if this provider was bound to an injector, false otherwise.
   */
  boolean injectorBound();

  /**
   * Get the underlying injector of this provider.
   *
   * @return the underlying injector of this provider.
   * @throws dev.derklaro.aerogel.AerogelException if this provider was not yet bound to an injector.
   * @see #injectorBound()
   * @see #withInjector(Injector)
   */
  @Override
  @NotNull Injector injector();

  /**
   * Copies this provider to use the given injector. This is used to override values in the current injection context.
   *
   * @param injector the injector to use for the provider.
   * @return a new contextual provider using the given injector.
   */
  @CheckReturnValue
  @NotNull LazyContextualProvider withInjector(@NotNull Injector injector);

  /**
   * Get the types that this provider constructs.
   *
   * @return the type that gets constructed by this provider.
   * @throws UnsupportedOperationException if this provider does not have the required type information present.
   */
  @Override
  @NotNull Type constructingType();

  /**
   * Creating a context builder based on a lazy provider is not allowed and will always result in an unsupported
   * operation exception. This is due to the fact that lazy providers should only be used while constructing in a
   * context, therefore the current context should be used rather than creating a new one.
   *
   * @return never.
   * @throws UnsupportedOperationException always.
   */
  @Override
  default @NotNull InjectionContext.Builder createContextBuilder() {
    throw new UnsupportedOperationException("unsupported on lazy provider");
  }
}
