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

package dev.derklaro.aerogel.auto.runtime;

import dev.derklaro.aerogel.binding.BindingConstructor;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an entry which can construct all bindings from an autoconfiguration file that was emitted during compile
 * time from the associated processing entry.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.STABLE, since = "2.0")
public interface AutoAnnotationReader {

  /**
   * Get the name of the emitted annotation entries which are handled by this reader.
   *
   * @return the name of the handled annotation types of this reader.
   */
  @NotNull String name();

  /**
   * Constructs all bindings which were emitted by the associated processing entry during compile time.
   *
   * @param sourceLoader the class loader which should be used when needing to load classes.
   * @param source       the data stream to read the binding data from.
   * @return the constructed bindings based on the data stream.
   * @throws IOException if an I/O error occurs.
   */
  @NotNull
  Collection<BindingConstructor> readBindings(
    @NotNull ClassLoader sourceLoader,
    @NotNull DataInputStream source
  ) throws IOException;
}
