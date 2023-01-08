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

package dev.derklaro.aerogel;

import java.lang.reflect.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A provider which handles a scope annotation. Each scope provider is applied one by one to a created context provider
 * allowing the provider to wrap the result of an underlying scope or provider.
 *
 * <p>For example a singleton scope provider would return a contextual provider which calls the downstream provider
 * given to the apply method if no value was provided before or return the value which was already provided.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@FunctionalInterface
@API(status = API.Status.STABLE, since = "2.0")
public interface ScopeProvider {

  /**
   * Applies the scope that is represented by this provider to the given downstream contextual provider. The method can
   * decide to either wrap the given provider or return the same provider in case no scoping must be applied. This
   * method should never return a scope which isn't interacting with the given downstream provider.
   *
   * @param constructingType the type constructed by the given downstream provider.
   * @param trackedElements  the elements which are tracked by the given downstream provider.
   * @param downstream       the last constructed provider, might either be the root or a scoped provider.
   * @return a wrapped provider which applies the current scope, or the same provider if no scoping is required.
   */
  @NotNull ContextualProvider<Object> applyScope(
    @NotNull Type constructingType,
    @NotNull Element[] trackedElements,
    @NotNull ContextualProvider<Object> downstream);
}
