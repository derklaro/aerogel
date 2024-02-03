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

package dev.derklaro.aerogel.binding.desc;

import java.lang.reflect.Constructor;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

final class ConstructorDescriptorImpl extends AnnotatedDescriptorImpl implements ConstructorDescriptor {

  private final Constructor<?> constructor;
  private final List<ParameterDescriptor> parameters;

  public ConstructorDescriptorImpl(@NotNull Constructor<?> constructor) {
    super(constructor);
    this.constructor = constructor;
    this.parameters = MethodDescriptorImpl.convertParameterArray(constructor.getParameters(), this);
  }

  @Override
  public boolean vararg() {
    return this.constructor.isVarArgs();
  }

  @Override
  @Unmodifiable
  public @NotNull List<ParameterDescriptor> parameters() {
    return this.parameters;
  }

  @Override
  public @NotNull Class<?> owner() {
    return this.constructor.getDeclaringClass();
  }

  @Override
  public @NotNull String name() {
    return this.constructor.getName();
  }

  @Override
  public int accessModifiers() {
    return this.constructor.getModifiers();
  }

  @Override
  public boolean synthetic() {
    return this.constructor.isSynthetic();
  }
}
