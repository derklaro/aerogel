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

package dev.derklaro.aerogel.internal.codegen;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.internal.asm.AsmPrimitives;
import dev.derklaro.aerogel.internal.asm.AsmUtils;
import dev.derklaro.aerogel.internal.unsafe.ClassDefiners;
import dev.derklaro.aerogel.internal.utility.Preconditions;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * A util for generating runtime proxies for interfaces instead of using reflection.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class InjectionTimeProxy {

  // methods that we handle specifically
  private static final Collection<String> IGNORED_METHODS = Arrays.asList(
    "toString()Ljava/lang/String;",
    "equals(Ljava/lang/Object;)Z",
    "hashCode()I");

  private static final String PROXY_NAME_FORMAT = "%s$Proxy_%d";
  // stuff for the InjectionTimeProxied class
  private static final String SET_DELEGATE_DESC = AsmUtils.methodDesc(void.class, Object.class);
  private static final String INJECTION_TIME_NAME = Type.getInternalName(InjectionTimeProxied.class);
  // stuff for the Preconditions class
  private static final String PRECONDITIONS_NAME = Type.getInternalName(Preconditions.class);
  private static final String CHECK_ARGUMENT_DESC = AsmUtils.methodDesc(void.class, boolean.class, String.class);
  // stuff for the Objects class
  private static final String OBJECTS_NAME = Type.getInternalName(Objects.class);
  private static final String ENSURE_NOT_NULL_DESC = AsmUtils.methodDesc(Object.class, Object.class, String.class);
  // the standard methods from the object class: toString, equals & hashCode
  private static final Method EQUALS;
  private static final Method TO_STRING;
  private static final Method HASH_CODE;

  static {
    try {
      // lookup toString, equals & hashCode
      TO_STRING = Object.class.getMethod("toString");
      HASH_CODE = Object.class.getMethod("hashCode");
      EQUALS = Object.class.getMethod("equals", Object.class);
    } catch (NoSuchMethodException exception) {
      // cannot happen - just explode
      throw new ExceptionInInitializerError(exception);
    }
  }

  private InjectionTimeProxy() {
    throw new UnsupportedOperationException();
  }

  /**
   * Makes a runtime proxy for the given interface class.
   *
   * @param interfaceClass the class to generate the proxy for.
   * @param <T>            the type of the class modeled.
   * @return the created proxy instance for the class.
   * @throws AerogelException if an exception occurs when defining and loading the class.
   */
  @SuppressWarnings("unchecked")
  public static <T> @NotNull T makeProxy(@NotNull Class<T> interfaceClass) {
    // get the type of the interface class
    Type interType = Type.getType(interfaceClass);
    // make a proxy name for the class
    String proxyName = String.format(
      PROXY_NAME_FORMAT,
      interType.getInternalName(),
      System.nanoTime());

    MethodVisitor mv;
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    // target Java 8 classes as the minimum requirement - add both super interfaces we need
    cw.visit(V1_8, AsmUtils.PUBLIC_FINAL | ACC_SUPER, proxyName, null, AsmUtils.OBJECT, new String[]{
      INJECTION_TIME_NAME,
      interType.getInternalName()
    });

    // add the field holding the delegate object later
    cw.visitField(ACC_PRIVATE, "delegate", AsmUtils.OBJECT_DESC, null, null).visitEnd();
    // add the field holding the information if the delegate was set already
    cw.visitField(ACC_PRIVATE, "knowsDelegate", Type.BOOLEAN_TYPE.getDescriptor(), null, null).visitEnd();

    // write the constructor
    mv = AsmUtils.beginConstructor(cw, "()V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // visit the setDelegate method
    mv = cw.visitMethod(ACC_PUBLIC, "setDelegate", SET_DELEGATE_DESC, null, null);
    mv.visitCode();
    // assign the given value to the field
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, proxyName, "delegate", AsmUtils.OBJECT_DESC);
    // assign the knowsDelegate field to true
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_1);
    mv.visitFieldInsn(PUTFIELD, proxyName, "knowsDelegate", Type.BOOLEAN_TYPE.getDescriptor());
    // finish the method
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // visit a special ensureCallable method which ensures that we actually can call methods to the delegate
    mv = cw.visitMethod(ACC_PRIVATE, "ensureCallable", "()V", null, null);
    mv.visitCode();
    // first ensure that knowsDelegate is true
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, "knowsDelegate", Type.BOOLEAN_TYPE.getDescriptor());
    mv.visitLdcInsn("Using a proxy before the delegate instance is available is not possible");
    mv.visitMethodInsn(INVOKESTATIC, PRECONDITIONS_NAME, "checkArgument", CHECK_ARGUMENT_DESC, false);
    // then ensure that the delegate instance is not null
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, "delegate", AsmUtils.OBJECT_DESC);
    mv.visitLdcInsn("The injection of the proxied class evaluated in null - this proxy is not usable");
    mv.visitMethodInsn(INVOKESTATIC, OBJECTS_NAME, "requireNonNull", ENSURE_NOT_NULL_DESC, false);
    // finish the method
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // visit the standard methods from the object class: toString, equals & hashCode first
    visitMethod(cw, EQUALS, proxyName, Object.class, interType);
    visitMethod(cw, TO_STRING, proxyName, Object.class, interType);
    visitMethod(cw, HASH_CODE, proxyName, Object.class, interType);

    // visit the method & methods of the super interfaces of the class
    visitAllMethods(cw, interfaceClass, proxyName, interType, new HashSet<>());

    // finish the class write & define
    cw.visitEnd();
    Class<?> definedClass = ClassDefiners.getDefiner().defineClass(proxyName, interfaceClass, cw.toByteArray());

    // construct an instance of the class
    try {
      Constructor<?> ctx = definedClass.getDeclaredConstructor();
      ctx.setAccessible(true);

      return (T) ctx.newInstance();
    } catch (ReflectiveOperationException exception) {
      throw AerogelException.forException(exception);
    }
  }

  /**
   * Visits the methods of the given interface class, and recursively those of the super interfaces as well.
   *
   * @param cw             the class write of the current class.
   * @param interfaceClass the interface class to visit the methods and super interfaces of.
   * @param proxyName      the name of the proxy class we are creating.
   * @param interType      the internal type of the interface class for which the proxy gets created.
   * @param seenMethods    the signatures of all methods which were already implemented.
   * @since 2.0
   */
  private static void visitAllMethods(
    @NotNull ClassVisitor cw,
    @NotNull Class<?> interfaceClass,
    @NotNull String proxyName,
    @NotNull Type interType,
    @NotNull Set<String> seenMethods
  ) {
    // visit the methods of the interface
    visitMethods(cw, interfaceClass, proxyName, interType, seenMethods);

    // visit each non-final method of the class and delegate it to the reference downstream when available
    Class<?>[] implementingInterfaces = interfaceClass.getInterfaces();
    for (Class<?> clazz : implementingInterfaces) {
      // visit the methods of the given class as well
      visitAllMethods(cw, clazz, proxyName, interType, seenMethods);
    }
  }

  /**
   * Visits all methods of the given class and implements them using the given class writer. Methods which were already
   * implemented or are ignored will be skipped.
   *
   * @param cw          the class write of the current class.
   * @param clazz       the class to visit the methods of.
   * @param proxyName   the name of the proxy class we are creating.
   * @param interType   the internal type of the interface class for which the proxy gets created.
   * @param seenMethods the signatures of all methods which were already implemented.
   * @since 2.0
   */
  private static void visitMethods(
    @NotNull ClassVisitor cw,
    @NotNull Class<?> clazz,
    @NotNull String proxyName,
    @NotNull Type interType,
    @NotNull Set<String> seenMethods
  ) {
    for (Method method : clazz.getDeclaredMethods()) {
      // skip every static method - we can't override them
      if (Modifier.isStatic(method.getModifiers())) {
        continue;
      }

      // visit & implement the method if not already seen
      String signature = String.format("%s%s", method.getName(), Type.getMethodDescriptor(method));
      if (!IGNORED_METHODS.contains(signature) && seenMethods.add(signature)) {
        visitMethod(cw, method, proxyName, clazz, interType);
      }
    }
  }

  /**
   * Visits the given method and generates a delegating call to the constructed instance.
   *
   * @param cw        the class write of the current class.
   * @param method    the method for which a proxy method should be created.
   * @param proxyName the name of the proxy class we are creating.
   * @param declaring the declaring class of the method.
   * @param interType the internal type of the interface class for which the proxy gets created.
   */
  private static void visitMethod(
    @NotNull ClassVisitor cw,
    @NotNull Method method,
    @NotNull String proxyName,
    @NotNull Class<?> declaring,
    @NotNull Type interType
  ) {
    // strip off all invalid modifiers like abstract or native
    int mod = Modifier.isPublic(method.getModifiers()) ? ACC_PUBLIC : ACC_PROTECTED;
    // store the method descriptor - we need it to invoke the downstream class
    String methodDesc = Type.getMethodDescriptor(method);
    // visit the method and copy each parameter of it
    MethodVisitor mv = cw.visitMethod(mod, method.getName(), methodDesc, null, null);
    mv.visitCode();
    // first check if we are able to call the method on the delegate object
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, proxyName, "ensureCallable", "()V", false);
    // load the delegate object to the stack
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, "delegate", AsmUtils.OBJECT_DESC);
    mv.visitTypeInsn(CHECKCAST, interType.getInternalName());
    // load each argument from the stack - some arguments are bigger than other - keep track of the reader index
    int ri = 1;
    for (Class<?> type : method.getParameterTypes()) {
      // ensure that the correct opcode is used to load a primitive type
      if (type.isPrimitive()) {
        ri += AsmPrimitives.load(type, mv, ri);
      } else {
        mv.visitVarInsn(ALOAD, ri++);
      }
    }
    // invoke the method
    mv.visitMethodInsn(
      declaring.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL,
      Type.getInternalName(declaring),
      method.getName(),
      methodDesc,
      declaring.isInterface());
    // ensure that we use the correct return type
    mv.visitInsn(AsmUtils.returnOpCode(method.getReturnType()));
    // finish the method
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Represents a proxyable object in the runtime which can receive a delegate object at a later point.
   *
   * @author Pasqual K.
   * @since 1.0
   */
  @FunctionalInterface
  public interface InjectionTimeProxied {

    /**
     * Sets the delegate instance for the current proxy.
     *
     * @param delegate the delegate instance to use for the current proxy.
     */
    void setDelegate(@Nullable Object delegate);
  }
}
