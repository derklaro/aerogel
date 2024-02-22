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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.internal.DefaultInjector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The main part of aerogel. The injector keeps track of all known bindings and shared them with their child injectors.
 * Every injector has always a binding for itself as long as the injector element was not overridden with another
 * biding. In normal cases a developer only interacts once with this injector - to bind all elements he will need later
 * and then to create the main instance of his application. From this point every injection should be done and the main
 * class constructed so that the application can get started. Create a new injector by using {@link #newInjector()}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface Injector {

  /**
   * Creates a new, empty injector instance.
   *
   * @return a new, empty injector instance.
   */
  @Contract(pure = true)
  static @NotNull Injector newInjector() {
    return new DefaultInjector(null);
  }

  /**
   *
   */
  @NotNull
  Optional<Injector> parentInjector();

  /**
   * Creates a new child injector which has this injector as it's parent injector. The child injector has access to all
   * bindings of the parent injector but not vice-versa.
   *
   * @return a new child injector of this injector.
   */
  @NotNull
  Injector createChildInjector();

  /**
   * Creates or gets the instance of the given class type.
   *
   * @param type the type of the element to get.
   * @param <T>  the type of the class modeled by the given class object.
   * @return the constructed instance of the class type, may be null.
   * @throws NullPointerException if {@code type} is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   * @since 1.2.0
   */
  <T> T instance(@NotNull Class<T> type);

  /**
   * Creates or gets the instance of the given type.
   *
   * @param type the type of the element to get.
   * @param <T>  the wildcard type of the element.
   * @return the constructed instance of the type, may be null.
   * @throws NullPointerException if {@code type} is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  <T> T instance(@NotNull Type type);

  /**
   * Get a member injector for all fields and methods annotated as {@literal @}{@code Inject} in the class. The returned
   * member injector is cached in this injector once this method was called for the given holder class, but any other
   * injector in the downstream injector chain will have no access to the injector.
   *
   * @param memberHolderClass the holder class of the members.
   * @return the created or cached member injector for the given {@code memberHolderClass}.
   * @throws NullPointerException if {@code memberHolderClass} is null.
   */
  @NotNull
  <T> MemberInjector<T> memberInjector(@NotNull Class<T> memberHolderClass);

  /**
   * Get the stored binding for the given {@code target} type. This call might create a binding for the given type if
   * the type is constructable. The binding can be stored in the parent injector chain as well.
   *
   * @param target the type of the binding to get.
   * @return the stored binding for the given element target.
   * @throws NullPointerException if {@code target} is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @NotNull
  BindingHolder binding(@NotNull Type target);

  /**
   * Get the stored binding for the given {@code element}. This call might create a binding for the given type if the
   * type is constructable. The binding can be stored in the parent injector chain as well.
   *
   * @param element the element of the binding to get.
   * @return the stored binding for the given element target.
   * @throws NullPointerException if {@code element} is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @NotNull
  BindingHolder binding(@NotNull Element element);

  /**
   * Get all bindings which are constructed for this injector.
   *
   * @return all bindings which are constructed for this injector.
   */
  @Unmodifiable
  @NotNull
  Collection<BindingHolder> bindings();

  /**
   * Registers the given annotation as a scope annotation to this injector. All child injectors will have the
   * information about the given scope present, unless the scope was registered to them specifically (or anywhere
   * downstream of this injector) as well.
   *
   * <p>This method does not validate that the given annotation is marked as &#064;Scope. If the scope annotation is
   * missing on the given annotation, and the scope is applied somewhere, the injector will not be able to understand
   * that the given annotation should get treated as a scope.
   *
   * @param scopeAnno the annotation to associate as a scope with the given scope provider.
   * @param provider  the provider to apply when the scope is requested via the given annotation.
   * @return the same injector as used to call the method, for chaining.
   * @throws NullPointerException if the given scope annotation class or scope provider is null.
   * @since 2.0
   */
  @API(status = API.Status.STABLE, since = "2.0")
  @NotNull
  Injector registerScope(@NotNull Class<? extends Annotation> scopeAnno, @NotNull ScopeProvider provider);

  /**
   * Removes all bindings which are directly registered in this injector and are passing the given filter predicate.
   *
   * @param filter the predicate to filter out the bindings to remove.
   * @return true if a binding was removed as a result of this call, false otherwise.
   * @throws NullPointerException if the given filter is null.
   * @since 2.0
   */
  @API(status = API.Status.EXPERIMENTAL, since = "2.0")
  boolean removeBindings(@NotNull Predicate<BindingHolder> filter);
}
