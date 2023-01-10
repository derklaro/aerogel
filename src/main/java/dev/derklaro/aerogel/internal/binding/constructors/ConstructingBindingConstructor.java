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
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.ScopeProvider;
import dev.derklaro.aerogel.internal.PassthroughException;
import dev.derklaro.aerogel.internal.binding.FunctionalContextualProvider;
import dev.derklaro.aerogel.internal.binding.defaults.BaseBindingConstructor;
import dev.derklaro.aerogel.internal.invoke.ParameterHelper;
import dev.derklaro.aerogel.internal.invoke.ParameterValueGetter;
import dev.derklaro.aerogel.internal.reflect.TypeUtil;
import dev.derklaro.aerogel.internal.util.MethodHandleUtil;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.util.Set;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A binding constructor which uses a constructor to create a new instance.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.binding.*")
public final class ConstructingBindingConstructor extends BaseBindingConstructor {

  private final MethodHandle targetConstructor;
  private final ParameterValueGetter parameterValueGetter;

  /**
   * Constructs a new constructing binding constructor.
   *
   * @param types             the types which all underlying binding holders should target.
   * @param scopes            the resolved scopes to apply when creating a binding holder.
   * @param unresolvedScopes  the unresolved scopes to resolve and apply when creating a binding holder.
   * @param targetConstructor the constructor to invoke to resolve the binding value.
   */
  public ConstructingBindingConstructor(
    @NotNull Set<Element> types,
    @NotNull Set<ScopeProvider> scopes,
    @NotNull Set<Class<? extends Annotation>> unresolvedScopes,
    @NotNull Constructor<?> targetConstructor
  ) {
    super(types, targetConstructor.getDeclaringClass(), scopes, unresolvedScopes);

    // assign the constructor & make sure that we can access it
    this.targetConstructor = MethodHandleUtil.toGenericMethodHandle(targetConstructor);
    this.parameterValueGetter = ParameterHelper.constructParameterSuppliers(targetConstructor.getParameters());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NotNull ContextualProvider<Object> constructProvider(@NotNull Injector injector) {
    return new FunctionalContextualProvider<>(injector, this.constructingType, this.types, (context, provider) -> {
      try {
        // get the parameter values & construct a new instance
        Object[] paramValues = this.parameterValueGetter.resolveParamInstances(context, provider, this.types, injector);
        return MethodHandleUtil.invokeConstructor(this.targetConstructor, paramValues);
      } catch (PassthroughException | AerogelException passthroughException) {
        throw passthroughException;
      } catch (Throwable throwable) {
        // unexpected, explode
        String constructorDesc =
          TypeUtil.toPrettyString(this.constructingType) + "(" + this.targetConstructor.type() + ")";
        throw AerogelException.forMessagedException(
          "Unable to construct requested value constructor " + constructorDesc,
          throwable);
      }
    });
  }
}
