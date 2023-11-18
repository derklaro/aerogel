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

package dev.derklaro.aerogel.internal.context.scope;

import dev.derklaro.aerogel.internal.context.scope.threadlocal.ThreadLocalInjectionContextProvider;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A holder and loader for the injection context provider to use for all injectors. This loader uses the service
 * provider interface to locate custom implementations and falls back to using a provider implemented using thread
 * locals in case no custom implementation was provided.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0", consumers = "dev.derklaro.aerogel.internal.context.scope")
final class InjectionContextProviderHolder {

  private static volatile InjectionContextProvider LOADED_PROVIDER;

  private InjectionContextProviderHolder() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the singleton context provider that was already loaded or tries to load the context provider.
   *
   * @return the singleton context provider to use for all injectors.
   */
  public static @NotNull InjectionContextProvider getContextProvider() {
    // check if the provider was loaded before, just return the constructed instance in that case
    if (LOADED_PROVIDER != null) {
      return LOADED_PROVIDER;
    }

    // load all context provider services using the class loader of this class rather than falling back
    // to whatever the thread context class loader is (or, if none, the system class loader)
    ClassLoader cl = InjectionContextProviderHolder.class.getClassLoader();
    ServiceLoader<InjectionContextProvider> services = ServiceLoader.load(InjectionContextProvider.class, cl);

    // check if there is any service registered for the context provider holder
    // return the first one that can actually be loaded without any errors.
    // if there are none, just fall back to the default provider
    Iterator<InjectionContextProvider> serviceIterator = services.iterator();
    while (serviceIterator.hasNext()) {
      try {
        InjectionContextProvider loadedProvider = serviceIterator.next();
        LOADED_PROVIDER = loadedProvider;
        return loadedProvider;
      } catch (ServiceConfigurationError ignored) {
      }
    }

    // no context provider was given via service registry, just fall back to the default thread-local handling
    InjectionContextProvider provider = new ThreadLocalInjectionContextProvider();
    LOADED_PROVIDER = provider;
    return provider;
  }
}
