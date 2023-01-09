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

package dev.derklaro.aerogel.internal;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.ScopeProvider;
import dev.derklaro.aerogel.Singleton;
import dev.derklaro.aerogel.SpecifiedInjector;
import dev.derklaro.aerogel.binding.BindingConstructor;
import dev.derklaro.aerogel.binding.BindingHolder;
import dev.derklaro.aerogel.internal.context.util.ContextInstanceResolveHelper;
import dev.derklaro.aerogel.internal.member.DefaultMemberInjector;
import dev.derklaro.aerogel.internal.utility.InjectorUtil;
import dev.derklaro.aerogel.internal.utility.MapUtil;
import dev.derklaro.aerogel.member.MemberInjector;
import dev.derklaro.aerogel.util.Scopes;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of an injector. See {@link Injector#newInjector()}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel")
public final class DefaultInjector implements Injector {

  private final Injector parent;
  private final Map<Element, BindingHolder> bindings;
  private final Map<Class<?>, MemberInjector> cachedMemberInjectors;
  private final Map<Class<? extends Annotation>, ScopeProvider> scopes;

  // represents the binding for this injector
  private final BindingHolder injectorBinding;

  /**
   * Constructs a new injector using the given {@code parent} injector as its parent.
   *
   * @param parent the parent injector of the new injector, might be null if the injector has no parent.
   */
  public DefaultInjector(@Nullable Injector parent) {
    this.parent = parent;
    this.bindings = MapUtil.newConcurrentMap();
    this.cachedMemberInjectors = MapUtil.newConcurrentMap();
    this.scopes = MapUtil.newConcurrentMap();
    this.injectorBinding = InjectorUtil.INJECTOR_BINDING_CONSTRUCTOR.construct(this);

    // install the singleton scope
    this.registerScope(Singleton.class, Scopes.SINGLETON);
    this.registerScope(jakarta.inject.Singleton.class, Scopes.SINGLETON);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Injector parent() {
    return this.parent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector newChildInjector() {
    return new DefaultInjector(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull SpecifiedInjector newSpecifiedInjector() {
    return new DefaultSpecifiedInjector(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T instance(@NotNull Class<T> type) {
    return this.instance(Element.forType(type));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T instance(@NotNull Type type) {
    return this.instance(Element.forType(type));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T instance(@NotNull Element element) {
    BindingHolder bindingHolder = this.binding(element);
    return (T) ContextInstanceResolveHelper.resolveInstance(element.componentType(), bindingHolder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector install(@NotNull BindingConstructor constructor) {
    Objects.requireNonNull(constructor, "constructor");

    // construct the binding
    BindingHolder holder = constructor.construct(this);
    return this.install(holder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector install(@NotNull Iterable<BindingConstructor> constructors) {
    // install all constructors
    for (BindingConstructor constructor : constructors) {
      this.install(constructor);
    }
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector install(@NotNull BindingHolder bindingHolder) {
    Objects.requireNonNull(bindingHolder, "bindingHolder");

    // apply the binding to this injector
    for (Element type : bindingHolder.types()) {
      this.bindings.putIfAbsent(type, bindingHolder);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjector memberInjector(@NotNull Class<?> memberClazz) {
    Objects.requireNonNull(memberClazz, "memberClazz");
    return this.cachedMemberInjectors.computeIfAbsent(memberClazz, clazz -> new DefaultMemberInjector(this, clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable MemberInjector fastMemberInjector(@NotNull Class<?> memberHolderClass) {
    Objects.requireNonNull(memberHolderClass, "memberHolderClass");
    return this.cachedMemberInjectors.get(memberHolderClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingHolder binding(@NotNull Type target) {
    return this.binding(Element.forType(target));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingHolder binding(@NotNull Element element) {
    return this.bindingOr(element, InjectorUtil.createJITBindingFactory(this, element));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnknownNullability BindingHolder bindingOr(
    @NotNull Element element,
    @NotNull Supplier<BindingHolder> factory
  ) {
    Objects.requireNonNull(element, "element");
    Objects.requireNonNull(factory, "factory");

    // get the binding if a parent has already one
    BindingHolder holder = this.bindingOrNull(element);
    if (holder != null) {
      // cache locally to prevent further deep lookups
      this.bindings.putIfAbsent(element, holder);
      return holder;
    }

    // check if the element is of the type Injector - return the current injector for it
    if (InjectorUtil.INJECTOR_ELEMENT.equals(element)) {
      return this.injectorBinding;
    }

    // check if the element has special parameters - in this case we will strictly not mock the element
    if (element.hasSpecialRequirements()) {
      throw AerogelException.forMessageWithoutStack(
        "Element " + element + " has special properties, unable to make a runtime binding for it");
    }

    // construct a binding and store it, use putIfAbsent to prevent issues when concurrently accessed
    BindingHolder constructed = factory.get();
    BindingHolder present = this.bindings.putIfAbsent(element, constructed);

    // return the constructed or old binding holder
    return present != null ? present : constructed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable BindingHolder bindingOrNull(@NotNull Element element) {
    Objects.requireNonNull(element, "element");
    // check if we have a cached bindingHolder
    BindingHolder bindingHolder = this.bindings.get(element);
    // check if we need a parent injector lookup - skip the parent lookup if the element is the current injector element
    // in this case we always want to inject this injector, not the parent
    if (bindingHolder == null && this.parent != null && !InjectorUtil.INJECTOR_ELEMENT.equals(element)) {
      // check if one of the parents has a cached bindingHolder
      Injector injector = this.parent;
      do {
        // get a cached bindingHolder from the parent injector
        bindingHolder = injector.fastBinding(element);
        if (bindingHolder != null) {
          // found one!
          return bindingHolder;
        }
        // check the parent injector of the injector
        injector = injector.parent();
      } while (injector != null);
    }
    // no bindingHolder cached
    return bindingHolder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable BindingHolder fastBinding(@NotNull Element element) {
    Objects.requireNonNull(element, "element");
    // read from the store
    return this.bindings.get(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnmodifiableView @NotNull Collection<BindingHolder> bindings() {
    return Collections.unmodifiableCollection(this.bindings.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnmodifiableView @NotNull Collection<BindingHolder> allBindings() {
    // the resulting collection
    Collection<BindingHolder> bindings = new HashSet<>(this.bindings.values());
    // check if this injector has a parent
    if (this.parent != null) {
      // walk down the parent chain - add all of their bindings as well
      Injector target = this.parent;
      do {
        // add all bindings to the result
        bindings.addAll(target.bindings());
      } while ((target = target.parent()) != null);
    }
    // the return value should be unmodifiable
    return Collections.unmodifiableCollection(bindings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector registerScope(
    @NotNull Class<? extends Annotation> scopeAnno,
    @NotNull ScopeProvider provider
  ) {
    Objects.requireNonNull(scopeAnno, "scopeAnnotation");
    Objects.requireNonNull(provider, "provider");

    // register the scope
    this.scopes.put(scopeAnno, provider);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ScopeProvider scope(@NotNull Class<? extends Annotation> scopeAnnotation) {
    Objects.requireNonNull(scopeAnnotation, "scopeAnnotation");

    // check if the scope is present locally, if not try to resolve the scope from the parent injector
    ScopeProvider scope = this.scopes.get(scopeAnnotation);
    if (scope == null && this.parent != null) {
      Injector injector = this.parent;
      do {
        // check if the current injector has a scope present
        scope = injector.fastScope(scopeAnnotation);
        if (scope != null) {
          break;
        }
      } while ((injector = injector.parent()) != null);
    }

    // return the resolved scope
    return scope;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ScopeProvider fastScope(@NotNull Class<? extends Annotation> scopeAnnotation) {
    return this.scopes.get(scopeAnnotation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Unmodifiable
  public @NotNull Collection<ScopeProvider> scopes() {
    Collection<ScopeProvider> scopes = new HashSet<>(this.scopes.values());
    // check if this injector has a parent
    if (this.parent != null) {
      // walk down the parent chain - add all of their scopes as well
      Injector target = this.parent;
      do {
        scopes.addAll(target.scopes());
      } while ((target = target.parent()) != null);
    }

    // the return value should be unmodifiable
    return Collections.unmodifiableCollection(scopes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeBindings(@NotNull Predicate<BindingHolder> filter) {
    return this.bindings.values().removeIf(filter);
  }
}
