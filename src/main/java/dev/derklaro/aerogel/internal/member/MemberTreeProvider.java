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

package dev.derklaro.aerogel.internal.member;

import dev.derklaro.aerogel.Order;
import jakarta.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

final class MemberTreeProvider {

  private static final Comparator<Method> METHOD_COMPARATOR_BY_ORDER = (left, right) -> {
    int lo = extractMethodOrder(left);
    int ro = extractMethodOrder(right);
    return Integer.compare(lo, ro);
  };

  private final Class<?> baseClass;

  private final Set<Field> injectableFields = new LinkedHashSet<>();
  private final Set<Method> injectableMethods = new LinkedHashSet<>();

  private final Map<MethodSignature, List<Method>> methodsBySignature = new HashMap<>();

  public MemberTreeProvider(@NotNull Class<?> baseClass) {
    this.baseClass = baseClass;
  }

  private static int extractMethodOrder(@NotNull Method method) {
    Order order = method.getAnnotation(Order.class);
    return order == null ? Order.DEFAULT : order.value();
  }

  private static boolean methodOverriddenBy(@NotNull Method maybeOverride, @NotNull Method maybeOverridden) {
    int maybeOverriddenModifiers = maybeOverridden.getModifiers();
    if (Modifier.isPrivate(maybeOverriddenModifiers)) {
      // private methods cannot be overridden
      return false;
    }

    if (Modifier.isPublic(maybeOverriddenModifiers) || Modifier.isProtected(maybeOverriddenModifiers)) {
      // public and protected methods in the same class tree always override each other
      return true;
    }

    // package-private method, check if the methods share the same package
    Package maybeOverridePackage = maybeOverride.getDeclaringClass().getPackage();
    Package maybeOverriddenPackage = maybeOverridden.getDeclaringClass().getPackage();
    return Objects.equals(maybeOverridePackage, maybeOverriddenPackage);
  }

  public void resolveInjectableMembers() {
    // compute the Hierarchy tree starting from the base class
    List<Class<?>> hierarchyTree = this.computeHierarchyTree();
    int startIndex = hierarchyTree.size() - 1;

    for (int i = startIndex; i >= 0; i--) {
      Class<?> currentType = hierarchyTree.get(i);

      Field[] fields = currentType.getDeclaredFields();
      for (Field field : fields) {
        if (field.isAnnotationPresent(Inject.class)) {
          this.injectableFields.add(field);
        }
      }

      List<Method> methods = new ArrayList<>(Arrays.asList(currentType.getDeclaredMethods()));
      methods.sort(METHOD_COMPARATOR_BY_ORDER);
      for (Method method : methods) {
        // ignore methods added by the compiler
        if (method.isSynthetic()
          || method.getTypeParameters().length > 0
          || Modifier.isAbstract(method.getModifiers())) {
          continue;
        }

        if (method.isAnnotationPresent(Inject.class)) {
          // register static methods as-is; these cannot be overridden higher up in the tree
          // in case we encounter a non-static method, we need to decide which action to take based on the override status
          if (Modifier.isStatic(method.getModifiers())) {
            this.injectableMethods.add(method);
          } else {
            this.replaceOrRegisterMethod(method, false);
          }
        } else {
          // the method is not injectable, remove all injectable super methods
          this.replaceOrRegisterMethod(method, true);
        }
      }
    }
  }

  @Unmodifiable
  public @NotNull List<InjectableMember> toMemberTree() {
    List<InjectableMember> result = new ArrayList<>();

    // 1. injection step: static fields
    for (Field field : this.injectableFields) {
      if (Modifier.isStatic(field.getModifiers())) {
        InjectableMember.InjectableField injectableField = new InjectableMember.InjectableField(field);
        result.add(injectableField);
      }
    }

    // 2. injection step: static methods
    for (Method method : this.injectableMethods) {
      if (Modifier.isStatic(method.getModifiers())) {
        InjectableMember.InjectableMethod injectableMethod = new InjectableMember.InjectableMethod(method);
        result.add(injectableMethod);
      }
    }

    // 3. injection step: instance fields
    for (Field field : this.injectableFields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        InjectableMember.InjectableField injectableField = new InjectableMember.InjectableField(field);
        result.add(injectableField);
      }
    }

    // 4. injection step: instance methods
    for (Method method : this.injectableMethods) {
      if (!Modifier.isStatic(method.getModifiers())) {
        InjectableMember.InjectableMethod injectableMethod = new InjectableMember.InjectableMethod(method);
        result.add(injectableMethod);
      }
    }

    return Collections.unmodifiableList(result);
  }

  private void replaceOrRegisterMethod(@NotNull Method method, boolean forceUnregister) {
    MethodSignature signature = new MethodSignature(method);
    List<Method> knownMethods = this.methodsBySignature.get(signature);
    if (knownMethods == null) {
      // method signature was not yet encountered, register the current method as the first one
      // unless this call was explicitly made to remove these methods
      if (!forceUnregister) {
        List<Method> methods = new ArrayList<>();
        methods.add(method);
        this.methodsBySignature.put(signature, methods);
        this.injectableMethods.add(method);
      }
    } else {
      // try to find methods that are already registered and are being overridden by the given method
      boolean overrideFound = false;
      ListIterator<Method> iterator = knownMethods.listIterator();
      while (iterator.hasNext()) {
        Method next = iterator.next();
        if (methodOverriddenBy(method, next)) {
          // override detected, remove or replace it
          if (forceUnregister) {
            iterator.remove();
          } else {
            iterator.set(method);
          }

          // due to an override being present, the current method can be removed from the injectable methods
          // as it should be replaced by the overriding method later on
          overrideFound = true;
          this.injectableMethods.remove(next);
        }
      }

      // in case the method shouldn't be removed and no override is present the method must be registered
      if (!overrideFound && !forceUnregister) {
        knownMethods.add(method);
        this.injectableMethods.add(method);
      }
    }
  }

  private @NotNull List<Class<?>> computeHierarchyTree() {
    Class<?> type = this.baseClass;
    List<Class<?>> result = new ArrayList<>();
    while (type != null && type != Object.class) {
      result.add(type);
      type = type.getSuperclass();
    }

    return result;
  }

  /**
   * A signature that uniquely identifies a method. Takes the name and parameter types into account (changing the return
   * type is not making the method unique).
   *
   * @author Pasqual Koschmieder
   * @since 3.0
   */
  @API(status = API.Status.INTERNAL, since = "3.0")
  private static final class MethodSignature {

    private final int hash;

    private final String name;
    private final Class<?>[] parameters;

    public MethodSignature(@NotNull Method method) {
      this.name = method.getName();
      this.parameters = method.getParameterTypes();
      this.hash = Objects.hash(this.name, Arrays.hashCode(this.parameters));
    }

    @Override
    public int hashCode() {
      return this.hash;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MethodSignature that = (MethodSignature) o;
      return Objects.equals(name, that.name) && Objects.deepEquals(parameters, that.parameters);
    }
  }
}
