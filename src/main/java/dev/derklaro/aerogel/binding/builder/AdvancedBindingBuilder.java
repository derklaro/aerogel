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

package dev.derklaro.aerogel.binding.builder;

import java.lang.invoke.MethodHandles;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * Builder step that allows to set advanced options for the target binding. Settings these options is not required, but
 * might enable some extra behaviour or might be even required for the injection process to work as expected.
 *
 * @param <T> the type of values handled by this binding that is being built.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface AdvancedBindingBuilder<T> extends TargetableBindingBuilder<T> {

  /**
   * Sets the lookup instance that should be used to get access to members in the target implementation class during
   * injection. This is especially useful when, for example, modules are used and class member visibility is blocked to
   * the injector. The provided lookup will be used for every operation that includes reflective access (constructor,
   * method and field injection).
   *
   * @param lookup the lookup to use for reflective operations on members related to the binding.
   * @return this builder, for chaining or finishing the binding process.
   */
  @NotNull
  AdvancedBindingBuilder<T> memberLookup(@NotNull MethodHandles.Lookup lookup);
}
