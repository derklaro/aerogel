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

package aerogel;

import aerogel.internal.context.DefaultInjectionContextBuilder;
import java.util.Map;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents a context used to inject a type. It holds all information which are used for the current injection
 * process.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public interface InjectionContext {

  /**
   * Represents a null object which is used to indicate that a value of the overridden types is null.
   */
  Object NIL = new Object();

  /**
   * Constructs a new builder for an {@link InjectionContext}.
   *
   * @return a new builder for an {@link InjectionContext}.
   */
  @Contract(pure = true)
  static @NotNull Builder builder() {
    return new DefaultInjectionContextBuilder();
  }

  /**
   * Gets the injector from which the injection process was triggered and which is used for instance lookups.
   *
   * @return the associated injector of this context.
   */
  @NotNull Injector injector();

  /**
   * Get the current element this context is constructing. This value is only the top level element. If an element
   * request is passed to this context and requires the construct of more element, this method will return the element
   * for which the elements get created.
   *
   * @return the current constructing element.
   * @since 1.2.0
   */
  @NotNull Element currentElement();

  /**
   * Get the types which were overridden when creating this context using the {@link Builder#override(Element, Object)}
   * method.
   *
   * @return the overridden types in this context.
   * @since 1.2.0
   */
  @Unmodifiable
  @NotNull Map<Element, Object> overriddenTypes();

  @Nullable <T> T findConstructedValue(@NotNull Element element);

  /**
   * Tries to find or construct the given element in the known types or the associated injector.
   *
   * @param element the element to search.
   * @param <T>     the type of the expected element.
   * @return the constructed or cached instance for the given {@code element}, may be null.
   * @throws NullPointerException if {@code element} is null.
   * @throws AerogelException     if an exception occurs while constructing the element instance.
   */
  @Nullable <T> T findInstance(@NotNull Element element);

  /**
   * Used to indicate from a {@link BindingHolder} that the construction of the associated element with {@code element}
   * was successfully done.
   *
   * @param element         the element type of the constructed instance.
   * @param result          the resulting constructed instance, may be null.
   * @param doInjectMembers if the members (method, fields) of the constructed object should get injected.
   * @throws NullPointerException if {@code element} is null.
   * @throws AerogelException     if the member injection of the resulting {@code result} failed.
   */
  void constructDone(@NotNull Element element, @Nullable Object result, boolean doInjectMembers);

  /**
   * A builder for an {@link InjectionContext}.
   *
   * @author Pasqual K.
   * @since 1.0
   */
  interface Builder {

    /**
     * Sets the injector of the newly created context.
     *
     * @param injector the injector to use.
     * @return the same instance of the builder as used to call the method, for chaining.
     * @throws NullPointerException if {@code injector} is null.
     */
    @NotNull Builder injector(@NotNull Injector injector);

    /**
     * Overrides the given {@code element} with the given {@code instance}.
     *
     * @param element  the element to override.
     * @param instance the instance to use instead of constructing.
     * @param <T>      the type of the overridden instance.
     * @return the same instance of the builder as used to call the method, for chaining.
     * @throws NullPointerException if {@code element} is null.
     */
    @NotNull <T> Builder override(@NotNull Element element, @Nullable T instance);

    /**
     * Builds the injection context instance. This builder can then be re-used to override other instance or set another
     * injector and rebuild the context.
     *
     * @return the newly created injection context instance.
     * @throws NullPointerException if no injector was set.
     */
    @Contract(pure = true)
    @NotNull InjectionContext build();
  }
}
