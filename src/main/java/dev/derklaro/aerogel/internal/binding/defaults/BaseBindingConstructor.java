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

package dev.derklaro.aerogel.internal.binding.defaults;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.ScopeProvider;
import dev.derklaro.aerogel.binding.BindingConstructor;
import dev.derklaro.aerogel.binding.BindingHolder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import org.apiguardian.api.API;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The default implementation of a binding constructor which takes over most of the functionality to make downstream
 * implementations easier.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.binding.*")
public abstract class BaseBindingConstructor implements BindingConstructor {

  private static final Element[] EMPTY_ELEMENT_ARRAY = new Element[0];

  protected final Element[] types;
  protected final Type constructingType;
  protected final Set<ScopeProvider> scopes;
  protected final Set<Class<? extends Annotation>> unresolvedScopes;

  /**
   * Constructs a new base binding constructor.
   *
   * @param types            the types which all underlying binding holders should target.
   * @param scopes           the resolved scopes to apply when creating a binding holder.
   * @param unresolvedScopes the unresolved scopes to resolve and apply when creating a binding holder.
   */
  public BaseBindingConstructor(
    @NotNull Set<Element> types,
    @NotNull Type constructingType,
    @NotNull Set<ScopeProvider> scopes,
    @NotNull Set<Class<? extends Annotation>> unresolvedScopes
  ) {
    this.types = types.toArray(EMPTY_ELEMENT_ARRAY);
    this.constructingType = constructingType;
    this.scopes = scopes;
    this.unresolvedScopes = unresolvedScopes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingHolder construct(@NotNull Injector injector) throws AerogelException {
    // build the base provider
    ContextualProvider<Object> provider = this.constructProvider(injector);

    // apply the known scopes
    for (ScopeProvider scope : this.scopes) {
      provider = scope.applyScope(this.constructingType, this.types, provider.asUpstreamContext());
    }

    // resolve and apply the unresolved scopes
    for (Class<? extends Annotation> unresolvedScope : this.unresolvedScopes) {
      // try to resolve the scope, error out if not possible
      ScopeProvider resolvedScope = injector.scope(unresolvedScope);
      if (resolvedScope == null) {
        throw AerogelException.forMessage("Unable to resolve scope for annotation @" + unresolvedScope.getName());
      }

      // apply the resolved scope
      provider = resolvedScope.applyScope(this.constructingType, this.types, provider.asUpstreamContext());
    }

    // build the binding
    return new DefaultBindingHolder(injector, this.types, provider);
  }

  /**
   * Method to be overridden by consumers to construct the base provider to which further actions are applied in order
   * to build the final binding holder.
   *
   * @param injector the injector which requested the build of the binding.
   * @return the base provider constructed by the implementing consumer.
   * @throws NullPointerException if the given injector is null.
   * @throws AerogelException     if an exception occurred while constructing the base provider.
   */
  @ApiStatus.OverrideOnly
  protected abstract @NotNull ContextualProvider<Object> constructProvider(@NotNull Injector injector);
}
