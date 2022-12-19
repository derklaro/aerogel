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

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.internal.BaseContextualProvider;
import java.util.function.Function;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A contextual provider implementation which uses a function to obtain the underlying value.
 *
 * @param <T> the type constructed by this provider.
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.binding.*")
public final class FunctionalContextualProvider<T> extends BaseContextualProvider<T> {

  private final Element[] trackedElements;
  private final Function<InjectionContext, T> downstream;

  /**
   * Constructs a new functional contextual provider instance.
   *
   * @param injector        the injector associated with this provider.
   * @param trackedElements the elements tracked by this provider.
   * @param downstream      the downstream function to call to obtain the underlying value.
   */
  public FunctionalContextualProvider(
    @NotNull Injector injector,
    @NotNull Element[] trackedElements,
    @NotNull Function<InjectionContext, T> downstream
  ) {
    super(injector);
    this.trackedElements = trackedElements;
    this.downstream = downstream;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable T get(@NotNull InjectionContext context) throws AerogelException {
    // get the value from the provided downstream function
    T value = this.downstream.apply(context);

    // call construct done for the result
    this.callConstructDone(context, this.trackedElements, value, true);

    // return the value
    return value;
  }
}
