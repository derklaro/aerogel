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

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * Advanced options that can be configured when building a binding. Settings these options is not strictly required for
 * the injection process to work, however, the injection process can be configured as needed using these options.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface BindingOptions {

  /**
   * Defines the lookup that should be used to access members in the classes that is targeted by the associated binding.
   * This can be the injectable constructor, fields and methods. If not defined, the public lookup will be used which
   * can lead to access errors due to limited member visibility.
   *
   * @return the lookup that should be used to access members in the classes that is targeted by the associated binding.
   */
  @NotNull
  Optional<MethodHandles.Lookup> memberLookup();
}
