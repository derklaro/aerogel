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

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A getter which can provide the values for parameters of an executable element, in the present order.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@FunctionalInterface
@API(status = API.Status.INTERNAL, since = "2.ß", consumers = "dev.derklaro.aerogel.internal.*")
public interface ParameterValueGetter {

  /**
   * Resolves the instances for each parameter provided to an executable element. The returned object array is in the
   * same order as the provided parameters. The instances for the parameters are resolved through the given injector and
   * injection context.
   *
   * @param context  the context which is currently constructing the elements which needs the parameter instances.
   * @param elements the elements which are representing the return value of the executable element.
   * @param injector the injector which requested the instantiation of the parent executable member.
   * @return the param instances to invoke the executable element, in order.
   * @throws NullPointerException if the given context, elements or injector is null.
   * @throws AerogelException     if an issue occurred while looking up the parameter instances.
   */
  @NotNull Object[] resolveParamInstances(
    @NotNull InjectionContext context,
    @NotNull ContextualProvider<?> provider,
    @NotNull Element[] elements,
    @NotNull Injector injector);
}
