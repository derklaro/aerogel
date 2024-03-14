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

package dev.derklaro.aerogel.internal.annotation;

import dev.derklaro.aerogel.internal.util.MapUtil;
import java.lang.annotation.Annotation;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public final class AnnotationDesc {

  private static final Map<Class<?>, AnnotationDesc> CACHE = MapUtil.newConcurrentMap();

  private final boolean scope;
  private final boolean qualifier;

  private final Map<String, Member> members;
  private final RetentionPolicy retentionPolicy;
  private final Class<? extends Annotation> annotationType;

  private AnnotationDesc(@NotNull Class<? extends Annotation> annotationType) {
    Method[] methods = annotationType.getDeclaredMethods();
    if (methods.length == 0) {
      this.members = Collections.emptyMap();
    } else {
      Map<String, Member> members = new HashMap<>(methods.length + 1, 1.0f);
      for (Method method : methods) {
        if (Modifier.isPublic(method.getModifiers())
          && Modifier.isAbstract(method.getModifiers())
          && !method.isSynthetic()
        ) {
          // no other checks needed, no other methods than property declarations are allowed
          // see JLS chapter 9: https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.1
          Member member = new Member(method.getName(), method.getReturnType(), method.getDefaultValue());
          members.put(member.name(), member);
        }
      }

      this.members = Collections.unmodifiableMap(members);
    }

    this.retentionPolicy = AnnotationUtil.extractRetention(annotationType);
    this.scope = InjectAnnotationUtil.validScopeAnnotation(annotationType);
    this.qualifier = InjectAnnotationUtil.validQualifierAnnotation(annotationType);

    this.annotationType = annotationType;
  }

  public static @NotNull AnnotationDesc of(@NotNull Class<? extends Annotation> annotationType) {
    AnnotationDesc cached = CACHE.get(annotationType);
    if (cached != null) {
      return cached;
    }

    AnnotationDesc desc = new AnnotationDesc(annotationType);
    cached = CACHE.putIfAbsent(annotationType, desc);
    return cached != null ? cached : desc;
  }

  public boolean scope() {
    return this.scope;
  }

  public boolean qualifier() {
    return this.qualifier;
  }

  public boolean hasProperties() {
    return !this.members.isEmpty();
  }

  public @NotNull RetentionPolicy retentionPolicy() {
    return this.retentionPolicy;
  }

  public @NotNull Class<? extends Annotation> annotationType() {
    return this.annotationType;
  }

  @Unmodifiable
  public @NotNull Map<String, Member> members() {
    return this.members;
  }

  public static final class Member {

    private final String name;
    private final Class<?> type;
    private final Object defaultValue;

    public Member(@NotNull String name, @NotNull Class<?> type, @Nullable Object defaultValue) {
      this.name = name;
      this.type = type;
      this.defaultValue = defaultValue;
    }

    public String name() {
      return this.name;
    }

    public Class<?> type() {
      return this.type;
    }

    public Object defaultValue() {
      return this.defaultValue;
    }
  }
}
