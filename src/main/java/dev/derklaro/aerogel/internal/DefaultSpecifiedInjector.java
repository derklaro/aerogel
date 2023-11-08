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
import dev.derklaro.aerogel.SpecifiedInjector;
import dev.derklaro.aerogel.binding.BindingConstructor;
import dev.derklaro.aerogel.binding.BindingHolder;
import dev.derklaro.aerogel.internal.context.util.ContextInstanceResolveHelper;
import dev.derklaro.aerogel.internal.member.DefaultMemberInjector;
import dev.derklaro.aerogel.internal.util.InjectorUtil;
import dev.derklaro.aerogel.internal.util.MapUtil;
import dev.derklaro.aerogel.member.MemberInjector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The default implementation of a specified injector.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.EXPERIMENTAL, since = "2.0")
public final class DefaultSpecifiedInjector implements SpecifiedInjector {

  // for trusted injector access
  final InjectorBindingCollection specificBindings;

  private final Injector parent;
  private final Map<Class<?>, MemberInjector> cachedMemberInjectors;

  /**
   * Constructs a new default specified injector instance.
   *
   * @param parent the parent injector of the specified injector.
   * @throws NullPointerException if the given parent is null.
   */
  public DefaultSpecifiedInjector(@NotNull Injector parent) {
    this.parent = parent;
    this.specificBindings = new InjectorBindingCollection();
    this.cachedMemberInjectors = MapUtil.newConcurrentMap();
    this.specificBindings.registerBinding(InjectorUtil.INJECTOR_BINDING_CONSTRUCTOR.construct(this));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector parent() {
    return this.parent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector install(@NotNull BindingConstructor constructor) {
    // construct the binding in this injector and install it in the parent
    BindingHolder bindingHolder = constructor.construct(this);
    return this.parent.install(bindingHolder);
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
    return this.parent.install(bindingHolder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjector memberInjector(@NotNull Class<?> memberHolderClass) {
    return this.cachedMemberInjectors.computeIfAbsent(
      memberHolderClass,
      clazz -> new DefaultMemberInjector(this, clazz));
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
    // get from the local injector if given
    BindingHolder known = this.findSpecifiedBinding(element);
    if (known != null) {
      return known;
    }

    // get or construct the binding from the parent
    return this.parent.bindingOr(element, InjectorUtil.createJITBindingFactory(this, element));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnknownNullability BindingHolder bindingOr(
    @NotNull Element element,
    @NotNull Supplier<BindingHolder> factory
  ) {
    // get from the local injector or from the parent
    BindingHolder known = this.findSpecifiedBinding(element);
    return known != null ? known : this.parent.bindingOr(element, factory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable BindingHolder bindingOrNull(@NotNull Element element) {
    // get from the local injector or from the parent
    BindingHolder known = this.findSpecifiedBinding(element);
    return known != null ? known : this.parent.bindingOrNull(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable BindingHolder fastBinding(@NotNull Element element) {
    // get from the local injector or from the parent
    BindingHolder known = this.findSpecifiedBinding(element);
    return known != null ? known : this.parent.fastBinding(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NotNull Collection<BindingHolder> bindings() {
    return this.specificBindings.allBindings();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NotNull Collection<BindingHolder> allBindings() {
    // copy the bindings of this injector
    Collection<BindingHolder> bindings = new ArrayList<>();
    this.specificBindings.trustedBindingsCopyInto(bindings);

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
    this.parent.registerScope(scopeAnno, provider);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ScopeProvider scope(@NotNull Class<? extends Annotation> scopeAnnotation) {
    return this.parent.scope(scopeAnnotation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ScopeProvider fastScope(@NotNull Class<? extends Annotation> scopeAnnotation) {
    return this.parent.fastScope(scopeAnnotation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NotNull Collection<ScopeProvider> scopes() {
    return this.parent.scopes();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector firstNonSpecifiedParent() {
    Injector parent = this.parent;
    do {
      if (!(parent instanceof SpecifiedInjector)) {
        return parent;
      }
    } while ((parent = parent.parent()) != null);

    // should not reach here
    throw AerogelException.forMessage("Specified injector has no non-specified injector parent");
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
  public <T> @UnknownNullability T instance(@NotNull Class<T> type) {
    return this.instance(Element.forType(type));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T instance(@NotNull Type type) {
    return this.instance(Element.forType(type));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @UnknownNullability T instance(@NotNull Element element) {
    BindingHolder binding = this.binding(element);
    return (T) ContextInstanceResolveHelper.resolveInstance(element, binding);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull SpecifiedInjector installSpecified(@NotNull BindingConstructor constructor) {
    // construct the binding from the constructor & register it
    BindingHolder constructed = constructor.construct(this);
    this.specificBindings.registerBinding(constructed);

    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull SpecifiedInjector installSpecified(@NotNull Iterable<BindingConstructor> constructors) {
    // allows a write operation to the underlying collection with only a single lock operation
    this.specificBindings.withTrustedWriteAccess(bindingList -> {
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
  public boolean removeBindings(@NotNull Predicate<BindingHolder> filter) {
    return this.specificBindings.removeMatchingBindings(filter);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeConstructedBindings() {
    return this.parent.removeBindings(binding -> binding.injector() == DefaultSpecifiedInjector.this);
  }

  /**
   * Tries to find a specified bindings added to this injector which matches the given element.
   *
   * @param element the element to find a binding for.
   * @return the binding that matches the given element, null if no such binding is added to this injector.
   */
  private @Nullable BindingHolder findSpecifiedBinding(@NotNull Element element) {
    return this.specificBindings.findBinding(element);
  }
}
