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
import java.lang.reflect.Constructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ConstructingBindingHolder extends AbstractBindingHolder {

  private final InstanceMaker constructor;

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

  public static @NotNull ConstructingBindingHolder create(@NotNull Injector injector, @NotNull Element element) {
    // read the type from the element
    Class<?> type = ReflectionUtils.rawType(element.componentType());
    // read the component data from the class
    ProvidedBy provided = type.getAnnotation(ProvidedBy.class);
    // create a binding holder based on the information
    if (provided != null) {
      return create(injector, element, Element.get(provided.value()));
    } else {
      // check if we can construct the type
      ReflectionUtils.ensureInstantiable(type);
      // get the injection class data from the type
      Constructor<?> injectionPoint = InjectionClassLookup.findInjectableConstructor(type);
      // create the holder
      return new ConstructingBindingHolder(element, element, injector, injectionPoint, JakartaBridge.isSingleton(type));
    }
  }

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

  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T get(@NotNull InjectionContext context) {
    // construct the value
    T value = (T) this.constructor.getInstance(context);
    // push the construction done notice to the context
    context.constructDone(this.targetType, value, false);
    context.constructDone(this.bindingType, value, true);
    // return
    return value;
  }
}
