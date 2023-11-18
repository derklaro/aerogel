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

package dev.derklaro.aerogel.internal.invoke;

import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.Provider;
import dev.derklaro.aerogel.context.InjectionContext;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import dev.derklaro.aerogel.internal.util.ElementHelper;
import java.lang.reflect.Parameter;
import java.util.function.UnaryOperator;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility to class to make working with parameter construction easier.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.*")
public final class ParameterHelper {

  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  private static final ParameterValueGetter EMPTY_SUPPLIER = (__, ____) -> EMPTY_OBJECT_ARRAY;

  private ParameterHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Constructs a function which takes an injection context and returns a new object array which represents the value
   * for each parameter, in order. This method handles the request to inject a provider appropriately.
   *
   * @param parameters the parameters which the target executable element takes.
   * @return a parameter value getter which resolves the value for each given parameter.
   * @throws NullPointerException if the given parameter array or one element of it is null.
   */
  @SuppressWarnings("unchecked")
  public static @NotNull ParameterValueGetter constructParameterSuppliers(@NotNull Parameter[] parameters) {
    // if no parameters were given there is nothing to do
    if (parameters.length == 0) {
      return EMPTY_SUPPLIER;
    }

    // build a value supplier for each parameter
    ParameterResolver[] resolvers = new ParameterResolver[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      // build an element for the parameter at the given index
      Parameter parameter = parameters[i];
      Element element = ElementHelper.buildElement(parameter, parameter.getDeclaredAnnotations());

      // construct the base function to resolve the parameter
      ParameterResolver resolver;
      if (JakartaBridge.isProvider(parameter.getType())) {
        // get the provider for the type & check if we need to bridge to provider as the value takes a jakarta one
        resolver = (injector, context) -> context.resolveProvider(element);
        if (JakartaBridge.needsProviderWrapping(parameter.getType())) {
          resolver = resolver.then(provider -> JakartaBridge.bridgeJakartaProvider((Provider<Object>) provider));
        }
      } else {
        // we can construct the value directly from the context
        resolver = (injector, context) -> {
          ContextualProvider<?> parameterProvider = context.resolveProvider(element);
          InjectionContext parameterContext = context.enterSubcontext(
            parameter.getParameterizedType(),
            parameterProvider,
            element);

          return parameterContext.resolveInstance();
        };
      }

      // set the created resolver
      resolvers[i] = resolver;
    }

    // use the parameters suppliers to construct a function for all parameters
    return (context, injector) -> {
      Object[] values = new Object[resolvers.length];
      for (int i = 0; i < resolvers.length; i++) {
        // get and store each value
        Object value = resolvers[i].resolveInstance(injector, context);
        values[i] = value;
      }
      return values;
    };
  }

  /**
   * The resolver for a single parameter.
   *
   * @author Pasqual K.
   * @since 2.0
   */
  @FunctionalInterface
  private interface ParameterResolver {

    /**
     * Resolves the instance for the associated parameter.
     *
     * @param injector the injector which requested the instantiation of the parent executable member.
     * @param context  the context which is currently constructing the elements which needs the parameter instances.
     * @return the instance to use for the associated parameter.
     */
    @Nullable Object resolveInstance(@NotNull Injector injector, @NotNull InjectionContext context);

    /**
     * Returns a new resolver which resolves the instance of this resolver and then passes the resolved instance to the
     * given unary operator and returns the return value of the operator.
     *
     * @param downstream the operator to apply after resolving the instance of the associated parameter.
     * @return a new resolver which resolves the instance of this parameter and then applies it to the downstream.
     */
    default @NotNull ParameterResolver then(@NotNull UnaryOperator<Object> downstream) {
      return (injector, context) -> {
        Object ourValue = this.resolveInstance(injector, context);
        return downstream.apply(ourValue);
      };
    }
  }
}
