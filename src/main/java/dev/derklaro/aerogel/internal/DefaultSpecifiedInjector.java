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

package dev.derklaro.aerogel.internal;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.BindingConstructor;
import dev.derklaro.aerogel.BindingHolder;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.MemberInjector;
import dev.derklaro.aerogel.SpecifiedInjector;
import dev.derklaro.aerogel.internal.binding.ConstructingBindingHolder;
import dev.derklaro.aerogel.internal.utility.InjectorUtil;
import dev.derklaro.aerogel.internal.utility.MapUtil;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of a specified injector.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.EXPERIMENTAL, since = "2.0")
public final class DefaultSpecifiedInjector implements SpecifiedInjector {

  private final Injector parent;
  private final Map<Element, BindingHolder> specificBindings;

  /**
   * Constructs a new default specified injector instance.
   *
   * @param parent the parent injector of the specified injector.
   * @throws NullPointerException if the given parent is null.
   */
  public DefaultSpecifiedInjector(@NotNull Injector parent) {
    this.parent = Objects.requireNonNull(parent, "parent");
    this.specificBindings = MapUtil.newConcurrentMap();
    this.specificBindings.put(InjectorUtil.INJECTOR_ELEMENT, InjectorUtil.INJECTOR_BINDING_CONSTRUCTOR.construct(this));
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
    return this.parent.install(constructor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector install(@NotNull Iterable<BindingConstructor> constructors) {
    return this.parent.install(constructors);
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public @NotNull MemberInjector memberInjector(@NotNull Class<?> memberHolderClass) {
    return this.parent.memberInjector(memberHolderClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable MemberInjector fastMemberInjector(@NotNull Class<?> memberHolderClass) {
    return this.parent.fastMemberInjector(memberHolderClass);
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
    // get from the local injector or from the parent
    BindingHolder known = this.specificBindings.get(element);
    return known != null ? known : this.parent.binding(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnknownNullability BindingHolder bindingOr(@NotNull Element element, @NotNull BindingConstructor factory) {
    // get from the local injector or from the parent
    BindingHolder known = this.specificBindings.get(element);
    return known != null ? known : this.parent.bindingOr(element, factory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable BindingHolder bindingOrNull(@NotNull Element element) {
    // get from the local injector or from the parent
    BindingHolder known = this.specificBindings.get(element);
    return known != null ? known : this.parent.bindingOrNull(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable BindingHolder fastBinding(@NotNull Element element) {
    // get from the local injector or from the parent
    BindingHolder known = this.specificBindings.get(element);
    return known != null ? known : this.parent.fastBinding(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnmodifiableView @NotNull Collection<BindingHolder> bindings() {
    return Collections.unmodifiableCollection(this.specificBindings.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Unmodifiable @NotNull Collection<BindingHolder> allBindings() {
    // the resulting collection
    Collection<BindingHolder> bindings = new HashSet<>(this.specificBindings.values());
    // walk down the parent chain - add all of their bindings as well
    Injector target = this.parent;
    do {
      // add all bindings to the result
      bindings.addAll(target.bindings());
    } while ((target = target.parent()) != null);
    // the return value should be unmodifiable
    return Collections.unmodifiableCollection(bindings);
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
    // check if we have a known binding
    BindingHolder registered = this.specificBindings.get(element);
    if (registered != null) {
      return (T) registered.get();
    }

    // get from the parent injector
    BindingHolder parentHolder = this.parent.bindingOr(element, $ -> ConstructingBindingHolder.create(this, element));
    return (T) parentHolder.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull SpecifiedInjector installSpecified(@NotNull BindingConstructor constructor) {
    // construct the binding from the constructor & register it
    BindingHolder constructed = constructor.construct(this);
    for (Element type : constructed.types()) {
      this.specificBindings.putIfAbsent(type, constructed);
    }

    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull SpecifiedInjector installSpecified(@NotNull Iterable<BindingConstructor> constructors) {
    // install all bindings
    constructors.forEach(this::installSpecified);
    return this;
  }
}