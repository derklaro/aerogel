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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

final class MethodDescriptorImpl extends AnnotatedDescriptorImpl implements MethodDescriptor {

  private final Method method;
  private final List<ParameterDescriptor> parameters;

  public MethodDescriptorImpl(@NotNull Method method) {
    super(method);
    this.method = method;
    this.parameters = convertParameterArray(method.getParameters(), this);
  }

  @Unmodifiable
  static @NotNull List<ParameterDescriptor> convertParameterArray(
    @NotNull Parameter[] parameters,
    @NotNull ExecutableDescriptor owner
  ) {
    if (parameters.length == 0) {
      return Collections.emptyList();
    } else if (parameters.length == 1) {
      ParameterDescriptor firstParamDesc = new ParameterDescriptorImpl(parameters[0], owner);
      return Collections.singletonList(firstParamDesc);
    } else {
      List<ParameterDescriptor> parameterDescriptors = new ArrayList<>(parameters.length);
      for (Parameter parameter : parameters) {
        ParameterDescriptor descriptor = new ParameterDescriptorImpl(parameter, owner);
        parameterDescriptors.add(descriptor);
      }

      return Collections.unmodifiableList(parameterDescriptors);
    }
  }

  @Override
  public boolean interfaceDefault() {
    return this.method.isDefault();
  }

  @Override
  public @NotNull Class<?> returnType() {
    return this.method.getReturnType();
  }

  @Override
  public @NotNull Type genericReturnType() {
    return this.method.getGenericReturnType();
  }

  @Override
  public boolean vararg() {
    return this.method.isVarArgs();
  }

  @Override
  @Unmodifiable
  public @NotNull List<ParameterDescriptor> parameters() {
    return this.parameters;
  }

  @Override
  public @NotNull Class<?> owner() {
    return this.method.getDeclaringClass();
  }

  @Override
  public @NotNull String name() {
    return this.method.getName();
  }

  @Override
  public int accessModifiers() {
    return this.method.getModifiers();
  }

  @Override
  public boolean synthetic() {
    return this.method.isSynthetic();
  }
}
