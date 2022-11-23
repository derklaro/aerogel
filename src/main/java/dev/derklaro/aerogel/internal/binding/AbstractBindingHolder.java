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

package dev.derklaro.aerogel.internal.binding;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.BindingHolder;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.internal.context.holder.InjectionContextHolder;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract implementation of a binding holder which can be used by any binding to simplify the code.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel")
public abstract class AbstractBindingHolder implements BindingHolder {

  protected final Injector injector;
  protected final Element[] targetType;
  protected final Element bindingType;
  protected final Element[] constructedElements;

  /**
   * Constructs a new abstract binding holder.
   *
   * @param type     the type of the binding.
   * @param binding  the type to which the given type is bound.
   * @param injector the injector to which this binding was bound.
   */
  public AbstractBindingHolder(@NotNull Element[] type, @NotNull Element binding, @NotNull Injector injector) {
    this.targetType = Objects.requireNonNull(type, "Target type is required to construct");
    this.bindingType = Objects.requireNonNull(binding, "Binding type is required to construct");
    this.injector = Objects.requireNonNull(injector, "The parent injector is required to construct");

    // collect the elements which are constructed by this binding
    Set<Element> constructedElements = new LinkedHashSet<>(Arrays.asList(type));
    constructedElements.add(binding);
    this.constructedElements = constructedElements.toArray(new Element[0]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector injector() {
    return this.injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element[] types() {
    return this.targetType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element binding() {
    return this.bindingType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object get() {
    try {
      // enter the current context & get the element
      InjectionContext context = InjectionContextHolder.enter(this.injector);
      Object constructedValue = this.get(context);

      // leave the context & ensure we're done when if we released the context
      if (InjectionContextHolder.leave()) {
        context.ensureComplete();
      }

      // return the value
      return constructedValue;
    } catch (Throwable throwable) {
      // force leave the current injection context and re-throw the exception
      InjectionContextHolder.forceLeave();
      throw AerogelException.forMessagedException("Unable to get bound type of " + this, throwable);
    }
  }

  /**
   * Calls the construct done method on the given context for all bindings types of this binding.
   *
   * @param context       the context to call the construct done indication to.
   * @param constructed   the value that was constructed.
   * @param injectMembers if member injection should be done for the constructed value.
   * @since 2.0
   */
  protected void callConstructDone(
    @NotNull InjectionContext context,
    @Nullable Object constructed,
    boolean injectMembers
  ) {
    // push the constructed value to the context, check if member injection should be done
    boolean doMemberInjection = false;
    for (Element constructedElement : this.constructedElements) {
      doMemberInjection |= context.storeValue(constructedElement, constructed);
    }

    // call the post construct on the given context, do member injection if needed
    for (int i = 0, typeLength = this.constructedElements.length; i < typeLength; i++) {
      context.postConstruct(this.constructedElements[i], constructed, i == 0 && injectMembers && doMemberInjection);
    }
  }
}
