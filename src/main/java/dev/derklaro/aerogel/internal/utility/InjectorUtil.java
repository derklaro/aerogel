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

package dev.derklaro.aerogel.internal.utility;

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.ProvidedBy;
import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.binding.BindingConstructor;
import dev.derklaro.aerogel.binding.BindingHolder;
import dev.derklaro.aerogel.internal.reflect.TypeUtil;
import dev.derklaro.aerogel.util.Scopes;
import java.util.function.Supplier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class which makes it easier to work with injector bindings.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel")
public final class InjectorUtil {

  /**
   * An element which just represents an injector, without special requirements.
   */
  public static final Element INJECTOR_ELEMENT = Element.forType(Injector.class);
  /**
   * A binding constructor which creates a new binding holder for the injector calling the method.
   */
  public static final BindingConstructor INJECTOR_BINDING_CONSTRUCTOR = BindingBuilder.create()
    .bind(INJECTOR_ELEMENT)
    .scoped(Scopes.SINGLETON)
    .toLazyInstance(injector -> injector);

  private InjectorUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * A factory to create a new just-in-time binding for the given element in the given injector.
   *
   * @param injector the injector to construct the binding for.
   * @param element  the element to bind to.
   * @return the created binding holder factory.
   * @throws NullPointerException if the given injector or element is null.
   */
  public static @NotNull Supplier<BindingHolder> createJITBindingFactory(
    @NotNull Injector injector,
    @NotNull Element element
  ) {
    return () -> {
      // resolve the raw type of the given element
      Class<?> rawType = TypeUtil.rawType(element.componentType());

      // check if @ProvidedBy is added to the type
      ProvidedBy providedBy = rawType.getDeclaredAnnotation(ProvidedBy.class);
      if (providedBy != null) {
        return BindingBuilder.create().bindFully(element).toConstructing(providedBy.value()).construct(injector);
      } else {
        return BindingBuilder.create().bindFully(element).toConstructing(rawType).construct(injector);
      }
    };
  }
}
