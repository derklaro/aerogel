/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
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

package aerogel.internal.binding;

import aerogel.Element;
import aerogel.InjectionContext;
import aerogel.Injector;
import aerogel.ProvidedBy;
import aerogel.internal.codegen.ClassInstanceMaker;
import aerogel.internal.codegen.InstanceMaker;
import aerogel.internal.jakarta.JakartaBridge;
import aerogel.internal.reflect.InjectionClassLookup;
import aerogel.internal.reflect.ReflectionUtils;
import aerogel.internal.utility.ElementHelper;
import java.lang.reflect.Constructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A binding holder which uses class construction based on a class constructor.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class ConstructingBindingHolder extends AbstractBindingHolder {

  private final InstanceMaker constructor;

  /**
   * Creates a new constructing binding instance.
   *
   * @param targetType        the type of the binding.
   * @param bindingType       the type to which the given type is bound.
   * @param injector          the injector to which this binding was bound.
   * @param injectionPoint    the constructor to use to create the class instances.
   * @param shouldBeSingleton if the result of the instantiation call should be a singleton object.
   */
  public ConstructingBindingHolder(
    @NotNull Element targetType,
    @NotNull Element bindingType,
    @NotNull Injector injector,
    @NotNull Constructor<?> injectionPoint,
    boolean shouldBeSingleton
  ) {
    super(targetType, bindingType, injector);
    this.constructor = ClassInstanceMaker.forConstructor(injectionPoint, shouldBeSingleton);
  }

  /**
   * Makes a new constructing binding holder for the given {@code element}.
   *
   * @param injector the injector to which this binding was bound.
   * @param element  the type of the binding.
   * @return the created constructing binding holder.
   * @throws UnsupportedOperationException if the element's type is not instantiable.
   */
  public static @NotNull ConstructingBindingHolder create(@NotNull Injector injector, @NotNull Element element) {
    // read the type from the element
    Class<?> type = ReflectionUtils.rawType(element.componentType());
    // read the component data from the class
    ProvidedBy provided = type.getAnnotation(ProvidedBy.class);
    // create a binding holder based on the information
    if (provided != null) {
      return create(injector, element, ElementHelper.buildElement(provided.value()));
    } else {
      // check if we can construct the type
      ReflectionUtils.ensureInstantiable(type);
      // get the injection class data from the type
      Constructor<?> injectionPoint = InjectionClassLookup.findInjectableConstructor(type);
      // create the holder
      return new ConstructingBindingHolder(element, element, injector, injectionPoint, JakartaBridge.isSingleton(type));
    }
  }

  /**
   * Makes a new constructing binding holder for the given {@code bound}.
   *
   * @param injector the injector to which this binding was bound.
   * @param element  the type of the binding.
   * @param bound    the type to which the element was bound.
   * @return the created constructing binding holder.
   * @throws UnsupportedOperationException if the bound's type is not instantiable.
   */
  public static @NotNull ConstructingBindingHolder create(
    @NotNull Injector injector,
    @NotNull Element element,
    @NotNull Element bound
  ) {
    // read the type from the bound element
    Class<?> type = ReflectionUtils.rawType(bound.componentType());
    // check if we can construct the type
    ReflectionUtils.ensureInstantiable(type);
    // get the injection class data from the component type
    boolean singleton = JakartaBridge.isSingleton(type);
    Constructor<?> injectionPoint = InjectionClassLookup.findInjectableConstructor(type);
    // create the holder
    return new ConstructingBindingHolder(element, bound, injector, injectionPoint, singleton);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T get(@NotNull InjectionContext context) {
    // construct the value
    T value = (T) this.constructor.getInstance(context);
    // push the construction done notice to the context
    context.constructDone(this.targetType, value, true);
    context.constructDone(this.bindingType, value, false);
    // return
    return value;
  }
}
