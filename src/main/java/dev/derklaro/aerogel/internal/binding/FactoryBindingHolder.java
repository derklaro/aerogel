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

package dev.derklaro.aerogel.internal.binding;

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.internal.codegen.FactoryMethodInstanceMaker;
import java.lang.reflect.Method;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A binding which gets instances from a factory method which constructs it.
 *
 * @author Pasqual K.
 * @see dev.derklaro.aerogel.Bindings#factory(Method)
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel")
public final class FactoryBindingHolder extends InstanceMakerBindingHolder {

  /**
   * Constructs a new factory binding holder.
   *
   * @param binding           the type to which the given type is bound.
   * @param injector          the injector to which this binding was bound.
   * @param factoryMethod     the factory method to use to construct the instances.
   * @param shouldBeSingleton if the result of the factory call should be a singleton object.
   * @param type              the type of the binding.
   */
  public FactoryBindingHolder(
    @NotNull Element binding,
    @NotNull Injector injector,
    @NotNull Method factoryMethod,
    boolean shouldBeSingleton,
    @NotNull Element... type
  ) {
    super(type, binding, injector, FactoryMethodInstanceMaker.forMethod(binding, factoryMethod, shouldBeSingleton));
  }
}
