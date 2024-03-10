/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2024 Pasqual K. and contributors
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

package dev.derklaro.aerogel.internal.scope;

import dev.derklaro.aerogel.ScopeApplier;
import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import dev.derklaro.aerogel.internal.util.NullMask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SingletonScopeApplier implements ScopeApplier {

  @Override
  public @NotNull <T> ProviderWithContext<T> applyScope(
    @NotNull BindingKey<T> key,
    @NotNull ProviderWithContext<T> original
  ) {
    return new SingletonScopeProvider<>(original);
  }

  private static final class SingletonScopeProvider<T> implements ProviderWithContext<T> {

    private final Lock lock;
    private final ProviderWithContext<T> delegate;

    private volatile Object singletonMasked;

    public SingletonScopeProvider(@NotNull ProviderWithContext<T> delegate) {
      this.delegate = delegate;
      this.lock = new ReentrantLock();
    }

    @Override
    public @Nullable T get(@NotNull InjectionContext context) {
      T value = this.getOrConstruct(context);
      context.delegateToContextualSingleton(value);
      return value;
    }

    private @Nullable T getOrConstruct(@NotNull InjectionContext context) {
      // check if the value was already constructed
      Object constructed = this.singletonMasked;
      if (constructed != null) {
        return this.unmask(constructed);
      }

      this.lock.lock();
      try {
        // doubly checked locking - check if the value is constructed now
        constructed = this.singletonMasked;
        if (constructed != null) {
          return this.unmask(constructed);
        }

        // still not constructed, construct now
        T constructedFromDelegate = this.delegate.get(context);
        this.singletonMasked = NullMask.mask(constructedFromDelegate);
        return constructedFromDelegate;
      } finally {
        this.lock.unlock();
      }
    }

    @SuppressWarnings("unchecked")
    private @Nullable T unmask(@NotNull Object val) {
      return (T) NullMask.unmask(val);
    }

    @Override
    public @NotNull String toString() {
      return "Singleton(" + this.delegate + ")";
    }
  }
}
