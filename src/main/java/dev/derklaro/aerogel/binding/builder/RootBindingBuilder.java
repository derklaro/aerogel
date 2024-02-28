/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2024 Pasqual K. and contributors
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

package dev.derklaro.aerogel.binding.builder;

import dev.derklaro.aerogel.binding.key.BindingKey;
import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * The root entrypoint to build a binding that can be registered into an injector. A binding builder can be obtained
 * from an injector, and configuration values (such as scopes) will be obtained from that injector. However, bindings
 * built by this builder can be applied to every injector.
 * <p>
 * Multiple binding instances can be built from a single root binding builder, there is no need to re-obtain a new
 * builder instance every time a new binding should be built.
 * <p>
 * Building a binding includes three steps for non-dynamic bindings. First the type that should be bound is selected.
 * That type is the target element type to bind. Following that, the binding can be narrowed down by qualifying or
 * scoping in the next steps. Finally, the provider for the element must be specified. This can be an implementing
 * class, factory method or provider.
 * <p>
 * A simple example for binding an interface {@code UserService} to {@code UserServiceImpl}:
 * <pre>
 * {@code
 * var binding = builder
 *   .bind(UserService.class)
 *   .toConstructingClass(UserServiceImpl.class);
 * }
 * </pre>
 * If there are multiple implementations, and an in-memory version that is qualified by {@code @InMemory} should be
 * injected, the binding can be qualified with:
 * <pre>
 * {@code
 * var binding = builder
 *   .bind(UserService.class)
 *   .qualifiedWith(InMemory.class)
 *   .toConstructingClass(UserServiceImpl.class);
 * }
 * </pre>
 * Sometimes it is useful to provide a specific instance for a value depending on the current scope. That can for
 * example be the current HTTP request that is being handled. By default, a singleton scope is supported. Applying the
 * scope instructs the injector to only construct a singleton instance of the target element, and re-use that instance
 * on every injection request:
 * <pre>
 * {@code
 * var binding = builder
 *   .bind(UserService.class)
 *   .qualifiedWith(InMemory.class)
 *   .scopedWith(Singleton.class)
 *   .toConstructingClass(UserServiceImpl.class);
 * }
 * </pre>
 * Note: when setting an explicit scope during build of the binding, the scope annotation applied to implementation
 * classes are ignored. Only if no scope was explicitly set, the scope annotation on the target are applied as well. If
 * explicitly no scoping should be applied, but scope annotations are present on the target, use {@code .unscoped()}
 * when building the binding:
 * <pre>
 * {@code
 * var binding = builder
 *   .bind(UserService.class)
 *   .qualifiedWith(InMemory.class)
 *   .unscoped()
 *   .toConstructingClass(UserServiceImpl.class);
 * }
 * </pre>
 * Generic types can also be bound. Note that the binding construct is <strong>not</strong> type safe with generic types
 * passed to {@code bind()}. The resulting binding must be properly annotated to ensure type safety. Following is an
 * example how to build a binding for a set of user services {@code UninstalledBinding<Set<UserService>>}. The geantyref
 * library can be used to build the required generic types. Either by using a type token:
 * <pre>
 * {@code builder.bind(new TypeToken<Set<UserService>>() {})}
 * </pre>
 * or by constructing the requested type:
 * <pre>
 * {@code builder.bind(TypeFactory.parameterizedClass(Set.class, UserService.class))}
 * </pre>
 * <p>
 * The builder also allows the construction of dynamic bindings. Dynamic bindings are not bind to a specific type, but
 * rather provide a matcher for injectable elements. Once a matcher matches an injectable element, a binding is
 * constructed based on it. This also means that dynamic bindings must be predictive. The same construction of type and
 * qualifier must always result in the same binding. Dynamic bindings are always only called once, the result is stored
 * as a concrete binding. However, dynamic bindings are allowed to match multiple types. See the far more complete
 * explanation and examples about dynamic bindings at {@link DynamicBindingBuilder}.
 * <p>
 * A dynamic binding can be built from this builder using:
 * <pre>
 * {@code
 * var binding = builder
 *   .bindDynamically()
 *   .exactRawType(UserService.class)
 *   .annotationPresent(InMemory.class)
 *   .toKeyedBindingProvider(bb -> bb
 *     .scopedWith(Singleton.class)
 *     .toConstructingClass(UserServiceImpl.class));
 * }
 * </pre>
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface RootBindingBuilder {

  /**
   * Starts the build process of a dynamic binding. See examples in the root documentation or in
   * {@link DynamicBindingBuilder} for more details on dynamic bindings.
   *
   * @return a new builder for dynamic bindings.
   */
  @NotNull
  DynamicBindingBuilder bindDynamically();

  /**
   * Starts the bind process for a new binding that targets the given type. This method takes a generic type, which
   * causes the build process to <strong>not</strong> be type-safe. If possible, prefer using {@link #bind(TypeToken)}
   * instead.
   * <p>
   * If necessary, types passed to this method can be constructed using geantyref:
   * <pre>
   * {@code builder.bind(TypeFactory.parameterizedClass(Set.class, UserService.class))}
   * </pre>
   *
   * @param type the type that the constructed binding should target.
   * @param <T>  the type that the constructed binding should target.
   * @return a new binding builder to construct a binding for the given type.
   * @see #bind(TypeToken)
   */
  @NotNull
  <T> QualifiableBindingBuilder<T> bind(@NotNull Type type);

  /**
   * Starts the bind process for a new binding that targets the given type. This method should only be used if the type
   * to bind is not generic. For example, binding {@code List.class} with this method, would result in matching all
   * elements with a raw list type, but not with parameter arguments. For these types, prefer using
   * {@link #bind(TypeToken)} instead.
   *
   * @param type the raw type that the constructed binding should target.
   * @param <T>  the type that the constructed binding should target.
   * @return a new binding builder to construct a binding for the given type.
   * @see #bind(TypeToken)
   */
  @NotNull
  <T> QualifiableBindingBuilder<T> bind(@NotNull Class<T> type);

  /**
   * Starts the bind process for a new binding that targets the type wrapped in the given type token. This method allows
   * for type-safe building, even for nested generic types and should be preferred over the {@link #bind(Type)} method.
   * Example usage:
   * <pre>
   * {@code builder.bind(new TypeToken<Set<UserService>>() {})}
   * </pre>
   *
   * @param typeToken the type token that wraps the type that the constructed binding should target.
   * @param <T>       the type that the constructed binding should target.
   * @return a new binding builder to construct a binding for the given type.
   */
  @NotNull
  <T> QualifiableBindingBuilder<T> bind(@NotNull TypeToken<T> typeToken);

  /**
   * Starts the bind process for a new binding that targets the type wrapped in the given binding key. If this method is
   * used, no qualifier annotation can be set on the builder as the annotation should be defined on the binding key.
   *
   * @param bindingKey the binding key that wraps the type that the constructed binding should target.
   * @param <T>        the type that the constructed binding should target.
   * @return a new binding builder to construct a binding for the given type.
   */
  @NotNull
  <T> ScopeableBindingBuilder<T> bind(@NotNull BindingKey<T> bindingKey);
}
