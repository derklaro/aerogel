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
import dev.derklaro.aerogel.internal.util.InjectorUtil;
import dev.derklaro.aerogel.internal.util.MapUtil;
import dev.derklaro.aerogel.member.MemberInjector;
import dev.derklaro.aerogel.util.Scopes;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The default implementation of an injector. See {@link Injector#newInjector()}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel")
public final class DefaultInjector implements Injector {

  // for trusted injector access
  final InjectorBindingCollection bindings;

  private final Injector parent;
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
    this.bindings = new InjectorBindingCollection();
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
    return (T) ContextInstanceResolveHelper.resolveInstance(element, bindingHolder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector install(@NotNull BindingConstructor constructor) {
    // construct the binding
    BindingHolder holder = constructor.construct(this);
    return this.install(holder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector install(@NotNull Iterable<BindingConstructor> constructors) {
    // allows a write operation to the underlying collection with only a single lock operation
    this.bindings.withTrustedWriteAccess(bindingList -> {
      for (BindingConstructor constructor : constructors) {
        bindingList.add(constructor.construct(this));
      }
    });

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector install(@NotNull BindingHolder bindingHolder) {
    this.bindings.registerBinding(bindingHolder);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjector memberInjector(@NotNull Class<?> memberClazz) {
    return this.cachedMemberInjectors.computeIfAbsent(memberClazz, clazz -> new DefaultMemberInjector(this, clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable MemberInjector fastMemberInjector(@NotNull Class<?> memberHolderClass) {
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
    // check if the element is of the type Injector - return the current injector for it
    if (InjectorUtil.INJECTOR_ELEMENT.equals(element)) {
      return this.injectorBinding;
    }

    // get the binding from the injector tree
    BindingHolder holder = this.bindingOrNull(element);
    if (holder != null) {
      return holder;
    }

    // check if the element has special parameters - in this case we will strictly not mock the element
    if (element.hasSpecialRequirements()) {
      throw AerogelException.forMessageWithoutStack(
        "Element " + element + " has special properties, unable to make a runtime binding for it");
    }

    // construct a runtime binding, store & return it
    BindingHolder constructed = factory.get();
    this.bindings.registerBinding(constructed);
    return constructed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable BindingHolder bindingOrNull(@NotNull Element element) {
    // check if the element is of the type Injector - return the current injector for it
    if (InjectorUtil.INJECTOR_ELEMENT.equals(element)) {
      return this.injectorBinding;
    }

    // try to resolve the element directly from this injector
    BindingHolder bindingHolder = this.fastBinding(element);
    if (bindingHolder != null) {
      return bindingHolder;
    }

    // try to resolve the binding from one of the parent injectors
    if (this.parent != null) {
      Injector injector = this.parent;
      do {
        // get a cached bindingHolder from the parent injector
        bindingHolder = injector.fastBinding(element);
        if (bindingHolder != null) {
          return bindingHolder;
        }

        // check the parent injector of the injector
        injector = injector.parent();
      } while (injector != null);
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable BindingHolder fastBinding(@NotNull Element element) {
    return this.bindings.findBinding(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NotNull Collection<BindingHolder> bindings() {
    return this.bindings.allBindings();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NotNull Collection<BindingHolder> allBindings() {
    // copy the bindings of this injector
    Collection<BindingHolder> bindings = new ArrayList<>();
    this.bindings.trustedBindingsCopyInto(bindings);

    // register all bindings from the parent injectors
    InjectorBindingCollection.copyBindingsFromParents(this.parent, bindings);
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
    // register the scope
    this.scopes.put(scopeAnno, provider);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ScopeProvider scope(@NotNull Class<? extends Annotation> scopeAnnotation) {
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
    if (this.parent != null) {
      // walk down the parent chain - add all of their scopes as well
      scopes.addAll(this.parent.scopes());
    }

    return Collections.unmodifiableCollection(scopes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeBindings(@NotNull Predicate<BindingHolder> filter) {
    return this.bindings.removeMatchingBindings(filter);
  }
}
