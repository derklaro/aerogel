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

package dev.derklaro.aerogel.auto.internal.runtime;

import static dev.derklaro.aerogel.auto.internal.utility.ClassLoadingUtil.loadClass;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.auto.runtime.AbstractAutoAnnotationReader;
import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.binding.BindingConstructor;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * The runtime reader for emitted factory auto annotation entries.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0")
public final class FactoryAutoAnnotationReader extends AbstractAutoAnnotationReader {

  /**
   * Constructs a new factory auto annotation reader instance.
   */
  public FactoryAutoAnnotationReader() {
    super("factory");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<BindingConstructor> readBindings(
    @NotNull ClassLoader sourceLoader,
    @NotNull DataInputStream source
  ) throws IOException {
    short dataVersion = source.readShort(); // the data version used to write the data
    String methodName = source.readUTF(); // the name of the factory method
    String enclosingClass = source.readUTF(); // the class which declares the method

    // read the method argument types as an array from the stream
    String[] typeArguments = new String[source.readInt()];
    for (int i = 0; i < typeArguments.length; i++) {
      typeArguments[i] = source.readUTF();
    }

    try {
      // load the declaring class
      Class<?> declaringClass = loadClass(sourceLoader, enclosingClass);

      // load the type arguments as classes to a new array
      Class<?>[] params = new Class<?>[typeArguments.length];
      for (int i = 0; i < typeArguments.length; i++) {
        params[i] = loadClass(sourceLoader, typeArguments[i]);
      }

      // get the factory method from the declaring class & create a constructing holder from that
      BindingConstructor constructor = BindingBuilder.create().toFactory(declaringClass, methodName, params);
      return Collections.singleton(constructor);
    } catch (Exception exception) {
      throw AerogelException.forMessagedException(String.format(
        "Unable to construct factory binding on %s.%s(%s)",
        enclosingClass,
        methodName,
        String.join(", ", typeArguments)
      ), exception);
    }
  }
}
