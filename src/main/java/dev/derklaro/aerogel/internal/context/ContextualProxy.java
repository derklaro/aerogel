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

import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.internal.proxy.ProxyMapping;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A created proxy associated with a context. This proxy mapping contains more info which allows to resolve the context
 * which requested this proxy instance.
 * <p>
 * This proxy also holds a remove listener to execute various actions after the construction finished. Note that there
 * is no requirement for the remove listener to be called.
 *
 * @author Pasqual Koschmieder
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0")
final class ContextualProxy {

  final ProxyMapping proxyMapping;
  final InstalledBinding<?> binding;

  Runnable removeListener;
  boolean removeListenerExecuted = false;

  /**
   * Constructs a new contextual proxy.
   *
   * @param removeListener the remove listener to call when this mapping gets disconnected from a context.
   * @param proxyMapping   the actual created proxy.
   * @param binding        the provider that created this proxy mapping.
   */
  public ContextualProxy(
    @NotNull Runnable removeListener,
    @NotNull ProxyMapping proxyMapping,
    @NotNull InstalledBinding<?> binding
  ) {
    this.removeListener = removeListener;
    this.proxyMapping = proxyMapping;
    this.binding = binding;
  }

  /**
   * Sets the delegate of this proxy mapping stored in this mapping unless the delegate is already present.
   *
   * @param delegate the delegate to use for the created proxy.
   */
  public void setDelegate(@Nullable Object delegate) {
    if (!this.proxyMapping.isDelegatePresent()) {
      this.proxyMapping.setDelegate(delegate);
    }
  }

  /**
   * Executes the remove listener associated with this proxy and marks the remove listener as executed. Subsequent calls
   * to this method will have no effect
   */
  public void executeRemoveListener() {
    Runnable removeListener = this.removeListener;
    if (removeListener != null && !this.removeListenerExecuted) {
      removeListener.run();

      this.removeListener = null;
      this.removeListenerExecuted = true;
    }
  }
}
