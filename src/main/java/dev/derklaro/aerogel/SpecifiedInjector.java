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

package dev.derklaro.aerogel;

import java.lang.reflect.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents a specific injector instance which only provides access to specifically installed bindings. All other
 * calls will be redirected to the parent injector, therefore this injector will never try to construct types in the
 * runtime which aren't specifically requested.
 *
 * <p>Note that a specific injector might register bindings to the parent injector when runtime binding construction is
 * needed. This is due to the fact that construction bindings are bound to an injector, but in order to assure that the
 * constructing holder knows all elements from the current injector, and the parent injector is aware of the
 * construction and can re-use the binding, this behaviour is required. This behaviour is for example useful if multiple
 * injectors with the same type are required, but all injectors should share the same instances. For example: when
 * writing a plugin system, each plugin should be able to get their description injected (specifically bound), but
 * should be able to access the constructed instances from other plugins (requested through the specified injector,
 * registered in the parent).
 * <br><strong>If this behaviour is not requested, try using a child injector instead.</strong>
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.EXPERIMENTAL, since = "2.0")
public interface SpecifiedInjector extends Injector {

  /**
   * Get the first injector which is not a specified injector in the current chain.
   *
   * @return the first non-specified parent injector.
   */
  @NotNull Injector firstNonSpecifiedParent();

  /**
   * Creates a new child injector from <strong>the first non-specified injector</strong> in the chain. For more info
   * about child injectors see {@link Injector#newChildInjector()}.
   *
   * @return a new child injector from a non-specified injector.
   */
  @Override
  @NotNull Injector newChildInjector();

  /**
   * Creates a new specified injector with this injector as its parent.
   *
   * @return a new specified injector of this injector.
   */
  @Override
  @NotNull SpecifiedInjector newSpecifiedInjector();

  /**
   * Tries to construct an instance from the installed bindings in this specified injector, falls back to the parent
   * injector if no binding is known to this injector.
   *
   * @param type the type of the element to get.
   * @param <T>  the type of the class modeled by the given class object.
   * @return the constructed instance of the class type, may be null.
   * @throws NullPointerException if the given type is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @Override
  <T> @UnknownNullability T instance(@NotNull Class<T> type);

  /**
   * Tries to construct an instance from the installed bindings in this specified injector, falls back to the parent
   * injector if no binding is known to this injector.
   *
   * @param type the type of the element to get.
   * @param <T>  the wildcard type of the element.
   * @return the constructed instance of the type, may be null.
   * @throws NullPointerException if the given type is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @Override
  <T> @UnknownNullability T instance(@NotNull Type type);

  /**
   * Tries to construct an instance from the installed bindings in this specified injector, falls back to the parent
   * injector if no binding is known to this injector.
   *
   * @param element the element to get.
   * @param <T>     the type of the element.
   * @return the constructed instance of the type, may be null.
   * @throws NullPointerException if the given element is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @Override
  <T> @UnknownNullability T instance(@NotNull Element element);

  /**
   * Installs a binding constructor which is specifically bound to this injector and should be used when instances are
   * requested over the use of the parent injector.
   *
   * @param constructor the constructor to use for registration.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given constructor is null.
   */
  @NotNull SpecifiedInjector installSpecified(@NotNull BindingConstructor constructor);

  /**
   * Installs the binding constructors which are specifically bound to this injector and should be used when instances
   * are requested over the use of the parent injector.
   *
   * @param constructors the constructors to use for registration.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given constructor iterable is null.
   */
  @NotNull SpecifiedInjector installSpecified(@NotNull Iterable<BindingConstructor> constructors);
}
