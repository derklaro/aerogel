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

import dev.derklaro.aerogel.binding.BindingConstructor;
import dev.derklaro.aerogel.binding.BindingHolder;
import dev.derklaro.aerogel.internal.DefaultInjector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

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
   * Get the parent injector of this injector.
   *
   * @return the parent injector of this injector or {@code null} if the injector has no parent.
   */
  @Nullable Injector parent();

  /**
   * Creates a new child injector which has this injector as it's parent injector. The child injector has access to all
   * bindings of the parent injector but not vice-versa.
   *
   * @return a new child injector of this injector.
   */
  @NotNull Injector newChildInjector();

  /**
   * Returns a new specified injector which has this injector as its parent injector. Note that specified injectors can
   * be the parent of a specified injector as well.
   *
   * @return a new specified injector of this injector.
   * @since 2.0
   */
  @API(status = API.Status.EXPERIMENTAL, since = "2.0")
  @NotNull SpecifiedInjector newSpecifiedInjector();

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
  @UnknownNullability <T> T instance(@NotNull Class<T> type);

  /**
   * Creates or gets the instance of the given type.
   *
   * @param type the type of the element to get.
   * @param <T>  the wildcard type of the element.
   * @return the constructed instance of the type, may be null.
   * @throws NullPointerException if {@code type} is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @UnknownNullability <T> T instance(@NotNull Type type);

  /**
   * Creates or gets the instance of the given element.
   *
   * @param element the element to get.
   * @param <T>     the type of the element.
   * @return the constructed instance of the type, may be null.
   * @throws NullPointerException if {@code type} is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @UnknownNullability <T> T instance(@NotNull Element element);

  /**
   * Installs the binding constructed by the given {@code constructor} to this injector.
   *
   * @param constructor the constructor to install.
   * @return the same instance as used to call this method, for chaining.
   * @throws NullPointerException if {@code constructor} is null or the constructor constructs a null value.
   */
  @NotNull Injector install(@NotNull BindingConstructor constructor);

  /**
   * Installs all bindings constructed by each element of the given {@code constructors} to this injector.
   *
   * @param constructors the constructors to install.
   * @return the same instance as used to call this method, for chaining.
   * @throws NullPointerException if {@code constructor} is null or the constructor constructs a null value.
   */
  @NotNull Injector install(@NotNull Iterable<BindingConstructor> constructors);

  /**
   * Get a member injector for all fields and methods annotated as {@literal @}{@code Inject} in the class. This might
   * be a cached injector of this or any injector in the parent injector chain.
   *
   * @param memberHolderClass the holder class of the members.
   * @return the created or cached member injector for the given {@code memberHolderClass}.
   * @throws NullPointerException if {@code memberHolderClass} is null.
   */
  @NotNull MemberInjector memberInjector(@NotNull Class<?> memberHolderClass);

  /**
   * Get the cached member injector of this injector if present.
   *
   * @param memberHolderClass the holder class of the members.
   * @return the cached member injector or null if no member injector is cached.
   * @throws NullPointerException if {@code memberHolderClass} is null.
   */
  @Nullable MemberInjector fastMemberInjector(@NotNull Class<?> memberHolderClass);

  /**
   * Get the stored binding for the given {@code target} type. This call might create a binding for the given type if
   * the type is constructable. The binding can be stored in the parent injector chain as well.
   *
   * @param target the type of the binding to get.
   * @return the stored binding for the given element target.
   * @throws NullPointerException if {@code target} is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @NotNull BindingHolder binding(@NotNull Type target);

  /**
   * Get the stored binding for the given {@code element}. This call might create a binding for the given type if the
   * type is constructable. The binding can be stored in the parent injector chain as well.
   *
   * @param element the element of the binding to get.
   * @return the stored binding for the given element target.
   * @throws NullPointerException if {@code element} is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   */
  @NotNull BindingHolder binding(@NotNull Element element);

  /**
   * Gets the stored binding for the given element or calls the given factory to obtain a binding holder. The binding
   * can be stored in the parent injector chain as well.
   *
   * @param element the element of the binding to get.
   * @param factory the factory to call if no binding is available.
   * @return the stored binding for the given element target.
   * @throws NullPointerException if the given element or factory is null.
   * @throws AerogelException     if no binding is present and no runtime binding can be created.
   * @since 2.0
   */
  @API(status = API.Status.EXPERIMENTAL, since = "2.0")
  @UnknownNullability BindingHolder bindingOr(@NotNull Element element, @NotNull Supplier<BindingHolder> factory);

  /**
   * Get the stored binding for the given {@code element}. The binding can be stored in the parent injector chain as
   * well.
   *
   * @param element the element of the binding to get.
   * @return the stored binding for the given element target or null if no type is stored for the element.
   * @throws NullPointerException if {@code element} is null.
   */
  @Nullable BindingHolder bindingOrNull(@NotNull Element element);

  /**
   * Get the stored binding for the given {@code element} in this injector.
   *
   * @param element the element of the binding to get.
   * @return the stored binding for the given element target or null if no type is stored for the element.
   * @throws NullPointerException if {@code element} is null.
   */
  @Nullable BindingHolder fastBinding(@NotNull Element element);

  /**
   * Get all bindings which are constructed for this injector.
   *
   * @return all bindings which are constructed for this injector.
   */
  @UnmodifiableView
  @NotNull Collection<BindingHolder> bindings();

  /**
   * Get all bindings which are constructed for this injector and every parent injector in the chain.
   *
   * @return all bindings of this and all parent injectors.
   */
  @Unmodifiable
  @NotNull Collection<BindingHolder> allBindings();

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
  @NotNull Injector registerScope(@NotNull Class<? extends Annotation> scopeAnno, @NotNull ScopeProvider provider);

  /**
   * Gets the registered scope provider associated with the given annotation from this or any parent injector. The first
   * scope registration in the tree will be used.
   *
   * @param scopeAnnotation the scope annotation to find the provider of.
   * @return the scope provider associated with the given annotation, null if not registered.
   * @since 2.0
   */
  @API(status = API.Status.STABLE, since = "2.0")
  @Nullable ScopeProvider scope(@NotNull Class<? extends Annotation> scopeAnnotation);

  /**
   * Get the registered scope from this provider. This method will not check any parent injector.
   *
   * @param scopeAnnotation the scope annotation to find the local provider of.
   * @return the scope provider associated with the given annotation in this injector, null if not registered.
   * @since 2.0
   */
  @API(status = API.Status.STABLE, since = "2.0")
  @Nullable ScopeProvider fastScope(@NotNull Class<? extends Annotation> scopeAnnotation);

  /**
   * Get all registered scopes from the full injector tree, travelling up all injectors.
   *
   * @return the registered scopes in all injectors up the tree.
   * @since 2.0
   */
  @Unmodifiable
  @API(status = API.Status.STABLE, since = "2.0")
  @NotNull Collection<ScopeProvider> scopes();

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
