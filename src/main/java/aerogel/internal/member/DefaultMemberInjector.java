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

import aerogel.Element;
import aerogel.InjectionContext;
import aerogel.Injector;
import aerogel.MemberInjectionSettings;
import aerogel.MemberInjector;
import aerogel.Provider;
import aerogel.internal.asm.AsmUtils;
import aerogel.internal.jakarta.JakartaBridge;
import aerogel.internal.reflect.ReflectionUtils;
import aerogel.internal.utility.ElementHelper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
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

  private final Predicate<Member> onlyThisClass;
  private final Predicate<Member> onlyNotThisClass;

  private final Collection<InjectableField> staticFields;
  private final Collection<InjectableField> instanceFields;

  private final Collection<InjectableMethod> staticMethods;
  private final Collection<InjectableMethod> instanceMethods;

  // static member injection should only be done once, never twice
  // @todo: notify the parent member injectors to not inject the static members again
  private final AtomicBoolean didStaticFieldInjection = new AtomicBoolean();
  private final AtomicBoolean didStaticSupertypeFieldInjection = new AtomicBoolean();

  private final AtomicBoolean didStaticMethodInjection = new AtomicBoolean();
  private final AtomicBoolean didStaticSupertypeMethodInjection = new AtomicBoolean();

  public DefaultMemberInjector(@NotNull Injector injector, @NotNull Class<?> target) {
    this.injector = injector;
    this.targetClass = target;
    // these are just all fields, lazy initialized
    Collection<InjectableField> staticFields = null;
    Collection<InjectableField> instanceFields = null;
    // these are holding two things - the method signature mapped to the actual value
    Map<String, InjectableMethod> staticMethods = null;
    Map<String, InjectableMethod> instanceMethods = null;

    // read all fields & methods in reverse (super fields & method should get injected before implementation ones)
    List<Class<?>> hierarchyTree = ReflectionUtils.hierarchyTree(target);
    for (int i = hierarchyTree.size() - 1; i >= 0; i--) {
      Class<?> targetClass = hierarchyTree.get(i);
      // methods
      for (Method method : targetClass.getDeclaredMethods()) {
        // check if the method is marked as @Inject
        if (!method.isBridge() && !method.isSynthetic()) {
          // the signature is used to check if we already found a comparable method
          String visibility = ReflectionUtils.shortVisibilitySummary(method);
          String signature = String.format("[%s]%s%s", visibility, method.getName(), AsmUtils.methodDesc(method));
          // check if the method is an overridden one
          if (!JakartaBridge.isInjectable(method)) {
            if (Modifier.isStatic(method.getModifiers())) {
              // check if the method was already read but the overridden method is no longer annotated
              // in this case remove the method
              if (staticMethods != null) {
                staticMethods.remove(signature);
              }
            } else {
              // check if the method was already read but the overridden method is no longer annotated
              // in this case remove the method
              if (instanceMethods != null) {
                instanceMethods.remove(signature);
              }
            }
            // continue - the method is not annotated as @Inject
            continue;
          }
          // check if the method is not abstract - how the heck should we invoke them
          if (Modifier.isAbstract(method.getModifiers())) {
            throw new IllegalArgumentException(String.format(
              "Method %s in %s is abstract and cannot get injected",
              method.getName(),
              method.getDeclaringClass()));
          }
          // disable these checks - we do want to access them
          method.setAccessible(true);
          // check if the method is an instance or static method
          if (Modifier.isStatic(method.getModifiers())) {
            // put the method in the map if there was never a method in there before
            // otherwise check if we already saw a method like that one
            if (staticMethods == null) {
              staticMethods = new ConcurrentHashMap<>();
              staticMethods.put(signature, new InjectableMethod(method));
            } else if (!staticMethods.containsKey(signature)) {
              staticMethods.put(signature, new InjectableMethod(method));
            }
          } else {
            // put the method in the map if there was never a method in there before
            // otherwise check if we already saw a method like that one
            if (instanceMethods == null) {
              instanceMethods = new ConcurrentHashMap<>();
              instanceMethods.put(signature, new InjectableMethod(method));
            } else if (!instanceMethods.containsKey(signature)) {
              instanceMethods.put(signature, new InjectableMethod(method));
            }
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
            // put the field in the map if there was never a field in there before
            // otherwise check if we already saw a field like that one
            if (staticFields == null) {
              staticFields = new CopyOnWriteArraySet<>();
            }
            staticFields.add(new InjectableField(field));
          } else {
            // put the field in the map if there was never a field in there before
            // otherwise check if we already saw a field like that one
            if (instanceFields == null) {
              instanceFields = new CopyOnWriteArraySet<>();
            }
            instanceFields.add(new InjectableField(field));
          }
        }
      }
    }
    // assign the collection values in the class to the values of the maps
    this.staticFields = staticFields == null ? Collections.emptySet() : staticFields;
    this.instanceFields = instanceFields == null ? Collections.emptySet() : instanceFields;

    this.staticMethods = staticMethods == null ? Collections.emptySet() : staticMethods.values();
    this.instanceMethods = instanceMethods == null ? Collections.emptySet() : instanceMethods.values();

    // initialize the predicates to test whether a member belongs to the direct target class or not
    this.onlyThisClass = member -> member.getDeclaringClass() == this.targetClass;
    this.onlyNotThisClass = member -> member.getDeclaringClass() != this.targetClass;
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
    // according to the jakarta injection rules, all fields then all methods need to get injected
    // every static method must only be injected once
    // supertype fields
    if (!didStaticSupertypeFieldInjection.getAndSet(true)) {
      this.injectStaticFields(settings, context, this.onlyNotThisClass);
    }
    // supertype methods
    if (!didStaticSupertypeMethodInjection.getAndSet(true)) {
      this.injectStaticMethods(settings, context, this.onlyNotThisClass);
    }
    // supertype fields
    if (!this.didStaticFieldInjection.getAndSet(true)) {
      this.injectStaticFields(settings, context, this.onlyThisClass);
    }
    // supertype fields
    if (!didStaticMethodInjection.getAndSet(true)) {
      this.injectStaticMethods(settings, context, this.onlyThisClass);
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
    // do the static member injection first
    this.inject(settings, context);
    // instance methods
    this.injectInstanceFields(instance, settings, context, this.onlyNotThisClass);
    this.injectInstanceMethods(instance, settings, context, this.onlyNotThisClass);
    this.injectInstanceFields(instance, settings, context, this.onlyThisClass);
    this.injectInstanceMethods(instance, settings, context, this.onlyThisClass);
  }

  @Override
  public void injectField(@NotNull String name) {
    // try to find a static field with the given name and inject that
    for (InjectableField injectable : this.staticFields) {
      if (injectable.field.getName().equals(name)) {
        this.injectField(null, injectable, null);
        return; // a field name is unique
      }
    }
    // no such field found
    throw new IllegalArgumentException(String.format("No static field with name %s in %s", name, this.targetClass));
  }

  @Override
  public void injectField(@NotNull Object instance, @NotNull String name) {
    // try to find a static field with the given name and inject that
    for (InjectableField injectable : this.staticFields) {
      if (injectable.field.getName().equals(name)) {
        this.injectField(null, injectable, null);
        return; // a field name is unique
      }
    }
    // try to find an instance field with the given name and inject that
    for (InjectableField injectable : this.instanceFields) {
      if (injectable.field.getName().equals(name)) {
        this.injectField(instance, injectable, null);
        return; // a field name is unique
      }
    }
    // no such field found
    throw new IllegalArgumentException(String.format("No field with name %s in %s", name, this.targetClass));
  }

  @Override
  public @Nullable Object injectMethod(@NotNull String name, @NotNull Class<?>... parameterTypes) {
    // try to find a static method with the given name and inject that
    for (InjectableMethod injectable : this.staticMethods) {
      if (injectable.method.getName().equals(name) && Arrays.equals(injectable.parameterTypes, parameterTypes)) {
        return this.injectMethod(null, injectable, null);
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
    for (InjectableMethod injectable : this.staticMethods) {
      if (injectable.method.getName().equals(name) && Arrays.equals(injectable.parameterTypes, params)) {
        return this.injectMethod(null, injectable, null);
      }
    }
    // try to find an instance method with the given name and inject that
    for (InjectableMethod injectable : this.instanceMethods) {
      if (injectable.method.getName().equals(name) && Arrays.equals(injectable.parameterTypes, params)) {
        return this.injectMethod(instance, injectable, null);
      }
    }
    // no such method found
    throw new IllegalArgumentException(String.format(
      "No method with name %s and parameters %s in %s",
      name,
      Arrays.toString(params),
      this.targetClass));
  }

  private void injectStaticFields(
    @NotNull MemberInjectionSettings settings,
    @Nullable InjectionContext context,
    @NotNull Predicate<Member> preTester
  ) {
    // check if we need to inject all static fields
    if (settings.injectStaticFields()) {
      // loop and search for every field we should inject
      for (InjectableField field : this.staticFields) {
        if (preTester.test(field.field) && this.fieldMatches(field.field, settings, null)) {
          this.injectField(null, field, context);
        }
      }
    }
  }

  private void injectInstanceFields(
    @NotNull Object instance,
    @NotNull MemberInjectionSettings settings,
    @Nullable InjectionContext context,
    @NotNull Predicate<Member> preTester
  ) {
    // check if we need (and can) to inject instance fields
    if (settings.injectInstanceFields()) {
      // loop and search for every field we should inject
      for (InjectableField injectable : this.instanceFields) {
        if (preTester.test(injectable.field) && this.fieldMatches(injectable.field, settings, instance)) {
          this.injectField(instance, injectable, context);
        }
      }
    }
  }

  private void injectStaticMethods(
    @NotNull MemberInjectionSettings settings,
    @Nullable InjectionContext context,
    @NotNull Predicate<Member> preTester
  ) {
    // check if we need to inject all static methods
    if (settings.injectStaticMethods()) {
      // loop and search for every method we should inject
      for (InjectableMethod injectable : this.staticMethods) {
        if (preTester.test(injectable.method) && this.methodMatches(injectable.method, settings)) {
          this.injectMethod(null, injectable, context);
        }
      }
    }
  }

  private void injectInstanceMethods(
    @NotNull Object instance,
    @NotNull MemberInjectionSettings settings,
    @Nullable InjectionContext context,
    @NotNull Predicate<Member> preTester
  ) {
    // check if we need to inject instance methods
    if (settings.injectInstanceMethods()) {
      // loop and search for every method we should inject
      for (InjectableMethod injectable : this.instanceMethods) {
        if (preTester.test(injectable.method) && this.methodMatches(injectable.method, settings)) {
          this.injectMethod(instance, injectable, context);
        }
      }
    }
  }

  private @Nullable Object injectMethod(
    @Nullable Object instance,
    @NotNull InjectableMethod method,
    @Nullable InjectionContext context
  ) {
    try {
      // lookup the parameters for the method injection - null means we should skip the injection
      Object[] params = this.lookupParamInstances(method, context);
      if (params != null) {
        // invoke the method using the collected parameters
        return method.method.invoke(instance, params);
      }
      // return null if we didn't invoke the method
      return null;
    } catch (IllegalAccessException | InvocationTargetException exception) {
      throw new IllegalArgumentException("Unable to invoke method", exception);
    }
  }

  private void injectField(
    @Nullable Object instance,
    @NotNull InjectableField injectable,
    @Nullable InjectionContext context
  ) {
    try {
      Object fieldValue;
      // check if the field is a provider
      if (JakartaBridge.isProvider(injectable.field.getType())) {
        Provider<?> provider = context == null
          ? this.injector.binding(ElementHelper.buildElement(injectable.field, injectable.annotations))
          : context.injector().binding(ElementHelper.buildElement(injectable.field, injectable.annotations));
        // check if the provider is a jakarta provider
        if (JakartaBridge.needsProviderWrapping(injectable.field.getType())) {
          fieldValue = JakartaBridge.bridgeJakartaProvider(provider);
        } else {
          fieldValue = provider;
        }
      } else {
        // just needs the direct instance of the field type
        fieldValue = context == null
          ? this.injector.instance(ElementHelper.buildElement(injectable.field, injectable.annotations))
          : context.findInstance(ElementHelper.buildElement(injectable.field, injectable.annotations));
      }
      // check if we got a field value - skip the set if the field is optional
      if (fieldValue != null || !injectable.optional) {
        // set the field using the collected parameter
        injectable.field.set(instance, fieldValue);
      }
    } catch (IllegalAccessException exception) {
      throw new IllegalArgumentException("Unable to set field value", exception);
    }
  }

  private @Nullable Object[] lookupParamInstances(
    @NotNull InjectableMethod injectable,
    @Nullable InjectionContext context
  ) {
    // check if we need to collect parameters at all
    if (injectable.method.getParameterCount() == 0) {
      return NO_PARAMS;
    } else {
      Object[] paramInstances = new Object[injectable.parameters.length];
      // find for every type an instance in the parent injector
      for (int i = 0; i < injectable.parameters.length; i++) {
        // make an element from the parameter
        Element element = ElementHelper.buildElement(injectable.parameters[i], injectable.parameterAnnotations[i]);
        // check if the parameter is a provider
        if (JakartaBridge.isProvider(injectable.parameters[i].getType())) {
          // we only need the binding, not the direct instance then
          Provider<?> provider = context == null
            ? this.injector.binding(element)
            : context.injector().binding(element);
          // check if the provider is a jakarta provider
          if (JakartaBridge.needsProviderWrapping(injectable.parameters[i].getType())) {
            paramInstances[i] = JakartaBridge.bridgeJakartaProvider(provider);
          } else {
            paramInstances[i] = provider;
          }
        } else {
          // we do need the direct instance of the type
          paramInstances[i] = context == null
            ? this.injector.instance(element)
            : context.findInstance(element);
          // check if the parameter is null and the method is optional - skip the method injection in that case
          if (paramInstances[i] == null && injectable.optional) {
            return null;
          }
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
        // tries to read the field value
        return ReflectionUtils.isUninitialized(field, on);
      } catch (IllegalAccessException ignored) {
      }
    }
    // fall-trough
    return true;
  }

  private static final class InjectableMethod {

    private final Method method;
    private final boolean optional;
    private final Parameter[] parameters;
    private final Class<?>[] parameterTypes;
    private final Annotation[][] parameterAnnotations;

    public InjectableMethod(@NotNull Method method) {
      this.method = method;
      this.optional = JakartaBridge.isOptional(method);
      this.parameters = method.getParameters(); // prevents copy of these
      this.parameterTypes = method.getParameterTypes(); // prevents copy of these
      this.parameterAnnotations = method.getParameterAnnotations(); // prevents copy of these
    }
  }

  private static final class InjectableField {

    private final Field field;
    private final boolean optional;
    private final Annotation[] annotations;

    public InjectableField(@NotNull Field field) {
      this.field = field;
      this.optional = JakartaBridge.isOptional(field);
      this.annotations = field.getDeclaredAnnotations(); // prevents copy of these
    }
  }
}
