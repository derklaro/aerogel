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

package dev.derklaro.aerogel.internal.binding;

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A binding holder which always returns the same instance of an object.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class ImmediateBindingHolder extends AbstractBindingHolder {

  private final Object result;
  private volatile boolean didMemberInjection;

  /**
   * Constructs a new immediate binding holder instance.
   *
   * @param targetType  the type of the binding.
   * @param bindingType the type to which the given type is bound.
   * @param injector    the injector to which this binding was bound.
   * @param result      the result which should always get returned.
   */
  public ImmediateBindingHolder(
    @NotNull Element bindingType,
    @NotNull Injector injector,
    @Nullable Object result,
    @NotNull Element... targetType
  ) {
    super(targetType, bindingType, injector);
    this.result = result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T get(@NotNull InjectionContext context) {
    // just notify that this is done
    for (int i = 0; i < this.targetType.length; i++) {
      context.constructDone(this.targetType[i], this.result, i == 0 && !this.didMemberInjection);
    }
    context.constructDone(this.bindingType, this.result, false);
    // mark that we did member injection
    this.didMemberInjection = true;
    // return
    return (T) this.result;
  }
}
