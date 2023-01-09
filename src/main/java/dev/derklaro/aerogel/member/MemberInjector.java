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

package dev.derklaro.aerogel.member;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.Inject;
import dev.derklaro.aerogel.Injector;
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
   * Injects all static members of the target class.
   *
   * @throws AerogelException if a field or method injection fails.
   */
  void inject();

  /**
   * Injects all static members of the target class.
   *
   * @param flags a bit mask representing the member types which should get injected.
   * @throws AerogelException     if a field or method injection fails.
   * @throws NullPointerException if {@code settings} is null.
   * @see InjectionSetting#toFlag(InjectionSetting...)
   */
  void inject(long flags);

  /**
   * Inject instance and static members into the given target instance. Note that static members are only injected once
   * per member injector lifecycle.
   *
   * <p>If the given instance is {@code null}, this method will only inject the static members of the target class.
   *
   * @param instance the instance to inject the members in.
   * @throws AerogelException if a field or method injection fails.
   */
  void inject(@Nullable Object instance);

  /**
   * Inject instance and static members into the given target instance. Note that static members are only injected once
   * per member injector lifecycle.
   *
   * <p>If the given instance is {@code null}, this method will only inject the static members of the target class.
   *
   * @param instance the instance to inject the members in.
   * @param flags    a bit mask representing the member types which should get injected.
   * @throws AerogelException if a field or method injection fails.
   * @see InjectionSetting#toFlag(InjectionSetting...)
   */
  void inject(@Nullable Object instance, long flags);

  /**
   * Injects the static field with the given name in the target class of this member injector. Note that the field must
   * be annotated as {@code @Inject} in order to be resolvable for this method.
   *
   * @param name the name of the field to inject.
   * @throws AerogelException     if no injectable field with the given name exists or the injection fails.
   * @throws NullPointerException if the given field name is null.
   */
  void injectField(@NotNull String name);

  /**
   * Injects the static or instance field with the given name in the target class of this member injector. Note that the
   * field must be annotated as {@code @Inject} in order to be resolvable for this method.
   *
   * <p>If the target field is non-static and no instance is provided to this method, the call is ignored silently.
   *
   * @param instance the instance to inject the field on, can be null for static fields.
   * @param name     the name of the field to inject.
   * @throws AerogelException     if no injectable field with the given name exists or the injection fails.
   * @throws NullPointerException if the given field name is null.
   */
  void injectField(@Nullable Object instance, @NotNull String name);

  /**
   * Injects the static method with the given name and parameter types in the target class of this member injector and
   * returns the method call result. Note that the field must be annotated as {@code @Inject} in order to be resolvable
   * for this method.
   *
   * @param name           the name of the method to invoke.
   * @param parameterTypes the parameter types of the method to invoke.
   * @return the result of the method invocation, may be null.
   * @throws AerogelException     if no injectable method which matches the requirements exists or the injection fails.
   * @throws NullPointerException if the given method name or parameter types array is null.
   */
  @Nullable Object injectMethod(@NotNull String name, @NotNull Class<?>... parameterTypes);

  /**
   * Injects the static or instance method with the given name and parameter types in the target class of this member
   * injector and returns the method call result. Note that the field must be annotated as {@code @Inject} in order to
   * be resolvable for this method.
   *
   * <p>If the target method is non-static and no instance is provided to this method, the call is ignored silently.
   *
   * @param instance       the instance to inject the method on, can be null for static methods.
   * @param name           the name of the method to invoke.
   * @param parameterTypes the parameter types of the method to invoke.
   * @return the result of the method invocation, may be null.
   * @throws AerogelException     if no injectable method which matches the requirements exists or the injection fails.
   * @throws NullPointerException if the given method name or parameter types array is null.
   */
  @Nullable Object injectMethod(@Nullable Object instance, @NotNull String name, @NotNull Class<?>... parameterTypes);
}
