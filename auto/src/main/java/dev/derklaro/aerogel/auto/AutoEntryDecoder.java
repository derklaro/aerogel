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

package dev.derklaro.aerogel.auto;

import java.io.DataInput;
import java.io.IOException;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A decoder for auto entries. A decoder can either be registered automatically via SPI or manually by registering it
 * into the registry of the target module.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface AutoEntryDecoder {

  /**
   * Get the unique identifier of this auto entry decoder. A decoder is called based on the id written into the given
   * input data stream (each entry must be prefixed with the decoder id to use).
   *
   * @return the unique identifier of this auto entry decoder.
   */
  @NotNull
  String id();

  /**
   * Decodes an entry based on the given data input. This decoder must make sure to read all the data that was written
   * during serialization so that the cursor position is at the head for the next deserialization step.
   *
   * @param dataInput the data input to read the necessary information from to decode the entry.
   * @param loader    the class loader to use when decoding types.
   * @return a collection of entries that were decoded from the given data input.
   * @throws IOException           if an I/O error occurs while reading from the given data input.
   * @throws IllegalStateException in case something goes wrong during binding deserialization.
   */
  @NotNull
  LazyBindingCollection decodeEntry(@NotNull DataInput dataInput, @NotNull ClassLoader loader) throws IOException;
}
