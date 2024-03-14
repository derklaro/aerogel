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

package dev.derklaro.aerogel.internal.member;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.MemberInjector;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A default implementation of a {@link MemberInjector}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class DefaultMemberInjector<T> implements MemberInjector<T> {

  private final Injector injector;
  private final Class<T> targetClass;

  private final List<MemberInjectionExecutor> memberInjectionExecutors;

  public DefaultMemberInjector(
    @NotNull Class<T> targetClass,
    @NotNull Injector injector,
    @NotNull MethodHandles.Lookup lookup
  ) {
    this.injector = injector;
    this.targetClass = targetClass;

    Collection<InjectableMember> injectableMembers = InjectionMemberCache.computeMemberTree(targetClass);
    this.memberInjectionExecutors = new ArrayList<>(injectableMembers.size());
    for (InjectableMember injectableMember : injectableMembers) {
      try {
        MemberInjectionExecutor executor = injectableMember.provideInjectionExecutor(injector, lookup);
        this.memberInjectionExecutors.add(executor);
      } catch (Exception ignored) {
        // exceptions are used here to indicate that injection the particular member is not supported, just ignore that
      }
    }
  }

  @Override
  public @NotNull Injector injector() {
    return this.injector;
  }

  @Override
  public @NotNull Class<T> target() {
    return this.targetClass;
  }

  @Override
  public void injectMembers(@Nullable T instance) {
    try {
      for (MemberInjectionExecutor memberInjectionExecutor : this.memberInjectionExecutors) {
        memberInjectionExecutor.executeInjection(instance);
      }
    } catch (Throwable throwable) {
      throw new IllegalStateException("Issue while injecting members in " + this.targetClass, throwable);
    }
  }
}
