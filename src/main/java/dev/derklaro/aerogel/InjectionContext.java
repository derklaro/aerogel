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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.internal.context.DefaultInjectionContextBuilder;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the context that is used as a node in a construction tree. A construction tree has at its root the actual
 * type that gets constructed, followed by a subcontext for each parameter, its sub-parameters and so on.
 *
 * <p>An example of a tree might look something like this:
 * <pre>
 *    [ StringHolder ]
 *       /       \
 *    [ Arg1 ]  [ Arg 2 ]
 *     /          /      \
 * [ Arg1-1 ]  [ Arg2-1 ] [ Arg2-2 ]
 * </pre>
 *
 * <p>A context might proxy types in order to support circular references. Proxies are inserted in form of virtual
 * contexts into an injection context, and the node is removed after the construction of the proxied type completed and
 * the proxy has a delegate available.
 *
 * <p>If a provider returns a {@link KnownValue} back to this context, the value returned by the provider will be
 * stored by the current context and will be available to the <strong>subtree</strong> of the associated context.
 * Proxies that were created to support circular references will all be completed with the value wrapped by the known
 * value, even if the proxy was <strong>not</strong> created by the current subtree.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface InjectionContext {

  /**
   * Constructs a new builder for an {@link InjectionContext}.
   *
   * @param constructingType the type that should be constructed.
   * @param callingProvider  the provider for which the context should get created.
   * @return a new builder for an {@link InjectionContext}.
   */
  @Contract(pure = true)
  static @NotNull Builder builder(@NotNull Type constructingType, @NotNull ContextualProvider<?> callingProvider) {
    return new DefaultInjectionContextBuilder(constructingType, callingProvider);
  }

  /**
   * Get the type that this injection context is currently constructing.
   *
   * @return the type that this context is currently constructing.
   */
  @NotNull Type constructingType();

  /**
   * Get the provider that is associated with this context.
   *
   * @return the provider associated with this context.
   */
  @NotNull ContextualProvider<?> callingProvider();

  /**
   * Resolves a provider for the given element. The resolve is done via the injector that is associated with this
   * context. If the root provider has an override for the given element present, a provider which represents the
   * overridden value is returned instead.
   *
   * @param element the element to resolve the provider for.
   * @return a provider that allows to resolve the underlying value of the given element.
   */
  @NotNull ContextualProvider<?> resolveProvider(@NotNull Element element);

  /**
   * Get the next context in the tree or null if this context is a leaf.
   *
   * @return the next context in the tree.
   */
  @Nullable InjectionContext next();

  /**
   * Get the previous context in the tree or null if this context is the root.
   *
   * @return the previous context in the tree.
   */
  @Nullable InjectionContext prev();

  /**
   * Get the root context in the current tree, this is a shortcut over iterating the tree until the root context is
   * reached.
   *
   * <p>Note that this method will always return a context, when called on the root context it will return the same
   * context as the method is called on.
   *
   * @return the root context of the current tree.
   */
  @NotNull InjectionContext root();

  /**
   * Enters a subcontext for the provider. If the provider was already encountered in the current tree, this context
   * will try to proxy either the context that is associated with the given provider or the current context. If both
   * attempts fail, an exception is thrown containing the full tree which was traversed until the context was hit. If
   * the current context gets proxied, the method is free to throw an exception to indicate that behaviour to the
   * calling method.
   *
   * <p>If the requested type was overridden a context is returned that instantly delegates the call to the overridden
   * value.
   *
   * @param provider the provider which requested the subcontext.
   * @param type     the type that should get constructed.
   * @return a new subcontext for the provider, might be a virtual context in case the type was proxied.
   * @throws AerogelException if an exception occurred while trying to proxy a type.
   */
  @NotNull InjectionContext enterSubcontext(@NotNull ContextualProvider<?> provider, @NotNull Type type);

  /**
   * Tries to resolve the instance that is represented by the underlying provider.
   *
   * @return the constructed type of the underlying provider, might be null.
   */
  @Nullable Object resolveInstance();

  /**
   * Adds a listener to this context that will be called once the construction of the type associated with this context
   * was constructed successfully. Note that listeners added to the context after a construction finished will not get
   * called for previous constructions, only for possible later ones.
   *
   * @param listener the listener to add.
   */
  void addConstructionListener(@NotNull BiConsumer<InjectionContext, Object> listener);

  /**
   * Requests the injection of members (fields and methods) into the given constructed value. Note that member
   * injections will be executed once at the end of the construction by the root context.
   *
   * <p>If the given value is null the type represented by this context is used and only static members will get
   * injected. In all other cases the type of the given value will be used (as defined by {@code value.getClass()}).
   *
   * @param value the value to inject members into, can be null.
   */
  void requestMemberInjection(@Nullable Object value);

  /**
   * Indicates that the construction process was completed successfully. The context should validate that there are no
   * more proxies without a delegate and execute all member injection requests.
   *
   * @throws AerogelException if this method was called on a non-root context.
   */
  void finishConstruction();

  /**
   * Get if this context is virtual. A virtual context is inserted into the tree but might be removed at a later step.
   * This might for example be the case for a proxied type.
   *
   * <p>In normal cases the context tree does not allow for duplicate nodes, but virtual contexts are allowed to exists
   * multiple times alongside a real context.
   *
   * @return true if this context is virtual, false otherwise.
   */
  boolean virtualContext();

  /**
   * Get if this context is the root of a tree.
   *
   * @return true if this is the root context of a context tree, false otherwise.
   */
  boolean rootContext();

  /**
   * Get if this context is the leaf of a tree.
   *
   * @return true if this is currently a leaf context of a context tree, false otherwise.
   */
  boolean leafContext();

  /**
   * A builder for an {@link InjectionContext}.
   *
   * @author Pasqual K.
   * @since 1.0
   */
  interface Builder {

    /**
     * Overrides the given {@code type} with the given {@code instance}.
     *
     * @param type     the type to override.
     * @param instance the instance to use instead of constructing.
     * @param <T>      the type of the overridden instance.
     * @return the same instance of the builder as used to call the method, for chaining.
     * @throws NullPointerException if {@code type} is null.
     */
    @NotNull <T> Builder override(@NotNull Type type, @Nullable T instance);

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
     */
    @Contract(pure = true)
    @NotNull InjectionContext build();

    /**
     * Builds the injection context instance. This builder can then be re-used to override other instance or set another
     * injector and rebuild the context.
     *
     * <p>The context constructed by this method will be set at the thread-local injection context. Note that this
     * method might return a subcontext if there is already a thread-local root context present.
     *
     * @return the injection context in the current thread-local situation, might be a subcontext.
     */
    @Contract(pure = true)
    @NotNull InjectionContext enterLocal();
  }
}
