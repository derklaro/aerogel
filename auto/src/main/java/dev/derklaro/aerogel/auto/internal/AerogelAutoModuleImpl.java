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

package dev.derklaro.aerogel.auto.internal;

import dev.derklaro.aerogel.auto.AerogelAutoModule;
import dev.derklaro.aerogel.auto.AutoEntryDecoder;
import dev.derklaro.aerogel.auto.LazyBindingCollection;
import dev.derklaro.aerogel.auto.internal.util.AutoDecodingUtil;
import dev.derklaro.aerogel.registry.Registry;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AerogelAutoModuleImpl implements AerogelAutoModule {

  private final Registry.WithKeyMapping<String, AutoEntryDecoder> decoderRegistry = Registry.createRegistryWithKeys();

  public AerogelAutoModuleImpl() {
    // load the AutoEntryDecoder on the classpath, ignore the implementations
    // that throw some error on load (for example due to a failed precondition)
    ClassLoader cl = AerogelAutoModuleImpl.class.getClassLoader();
    ServiceLoader<AutoEntryDecoder> serviceLoader = ServiceLoader.load(AutoEntryDecoder.class, cl);
    Iterator<AutoEntryDecoder> services = serviceLoader.iterator();
    while (services.hasNext()) {
      try {
        AutoEntryDecoder decoder = services.next();
        this.decoderRegistry.register(decoder.id(), decoder);
      } catch (ServiceConfigurationError ignored) {
      }
    }
  }

  @Override
  public @NotNull Registry.WithKeyMapping<String, AutoEntryDecoder> decoderRegistry() {
    return this.decoderRegistry;
  }

  @Override
  public @NotNull LazyBindingCollection deserializeBindings(@NotNull InputStream dataStream) {
    return this.deserializeBindings(dataStream, null);
  }

  @Override
  public @NotNull LazyBindingCollection deserializeBindings(
    @NotNull InputStream dataStream,
    @Nullable ClassLoader loader
  ) {
    ClassLoader classLoader = loader;
    if (classLoader == null) {
      ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
      ClassLoader thisClassLoader = AutoDecodingUtil.class.getClassLoader();
      classLoader = ctxLoader != null ? ctxLoader : thisClassLoader;
      if (classLoader == null) {
        // loaded by bootstrap class loader, fall back to system class loader
        classLoader = ClassLoader.getSystemClassLoader();
      }
    }

    try (DataInputStream dataInput = new DataInputStream(dataStream)) {
      return this.deserializeBindings(dataInput, classLoader, null);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to decode bindings from data input", exception);
    }
  }

  private @NotNull LazyBindingCollection deserializeBindings(
    @NotNull DataInputStream dataInput,
    @NotNull ClassLoader classLoader,
    @Nullable LazyBindingCollection previousResult
  ) throws IOException {
    String decoderId = dataInput.readUTF();
    AutoEntryDecoder decoder = this.decoderRegistry
      .get(decoderId)
      .orElseThrow(() -> new IllegalStateException("Decoder for bindings with id " + decoderId + " is not registered"));

    // decode the entry and combine it with the previous result (if any)
    LazyBindingCollection decoded = decoder.decodeEntry(dataInput, classLoader);
    LazyBindingCollection combined = previousResult == null ? decoded : previousResult.combine(decoded);

    // continue reading if there are more bindings to deserialize
    if (dataInput.available() > 0) {
      return this.deserializeBindings(dataInput, classLoader, combined);
    } else {
      return combined;
    }
  }
}
