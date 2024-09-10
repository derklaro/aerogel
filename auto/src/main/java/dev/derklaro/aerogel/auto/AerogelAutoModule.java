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

import dev.derklaro.aerogel.auto.internal.AerogelAutoModuleImpl;
import dev.derklaro.aerogel.registry.Registry;
import java.io.InputStream;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A runtime extension for aerogel which can be used to deserialize bindings which were previously constructed using an
 * annotation processor. Decoders are discovered automatically using the SPI, but can be registered dynamically at
 * runtime as well. Once deserialized, bindings can freely be constructed or installed into injectors.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface AerogelAutoModule {

  /**
   * Constructs a new instance of this module. During constructions the entry deserialized are discovered using SPI.
   *
   * @return a new instance of this module.
   */
  @Contract(value = " -> new", pure = true)
  static @NotNull AerogelAutoModule newInstance() {
    return new AerogelAutoModuleImpl();
  }

  /**
   * Get the decoder registry that is used by this module instance. The registry can be used to register additional
   * deserializers or unregister them as needed.
   *
   * @return the decoder registry of this module instance.
   */
  @NotNull
  Registry.WithKeyMapping<String, AutoEntryDecoder> decoderRegistry();

  /**
   * Deserializes the bindings that are contained in the given data stream. The given input stream is closed
   * automatically when this method completes.
   *
   * @param dataStream the data stream to decode.
   * @return the deserialized bindings contained in the given stream.
   * @throws IllegalStateException in case something goes wrong during binding deserialization.
   */
  @NotNull
  LazyBindingCollection deserializeBindings(@NotNull InputStream dataStream);

  /**
   * Deserializes the bindings that are contained in the given data stream. The given input stream is closed
   * automatically when this method completes.
   *
   * @param dataStream the data stream to decode.
   * @param loader     the class loader to use when decoding types.
   * @return the deserialized bindings contained in the given stream.
   * @throws IllegalStateException in case something goes wrong during binding deserialization.
   */
  @NotNull
  LazyBindingCollection deserializeBindings(@NotNull InputStream dataStream, @Nullable ClassLoader loader);
}
