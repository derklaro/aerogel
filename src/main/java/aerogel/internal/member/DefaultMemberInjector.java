/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
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

package aerogel.internal.member;

import aerogel.InjectionContext;
import aerogel.Injector;
import aerogel.MemberInjectionSettings;
import aerogel.MemberInjector;
import aerogel.Provider;
import aerogel.internal.jakarta.JakartaBridge;
import aerogel.internal.utility.ElementHelper;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultMemberInjector implements MemberInjector {

  private static final Object[] NO_PARAMS = new Object[0];

  private static final MemberInjectionSettings ALL = MemberInjectionSettings.builder().build();
  private static final MemberInjectionSettings ONLY_STATIC = MemberInjectionSettings.builder()
    .injectInstanceMethods(false)
    .injectInstanceFields(false)
    .build();

  private final Injector injector;
  private final Class<?> targetClass;

  private final Set<Field> staticFields;
  private final Set<Field> instanceFields;

  private final Set<Method> staticMethods;
  private final Set<Method> instanceMethods;

  public DefaultMemberInjector(@NotNull Injector injector, @NotNull Class<?> target) {
    this.injector = injector;
    this.targetClass = target;
    // initialize every field & method set with an empty concurrent set
    // @todo: can we only initialize these when we really need them? Or use an empty set instead?
    this.staticFields = ConcurrentHashMap.newKeySet();
    this.instanceFields = ConcurrentHashMap.newKeySet();
    this.staticMethods = ConcurrentHashMap.newKeySet();
    this.instanceMethods = ConcurrentHashMap.newKeySet();

    // read all fields & methods
    Class<?> targetClass = target;
    do {
      // methods
      for (Method method : targetClass.getDeclaredMethods()) {
        // check if the method is marked as @Inject
        if (JakartaBridge.isInjectable(method)) {
          method.setAccessible(true); // disable these checks - we do want to access them
          // check if the method is an instance or static method
          if (Modifier.isStatic(method.getModifiers())) {
            this.staticMethods.add(method);
          } else {
            // @todo: are there other modifiers we should check for?
            this.instanceMethods.add(method);
          }
        }
      }
      // fields
      for (Field field : targetClass.getDeclaredFields()) {
        // check if the field is marked as @Inject
        if (JakartaBridge.isInjectable(field)) {
          // check if the field is final - we cannot inject them as the modifiers field was added to the reflection
          // blocklist in Java 12 and a warning gets emitted since Java 9. There is no point to support this behaviour
          // for Java 8 users...
          // @todo: we could use codegen here - this allows access & modify of final fields
          if (Modifier.isFinal(field.getModifiers())) {
            throw new IllegalArgumentException(String.format(
              "Field %s in %s is final and cannot get injected",
              field.getName(),
              field.getDeclaringClass()));
          }
          // disable these checks - we do want to access them
          field.setAccessible(true);
          // check if the field is an instance or static field
          if (Modifier.isStatic(field.getModifiers())) {
            this.staticFields.add(field);
          } else {
            // @todo: are there other modifiers we should check for?
            this.instanceFields.add(field);
          }
        }
      }
    } while ((targetClass = targetClass.getSuperclass()) != Object.class);
  }

  @Override
  public @NotNull Injector injector() {
    return this.injector;
  }

  @Override
  public @NotNull Class<?> targetClass() {
    return this.targetClass;
  }

  @Override
  public void inject() {
    this.inject(ONLY_STATIC);
  }

  @Override
  public void inject(@NotNull MemberInjectionSettings settings) {
    this.inject(settings, (InjectionContext) null);
  }

  @Override
  public void inject(@NotNull MemberInjectionSettings settings, @Nullable InjectionContext context) {
    // check if we need to inject all static methods
    if (settings.injectStaticMethods()) {
      // loop and search for every method we should inject
      for (Method method : this.staticMethods) {
        if (this.methodMatches(method, settings)) {
          this.injectMethod(null, method, context);
        }
      }
    }
    // check if we need to inject all static fields
    if (settings.injectStaticFields()) {
      // loop and search for every field we should inject
      for (Field field : this.staticFields) {
        if (this.fieldMatches(field, settings, null)) {
          this.injectField(null, field, context);
        }
      }
    }
  }

  @Override
  public void inject(@NotNull Object instance) {
    this.inject(instance, ALL);
  }

  @Override
  public void inject(@NotNull Object instance, @NotNull MemberInjectionSettings settings) {
    this.inject(instance, settings, null);
  }

  @Override
  public void inject(
    @NotNull Object instance,
    @NotNull MemberInjectionSettings settings,
    @Nullable InjectionContext context
  ) {
    this.inject(settings, context); // injects static members if enabled
    // check if we need to inject instance methods
    if (settings.injectInstanceMethods()) {
      // loop and search for every method we should inject
      for (Method method : this.instanceMethods) {
        if (this.methodMatches(method, settings)) {
          this.injectMethod(instance, method, context);
        }
      }
    }
    // check if we need to inject instance fields
    if (settings.injectInstanceFields()) {
      // loop and search for every field we should inject
      for (Field field : this.instanceFields) {
        if (this.fieldMatches(field, settings, instance)) {
          this.injectField(instance, field, context);
        }
      }
    }
  }

  @Override
  public void injectField(@NotNull String name) {
    // try to find a static field with the given name and inject that
    for (Field field : this.staticFields) {
      if (field.getName().equals(name)) {
        this.injectField(null, field, null);
        return; // a field name is unique
      }
    }
    // no such field found
    throw new IllegalArgumentException(String.format("No static field with name %s in %s", name, this.targetClass));
  }

  @Override
  public void injectField(@NotNull Object instance, @NotNull String name) {
    // try to find a static field with the given name and inject that
    for (Field field : this.staticFields) {
      if (field.getName().equals(name)) {
        this.injectField(null, field, null);
        return; // a field name is unique
      }
    }
    // try to find an instance field with the given name and inject that
    for (Field field : this.instanceFields) {
      if (field.getName().equals(name)) {
        this.injectField(instance, field, null);
        return; // a field name is unique
      }
    }
    // no such field found
    throw new IllegalArgumentException(String.format("No field with name %s in %s", name, this.targetClass));
  }

  @Override
  public @Nullable Object injectMethod(@NotNull String name, @NotNull Class<?>... parameterTypes) {
    // try to find a static method with the given name and inject that
    for (Method method : this.staticMethods) {
      if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), parameterTypes)) {
        return this.injectMethod(null, method, null);
      }
    }
    // no such method found
    throw new IllegalArgumentException(String.format(
      "No static method with name %s and parameters %s in %s",
      name,
      Arrays.toString(parameterTypes),
      this.targetClass));
  }

  @Override
  public @Nullable Object injectMethod(@NotNull Object instance, @NotNull String name, @NotNull Class<?>... params) {
    // try to find a static method with the given name and inject that
    for (Method method : this.staticMethods) {
      if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), params)) {
        return this.injectMethod(null, method, null);
      }
    }
    // try to find an instance method with the given name and inject that
    for (Method method : this.instanceMethods) {
      if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), params)) {
        return this.injectMethod(instance, method, null);
      }
    }
    // no such method found
    throw new IllegalArgumentException(String.format(
      "No method with name %s and parameters %s in %s",
      name,
      Arrays.toString(params),
      this.targetClass));
  }

  private @Nullable Object injectMethod(
    @Nullable Object instance,
    @NotNull Method method,
    @Nullable InjectionContext context
  ) {
    try {
      // invoke the method using the collected parameters
      return method.invoke(instance, this.lookupParamInstances(method, context));
    } catch (IllegalAccessException | InvocationTargetException exception) {
      throw new IllegalArgumentException("Unable to invoke method", exception);
    }
  }

  private void injectField(@Nullable Object instance, @NotNull Field field, @Nullable InjectionContext context) {
    try {
      Object fieldValue;
      // check if the field is a provider
      if (JakartaBridge.isProvider(field.getType())) {
        Provider<?> provider = context == null
          ? this.injector.binding(ElementHelper.buildElement(field))
          : context.injector().binding(ElementHelper.buildElement(field));
        // check if the provider is a jakarta provider
        if (JakartaBridge.needsProviderWrapping(field.getType())) {
          fieldValue = JakartaBridge.bridgeJakartaProvider(provider);
        } else {
          fieldValue = provider;
        }
      } else {
        // just needs the direct instance of the field type
        fieldValue = context == null
          ? this.injector.instance(ElementHelper.buildElement(field))
          : context.findInstance(ElementHelper.buildElement(field));
      }
      // set the field using the collected parameter
      field.set(instance, fieldValue);
    } catch (IllegalAccessException exception) {
      throw new IllegalArgumentException("Unable to set field value", exception);
    }
  }

  private Object @NotNull [] lookupParamInstances(@NotNull Executable executable, @Nullable InjectionContext context) {
    // check if we need to collect parameters at all
    if (executable.getParameterCount() == 0) {
      return NO_PARAMS;
    } else {
      Parameter[] params = executable.getParameters(); // prevent clones
      Object[] paramInstances = new Object[params.length];
      // find for every type an instance in the parent injector
      for (int i = 0; i < params.length; i++) {
        // check if the parameter is a provider
        if (JakartaBridge.isProvider(params[i].getType())) {
          // we only need the binding, not the direct instance then
          Provider<?> provider = context == null
            ? this.injector.binding(ElementHelper.buildElement(params[i]))
            : context.injector().binding(ElementHelper.buildElement(params[i]));
          // check if the provider is a jakarta provider
          if (JakartaBridge.needsProviderWrapping(params[i].getType())) {
            paramInstances[i] = JakartaBridge.bridgeJakartaProvider(provider);
          } else {
            paramInstances[i] = provider;
          }
        } else {
          // we do need the direct instance of the type
          paramInstances[i] = context == null
            ? this.injector.instance(ElementHelper.buildElement(params[i]))
            : context.findInstance(ElementHelper.buildElement(params[i]));
        }
      }
      // return the collected instances
      return paramInstances;
    }
  }

  private boolean methodMatches(@NotNull Method method, @NotNull MemberInjectionSettings settings) {
    // check if the method is private & private method injection is enabled
    if (Modifier.isPrivate(method.getModifiers()) && !settings.injectPrivateMethods()) {
      return false;
    }
    // check if the method is inherited & inherited methods are enabled
    return method.getDeclaringClass().equals(this.targetClass) || settings.injectInheritedMethods();
  }

  private boolean fieldMatches(@NotNull Field field, @NotNull MemberInjectionSettings settings, @Nullable Object on) {
    // check if the method is private & private method injection is enabled
    if (Modifier.isPrivate(field.getModifiers()) && !settings.injectPrivateFields()) {
      return false;
    }
    // check if the method is inherited & inherited methods are enabled
    if (!field.getDeclaringClass().equals(this.targetClass) && !settings.injectInheritedFields()) {
      return false;
    }
    // check if the field is initialized
    if (settings.injectOnlyUninitializedFields()) {
      try {
        // try to read the field value
        return field.get(on) == null;
      } catch (IllegalAccessException ignored) {
      }
    }
    // fall-trough
    return true;
  }
}
