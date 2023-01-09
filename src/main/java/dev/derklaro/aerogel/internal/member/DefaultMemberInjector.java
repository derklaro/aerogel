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

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.Order;
import dev.derklaro.aerogel.Provider;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import dev.derklaro.aerogel.internal.reflect.ReflectionUtil;
import dev.derklaro.aerogel.internal.utility.ElementHelper;
import dev.derklaro.aerogel.internal.utility.MethodHandleUtil;
import dev.derklaro.aerogel.internal.utility.Preconditions;
import dev.derklaro.aerogel.member.InjectionSetting;
import dev.derklaro.aerogel.member.MemberInjector;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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

  private final Injector injector;
  private final Class<?> targetClass;

  private final Predicate<Member> memberInTargetClass;
  private final Predicate<Member> memberInInheritedClass;

  private final List<InjectableField> injectableFields;
  private final List<InjectableMethod> injectableMethods;
  private final List<InjectableMethod> postConstructMethods;

  /**
   * Constructs a new default member injection instance.
   *
   * @param injector the injector used for dependency instance lookups.
   * @param target   the target class for which this injector gets created.
   */
  public DefaultMemberInjector(@NotNull Injector injector, @NotNull Class<?> target) {
    this.injector = injector;
    this.targetClass = target;

    // build the member tree
    MemberTree memberTree = new MemberTree(target);
    memberTree.buildTree();

    // move from java.lang members to injectable ones
    this.injectableFields = memberTree.getInjectableFields().stream()
      .map(InjectableField::new)
      .collect(Collectors.toCollection(LinkedList::new));
    this.injectableMethods = memberTree.getInjectableMethods().stream()
      .map(InjectableMethod::new)
      .sorted()
      .collect(Collectors.toCollection(LinkedList::new));
    this.postConstructMethods = memberTree.getPostConstructMethods().stream()
      .map(InjectableMethod::new)
      .sorted()
      .collect(Collectors.toCollection(LinkedList::new));

    // initialize the predicates to test whether a member belongs to the direct target class or not
    this.memberInTargetClass = member -> member.getDeclaringClass() == this.targetClass;
    this.memberInInheritedClass = member -> member.getDeclaringClass() != this.targetClass;
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
    this.inject(InjectionSetting.FLAG_STATIC_MEMBERS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void inject(long flags) {
    this.inject(null, flags);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void inject(@Nullable Object instance) {
    this.inject(instance, InjectionSetting.FLAG_ALL_MEMBERS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void inject(@Nullable Object instance, long flags) {
    // static fields & methods
    this.injectStaticFields(this.memberInInheritedClass, flags);
    this.injectStaticMethods(this.memberInInheritedClass, flags);
    this.injectStaticFields(this.memberInTargetClass, flags);
    this.injectStaticMethods(this.memberInTargetClass, flags);

    // instance fields & methods
    this.injectInstanceFields(instance, this.memberInInheritedClass, flags);
    this.injectInstanceMethods(instance, this.memberInInheritedClass, flags);
    this.injectInstanceFields(instance, this.memberInTargetClass, flags);
    this.injectInstanceMethods(instance, this.memberInTargetClass, flags);

    // post construct listeners
    this.executePostConstructListeners(instance, flags);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void injectField(@NotNull String name) {
    // try to find a static field with the given name and inject that
    for (InjectableField injectable : this.injectableFields) {
      if (injectable.field.getName().equals(name)) {
        this.injectField(null, injectable);
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
  public void injectField(@Nullable Object instance, @NotNull String name) {
    // try to find a static field with the given name and inject that
    for (InjectableField injectable : this.injectableFields) {
      if (injectable.field.getName().equals(name)) {
        this.injectField(instance, injectable);
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
    // try to find a static method with the given name and inject that
    for (InjectableMethod injectable : this.injectableMethods) {
      if (injectable.method.getName().equals(name) && Arrays.equals(injectable.parameterTypes, parameterTypes)) {
        return this.injectMethod(null, injectable);
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
  public @Nullable Object injectMethod(@Nullable Object instance, @NotNull String name, @NotNull Class<?>... params) {
    // try to find a method with the given name and inject that
    for (InjectableMethod injectable : this.injectableMethods) {
      if (injectable.method.getName().equals(name) && Arrays.equals(injectable.parameterTypes, params)) {
        return this.injectMethod(instance, injectable);
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
   * @param flags     the flags to check against.
   * @param preTester the extra tester if a field matches.
   * @throws AerogelException if the field injection fails.
   */
  private void injectStaticFields(@NotNull Predicate<Member> preTester, long flags) {
    // check if we need to inject all static fields
    if (InjectionSetting.STATIC_FIELDS.enabled(flags)) {
      // loop and search for every field we should inject
      for (InjectableField field : this.injectableFields) {
        if (Modifier.isStatic(field.field.getModifiers())
          && preTester.test(field.field)
          && this.fieldMatches(field.field, null, flags)
        ) {
          this.injectField(null, field);
        }
      }
    }
  }

  /**
   * Injects all instance fields to the {@code instance} which also pass the {@code preTester}.
   *
   * @param instance  the instance to inject the fields into.
   * @param flags     the flags to check against.
   * @param preTester the extra tester if a field matches.
   * @throws AerogelException if the field injection fails.
   */
  private void injectInstanceFields(@Nullable Object instance, @NotNull Predicate<Member> preTester, long flags) {
    // check if we need (and can) to inject instance fields
    if (InjectionSetting.INSTANCE_FIELDS.enabled(flags)) {
      // loop and search for every field we should inject
      for (InjectableField injectable : this.injectableFields) {
        if (!Modifier.isStatic(injectable.field.getModifiers())
          && preTester.test(injectable.field)
          && this.fieldMatches(injectable.field, instance, flags)
        ) {
          this.injectField(instance, injectable);
        }
      }
    }
  }

  /**
   * Injects all static method which also pass the {@code preTester}.
   *
   * @param flags     the flags to check against.
   * @param preTester the extra tester if a method matches.
   * @throws AerogelException if the method injection fails.
   */
  private void injectStaticMethods(@NotNull Predicate<Member> preTester, long flags) {
    // check if we need to inject all static methods
    if (InjectionSetting.STATIC_METHODS.enabled(flags)) {
      // loop and search for every method we should inject
      for (InjectableMethod injectable : this.injectableMethods) {
        if (Modifier.isStatic(injectable.method.getModifiers())
          && preTester.test(injectable.method)
          && this.methodMatches(injectable.method, flags)
        ) {
          this.injectMethod(null, injectable);
        }
      }
    }
  }

  /**
   * Injects all method which also pass the {@code preTester}.
   *
   * @param instance  the instance to inject the methods on.
   * @param preTester the extra tester if a method matches.
   * @param flags     the flags to check against.
   * @throws AerogelException if the method injection fails.
   */
  private void injectInstanceMethods(@Nullable Object instance, @NotNull Predicate<Member> preTester, long flags) {
    // check if we need to inject instance methods
    if (InjectionSetting.INSTANCE_METHODS.enabled(flags)) {
      // loop and search for every method we should inject
      for (InjectableMethod injectable : this.injectableMethods) {
        if (!Modifier.isStatic(injectable.method.getModifiers())
          && preTester.test(injectable.method)
          && this.methodMatches(injectable.method, flags)
        ) {
          this.injectMethod(instance, injectable);
        }
      }
    }
  }

  /**
   * Executes all post construct listeners.
   *
   * @param instance the instance to inject the methods on.
   * @param flags    the flags to check against.
   * @throws AerogelException if the method injection fails.
   */
  private void executePostConstructListeners(@Nullable Object instance, long flags) {
    // check if we need to execute post construct listeners
    if (InjectionSetting.RUN_POST_CONSTRUCT_LISTENERS.enabled(flags)) {
      // loop and search for every method we should execute
      for (InjectableMethod injectable : this.postConstructMethods) {
        this.injectMethod(instance, injectable);
      }
    }
  }

  /**
   * Injects a specific method and return the method's return value.
   *
   * @param instance the instance to inject the methods on, can be null if the method is static.
   * @param method   the method to inject.
   * @return the result of the method invocation.
   * @throws AerogelException if the method injection fails.
   */
  private @Nullable Object injectMethod(@Nullable Object instance, @NotNull InjectableMethod method) {
    try {
      // ensure that we can call the method
      boolean isStatic = Modifier.isStatic(method.method.getModifiers());
      if (!isStatic && instance == null) {
        return null;
      }

      // lookup the parameters for the method injection and invoke the method with them
      Object[] params = this.lookupParamInstances(method);
      return method.invoke(instance, params);
    } catch (Throwable exception) {
      // we can ignore the error thrown by the method if the injection was optional
      if (!method.optional) {
        throw AerogelException.forMessagedException("Unable to invoke method " + method.method, exception);
      }
      // no return value known
      return null;
    }
  }

  /**
   * Injects a specific field.
   *
   * @param instance the instance to inject the field on, can be null if the field is static.
   * @param field    the field to inject.
   * @throws AerogelException if the field injection fails.
   */
  private void injectField(@Nullable Object instance, @NotNull InjectableField field) {
    try {
      // ensure that we can set the field value
      boolean isStatic = Modifier.isStatic(field.field.getModifiers());
      if (!isStatic && instance == null) {
        return;
      }

      // build an element for the field and resolve the associated instance
      Element fieldElement = ElementHelper.buildElement(field.field, field.annotations);
      Object fieldValue = this.lookupInstance(fieldElement, field.field.getType());

      // check if we are required to set the field value
      if (fieldValue == null) {
        Preconditions.checkArgument(
          field.optional,
          "Unable to resolve field value, but field " + field.field + " is required");
        return;
      }

      // set the field value
      field.setValue(instance, fieldValue);
    } catch (Throwable exception) {
      // if the field injection was optional we can ignore the error
      if (!field.optional) {
        throw AerogelException.forMessagedException("Unable to inject field value of " + field.field, exception);
      }
    }
  }

  /**
   * Lookups all dependencies for injection a specific method.
   *
   * @param injectable the method to inject.
   * @return all parameters needed to invoke the method.
   */
  private @NotNull Object[] lookupParamInstances(@NotNull InjectableMethod injectable) {
    // check if we need to collect parameters at all
    if (injectable.method.getParameterCount() == 0) {
      return NO_PARAMS;
    } else {
      // find for every type an instance in the parent injector
      Object[] paramInstances = new Object[injectable.parameters.length];
      for (int i = 0; i < injectable.parameters.length; i++) {
        Parameter parameter = injectable.parameters[i];

        // get an element representing the parameter and resolve the associated instance
        Element paramElement = ElementHelper.buildElement(parameter, injectable.parameterAnnotations[i]);
        paramInstances[i] = this.lookupInstance(paramElement, parameter.getType());
      }

      // return the collected instances
      return paramInstances;
    }
  }

  /**
   * Looks up the instance for the given element. If the element is a provider or jakarta provider, the instance is not
   * resolved and only the associated (wrapped) provider is returned.
   *
   * @param element the element of the type to get.
   * @param rawType the raw type which is requested.
   * @return the instance which can be used for further actions.
   * @throws AerogelException if an exception occurs looking up an instance.
   */
  private @Nullable Object lookupInstance(@NotNull Element element, @NotNull Class<?> rawType) {
    // check if the target type is a provider
    if (JakartaBridge.isProvider(rawType)) {
      // resolve the provider and wrap it to a jakarta provider if needed
      Provider<?> provider = this.injector.binding(element).provider();
      if (JakartaBridge.needsProviderWrapping(rawType)) {
        return JakartaBridge.bridgeJakartaProvider(provider);
      } else {
        return provider;
      }
    } else {
      // just get the instance of the element
      return this.injector.instance(element);
    }
  }

  /**
   * Checks if the given method matches the given injection settings.
   *
   * @param method the method to check.
   * @param flags  the flags to check against.
   * @return true if the method matches the settings, false otherwise.
   */
  private boolean methodMatches(@NotNull Method method, long flags) {
    // check if the method is private & private method injection is enabled
    if (Modifier.isPrivate(method.getModifiers()) && InjectionSetting.PRIVATE_METHODS.disabled(flags)) {
      return false;
    }

    // check if the method is not inherited
    if (method.getDeclaringClass().equals(this.targetClass)) {
      return true;
    }

    // check if inherited methods are enabled
    return InjectionSetting.INHERITED_METHODS.enabled(flags);
  }

  /**
   * Checks if the given field matches the given injection settings.
   *
   * @param field the field to check.
   * @param on    the instance to read the value of the field is necessary, can be null for static fields.
   * @param flags the flags to check against.
   * @return true if the field matches the settings, false otherwise.
   */
  private boolean fieldMatches(@NotNull Field field, @Nullable Object on, long flags) {
    // check if the method is private & private method injection is enabled
    if (Modifier.isPrivate(field.getModifiers()) && InjectionSetting.PRIVATE_FIELDS.disabled(flags)) {
      return false;
    }

    // check if the method is inherited & inherited methods are enabled
    if (!field.getDeclaringClass().equals(this.targetClass) && InjectionSetting.INHERITED_METHODS.disabled(flags)) {
      return false;
    }

    // check if the field is initialized
    if (InjectionSetting.ONLY_UNINITIALIZED_FIELDS.enabled(flags)) {
      try {
        // tries to read the field value
        return ReflectionUtil.isUninitialized(field, on);
      } catch (Exception ignored) {
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
    private final boolean onlyInjectOnce;
    private final Parameter[] parameters;
    private final Class<?>[] parameterTypes;
    private final Annotation[][] parameterAnnotations;

    private final MethodHandle methodHandle;

    // precomputed hash to improve speed down the line
    private final int hashCode;

    // if we injected the method at least once
    private boolean injectedOnce;

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
      this.onlyInjectOnce = Modifier.isStatic(method.getModifiers()); // only inject static methods once
      this.hashCode = this.method.hashCode() ^ Boolean.hashCode(this.optional);
      this.methodHandle = MethodHandleUtil.toGenericMethodHandle(method);

      Order orderAnnotation = method.getAnnotation(Order.class);
      this.order = orderAnnotation == null ? Order.DEFAULT : orderAnnotation.value();
    }

    /**
     * Invokes the underlying method and returns the call result, unless the method can only be called once and was
     * already injected.
     *
     * @param instance the instance to call the method on, can be null for static methods.
     * @param params   the parameters to pass to the method.
     * @return the method call result, might be null.
     * @throws Throwable anything thrown by the underlying method.
     */
    public @Nullable Object invoke(@Nullable Object instance, @NotNull Object[] params) throws Throwable {
      // check if we are allowed to inject again
      if (!this.onlyInjectOnce || !this.injectedOnce) {
        // mark as injected
        this.injectedOnce = true;

        // invoke the method
        instance = Modifier.isStatic(this.method.getModifiers()) ? null : instance;
        return MethodHandleUtil.invokeMethod(this.methodHandle, instance, params);
      }
      // already injected
      return null;
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
    private final boolean onlyInjectOnce;
    private final Annotation[] annotations;

    private final MethodHandle fieldSetter;

    // precomputed hash to improve speed down the line
    private final int hashCode;

    private boolean injectedOnce;

    /**
     * Constructs a new injectable field based on the given {@code field}.
     *
     * @param field the field to construct this holder for.
     */
    public InjectableField(@NotNull Field field) {
      this.field = field;
      this.optional = JakartaBridge.isOptional(field);
      this.annotations = field.getDeclaredAnnotations(); // prevents copy of these
      this.onlyInjectOnce = Modifier.isStatic(field.getModifiers()); // only inject static fields once
      this.hashCode = this.field.hashCode() ^ Boolean.hashCode(this.optional);
      this.fieldSetter = MethodHandleUtil.toGenericSetterMethodHandle(field);
    }

    /**
     * Sets the value of the underlying field, unless the value was already set and can only be set once.
     *
     * @param instance the instance to set the field on, null for static fields.
     * @param value    the value to set as the field value.
     * @throws Throwable any issue that occurred while setting the field value.
     */
    public void setValue(@Nullable Object instance, @Nullable Object value) throws Throwable {
      // check if we are allowed to inject again
      if (!this.onlyInjectOnce || !this.injectedOnce) {
        // mark as injected
        this.injectedOnce = true;

        // set the field value
        if (Modifier.isStatic(this.field.getModifiers())) {
          this.fieldSetter.invoke(value);
        } else {
          this.fieldSetter.invoke(instance, value);
        }
      }
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
