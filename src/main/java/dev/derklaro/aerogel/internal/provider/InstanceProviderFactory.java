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

package dev.derklaro.aerogel.internal.provider;

import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InstanceProviderFactory<T> implements ProviderFactory<T> {

  private final T instance;

  private InstanceProviderFactory(@Nullable T instance) {
    this.instance = instance;
  }

  public static @NotNull <T> InstanceProviderFactory<T> ofInstance(@Nullable T instance) {
    return new InstanceProviderFactory<>(instance);
  }

  @Override
  public @NotNull ProviderWithContext<T> constructProvider() {
    return new InstanceProvider<>(this.instance);
  }

  private static final class InstanceProvider<T> implements ProviderWithContext<T> {

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<InstanceProvider, Boolean> MEMBERS_INJECTED_UPDATER
      = AtomicReferenceFieldUpdater.newUpdater(InstanceProvider.class, Boolean.class, "membersInjected");

    private final T instance;
    private volatile Boolean membersInjected;

    public InstanceProvider(@Nullable T instance) {
      this.instance = instance;
      this.membersInjected = Boolean.FALSE;
    }

    @Override
    public @Nullable T get(@NotNull InjectionContext context) {
      if (this.membersInjected) {
        return this.instance;
      }

      // prevent possible duplicate member injections due to concurrent calls to get()
      if (MEMBERS_INJECTED_UPDATER.compareAndSet(this, Boolean.FALSE, Boolean.TRUE)) {
        context.requestMemberInjectionSameBinding(this.instance);
      }

      return this.instance;
    }

    @Override
    public @NotNull String toString() {
      return "Fixed(" + this.instance + ")";
    }
  }
}
