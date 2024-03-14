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

import dev.derklaro.aerogel.internal.util.MapUtil;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A utility class to build a tree of injectable members for a specified target class, ensuring that overridden methods
 * are handled correctly and are not called twice.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0")
final class InjectionMemberCache {

  private static final Map<Member, InjectableMember> STATIC_MEMBER_CACHE = MapUtil.newConcurrentMap();
  private static final Map<Class<?>, Collection<InjectableMember>> CACHE = MapUtil.newConcurrentMap();

  private InjectionMemberCache() {
    throw new UnsupportedOperationException();
  }

  public static @Nullable <M extends Member> InjectableMember getOrCreateStaticMember(
    @NotNull M member,
    @NotNull Function<M, InjectableMember> factory
  ) {
    if (!Modifier.isStatic(member.getModifiers())) {
      throw new IllegalArgumentException("cache only used for static members");
    }

    InjectableMember cached = STATIC_MEMBER_CACHE.get(member);
    if (cached == null) {
      InjectableMember computed = factory.apply(member);
      InjectableMember registered = STATIC_MEMBER_CACHE.putIfAbsent(member, computed);
      cached = registered != null ? registered : computed;
    }

    return cached;
  }

  @Unmodifiable
  public static @NotNull Collection<InjectableMember> computeMemberTree(@NotNull Class<?> type) {
    Collection<InjectableMember> tree = CACHE.get(type);
    if (tree == null) {
      MemberTreeProvider provider = new MemberTreeProvider(type);
      provider.resolveInjectableMembers();

      Collection<InjectableMember> computedTree = provider.toMemberTree();
      Collection<InjectableMember> registeredTree = CACHE.putIfAbsent(type, computedTree);
      tree = registeredTree != null ? registeredTree : computedTree;
    }

    return tree;
  }
}
