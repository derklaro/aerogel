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

package dev.derklaro.aerogel.internal.invoke;

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Provider;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import dev.derklaro.aerogel.internal.utility.ElementHelper;
import java.lang.reflect.Parameter;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A utility to class to make working with parameter construction easier.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.binding.constructors")
public final class ParameterHelper {

  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  private static final BiFunction<InjectionContext, Element[], Object[]> EMPTY_SUPPLIER = (__, ___) -> EMPTY_OBJECT_ARRAY;

  private ParameterHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Constructs a function which takes an injection context and returns a new object array which represents the value
   * for each parameter, in order. This method handles the request to inject a provider appropriately.
   *
   * <p>The returned function throws an {@link ConstructedValueException} if the value representing all given elements
   * (to the apply method of the constructed function) was constructed while constructing a parameter type.
   *
   * @param parameters the parameters which the target executable element takes.
   * @return a function which takes an injection context and returns the value for each parameter.
   * @throws NullPointerException if the given parameter array or one element of it is null.
   */
  @SuppressWarnings("unchecked")
  public static @NotNull BiFunction<InjectionContext, Element[], Object[]> constructParameterSuppliers(
    @NotNull Parameter[] parameters
  ) {
    // if no parameters were given there is nothing to do
    if (parameters.length == 0) {
      return EMPTY_SUPPLIER;
    }

    // build a value supplier for each parameter
    Function<InjectionContext, Object>[] suppliers = new Function[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      // build an element for the parameter at the given index
      Parameter parameter = parameters[i];
      Element element = ElementHelper.buildElement(parameter, parameter.getDeclaredAnnotations());

      // construct the base function to resolve the parameter
      Function<InjectionContext, Object> supplier;
      if (JakartaBridge.isProvider(parameter.getType())) {
        // get the provider for the type & check if we need to bridge to provider as the value takes a jakarta one
        supplier = context -> context.injector().binding(element).provider();
        if (JakartaBridge.needsProviderWrapping(parameter.getType())) {
          supplier = supplier.andThen(provider -> JakartaBridge.bridgeJakartaProvider((Provider<Object>) provider));
        }
      } else {
        // we can construct the value directly from the context
        supplier = context -> context.findInstance(element);
      }

      // set the created supplier
      suppliers[i] = supplier;
    }

    // use the parameters suppliers to construct a function for all parameters
    return (context, trackedElements) -> {
      Object[] values = new Object[suppliers.length];
      for (int i = 0; i < suppliers.length; i++) {
        // get and store each value
        Object value = suppliers[i].apply(context);
        values[i] = value;

        // check if we constructed the target value as a side effect of the previous call
        // and stop the current resolve in that case
        Object constructed = context.findConstructedValue(trackedElements);
        if (constructed != null) {
          throw new ConstructedValueException(constructed);
        }
      }
      return values;
    };
  }
}
