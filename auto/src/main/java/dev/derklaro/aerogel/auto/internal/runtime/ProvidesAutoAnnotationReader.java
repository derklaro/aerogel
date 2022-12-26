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

package dev.derklaro.aerogel.auto.internal.runtime;

import static dev.derklaro.aerogel.auto.internal.utility.ClassLoadingUtil.loadClass;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.auto.runtime.AbstractAutoAnnotationReader;
import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.binding.BindingConstructor;
import dev.derklaro.aerogel.internal.utility.ElementHelper;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

/**
 * The runtime reader for emitted provides auto annotation entries.
 *
 * @author Pasqual K.
 * @since 2.0
 */
public class ProvidesAutoAnnotationReader extends AbstractAutoAnnotationReader {

  /**
   * Constructs a new provides auto annotation reader instance.
   */
  public ProvidesAutoAnnotationReader() {
    super("provides");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<BindingConstructor> readBindings(
    @NotNull ClassLoader sourceLoader,
    @NotNull DataInputStream source
  ) throws IOException {
    try {
      // the data version used to write the data
      short dataVersion = source.readShort();

      // the class to which all provided classes should get bound
      Class<?> boundClass = loadClass(sourceLoader, source.readUTF());

      // read the amount of elements the given type is bound to & begin the build process
      int elements = source.readInt();
      BindingBuilder builder = BindingBuilder.create();

      // bind all provided classes
      for (int i = 0; i < elements; i++) {
        // load the surrounding class & build an element from it
        Class<?> providedClass = loadClass(sourceLoader, source.readUTF());
        Element element = ElementHelper.buildElement(providedClass, providedClass);

        // apply the element to the builder
        builder = builder.bindFully(element);
      }

      // construct the binding targeting the given bound type
      BindingConstructor constructor = builder.toConstructing(boundClass);
      return Collections.singleton(constructor);
    } catch (ClassNotFoundException exception) {
      throw AerogelException.forMessagedException("Unable to provide bindings constructors", exception);
    }
  }
}
