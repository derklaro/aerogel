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

package dev.derklaro.aerogel.auto;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import java.util.List;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A collection of bindings that are lazily constructed for a given injector.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface LazyBindingCollection {

  /**
   * Installs the bindings that are located in this collection directly into the given injector.
   *
   * @param injector the injector to construct the bindings for and install into.
   */
  void installBindings(@NotNull Injector injector);

  /**
   * Constructs all bindings for the given injector and returns them for further handling by the caller. There is no
   * guarantee that the returned list is mutable.
   *
   * @param injector the injector to construct the bindings for.
   * @return the uninstalled bindings that were constructed for the given injector.
   */
  @NotNull
  List<UninstalledBinding<?>> constructBindings(@NotNull Injector injector);

  /**
   * Combines this binding collection with the given other binding collection. Calls to the returned new binding
   * collection will return a combined result.
   *
   * @param other the binding collection to combine with this binding collection.
   * @return a new binding collection which executes all calls on this and the other binding collection.
   */
  @NotNull
  @Contract("_ -> new")
  LazyBindingCollection combine(@NotNull LazyBindingCollection other);
}
