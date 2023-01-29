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

package dev.derklaro.aerogel.internal.binding;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.ElementMatcher;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.Provider;
import dev.derklaro.aerogel.ScopeProvider;
import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.binding.BindingConstructor;
import dev.derklaro.aerogel.internal.binding.constructors.ConstructingBindingConstructor;
import dev.derklaro.aerogel.internal.binding.constructors.FactoryMethodBindingConstructor;
import dev.derklaro.aerogel.internal.binding.constructors.LazyInstanceBindingConstructor;
import dev.derklaro.aerogel.internal.binding.constructors.LazyProviderBindingConstructor;
import dev.derklaro.aerogel.internal.binding.constructors.ProviderBindingConstructor;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import dev.derklaro.aerogel.internal.reflect.InjectionClassLookup;
import dev.derklaro.aerogel.internal.reflect.TypeUtil;
import dev.derklaro.aerogel.internal.util.ElementHelper;
import dev.derklaro.aerogel.internal.util.Preconditions;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of a binding builder.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@SuppressWarnings("DuplicatedCode")
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.*")
public final class DefaultBindingBuilder implements BindingBuilder {

  private static final ElementMatcher DENYING_MATCHER = element -> false;

  private final Set<ScopeProvider> scopes = new LinkedHashSet<>();
  private final Set<Class<? extends Annotation>> unresolvedScopes = new LinkedHashSet<>();

  private ElementMatcher elementMatcher = DENYING_MATCHER;

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder bind(@NotNull Type type) {
    return this.bind(Element.forType(type));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder bindFully(@NotNull Type type) {
    // build an element from the given type & the raw type
    Class<?> rawType = TypeUtil.rawType(type);
    Element fullElement = ElementHelper.buildElement(type, rawType);

    // apply all scopes
    for (Annotation annotation : rawType.getDeclaredAnnotations()) {
      if (JakartaBridge.isScopeAnnotation(annotation)) {
        this.unresolvedScopes.add(annotation.annotationType());
      }
    }

    return this.bind(fullElement);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder bind(@NotNull Element element) {
    return this.bindMatching(ElementMatcher.matchesOne(element));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder bindFully(@NotNull Element element) {
    // apply all scopes
    Class<?> rawType = TypeUtil.rawType(element.componentType());
    for (Annotation annotation : rawType.getDeclaredAnnotations()) {
      if (JakartaBridge.isScopeAnnotation(annotation)) {
        this.unresolvedScopes.add(annotation.annotationType());
      }
    }

    return this.bind(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder bindAll(@NotNull Type... types) {
    for (Type type : types) {
      this.bind(type);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder bindAllFully(@NotNull Type... types) {
    for (Type type : types) {
      this.bindFully(type);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder bindAll(@NotNull Element... elements) {
    for (Element element : elements) {
      this.bind(element);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder bindAllFully(@NotNull Element... elements) {
    for (Element element : elements) {
      this.bindFully(element);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder bindMatching(@NotNull ElementMatcher matcher) {
    this.elementMatcher = this.elementMatcher.or(matcher);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder scoped(@NotNull ScopeProvider provider) {
    this.scopes.add(provider);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingBuilder scoped(@NotNull Class<? extends Annotation> scopeAnnotation) {
    this.unresolvedScopes.add(scopeAnnotation);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingConstructor toInstance(@Nullable Object instance) {
    if (instance == null) {
      return this.toProvider(Provider.immediate(null));
    } else {
      return this.toProvider(instance.getClass(), Provider.immediate(instance));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingConstructor toLazyInstance(@NotNull Function<Injector, Object> instanceSupplier) {
    // copy over all elements from this builder to allow further uses
    ElementMatcher elementMatcher = this.validateElementMatcher();
    Set<ScopeProvider> scopes = new LinkedHashSet<>(this.scopes);
    Set<Class<? extends Annotation>> unresolvedScopes = new LinkedHashSet<>(this.unresolvedScopes);

    // build the factory
    return new LazyInstanceBindingConstructor(elementMatcher, scopes, unresolvedScopes, instanceSupplier);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingConstructor toLazyProvider(
    @NotNull BiFunction<Element, Injector, Provider<Object>> providerSupplier
  ) {
    // copy over all elements from this builder to allow further uses
    ElementMatcher elementMatcher = this.validateElementMatcher();
    Set<ScopeProvider> scopes = new LinkedHashSet<>(this.scopes);
    Set<Class<? extends Annotation>> unresolvedScopes = new LinkedHashSet<>(this.unresolvedScopes);

    // build the factory
    return new LazyProviderBindingConstructor(elementMatcher, scopes, unresolvedScopes, providerSupplier);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingConstructor toProvider(@NotNull Provider<Object> provider) {
    // copy over all elements from this builder to allow further uses
    ElementMatcher elementMatcher = this.validateElementMatcher();
    Set<ScopeProvider> scopes = new LinkedHashSet<>(this.scopes);
    Set<Class<? extends Annotation>> unresolvedScopes = new LinkedHashSet<>(this.unresolvedScopes);

    // build the factory
    return new ProviderBindingConstructor(elementMatcher, scopes, unresolvedScopes, provider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingConstructor toProvider(@NotNull Class<?> type, @NotNull Provider<Object> provider) {
    // bind the type of the provider as well
    this.bindFully(type);

    // copy over all elements from this builder to allow further uses
    ElementMatcher elementMatcher = this.validateElementMatcher();
    Set<ScopeProvider> scopes = new LinkedHashSet<>(this.scopes);
    Set<Class<? extends Annotation>> unresolvedScopes = new LinkedHashSet<>(this.unresolvedScopes);

    // build the factory
    return new ProviderBindingConstructor(elementMatcher, scopes, unresolvedScopes, provider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingConstructor toConstructing(@NotNull Class<?> type) {
    // find the injection point in the given class
    Constructor<?> constructor = InjectionClassLookup.findInjectableConstructor(type);
    return this.toConstructing(constructor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingConstructor toConstructing(@NotNull Constructor<?> constructor) {
    // bind the declaring class of the constructor as well
    this.bindFully(constructor.getDeclaringClass());

    // copy over all elements from this builder to allow further uses
    ElementMatcher elementMatcher = this.validateElementMatcher();
    Set<ScopeProvider> scopes = new LinkedHashSet<>(this.scopes);
    Set<Class<? extends Annotation>> unresolvedScopes = new LinkedHashSet<>(this.unresolvedScopes);

    // build the factory
    return new ConstructingBindingConstructor(elementMatcher, scopes, unresolvedScopes, constructor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingConstructor toConstructing(@NotNull Class<?> clazz, @NotNull Class<?>... params) {
    try {
      // resolve the given constructor in the target class
      Constructor<?> constructor = clazz.getDeclaredConstructor(params);
      return this.toConstructing(constructor);
    } catch (NoSuchMethodException ignored) {
    }

    // unable to resolve
    throw AerogelException.forMessage(
      "Unable to resolve constructor in class " + clazz + " with params " + Arrays.toString(params));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingConstructor toFactory(@NotNull Method factoryMethod) {
    // validate the method
    Preconditions.checkArgument(Modifier.isStatic(factoryMethod.getModifiers()), "Factory method must be static");
    Preconditions.checkArgument(factoryMethod.getReturnType() != void.class, "Factory method cannot return void");

    // bind the declaring class of the factory method as well
    Element element = ElementHelper.buildElement(
      factoryMethod,
      factoryMethod.getDeclaredAnnotations(),
      factoryMethod.getReturnType().getDeclaredAnnotations());
    this.bindFully(element);

    // apply the scopes of the factory method
    for (Annotation annotation : factoryMethod.getDeclaredAnnotations()) {
      if (JakartaBridge.isScopeAnnotation(annotation)) {
        this.unresolvedScopes.add(annotation.annotationType());
      }
    }

    // copy over all elements from this builder to allow further uses
    ElementMatcher elementMatcher = this.validateElementMatcher();
    Set<ScopeProvider> scopes = new LinkedHashSet<>(this.scopes);
    Set<Class<? extends Annotation>> unresolvedScopes = new LinkedHashSet<>(this.unresolvedScopes);

    // build the factory
    return new FactoryMethodBindingConstructor(elementMatcher, scopes, unresolvedScopes, factoryMethod);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingConstructor toFactory(
    @NotNull Class<?> clazz,
    @NotNull String name,
    @NotNull Class<?>... params
  ) {
    try {
      // resolve the given factory method in the given class, try public first
      Method factoryMethod = clazz.getMethod(name, params);
      return this.toFactory(factoryMethod);
    } catch (NoSuchMethodException ignored) {
    }

    try {
      // resolve the given factory method in the given class, this time try all other types
      Method factoryMethod = clazz.getDeclaredMethod(name, params);
      return this.toFactory(factoryMethod);
    } catch (NoSuchMethodException ignored) {
    }

    // unable to resolve
    throw AerogelException.forMessage(
      "Unable to resolve factory method " + name + " in class " + clazz + " with params " + Arrays.toString(params));
  }

  /**
   * Validates that the element matcher constructed by this binding builder actually has elements to check for.
   *
   * @return the underlying element matcher of this builder if elements were applied to it.
   * @throws AerogelException if no elements were applied to the underlying matcher.
   */
  private @NotNull ElementMatcher validateElementMatcher() {
    Preconditions.checkArgument(this.elementMatcher != DENYING_MATCHER, "No elements to match given");
    return this.elementMatcher;
  }
}
