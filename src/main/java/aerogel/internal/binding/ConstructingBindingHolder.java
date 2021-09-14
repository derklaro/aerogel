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
import aerogel.internal.reflect.InjectionClassData;
import aerogel.internal.reflect.InjectionClassLookup;
import aerogel.internal.reflect.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ConstructingBindingHolder extends AbstractBindingHolder {

  private final InstanceMaker constructor;

  public ConstructingBindingHolder(
    @NotNull Element targetType,
    @NotNull Element bindingType,
    @NotNull Injector injector,
    @NotNull InjectionClassData data,
    boolean shouldBeSingleton
  ) {
    super(targetType, bindingType, injector);
    this.constructor = ClassInstanceMaker.forConstructor(data.injectConstructor(), shouldBeSingleton);
  }

  public static @NotNull ConstructingBindingHolder create(@NotNull Injector injector, @NotNull Element element) {
    // ensure that the element is a class
    if (!(element.componentType() instanceof Class<?>)) {
      throw new UnsupportedOperationException(
        "Unable to create dynamic binding for non-class " + element.componentType());
    }
    // cast to class for easier access
    Class<?> type = (Class<?>) element.componentType();
    // read the component data from the class
    ProvidedBy provided = type.getAnnotation(ProvidedBy.class);
    // create a binding holder based on the information
    if (provided != null) {
      return create(injector, element, Element.get(provided.value()));
    } else {
      // check if we can construct the type
      ReflectionUtils.ensureInstantiable(type);
      // get the injection class data from the type
      InjectionClassData data = InjectionClassLookup.lookup(type);
      // create the holder
      return new ConstructingBindingHolder(element, element, injector, data, JakartaBridge.isSingleton(type));
    }
  }

  public static @NotNull ConstructingBindingHolder create(
    @NotNull Injector injector,
    @NotNull Element type,
    @NotNull Element bound
  ) {
    // ensure that the element is a class
    if (!(type.componentType() instanceof Class<?>)) {
      throw new UnsupportedOperationException(
        "Unable to create dynamic binding for non-class " + type.componentType());
    }
    // ensure that the binding is a class
    if (!(bound.componentType() instanceof Class<?>)) {
      throw new UnsupportedOperationException(
        "Unable to create dynamic binding for non-class " + bound.componentType());
    }
    // get the injection class data from the component type
    boolean singleton = JakartaBridge.isSingleton((Class<?>) type.componentType())
      || JakartaBridge.isSingleton((Class<?>) bound.componentType());
    InjectionClassData data = InjectionClassLookup.lookup((Class<?>) bound.componentType());
    // create the holder
    return new ConstructingBindingHolder(type, bound, injector, data, singleton);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T get(@NotNull InjectionContext context) {
    // construct the value
    T value = (T) this.constructor.getInstance(context);
    // push the construction done notice to the context
    context.constructDone(this.targetType, value);
    context.constructDone(this.bindingType, value);
    // return
    return value;
  }
}
