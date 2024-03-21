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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.binding.UninstalledBinding;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A builder for an injector which only has very specific bindings present, and does not allow binding registration
 * during runtime. This means that bindings created by a targeted injector have access to specific bindings, but are
 * registered into the parent injector.
 * <p>
 * An example use case could be a module system which needs configuration bindings for the current module, but wants to
 * allow modules to access classes defined in other modules.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface TargetedInjectorBuilder {

  /**
   * Installs a binding which will be specific to the constructed injector.
   *
   * @param binding the binding to install.
   * @param <T>     the type of values handled by the given binding.
   * @return this builder, for chaining.
   * @throws IllegalStateException if {@code build()} was already called on this builder.
   */
  @NotNull
  @Contract("_ -> this")
  <T> TargetedInjectorBuilder installBinding(@NotNull UninstalledBinding<T> binding);

  /**
   * Builds the targeted injector instance from the bindings applied to this builder. The builder instance cannot be
   * reused after calling this method.
   *
   * @return the constructed injector using the bindings previously applied to this builder.
   * @throws IllegalStateException if {@code build()} was already called on this builder.
   */
  @NotNull
  Injector build();
}
