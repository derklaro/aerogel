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

package dev.derklaro.aerogel.internal.binding.constructors;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.ScopeProvider;
import dev.derklaro.aerogel.internal.binding.FunctionalContextualProvider;
import dev.derklaro.aerogel.internal.binding.defaults.BaseBindingConstructor;
import dev.derklaro.aerogel.internal.unsafe.UnsafeMemberAccess;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.BiFunction;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A binding constructor which uses a factory method to create a new instance.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.binding.*")
public final class FactoryMethodBindingConstructor extends BaseBindingConstructor {

  private final Method factoryMethod;
  private final BiFunction<InjectionContext, Element[], Object[]> parameterValuesSupplier;

  /**
   * Constructs a new factory method binding constructor.
   *
   * @param types            the types which all underlying binding holders should target.
   * @param scopes           the resolved scopes to apply when creating a binding holder.
   * @param unresolvedScopes the unresolved scopes to resolve and apply when creating a binding holder.
   * @param factoryMethod    the factory method to use to create new instances.
   */
  public FactoryMethodBindingConstructor(
    @NotNull Set<Element> types,
    @NotNull Set<ScopeProvider> scopes,
    @NotNull Set<Class<? extends Annotation>> unresolvedScopes,
    @NotNull Method factoryMethod
  ) {
    super(types, scopes, unresolvedScopes);

    // assign the constructor & make sure that we can access it
    this.factoryMethod = UnsafeMemberAccess.forceMakeAccessible(factoryMethod);
    this.parameterValuesSupplier = ParameterHelper.constructParameterSuppliers(factoryMethod.getParameters());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NotNull ContextualProvider<Object> constructProvider(@NotNull Injector injector) {
    return new FunctionalContextualProvider<>(injector, this.types, context -> {
      try {
        // get the parameter values & construct a new instance
        Object[] paramValues = this.parameterValuesSupplier.apply(context, this.types);
        return this.factoryMethod.invoke(null, paramValues);
      } catch (ConstructedValueException constructedException) {
        // the value was constructed while getting all parameter values
        return constructedException.constructedValue();
      } catch (ReflectiveOperationException ex) {
        // unexpected, explode
        throw AerogelException.forMessagedException("Unable to construct requested value using a constructor", ex);
      }
    });
  }
}
