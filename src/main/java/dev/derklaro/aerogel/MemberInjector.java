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

package dev.derklaro.aerogel;

import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Injects methods and fields in classes that are marked as injectable. This process is usually executed after a class
 * instance was created by an injector, but can be manually executed by obtaining a member injector from an injector.
 *
 * @param <T> the type to inject members in.
 * @author Pasqual Koschmieder
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface MemberInjector<T> {

  /**
   * Get the injector to which this member injector is bound.
   *
   * @return the injector to which this member injector is bound.
   */
  @NotNull
  Injector injector();

  /**
   * Get the target class of this member injector. This class was provided when the member injector from an injector.
   *
   * @return the target class of this member injector.
   */
  @NotNull
  Class<T> target();

  /**
   * Injects the members of a class in the given instance. If the instance of a class is created by an injector, this
   * process is executed automatically. Therefore, if all class instances are created automatically, there is no need to
   * call this method manually.
   * <p>
   * If the given instance is null, only static members will be injected in the target class, unless they were already
   * injected once by a different injector.
   * <p>
   * Member injection follows these ordering rules:
   * <ol>
   *   <li>supertype members are injected before subtype members.
   *   <li>static members are injected before instance members.
   *   <li>fields are injected before methods.
   *   <li>fields with the same modifier are sorted by name.
   *   <li>methods with the same modifier are sorted by {@link Order} or name for methods with the same ordering.
   * </ol>
   *
   * @param instance the instance to inject members in.
   */
  void injectMembers(@Nullable T instance);
}
