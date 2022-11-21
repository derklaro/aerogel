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

package dev.derklaro.aerogel.auto.internal;

import static java.util.Objects.requireNonNull;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.BindingConstructor;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.auto.AutoAnnotationEntry;
import dev.derklaro.aerogel.auto.AutoAnnotationRegistry;
import dev.derklaro.aerogel.auto.Factory;
import dev.derklaro.aerogel.auto.Provides;
import dev.derklaro.aerogel.auto.internal.holder.FactoryAutoAnnotationEntry;
import dev.derklaro.aerogel.auto.internal.holder.ProvidesAutoAnnotationEntry;
import dev.derklaro.aerogel.internal.utility.MapUtil;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A default implementation of an {@link AutoAnnotationRegistry}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.auto")
public final class DefaultAutoAnnotationRegistry implements AutoAnnotationRegistry {

  private final Map<String, AutoAnnotationEntry> entries = MapUtil.newConcurrentMap();

  /**
   * Constructs a new factory which by default supports the {@link Factory} and {@link Provides} annotation.
   */
  public DefaultAutoAnnotationRegistry() {
    this.entries.put("factory", new FactoryAutoAnnotationEntry());
    this.entries.put("provides", new ProvidesAutoAnnotationEntry());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull @UnmodifiableView Map<String, AutoAnnotationEntry> entries() {
    return Collections.unmodifiableMap(this.entries);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull AutoAnnotationRegistry unregisterEntry(@NotNull String name) {
    this.entries.remove(requireNonNull(name, "name"));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull AutoAnnotationRegistry registerEntry(@NotNull String name, @NotNull AutoAnnotationEntry entry) {
    requireNonNull(name, "name");
    requireNonNull(entry, "entry");
    // only register the entry if there is no entry yet
    this.entries.putIfAbsent(name, entry);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Set<BindingConstructor> makeConstructors(@NotNull ClassLoader loader, @NotNull String fileName) {
    try (InputStream in = loader.getResourceAsStream(fileName)) {
      // ensure that the file is actually there
      if (in == null) {
        throw AerogelException.forMessage("Loader " + loader + " is unable to provide " + fileName);
      }
      // load the constructors from the stream
      return this.makeConstructors(in);
    } catch (IOException exception) {
      throw AerogelException.forMessagedException("Unable to load file " + fileName + " from " + loader, exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Set<BindingConstructor> makeConstructors(@NotNull InputStream emittedFile) {
    try (DataInputStream in = new DataInputStream(emittedFile)) {
      // check if the stream has data
      if (in.available() > 0) {
        // the result data
        Set<BindingConstructor> result = new HashSet<>();
        // while the input stream has data available read it
        while (in.available() > 0) {
          // read the constructor which is responsible to read the data
          String decoder = in.readUTF();
          AutoAnnotationEntry entry = this.entries.get(decoder);
          // check if the entry exists
          if (entry == null) {
            throw AerogelException.forMessage("Defined reader " + decoder + " is not registered");
          }
          // read the constructors from the reader
          result.addAll(entry.makeBinding(in));
        }
        // done
        return result;
      } else {
        // no data in the set
        return Collections.emptySet();
      }
    } catch (IOException exception) {
      throw AerogelException.forMessagedException("Unable to decode the given file stream", exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void installBindings(@NotNull ClassLoader loader, @NotNull String fileName, @NotNull Injector target) {
    // load all the bindings constructors and install them to the injector
    for (BindingConstructor constructor : this.makeConstructors(loader, fileName)) {
      target.install(constructor);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void installBindings(@NotNull InputStream emittedFile, @NotNull Injector target) {
    // load all the bindings constructors and install them to the injector
    for (BindingConstructor constructor : this.makeConstructors(emittedFile)) {
      target.install(constructor);
    }
  }
}
