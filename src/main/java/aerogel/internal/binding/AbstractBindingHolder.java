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

import aerogel.BindingHolder;
import aerogel.Element;
import aerogel.InjectionContext;
import aerogel.Injector;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractBindingHolder implements BindingHolder {

  protected final Injector injector;
  protected final Element targetType;
  protected final Element bindingType;

  public AbstractBindingHolder(@NotNull Element type, @NotNull Element binding, @NotNull Injector injector) {
    this.targetType = Objects.requireNonNull(type, "Target type is required to construct");
    this.bindingType = Objects.requireNonNull(binding, "Binding type is required to construct");
    this.injector = Objects.requireNonNull(injector, "The parent injector is required to construct");
  }

  @Override
  public @NotNull Injector injector() {
    return this.injector;
  }

  @Override
  public @NotNull Element type() {
    return this.targetType;
  }

  @Override
  public @NotNull Element binding() {
    return this.bindingType;
  }

  @Override
  public @Nullable Object get() {
    return this.get(InjectionContext.builder().injector(this.injector).build());
  }
}
