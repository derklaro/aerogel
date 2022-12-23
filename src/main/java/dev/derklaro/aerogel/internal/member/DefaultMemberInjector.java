/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 Pasqual K. and contributors
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

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.MemberInjectionSettings;
import dev.derklaro.aerogel.MemberInjector;
import dev.derklaro.aerogel.Order;
import dev.derklaro.aerogel.PostConstruct;
import dev.derklaro.aerogel.Provider;
import dev.derklaro.aerogel.binding.BindingHolder;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import dev.derklaro.aerogel.internal.reflect.ReflectionUtil;
import dev.derklaro.aerogel.internal.unsafe.UnsafeMemberAccess;
import dev.derklaro.aerogel.internal.utility.ElementHelper;
import dev.derklaro.aerogel.internal.utility.MethodHandleUtil;
import dev.derklaro.aerogel.internal.utility.Preconditions;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A default implementation of a {@link MemberInjector}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal")
public final class DefaultMemberInjector implements MemberInjector {

  /**
   * A static object array representing a no-args injection dependencies array.
   */
  private static final Object[] NO_PARAMS = new Object[0];

  /**
   * A member injection setting which inject all members according to the setting standards.
   */
  private static final MemberInjectionSettings ALL = MemberInjectionSettings.builder().build();
  /**
   * A member injection setting which only injects static members.
   */
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
  private final Collection<InjectableMethod> postConstructMethods;

  private final Set<MemberType> injectedStaticMemberTypes = EnumSet.noneOf(MemberType.class);

  /**
   * Constructs a new default member injection instance.
   *
   * @param injector the injector used for dependency instance lookups.
   * @param target   the target class for which this injector gets created.
   */
  public DefaultMemberInjector(@NotNull Injector injector, @NotNull Class<?> target) {
    this.injector = injector;
    this.targetClass = target;

    // these are just all fields, lazy initialized
    Collection<InjectableField> staticFields = null;
    Collection<InjectableField> instanceFields = null;

    // these are holding two things - the method signature mapped to the actual value
    Collection<InjectableMethod> staticMethods = null;
    Map<String, InjectableMethod> instanceMethods = null;
    Map<String, InjectableMethod> postConstructMethods = null;

    // read all fields & methods in reverse (super fields & method should get injected before implementation ones)
    List<Class<?>> hierarchyTree = ReflectionUtil.hierarchyTree(target);
    int startIndex = hierarchyTree.size() - 1;
    for (int i = startIndex; i >= 0; i--) {
      Class<?> targetClass = hierarchyTree.get(i);
      // methods
      for (Method method : targetClass.getDeclaredMethods()) {
        // only check these once
        boolean injectable = JakartaBridge.isInjectable(method);
        boolean postConstructListener = method.isAnnotationPresent(PostConstruct.class);

        // if the method is static we can step all further steps
        if (Modifier.isStatic(method.getModifiers())) {
          Preconditions.checkArgument(!postConstructListener, "@PostConstruct method is static");

          // check if the method is injectable
          if (injectable) {
            UnsafeMemberAccess.forceMakeAccessible(method);

            // initialize the static methods & register the method
            if (staticMethods == null) {
              staticMethods = new ArrayList<>();
            }
            staticMethods.add(new InjectableMethod(method));
          }
          continue;
        }

        // the signature is used to check if we already found a comparable method
        String visibility = ReflectionUtil.shortVisibilitySummary(method);
        String parameterDesc = Arrays.stream(method.getParameterTypes())
          .map(Class::getName)
          .collect(Collectors.joining(", ", "(", ")"));
        String signature = String.format("[%s]%s%s", visibility, method.getName(), parameterDesc);

        // check if the method is an overridden one
        if (!injectable && !postConstructListener) {
          // check if the method was already read but the overridden method is no longer annotated
          // in this case remove the method
          if (instanceMethods != null) {
            instanceMethods.remove(signature);
          }
          if (postConstructMethods != null) {
            postConstructMethods.remove(signature);
          }
          continue;
        }

        Preconditions.checkArgument(
          !Modifier.isAbstract(method.getModifiers()),
          "abstract method is marked as @Inject/@PostConstruct");

        // disable these checks - we do want to access them
        UnsafeMemberAccess.forceMakeAccessible(method);

        // if the method is a post construct listener we can register it
        if (postConstructListener) {
          Preconditions.checkArgument(!injectable, "@PostConstruct method is marked as @Inject");
          Preconditions.checkArgument(method.getParameterCount() == 0, "@PostConstruct method takes arguments");

          // register the method
          if (postConstructMethods == null) {
            postConstructMethods = new HashMap<>();
          }
          postConstructMethods.put(signature, new InjectableMethod(method));
          continue;
        }

        // at this point the injectable boolean is always true - if there are further checks required add that here
        if (instanceMethods == null) {
          instanceMethods = new HashMap<>();
        }
        instanceMethods.put(signature, new InjectableMethod(method));
      }

      // fields
      for (Field field : targetClass.getDeclaredFields()) {
        // check if the field is marked as @Inject
        if (JakartaBridge.isInjectable(field)) {
          // check if the field is final - we cannot inject them as the modifiers field was added to the reflection
          // blocklist in Java 12 and a warning gets emitted since Java 9. There is no point to support this behaviour
          // for Java 8 users...
          if (Modifier.isFinal(field.getModifiers())) {
            throw AerogelException.forMessage(String.format(
              "Field %s in %s is final and cannot get injected",
              field.getName(),
              field.getDeclaringClass()));
          }
          // disable these checks - we do want to access them
          UnsafeMemberAccess.forceMakeAccessible(field);
          // check if the field is an instance or static field
          if (Modifier.isStatic(field.getModifiers())) {
            // put the field in the map if there was never a field in there before
            // otherwise check if we already saw a field like that one
            if (staticFields == null) {
              staticFields = new HashSet<>();
            }
            staticFields.add(new InjectableField(field));
          } else {
            // put the field in the map if there was never a field in there before
            // otherwise check if we already saw a field like that one
            if (instanceFields == null) {
              instanceFields = new HashSet<>();
            }
            instanceFields.add(new InjectableField(field));
          }
        }
      }
    }

    // assign the collection values in the class to the values of the maps
    this.staticFields = staticFields == null ? Collections.emptySet() : staticFields;
    this.instanceFields = instanceFields == null ? Collections.emptySet() : instanceFields;

    this.instanceMethods = sortMethods(instanceMethods);
    this.postConstructMethods = sortMethods(postConstructMethods);
    this.staticMethods = staticMethods == null ? Collections.emptySet() : sortMethods(staticMethods);

    // initialize the predicates to test whether a member belongs to the direct target class or not
    this.onlyThisClass = member -> member.getDeclaringClass() == this.targetClass;
    this.onlyNotThisClass = member -> member.getDeclaringClass() != this.targetClass;
  }

  /**
   * Extracts the values from the given map and sorts them according to the order annotation defined on the methods.
   *
   * @param methods the methods to extract and sort.
   * @return a collection of methods, ordered in the execution order.
   * @since 2.0
   */
  private static @NotNull Collection<InjectableMethod> sortMethods(@Nullable Map<String, InjectableMethod> methods) {
    // just do nothing if the methods are empty
    if (methods == null || methods.isEmpty()) {
      return Collections.emptyList();
    }

    // extract the methods into a list and sort it
    return sortMethods(methods.values());
  }

  /**
   * Sorts the given collection elements according to the order annotation defined on the methods.
   *
   * @param methods the methods to sort.
   * @return a new collection with the given elements in a sorted order.
   * @since 2.0
   */
  private static @NotNull Collection<InjectableMethod> sortMethods(@NotNull Collection<InjectableMethod> methods) {
    List<InjectableMethod> injectableMethods = new ArrayList<>(methods);
    Collections.sort(injectableMethods);
    return injectableMethods;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector injector() {
    return this.injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Class<?> targetClass() {
    return this.targetClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void inject() {
    this.inject(ONLY_STATIC);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void inject(@NotNull MemberInjectionSettings settings) {
    Objects.requireNonNull(settings, "settings");
    this.inject(settings, (InjectionContext) null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void inject(@NotNull MemberInjectionSettings settings, @Nullable InjectionContext context) {
    Objects.requireNonNull(settings, "settings");
    // according to the jakarta injection rules, all fields then all methods need to get injected
    // every static method must only be injected once
    // supertype fields
    if (this.injectedStaticMemberTypes.add(MemberType.INHERITED_FIELD)) {
      this.injectStaticFields(settings, context, this.onlyNotThisClass);
    }
    // supertype methods
    if (this.injectedStaticMemberTypes.add(MemberType.INHERITED_METHOD)) {
      this.injectStaticMethods(settings, context, this.onlyNotThisClass);
    }
    // direct fields
    if (this.injectedStaticMemberTypes.add(MemberType.FIELD)) {
      this.injectStaticFields(settings, context, this.onlyThisClass);
    }
    // direct methods
    if (this.injectedStaticMemberTypes.add(MemberType.METHOD)) {
      this.injectStaticMethods(settings, context, this.onlyThisClass);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void inject(@NotNull Object instance) {
    this.inject(instance, ALL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void inject(@NotNull Object instance, @NotNull MemberInjectionSettings settings) {
    this.inject(instance, settings, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void inject(
    @NotNull Object instance,
    @NotNull MemberInjectionSettings settings,
    @Nullable InjectionContext context
  ) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(settings, "settings");
    // do the static member injection first
    this.inject(settings, context);
    // instance methods
    this.injectInstanceFields(instance, settings, context, this.onlyNotThisClass);
    this.injectInstanceMethods(instance, settings, context, this.onlyNotThisClass);
    this.injectInstanceFields(instance, settings, context, this.onlyThisClass);
    this.injectInstanceMethods(instance, settings, context, this.onlyThisClass);
    this.executePostConstructListeners(instance, settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void injectField(@NotNull String name) {
    Objects.requireNonNull(name, "name");
    // try to find a static field with the given name and inject that
    for (InjectableField injectable : this.staticFields) {
      if (injectable.field.getName().equals(name)) {
        this.injectField(null, injectable, null);
        return; // a field name is unique
      }
    }
    // no such field found
    throw AerogelException.forMessage(String.format("No static field with name %s in %s", name, this.targetClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void injectField(@NotNull Object instance, @NotNull String name) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(name, "name");
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
    throw AerogelException.forMessage(String.format("No field with name %s in %s", name, this.targetClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object injectMethod(@NotNull String name, @NotNull Class<?>... parameterTypes) {
    Objects.requireNonNull(name, "name");
    // try to find a static method with the given name and inject that
    for (InjectableMethod injectable : this.staticMethods) {
      if (injectable.method.getName().equals(name) && Arrays.equals(injectable.parameterTypes, parameterTypes)) {
        return this.injectMethod(null, injectable, null);
      }
    }
    // no such method found
    throw AerogelException.forMessage(String.format(
      "No static method with name %s and parameters %s in %s",
      name,
      Arrays.toString(parameterTypes),
      this.targetClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object injectMethod(@NotNull Object instance, @NotNull String name, @NotNull Class<?>... params) {
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(name, "name");
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
    throw AerogelException.forMessage(String.format(
      "No method with name %s and parameters %s in %s",
      name,
      Arrays.toString(params),
      this.targetClass));
  }

  /**
   * Inject all static fields which also pass the {@code preTester}.
   *
   * @param settings  the settings of the injection.
   * @param context   the context of the injection or null if not in a context operation.
   * @param preTester the extra tester if a field matches.
   * @throws AerogelException if the field injection fails.
   */
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

  /**
   * Injects all instance fields to the {@code instance} which also pass the {@code preTester}.
   *
   * @param instance  the instance to inject the fields into.
   * @param settings  the settings of the injection.
   * @param context   the context of the injection or null if not in a context operation.
   * @param preTester the extra tester if a field matches.
   * @throws AerogelException if the field injection fails.
   */
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

  /**
   * Injects all static method which also pass the {@code preTester}.
   *
   * @param settings  the settings of the injection.
   * @param context   the context of the injection or null if not in a context operation.
   * @param preTester the extra tester if a method matches.
   * @throws AerogelException if the method injection fails.
   */
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

  /**
   * Injects all method which also pass the {@code preTester}.
   *
   * @param instance  the instance to inject the methods on.
   * @param settings  the settings of the injection.
   * @param context   the context of the injection or null if not in a context operation.
   * @param preTester the extra tester if a method matches.
   * @throws AerogelException if the method injection fails.
   */
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

  /**
   * Executes all post construct listeners.
   *
   * @param instance the instance to inject the methods on.
   * @param settings the settings of the injection.
   * @throws AerogelException if the method injection fails.
   */
  private void executePostConstructListeners(@NotNull Object instance, @NotNull MemberInjectionSettings settings) {
    // check if we need to execute post construct listeners
    if (settings.executePostConstructListeners()) {
      // loop and search for every method we should execute
      for (InjectableMethod injectable : this.postConstructMethods) {
        this.injectMethod(instance, injectable, null);
      }
    }
  }

  /**
   * Injects a specific method and return the method's return value.
   *
   * @param instance the instance to inject the methods on, can be null if the method is static.
   * @param method   the method to inject.
   * @param context  the context of the injection or null if not in a context operation.
   * @return the result of the method invocation.
   * @throws AerogelException if the method injection fails.
   */
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
        if (instance == null) {
          return params.length == 0 ? method.methodHandle.invoke((Object) null) : method.methodHandle.invoke(null, params);
        } else {
          return params.length == 0
            ? method.methodHandle.invoke(instance)
            : method.methodHandle.invoke(instance, params);
        }
      }
      // return null if we didn't invoke the method
      return null;
    } catch (Throwable exception) {
      throw AerogelException.forMessagedException("Unable to invoke method " + method.method, exception);
    }
  }

  /**
   * Injects a specific field.
   *
   * @param instance   the instance to inject the field on, can be null if the field is static.
   * @param injectable the field to inject.
   * @param context    the context of the injection or null if not in a context operation.
   * @throws AerogelException if the field injection fails.
   */
  private void injectField(
    @Nullable Object instance,
    @NotNull InjectableField injectable,
    @Nullable InjectionContext context
  ) {
    try {
      Object fieldValue;
      // check if the field is a provider
      if (JakartaBridge.isProvider(injectable.field.getType())) {
        BindingHolder bindingHolder = context == null
          ? this.injector.binding(ElementHelper.buildElement(injectable.field, injectable.annotations))
          : context.injector().binding(ElementHelper.buildElement(injectable.field, injectable.annotations));
        Provider<?> provider = bindingHolder.provider();
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
        if (instance == null) {
          injectable.fieldSetter.invoke(fieldValue);
        } else {
          injectable.fieldSetter.invoke(instance, fieldValue);
        }
      }
    } catch (Throwable exception) {
      throw AerogelException.forMessagedException("Unable to set field value", exception);
    }
  }

  /**
   * Lookups all dependencies for injection a specific method.
   *
   * @param injectable the method to inject.
   * @param context    the context of the injection or null if not in a context operation.
   * @return all parameters needed to invoke the method.
   */
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
            ? this.injector.binding(element).provider()
            : context.injector().binding(element).provider();
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

  /**
   * Checks if the given method matches the given injection settings.
   *
   * @param method   the method to check.
   * @param settings the settings to check against.
   * @return true if the method matches the settings, false otherwise.
   */
  private boolean methodMatches(@NotNull Method method, @NotNull MemberInjectionSettings settings) {
    // check if the method is private & private method injection is enabled
    if (Modifier.isPrivate(method.getModifiers()) && !settings.injectPrivateMethods()) {
      return false;
    }
    // check if the method is inherited & inherited methods are enabled
    return method.getDeclaringClass().equals(this.targetClass) || settings.injectInheritedMethods();
  }

  /**
   * Checks if the given field matches the given injection settings.
   *
   * @param field    the field to check.
   * @param settings the settings to check against.
   * @param on       the instance to read the value of the field is necessary, can be null for static fields.
   * @return true if the field matches the settings, false otherwise.
   */
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
        return ReflectionUtil.isUninitialized(field, on);
      } catch (IllegalAccessException ignored) {
      }
    }
    // fall-trough
    return true;
  }

  /**
   * Represents a data holder for a method which prevents runtime overloads.
   *
   * @author Pasqual K.
   * @since 1.0
   */
  private static final class InjectableMethod implements Comparable<InjectableMethod> {

    private final int order;
    private final Method method;
    private final boolean optional;
    private final Parameter[] parameters;
    private final Class<?>[] parameterTypes;
    private final Annotation[][] parameterAnnotations;

    private final MethodHandle methodHandle;

    // precomputed hash to improve speed down the line
    private final int hashCode;

    /**
     * Constructs a new injectable method based on the given {@code method}.
     *
     * @param method the method to construct this holder for.
     */
    public InjectableMethod(@NotNull Method method) {
      this.method = method;
      this.optional = JakartaBridge.isOptional(method);
      this.parameters = method.getParameters(); // prevents copy of these
      this.parameterTypes = method.getParameterTypes(); // prevents copy of these
      this.parameterAnnotations = method.getParameterAnnotations(); // prevents copy of these
      this.hashCode = this.method.hashCode() ^ Boolean.hashCode(this.optional);
      this.methodHandle = MethodHandleUtil.toGenericMethodHandle(method);

      Order orderAnnotation = method.getAnnotation(Order.class);
      this.order = orderAnnotation == null ? Order.DEFAULT : orderAnnotation.value();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return this.hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@NotNull DefaultMemberInjector.InjectableMethod o) {
      return Integer.compare(this.order, o.order);
    }
  }

  /**
   * Represents a data holder for a field which prevents runtime overloads.
   *
   * @author Pasqual K.
   * @since 1.0
   */
  private static final class InjectableField {

    private final Field field;
    private final boolean optional;
    private final Annotation[] annotations;

    private final MethodHandle fieldSetter;

    // precomputed hash to improve speed down the line
    private final int hashCode;

    /**
     * Constructs a new injectable field based on the given {@code field}.
     *
     * @param field the field to construct this holder for.
     */
    public InjectableField(@NotNull Field field) {
      this.field = field;
      this.optional = JakartaBridge.isOptional(field);
      this.annotations = field.getDeclaredAnnotations(); // prevents copy of these
      this.hashCode = this.field.hashCode() ^ Boolean.hashCode(this.optional);
      this.fieldSetter = MethodHandleUtil.toGenericSetterMethodHandle(field);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return this.hashCode;
    }
  }
}
