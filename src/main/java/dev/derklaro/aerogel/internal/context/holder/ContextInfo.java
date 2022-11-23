/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 Pasqual K. and contributors
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

package dev.derklaro.aerogel.internal.context.holder;

import dev.derklaro.aerogel.InjectionContext;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * Holds information about an injection context associated with the amount of times it got claimed.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.context.holder")
final class ContextInfo {

  private int claims;
  private final InjectionContext context;

  /**
   * Constructs a new context information for the given context with 0 claims.
   *
   * @param context the context to create the info for.
   */
  public ContextInfo(@NotNull InjectionContext context) {
    this.claims = 0;
    this.context = context;
  }

  /**
   * Claims this context by one.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  public @NotNull ContextInfo claim() {
    this.claims++;
    return this;
  }

  /**
   * Releases this context by one.
   *
   * @return true if the context is fully released as a result of this method, false otherwise.
   */
  public boolean release() {
    this.claims--;
    return this.claims == 0;
  }

  /**
   * Get the underlying context of this information.
   *
   * @return the underlying context.
   */
  public @NotNull InjectionContext context() {
    return this.context;
  }
}
