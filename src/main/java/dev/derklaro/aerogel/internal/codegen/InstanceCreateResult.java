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

package dev.derklaro.aerogel.internal.codegen;

import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of an instance construction from an {@link InstanceMaker}.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal")
public final class InstanceCreateResult {

  private final Object result;
  private final boolean doMemberInjection;

  /**
   * Constructs a new instance create result.
   *
   * @param result            the created instance, might be null.
   * @param doMemberInjection true if member injection is required, false otherwise.
   */
  public InstanceCreateResult(@Nullable Object result, boolean doMemberInjection) {
    this.result = result;
    this.doMemberInjection = doMemberInjection;
  }

  /**
   * Get the result of the instance create process, might be null.
   *
   * @return the created instance.
   */
  @SuppressWarnings("unchecked")
  public @Nullable <T> T constructedValue() {
    return (T) this.result;
  }

  /**
   * Get if member injection should be executed on the constructed value.
   *
   * @return true if member injection is required, false otherwise.
   */
  public boolean doMemberInjection() {
    return this.doMemberInjection;
  }
}
