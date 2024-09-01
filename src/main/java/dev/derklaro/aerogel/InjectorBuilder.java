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

import dev.derklaro.aerogel.binding.key.BindingKey;
import java.lang.invoke.MethodHandles;
import java.util.function.Predicate;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A builder for an injector which allows to fine-tune an injector instance. A new builder instance can be obtained by
 * using {@link Injector#builder()}. Using {@code Injector.builder().build()} would create an injector instance
 * identical to {@link Injector#newInjector()}.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface InjectorBuilder {

  /**
   * Sets the member lookup that will be used to resolve reflective members of classes when needed (such as constructors
   * when resolving injection points and field/methods for member injection). If not explicitly set a default lookup
   * will be used instead.
   *
   * @param lookup the lookup instance to use for member lookups.
   * @return this builder, for chaining.
   */
  @NotNull
  @Contract("_ -> this")
  InjectorBuilder memberLookup(@NotNull MethodHandles.Lookup lookup);

  /**
   * Sets the filter for jit bindings to use. By default, a jit binding can be created for all binding keys. The given
   * filter will be called for every key for which a jit binding would be necessary. If the filter indicates that no jit
   * binding should be created for a key an exception is thrown during the injection process.
   *
   * @param filter the filter to use for jit bindings.
   * @return this builder, for chaining.
   */
  @NotNull
  @Contract("_ -> this")
  InjectorBuilder jitBindingFilter(@NotNull Predicate<BindingKey<?>> filter);

  /**
   * Constructs a new injector instance based on the options provided to this builder.
   *
   * @return a new injector instance based on the options provided to this builder.
   */
  @NotNull
  @Contract(value = " -> new", pure = true)
  Injector build();
}
