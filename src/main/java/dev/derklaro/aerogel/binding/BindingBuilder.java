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

package dev.derklaro.aerogel.binding;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.Provider;
import dev.derklaro.aerogel.ScopeProvider;
import dev.derklaro.aerogel.internal.binding.DefaultBindingBuilder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A builder which allows the simple creation of bindings.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.STABLE, since = "2.0")
public interface BindingBuilder {

  /**
   * Constructs a new builder instance.
   *
   * @return a new builder instance.
   */
  @Contract(pure = true)
  static @NotNull BindingBuilder create() {
    return new DefaultBindingBuilder();
  }

  /**
   * Binds the given type as one type that is handled by the final call to one of building methods. This methods will
   * not check for any scope annotations on the given type, use {@link #bindFully(Type)} for that instead.
   *
   * @param type the type to bind.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given type is null.
   */
  @NotNull BindingBuilder bind(@NotNull Type type);

  /**
   * Binds the given type as one type that is handled by the final call to one of building methods. This method will
   * apply all scopes present on the given type to this builder as well.
   *
   * <p>Note that while this method accepts a generic type, you can only pass one of:
   * <ol>
   *   <li>Classes
   *   <li>Generic Array Types (where the component type matches this list as well)
   *   <li>Parameterized Types (where the raw type matches this list as well)
   * </ol>
   *
   * @param type the type to bind.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given type is null.
   */
  @NotNull BindingBuilder bindFully(@NotNull Type type);

  /**
   * Binds the given element as one type that is handled by the final call to one of building methods. This methods will
   * not check for any scope annotations on the given type, use {@link #bindFully(Element)} for that instead.
   *
   * @param element the element to bind.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given element is null.
   */
  @NotNull BindingBuilder bind(@NotNull Element element);

  /**
   * Binds the given element as one type that is handled by the final call to one of building methods. This method will
   * apply all scopes present on the given type to this builder as well.
   *
   * <p>Note that while this method accepts an element which contains a generic type, you can only pass one of:
   * <ol>
   *   <li>Classes
   *   <li>Generic Array Types (where the component type matches this list as well)
   *   <li>Parameterized Types (where the raw type matches this list as well)
   * </ol>
   *
   * @param element the element to bind.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given type is null.
   */
  @NotNull BindingBuilder bindFully(@NotNull Element element);

  /**
   * Binds all the given types as types that are handled by the final call to one of building methods. This methods will
   * not check for any scope annotations on the given types, use {@link #bindAllFully(Type...)} for that instead.
   *
   * @param types the types to bind.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given type array or one element is null.
   */
  @NotNull BindingBuilder bindAll(@NotNull Type... types);

  /**
   * Binds all the given types as types that are handled by the final call to one of building methods. This method will
   * apply all scopes present on the given type to this builder as well.
   *
   * <p>Note that while this method accepts a generic types, you can only pass one of:
   * <ol>
   *   <li>Classes
   *   <li>Generic Array Types (where the component type matches this list as well)
   *   <li>Parameterized Types (where the raw type matches this list as well)
   * </ol>
   *
   * @param types the types to bind.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given type array or one element is null.
   */
  @NotNull BindingBuilder bindAllFully(@NotNull Type... types);

  /**
   * Binds all the given element component types as types that are handled by the final call to one of building methods.
   * This methods will not check for any scope annotations on the given types, use {@link #bindAllFully(Element...)} for
   * that instead.
   *
   * @param elements the elements to bind.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given element array or one element is null.
   */
  @NotNull BindingBuilder bindAll(@NotNull Element... elements);

  /**
   * Binds all the given element component types as types that are handled by the final call to one of building methods.
   * This method will apply all scopes present on the given type to this builder as well.
   *
   * <p>Note that while this method accepts elements that contain generic types, you can only pass one of:
   * <ol>
   *   <li>Classes
   *   <li>Generic Array Types (where the component type matches this list as well)
   *   <li>Parameterized Types (where the raw type matches this list as well)
   * </ol>
   *
   * @param elements the elements to bind.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given element array or one element is null.
   */
  @NotNull BindingBuilder bindAllFully(@NotNull Element... elements);

  /**
   * Applies the given scope provider as a scope to the final constructed binding holders. The order of applied scopes
   * is fifo, and applying the same scope twice will ignore the second call. Note that the resolved scopes added by this
   * method are applied before the unresolved scopes (added by {@link #scoped(Class)}).
   *
   * @param provider the scope provider to apply.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given provider is null.
   */
  @NotNull BindingBuilder scoped(@NotNull ScopeProvider provider);

  /**
   * Applies the given scope annotation as an unresolved scope to the final constructed binding holders. The order of
   * unresolved scopes is fifo, and applying the same scope annotation twice will ignore the second call. Note that the
   * resolved scopes (added by {@link #scoped(ScopeProvider)}) are applied before the unresolved scopes.
   *
   * <p>The target injector of the build binding constructor must have a mapping for this scope present, if no mapping
   * can be found, the construction of the binding holder will result in an exception.
   *
   * @param scopeAnnotation the scope annotation to apply.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given scope annotation is null.
   * @see Injector#registerScope(Class, ScopeProvider)
   */
  @NotNull BindingBuilder scoped(@NotNull Class<? extends Annotation> scopeAnnotation);

  /**
   * Constructs a new binding which targets all elements and scopes added to this builder. The binding will be bound to
   * the given object instance and always immediately return the given instance.
   *
   * <p>This method will fully bind the instance type if the given instance is present (as returned by
   * {@code instance.getClass()}).
   *
   * <p>Note: after calling this method all values will be copied in the constructed binding, meaning that this builder
   * can be re-used with the previously added values without accidentally leaking changes into the created binding
   * constructor.
   *
   * @param instance the instance to bind to, might be null.
   * @return a binding constructor targeting all elements and scopes, returning the given instance immediately.
   */
  @NotNull BindingConstructor toInstance(@Nullable Object instance);

  /**
   * Constructs a new binding which targets all elements and scopes added to this builder. The binding will be bound to
   * the given function and return the value computed by the given function each time the underlying binding is
   * requested.
   *
   * <p>The given function will receive the current injector of the construction context.
   *
   * <p>Note: after calling this method all values will be copied in the constructed binding, meaning that this builder
   * can be re-used with the previously added values without accidentally leaking changes into the created binding
   * constructor.
   *
   * @param instanceSupplier the function to call each time an injector requests an instance from the binding.
   * @return a binding constructor targeting all elements and scopes, returning the call result of the given function.
   * @throws NullPointerException if the given supplier is null.
   */
  @NotNull BindingConstructor toLazyInstance(@NotNull Function<Injector, Object> instanceSupplier);

  /**
   * Constructs a new binding which targets all elements and scopes added to this builder. The binding will be bound to
   * the given provider and will request the instance from the provider each time an injector requests the instance.
   *
   * <p>If the given provider is a contextual provider, the current injection context will be passed to the provider if
   * it is available.
   *
   * <p>Note: after calling this method all values will be copied in the constructed binding, meaning that this builder
   * can be re-used with the previously added values without accidentally leaking changes into the created binding
   * constructor.
   *
   * @param provider the provider to call each time an injector requests an instance from the binding.
   * @return a binding constructor targeting all elements and scopes, returning the call result of the given provider.
   * @throws NullPointerException if the given provider is null.
   */
  @NotNull BindingConstructor toProvider(@NotNull Provider<Object> provider);

  /**
   * Constructs a new binding which targets all elements and scopes added to this builder. The binding will be bound to
   * the given provider and will request the instance from the provider each time an injector requests the instance.
   *
   * <p>If the given provider is a contextual provider, the current injection context will be passed to the provider if
   * it is available.
   *
   * <p>The given type will be fully bound in this builder.
   *
   * <p>Note: after calling this method all values will be copied in the constructed binding, meaning that this builder
   * can be re-used with the previously added values without accidentally leaking changes into the created binding
   * constructor.
   *
   * @param type     the component type of the given provider. This type will be fully bound in this builder.
   * @param provider the provider to call each time an injector requests an instance from the binding.
   * @return a binding constructor targeting all elements and scopes, returning the call result of the given provider.
   * @throws NullPointerException if the given provider is null.
   */
  @NotNull BindingConstructor toProvider(@NotNull Class<?> type, @NotNull Provider<Object> provider);

  /**
   * Constructs a new binding which targets all elements and scopes added to this builder. The binding will be bound to
   * the injectable constructor in the given class and will call the constructor each time a new instance gets
   * requested.
   *
   * <p>The selection of the injectable constructor is done in 3 possible ways (in order). If one steps succeeds all
   * other checks are not executed:
   * <ol>
   *   <li>Searches for a constructor which is annotated as &#064;Inject.
   *   <li>If the given class is a record the constructor with all record component arguments is used.
   *   <li>The constructor which takes no arguments.
   * </ol>
   *
   * <p>The injectable constructor can use any visibility level, however on some JVMs this method might fail if the
   * caller has no direct access to the constructor and the constructor must be made accessible.
   *
   * <p>The given class will be fully bound in this builder.
   *
   * <p>Note: after calling this method all values will be copied in the constructed binding, meaning that this builder
   * can be re-used with the previously added values without accidentally leaking changes into the created binding
   * constructor.
   *
   * @param type the type to construct when an instance is requested from an injector.
   * @return a binding constructor targeting all elements and scopes, returning a new instance of the given type.
   * @throws NullPointerException if the given class type is null.
   * @throws AerogelException     if no or multiple injectable constructors were found in the given class.
   */
  @NotNull BindingConstructor toConstructing(@NotNull Class<?> type);

  /**
   * Constructs a new binding which targets all elements and scopes added to this builder. The binding will be bound to
   * the given constructor and will call the constructor each time a new instance gets requested.
   *
   * <p>The given constructor can use any visibility level, however on some JVMs this method might fail if the
   * caller has no direct access to the constructor and the constructor must be made accessible.
   *
   * <p>The declaring class of the given constructor will be fully bound in this builder.
   *
   * <p>Note: after calling this method all values will be copied in the constructed binding, meaning that this builder
   * can be re-used with the previously added values without accidentally leaking changes into the created binding
   * constructor.
   *
   * @param constructor the constructor to call when an instance is requested from an injector.
   * @return a binding constructor targeting all elements and scopes, returning a new instance using the constructor.
   * @throws NullPointerException if the given constructor is null.
   */
  @NotNull BindingConstructor toConstructing(@NotNull Constructor<?> constructor);

  /**
   * Constructs a new binding which targets all elements and scopes added to this builder. The binding will be bound to
   * the constructor in the given target class with the given parameter types and will call the constructor each time a
   * new instance gets requested.
   *
   * <p>The target constructor can use any visibility level, however on some JVMs this method might fail if the
   * caller has no direct access to the constructor and the constructor must be made accessible.
   *
   * <p>The declaring class of the given constructor will be fully bound in this builder.
   *
   * <p>Note: after calling this method all values will be copied in the constructed binding, meaning that this builder
   * can be re-used with the previously added values without accidentally leaking changes into the created binding
   * constructor.
   *
   * @param clazz  the class to search the constructor in.
   * @param params the param types of the constructor to use.
   * @return a binding constructor targeting all elements and scopes, returning a new instance using the constructor.
   * @throws NullPointerException if the given constructor is null.
   * @throws AerogelException     if the no constructor with the given params can be found in the target class.
   */
  @NotNull BindingConstructor toConstructing(@NotNull Class<?> clazz, @NotNull Class<?>... params);

  /**
   * Constructs a new binding which targets all elements and scopes added to this builder. The binding will be bound to
   * the given factory method which will be called each time a new instance gets requested.
   *
   * <p>A factory method must match the following requirements:
   * <ol>
   *   <li>The method must be static.
   *   <li>The method is not allowed to return {@code void}.
   * </ol>
   *
   * <p>The target method can use any visibility level, however on some JVMs this method might fail if the
   * caller has no direct access to the constructor and the constructor must be made accessible.
   *
   * <p>The return type of the given factory method will be fully bound in this builder including all scopes and
   * qualifiers added directly to the factory method.
   *
   * <p>Note: after calling this method all values will be copied in the constructed binding, meaning that this builder
   * can be re-used with the previously added values without accidentally leaking changes into the created binding
   * constructor.
   *
   * @param factoryMethod the factory method to call when an instance is requested from an injector.
   * @return a binding constructor targeting all elements and scopes, returning a new instance using the factory method.
   * @throws NullPointerException if the given factory method is null.
   * @throws AerogelException     if the given method is either not static or returns {@code void}.
   */
  @NotNull BindingConstructor toFactory(@NotNull Method factoryMethod);

  /**
   * Constructs a new binding which targets all elements and scopes added to this builder. The binding will be bound to
   * the factory method declared in the given class with the given name and parameter types which will be called each
   * time a new instance gets requested.
   *
   * <p>A factory method must match the following requirements:
   * <ol>
   *   <li>The method must be static.
   *   <li>The method is not allowed to return {@code void}.
   * </ol>
   *
   * <p>This method will search for an inherited method from the given class tree as well.
   *
   * <p>The target method can use any visibility level, however on some JVMs this method might fail if the
   * caller has no direct access to the constructor and the constructor must be made accessible.
   *
   * <p>The return type of the given factory method will be fully bound in this builder including all scopes and
   * qualifiers added directly to the factory method.
   *
   * <p>Note: after calling this method all values will be copied in the constructed binding, meaning that this builder
   * can be re-used with the previously added values without accidentally leaking changes into the created binding
   * constructor.
   *
   * @param clazz  the class which declares the target factory method.
   * @param name   the name of the given factory method.
   * @param params the parameter types of the target factory method.
   * @return a binding constructor targeting all elements and scopes, returning a new instance using the factory method.
   * @throws NullPointerException if the given class, name or parameter array is null.
   * @throws AerogelException     if the target method cannot be found or is either not static or returns {@code void}.
   */
  @NotNull BindingConstructor toFactory(@NotNull Class<?> clazz, @NotNull String name, @NotNull Class<?>... params);
}
