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

package dev.derklaro.aerogel.binding;

import dev.derklaro.aerogel.binding.key.BindingKey;
import java.util.Optional;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A binding that can provide bindings for elements dynamically, for example based on annotation values. When a matching
 * request is made to a dynamic binding, the binding can decide if it can provide a binding for the requested element or
 * not. Return values are required to be predictable, therefore calling the {@link #tryMatch(BindingKey)} method with
 * the same binding key twice, should always return the same binding instance. Once a binding was resolved from a
 * dynamic binding, no subsequent dynamic bindings will be called, and the result will be stored as a fixed binding.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface DynamicBinding {

  /**
   * Get if this dynamic binding would support creating a binding instance for the given binding key. If this method
   * returns {@code true} for the given binding key, a subsequent call to {@link #tryMatch(BindingKey)} with the same
   * key <strong>MUST</strong> return a binding.
   *
   * @param key the key to check for support.
   * @return true if this binding supports the given key, false otherwise.
   */
  boolean supports(@NotNull BindingKey<?> key);

  /**
   * Tries to match this binding against the given binding key. If this binding matches the given key and want to
   * provide a binding for it, the method should return an optional containing an uninstalled binding to use for the
   * element. In case this binding can't provide an instance for the key, the method should return an empty optional.
   *
   * @param key the binding key of the element that gets injected.
   * @param <T> the type of value handled by the binding.
   * @return an uninstalled binding if this binding matches, an empty optional otherwise.
   */
  @NotNull
  <T> Optional<UninstalledBinding<T>> tryMatch(@NotNull BindingKey<T> key);
}
