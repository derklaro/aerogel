/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
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

package dev.derklaro.aerogel.internal.context;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.MemberInjector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A request to inject members into the constructed value.
 *
 * @author Pasqual K.
 * @since 2.0
 */
final class MemberInjectionRequest {

  private final long flag;
  private final Class<?> targetClass;
  private final Object constructedValue;
  private final Injector parentInjector;

  private final int hashCode;

  /**
   * Constructs a new member injection request.
   *
   * @param flag             the flag to pass to the member injector when injecting.
   * @param targetClass      the target class to resolve the fields and methods to call from.
   * @param constructedValue the value that was constructed and should receive member injection.
   * @param parentInjector   the injector which should be used as the parent of the member injector.
   */
  public MemberInjectionRequest(
    long flag,
    @NotNull Class<?> targetClass,
    @Nullable Object constructedValue,
    @NotNull Injector parentInjector
  ) {
    this.flag = flag;
    this.targetClass = targetClass;
    this.constructedValue = constructedValue;
    this.parentInjector = parentInjector;
    this.hashCode = targetClass.hashCode() ^ System.identityHashCode(constructedValue);
  }

  /**
   * Executes the requested member injection.
   */
  public void executeMemberInjection() {
    MemberInjector memberInjector = this.parentInjector.memberInjector(this.targetClass);
    memberInjector.inject(this.constructedValue, this.flag);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MemberInjectionRequest that = (MemberInjectionRequest) o;
    return this.hashCode == that.hashCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.hashCode;
  }
}
