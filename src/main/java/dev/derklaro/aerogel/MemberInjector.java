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

package dev.derklaro.aerogel;

import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Injects dependencies into class members (fields and methods). This injector is used internally to inject the members
 * of a class after constructing it. It can be used as well to call methods or inject fields multiple times or on
 * classes which are not created by an {@link Injector}. A member injector can only be obtained from an injector
 * instance by using {@link Injector#memberInjector(Class)}. All members which are requesting a dependency injection
 * must be annotated as {@link Inject}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface MemberInjector {

  /**
   * Get the injector which is used for instance lookups.
   *
   * @return the injector which is used for instance lookups.
   */
  @NotNull Injector injector();

  /**
   * Get the target class of this member injector. This class was provided when obtaining an instance from an injector.
   *
   * @return the target class of this member injector.
   */
  @NotNull Class<?> targetClass();

  /**
   * Injects all static members of the target class using the default {@link MemberInjectionSettings}.
   *
   * @throws AerogelException if a field or method injection fails.
   */
  void inject();

  /**
   * Injects all static members of the target class using the given {@code settings}.
   *
   * @param settings the settings to use when injecting the members.
   * @throws AerogelException     if a field or method injection fails.
   * @throws NullPointerException if {@code settings} is null.
   */
  void inject(@NotNull MemberInjectionSettings settings);

  /**
   * Inject all members into the given {@code instance} using the default {@link MemberInjectionSettings}.
   *
   * @param instance the instance to inject the members in.
   * @throws AerogelException     if a field or method injection fails.
   * @throws NullPointerException if {@code instance} is null.
   */
  void inject(@NotNull Object instance);

  /**
   * Inject all members into the given {@code instance} using the given {@code settings}.
   *
   * @param instance the instance to inject the members in.
   * @param settings the settings to use when injecting the members.
   * @throws AerogelException     if a field or method injection fails.
   * @throws NullPointerException if {@code instance} or {@code settings} is null.
   */
  void inject(@NotNull Object instance, @NotNull MemberInjectionSettings settings);

  /**
   * Injects a specific static field in the target class.
   *
   * @param name the name of the field to inject.
   * @throws AerogelException     if no injectable field with the given {@code name} exists or the injection fails.
   * @throws NullPointerException if {@code name} is null.
   */
  void injectField(@NotNull String name);

  /**
   * Injects a specific field in the target class on the given {@code instance}.
   *
   * @param instance the instance to inject the field on.
   * @param name     the name of the field to inject.
   * @throws AerogelException     if no injectable field with the given {@code name} exists or the injection fails.
   * @throws NullPointerException if {@code instance} or {@code name} is null.
   */
  void injectField(@NotNull Object instance, @NotNull String name);

  /**
   * Injects a specific static method in the target class.
   *
   * @param name           the name of the method to invoke.
   * @param parameterTypes the parameter types of the method to invoke.
   * @return the result of the method invocation, may be null.
   * @throws AerogelException     if no injectable method with the given {@code name} exists or the injection fails.
   * @throws NullPointerException if {@code name} is null.
   */
  @Nullable Object injectMethod(@NotNull String name, @NotNull Class<?>... parameterTypes);

  /**
   * Injects a specific method in the target class on the given {@code instance}.
   *
   * @param instance       the instance to inject the method on.
   * @param name           the name of the method to invoke.
   * @param parameterTypes the parameter types of the method to invoke.
   * @return the result of the method invocation, may be null.
   * @throws AerogelException     if no injectable method with the given {@code name} exists or the injection fails.
   * @throws NullPointerException if {@code instance} or {@code name} is null.
   */
  @Nullable Object injectMethod(@NotNull Object instance, @NotNull String name, @NotNull Class<?>... parameterTypes);
}
