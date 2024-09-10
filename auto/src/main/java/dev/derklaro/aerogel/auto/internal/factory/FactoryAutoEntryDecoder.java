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

package dev.derklaro.aerogel.auto.internal.factory;

import dev.derklaro.aerogel.auto.AutoEntryDecoder;
import dev.derklaro.aerogel.auto.LazyBindingCollection;
import dev.derklaro.aerogel.auto.annotation.Factory;
import dev.derklaro.aerogel.auto.internal.util.AutoDecodingUtil;
import dev.derklaro.aerogel.binding.key.BindingKey;
import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public final class FactoryAutoEntryDecoder implements AutoEntryDecoder {

  @Override
  public @NotNull String id() {
    return Factory.CODEC_ID;
  }

  @Override
  public @NotNull LazyBindingCollection decodeEntry(
    @NotNull DataInput dataInput,
    @NotNull ClassLoader loader
  ) throws IOException {
    String methodName = dataInput.readUTF();
    Class<?> declaring = AutoDecodingUtil.decodeType(dataInput, loader);
    Class<?>[] methodArgTypes = AutoDecodingUtil.decodeTypes(dataInput, loader);

    try {
      Method factoryMethod = declaring.getDeclaredMethod(methodName, methodArgTypes);
      BindingKey<?> methodBindingKey = BindingKey
        .of(factoryMethod.getGenericReturnType())
        .selectQualifier(factoryMethod.getAnnotations());
      return new FactoryLazyBindingCollection(methodBindingKey, factoryMethod);
    } catch (NoSuchMethodException exception) {
      String formattedArgs = Arrays.stream(methodArgTypes).map(Class::getName).collect(Collectors.joining(", "));
      throw new IllegalStateException(String.format(
        "Factory method %s(%s) not found in %s",
        methodName, formattedArgs, declaring.getName()));
    }
  }
}
