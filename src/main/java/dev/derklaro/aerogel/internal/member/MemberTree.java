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

import dev.derklaro.aerogel.PostConstruct;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import dev.derklaro.aerogel.internal.reflect.ReflectionUtil;
import dev.derklaro.aerogel.internal.unsafe.UnsafeMemberAccess;
import dev.derklaro.aerogel.internal.utility.Preconditions;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class to build a tree of injectable members for a specified target class, ensuring that overridden methods
 * are handled correctly and are not called twice.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.member")
final class MemberTree {

  private final Class<?> baseType;

  private final List<Field> injectableFields = new LinkedList<>();
  private final List<Method> injectableMethods = new LinkedList<>();
  private final List<Method> postConstructMethods = new LinkedList<>();

  /**
   * Constructs a new member tree instance.
   *
   * @param baseType the base type to build the injection tree for.
   */
  public MemberTree(@NotNull Class<?> baseType) {
    this.baseType = baseType;
  }

  /**
   * Actually builds the injection tree for the base type of this tree.
   */
  @SuppressWarnings("deprecation") // isAccessible() is deprecated, but it does exactly what we want
  public void buildTree() {
    List<Class<?>> hierarchyTree = ReflectionUtil.hierarchyTree(this.baseType);

    // walk down the tree in reverse
    int startIndex = hierarchyTree.size() - 1;
    for (int i = startIndex; i >= 0; i--) {
      Class<?> currentType = hierarchyTree.get(i);

      // resolve all injectable fields
      Field[] fields = currentType.getDeclaredFields();
      for (Field field : fields) {
        if (JakartaBridge.isInjectable(field)) {
          // make the field accessible and register it if we can access it after the call
          Field accessible = UnsafeMemberAccess.makeAccessible(field);
          if (accessible.isAccessible()) {
            this.injectableFields.add(accessible);
          }
        }
      }

      // resolve all methods
      Method[] methods = currentType.getDeclaredMethods();
      for (Method method : methods) {
        // ignore abstract & native methods
        if (Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers())) {
          continue;
        }

        if (JakartaBridge.isInjectable(method)) {
          // make the method accessible
          Method accessible = UnsafeMemberAccess.makeAccessible(method);
          if (accessible.isAccessible()) {
            // register static methods instantly
            if (Modifier.isStatic(method.getModifiers())) {
              this.injectableMethods.add(accessible);
              continue;
            }

            // register or replace the method in the injectable ones if it was overridden
            this.replaceOrRegisterMethod(this.injectableMethods, method, false);
          }
          continue;
        } else {
          // check if the override is still annotated
          this.replaceOrRegisterMethod(this.injectableMethods, method, true);
        }

        // check if the method is a post construct listener
        boolean postConstructListener = method.isAnnotationPresent(PostConstruct.class);
        if (postConstructListener) {
          // validate that the method is valid
          Preconditions.checkArgument(method.getParameterCount() == 0, "@PostConstruct takes arguments");
          Preconditions.checkArgument(!Modifier.isStatic(method.getModifiers()), "@PostConstruct method is static");

          // replace or register the method
          this.replaceOrRegisterMethod(this.postConstructMethods, method, false);
        } else {
          // check if the override is still annotated
          this.replaceOrRegisterMethod(this.postConstructMethods, method, true);
        }
      }
    }
  }

  /**
   * Sets or removes the given method in the given known methods list if a method with the same signature is overridden
   * by the given method.
   *
   * @param knownMethods the methods to replace, register or unregister the given method from.
   * @param method       the method that should be tested for.
   * @param unregister   if the method which is overridden by the given method should be unregistered.
   */
  private void replaceOrRegisterMethod(@NotNull List<Method> knownMethods, @NotNull Method method, boolean unregister) {
    // check if the method is overridden
    int methodSignatureHash = ReflectionUtil.signatureHashCode(method);
    for (int i = 0; i < knownMethods.size(); i++) {
      Method knownMethod = knownMethods.get(i);

      // check if the signature of the methods matches
      int knownMethodSignatureHash = ReflectionUtil.signatureHashCode(knownMethod);
      if (knownMethodSignatureHash != methodSignatureHash) {
        continue;
      }

      // check if the method was overridden
      boolean overridden = ReflectionUtil.overrides(method, knownMethod);
      if (overridden) {
        // check if we should unregister or replace the method
        if (unregister) {
          knownMethods.remove(knownMethod);
        } else {
          knownMethods.set(i, method);
        }
        // stop here
        return;
      }
    }

    // method is not overridden, register it if this call isn't purely made to unregister an invalid method
    if (!unregister) {
      knownMethods.add(method);
    }
  }

  /**
   * Get the injectable fields that were resolved by this tree search.
   *
   * @return the injectable fields.
   */
  public @NotNull List<Field> getInjectableFields() {
    return this.injectableFields;
  }

  /**
   * Get the injectable methods that were resolved by this tree search.
   *
   * @return the injectable methods.
   */
  public @NotNull List<Method> getInjectableMethods() {
    return this.injectableMethods;
  }

  /**
   * Get the post construct methods that were resolved by this tree search.
   *
   * @return the post construct methods.
   */
  public @NotNull List<Method> getPostConstructMethods() {
    return this.postConstructMethods;
  }
}
