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

package dev.derklaro.aerogel.internal.proxy;

import dev.derklaro.aerogel.internal.util.NullMask;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the mapping between a proxy, and it's delegating invocation handler which can accept a delegating value.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0")
public final class ProxyMapping {

  private final Object proxy;
  private final InjectionTimeProxy.DelegatingInvocationHandler invocationHandler;

  /**
   * Constructs a new proxy mapping.
   *
   * @param proxy   the constructed proxy instance.
   * @param handler the delegating invocation handler associated with the proxy.
   */
  ProxyMapping(@NotNull Object proxy, @NotNull InjectionTimeProxy.DelegatingInvocationHandler handler) {
    this.proxy = proxy;
    this.invocationHandler = handler;
  }

  /**
   * Get the underlying proxy instance.
   *
   * @return the underlying proxy instance.
   */
  public @NotNull Object proxy() {
    return this.proxy;
  }

  public void setDelegate(@Nullable Object delegate) {
    if (this.invocationHandler.delegate != null) {
      throw new IllegalStateException("delegate for proxy already present");
    }

    this.invocationHandler.delegate = NullMask.mask(delegate);
  }

  public boolean isDelegatePresent() {
    return this.invocationHandler.delegate != null;
  }
}
