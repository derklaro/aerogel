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

package dev.derklaro.aerogel.internal.binding.constructors;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.ElementMatcher;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.Provider;
import dev.derklaro.aerogel.ScopeProvider;
import dev.derklaro.aerogel.binding.BindingHolder;
import dev.derklaro.aerogel.internal.binding.defaults.BaseBindingConstructor;
import dev.derklaro.aerogel.internal.provider.FunctionalContextualProvider;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A binding constructor which produces binding holders that are lazily resolving a contextual provider based on the
 * element requested from the binding.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.binding.*")
public final class LazyProviderBindingConstructor extends BaseBindingConstructor {

  private final BiFunction<Element, Injector, Provider<Object>> providerFactory;

  /**
   * Constructs a new lazy provider binding constructor.
   *
   * @param matcher          a matcher for all elements that are supported by this constructor.
   * @param scopes           the resolved scopes to apply when creating a binding holder.
   * @param unresolvedScopes the unresolved scopes to resolve and apply when creating a binding holder.
   * @param providerFactory  the factory to get an instance provider based on an element from.
   */
  public LazyProviderBindingConstructor(
    @NotNull ElementMatcher matcher,
    @NotNull Set<ScopeProvider> scopes,
    @NotNull Set<Class<? extends Annotation>> unresolvedScopes,
    @NotNull BiFunction<Element, Injector, Provider<Object>> providerFactory
  ) {
    super(Object.class, matcher, scopes, unresolvedScopes);
    this.providerFactory = providerFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingHolder construct(@NotNull Injector injector) throws AerogelException {
    return new LazyBindingHolder(injector);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NotNull ContextualProvider<Object> constructProvider(@NotNull Injector injector) {
    throw new UnsupportedOperationException();
  }

  /**
   * A binding holder that lazily populates the binding provider based on the requested element.
   *
   * @author Pasqual K.
   * @since 2.0
   */
  @API(status = API.Status.INTERNAL, since = "2.0")
  private final class LazyBindingHolder implements BindingHolder {

    private final Injector injector;
    private final Lock cacheLock = new ReentrantLock();
    private final Map<Element, ContextualProvider<Object>> providerCache;

    /**
     * Constructs a new lazy binding holder.
     *
     * @param injector the injector that is associated with this binding holder.
     */
    public LazyBindingHolder(@NotNull Injector injector) {
      this.injector = injector;
      this.providerCache = new WeakHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Injector injector() {
      return this.injector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ElementMatcher elementMatcher() {
      return LazyProviderBindingConstructor.this.elementMatcher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ContextualProvider<Object> provider(@NotNull Element requestedElement) {
      this.cacheLock.lock();
      try {
        // we cannot use double check locking here, as each call to the map might
        // remove elements (that were expunged)
        ContextualProvider<Object> provider = this.providerCache.get(requestedElement);
        if (provider == null) {
          // provider not yet cached, build the base provider and apply the scopes to it
          ContextualProvider<Object> baseProvider = this.requestBaseProvider(requestedElement);
          provider = LazyProviderBindingConstructor.this.applyScopes(this.injector, baseProvider);

          // only cache the provider if we had to apply any scope to it
          if (baseProvider != provider) {
            this.providerCache.put(requestedElement, provider);
          }
        }

        // return the constructed provider
        return provider;
      } finally {
        this.cacheLock.unlock();
      }
    }

    /**
     * Constructs the base provider by requesting it from the underlying factory method.
     *
     * @param element the element to get the base provider for.
     * @return the base provider for the given element, constructed by the underlying factory method.
     */
    private @NotNull ContextualProvider<Object> requestBaseProvider(@NotNull Element element) {
      Provider<Object> baseProvider = LazyProviderBindingConstructor.this.providerFactory.apply(element, this.injector);
      if (baseProvider instanceof ContextualProvider) {
        return (ContextualProvider<Object>) baseProvider;
      } else {
        return new FunctionalContextualProvider<>(
          this.injector,
          LazyProviderBindingConstructor.this.constructingType,
          LazyProviderBindingConstructor.this.elementMatcher,
          (context, provider) -> baseProvider.get());
      }
    }
  }
}
