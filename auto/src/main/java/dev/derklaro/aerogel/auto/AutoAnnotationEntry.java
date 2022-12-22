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

package dev.derklaro.aerogel.auto;

import dev.derklaro.aerogel.binding.BindingConstructor;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A factory for reading and re-constructing bindings to and from data streams.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface AutoAnnotationEntry {

  /**
   * Emits the collected data of this entry to the given data output.
   *
   * @param out the data output.
   * @throws IOException if an I/O error occurs.
   */
  void emit(@NotNull DataOutputStream out) throws IOException;

  /**
   * Re-constructs all bindings which were emitted by this factory.
   *
   * @param loader the class loader to use when loading of classes is required.
   * @param in     the data input to read the data previously written from.
   * @return the constructed bindings based on the data from the input stream.
   * @throws IOException if an I/O error occurs.
   */
  @Unmodifiable
  @NotNull Set<BindingConstructor> makeBinding(
    @NotNull ClassLoader loader,
    @NotNull DataInputStream in
  ) throws IOException;
}
