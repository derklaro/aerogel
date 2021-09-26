/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
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

package aerogel.internal.binding;

import aerogel.Element;
import aerogel.InjectionContext;
import aerogel.Injector;
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

  /**
   * Constructs a new immediate binding holder instance.
   *
   * @param targetType  the type of the binding.
   * @param bindingType the type to which the given type is bound.
   * @param injector    the injector to which this binding was bound.
   * @param result      the result which should always get returned.
   */
  public ImmediateBindingHolder(
    @NotNull Element targetType,
    @NotNull Element bindingType,
    @NotNull Injector injector,
    @Nullable Object result
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
    context.constructDone(this.targetType, this.result, true);
    context.constructDone(this.bindingType, this.result, false);
    // return
    return (T) this.result;
  }
}
