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

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.auto.internal.DefaultAutoAnnotationRegistry;
import dev.derklaro.aerogel.binding.BindingConstructor;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A runtime registry for annotation loading based on emitted file output during compile time.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface AutoAnnotationRegistry {

  /**
   * Creates a new default implementation instance of an {@link AutoAnnotationRegistry}.
   *
   * @return a new default implementation instance.
   */
  static @NotNull AutoAnnotationRegistry newRegistry() {
    return new DefaultAutoAnnotationRegistry();
  }

  /**
   * An unmodifiable view of all the automated factories to load annotations.
   *
   * @return all the automated factories to load annotations.
   */
  @NotNull
  @UnmodifiableView Map<String, AutoAnnotationEntry> entries();

  /**
   * Unregisters a factory by its name.
   *
   * @param name the name of the factory to unregister.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if name is null.
   */
  @NotNull AutoAnnotationRegistry unregisterEntry(@NotNull String name);

  /**
   * Registers a new factory if its name is not already present.
   *
   * @param name  the name of the factory to register.
   * @param entry the factory instance to register.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if name or entry is null.
   */
  @NotNull AutoAnnotationRegistry registerEntry(@NotNull String name, @NotNull AutoAnnotationEntry entry);

  /**
   * Makes all binding constructors which were emitted to the target file.
   *
   * @param loader   the loader in which the file resource is located.
   * @param fileName the name of the file resource to load.
   * @return all constructed bindings based on the file output.
   * @throws AerogelException if an I/O exception occurs while loading or closing the data stream.
   */
  @NotNull Set<BindingConstructor> makeConstructors(@NotNull ClassLoader loader, @NotNull String fileName);

  /**
   * Makes all binding constructors which were emitted to the target stream.
   *
   * @param emittedFile the input stream of the file to read from.
   * @return all constructed bindings based on the file output.
   * @throws AerogelException if an I/O exception occurs while loading, reading or closing the data stream.
   */
  @NotNull Set<BindingConstructor> makeConstructors(@NotNull InputStream emittedFile);

  /**
   * Makes and installs all bindings which were emitted to the target file.
   *
   * @param loader   the loader in which the file resource is located.
   * @param fileName the name of the file resource to load.
   * @param target   the injector to install the bindings to.
   * @throws AerogelException if an I/O exception occurs while loading or closing the data stream.
   */
  void installBindings(@NotNull ClassLoader loader, @NotNull String fileName, @NotNull Injector target);

  /**
   * Makes and installs all binding constructors which were emitted to the target stream.
   *
   * @param emittedFile the input stream of the file to read from.
   * @param target      the injector to install the bindings to.
   * @throws AerogelException if an I/O exception occurs while loading, reading or closing the data stream.
   */
  void installBindings(@NotNull InputStream emittedFile, @NotNull Injector target);
}
