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

package aerogel.internal.codegen;

import static aerogel.internal.asm.AsmUtils.CONSTRUCTOR_NAME;
import static aerogel.internal.asm.AsmUtils.INSTANCE_MAKER;
import static aerogel.internal.asm.AsmUtils.OBJECT;
import static aerogel.internal.asm.AsmUtils.OBJECT_DESC;
import static aerogel.internal.asm.AsmUtils.PRIVATE_FINAL;
import static aerogel.internal.asm.AsmUtils.PUBLIC_FINAL;
import static aerogel.internal.asm.AsmUtils.TYPES;
import static aerogel.internal.asm.AsmUtils.TYPES_DEC;
import static aerogel.internal.asm.AsmUtils.beginConstructor;
import static aerogel.internal.asm.AsmUtils.consDesc;
import static aerogel.internal.asm.AsmUtils.descToMethodDesc;
import static aerogel.internal.asm.AsmUtils.intName;
import static aerogel.internal.asm.AsmUtils.methodDesc;
import static aerogel.internal.asm.AsmUtils.pushInt;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import aerogel.Element;
import aerogel.InjectionContext;
import aerogel.Provider;
import aerogel.internal.asm.AsmPrimitives;
import aerogel.internal.jakarta.JakartaBridge;
import aerogel.internal.reflect.ReflectionUtils;
import aerogel.internal.unsafe.ClassDefiners;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public final class ClassInstanceMaker {

  // the holder for singleton instances
  static final String HOLDER = "holder";
  static final String HOLDER_DESC = org.objectweb.asm.Type.getDescriptor(AtomicReference.class);
  static final String HOLDER_NAME = org.objectweb.asm.Type.getInternalName(AtomicReference.class);
  // the injection context
  static final String FIND_INSTANCE = "findInstance";
  static final String FIND_INSTANCE_DESC = methodDesc(Object.class, Element.class);
  static final String INJ_CONTEXT_DESC = org.objectweb.asm.Type.getDescriptor(InjectionContext.class);
  static final String INJ_CONTEXT_NAME = org.objectweb.asm.Type.getInternalName(InjectionContext.class);
  // the provider wrapping stuff
  static final String OF_PROV_DESC = methodDesc(Provider.class, Object.class);
  static final String PROV_NAME = org.objectweb.asm.Type.getInternalName(Provider.class);
  static final String JAKARTA_BRIDGE = org.objectweb.asm.Type.getInternalName(JakartaBridge.class);
  static final String PROV_JAKARTA_DESC = methodDesc(jakarta.inject.Provider.class, Provider.class);
  // other stuff
  static final Type[] NO_TYPE = new Type[0];
  static final String GET_INSTANCE = "getInstance";
  static final String PROXY_CLASS_NAME_FORMAT = "%s$Invoker_%d";
  static final String ELEMENT_DESC = org.objectweb.asm.Type.getInternalName(Element.class);

  private ClassInstanceMaker() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull InstanceMaker forConstructor(@NotNull Constructor<?> target, boolean singleton) {
    // extract the wrapping class of the constructor
    Class<?> ct = target.getDeclaringClass();
    // the types used for the class init
    Type[] types;
    // make a proxy name for the class
    String proxyName = String.format(
      PROXY_CLASS_NAME_FORMAT,
      org.objectweb.asm.Type.getType(ct).getInternalName(),
      System.nanoTime());

    MethodVisitor mv;
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    // target Java 8 classes as the minimum requirement
    cw.visit(V1_8, PUBLIC_FINAL | ACC_SUPER, proxyName, null, OBJECT, INSTANCE_MAKER);

    // adds the type[] fields to the class
    cw.visitField(PRIVATE_FINAL, TYPES, TYPES_DEC, null, null).visitEnd();
    // if this is a singleton add the atomic reference field which will hold that instance later
    if (singleton) {
      cw.visitField(PRIVATE_FINAL, HOLDER, HOLDER_DESC, null, null).visitEnd();
    }

    // visit the constructor
    mv = beginConstructor(cw, descToMethodDesc(TYPES_DEC, void.class));
    // assign the type field
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, proxyName, TYPES, TYPES_DEC);
    // assign the singleton AtomicReference field if this is a singleton
    if (singleton) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitTypeInsn(NEW, HOLDER_NAME);
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, HOLDER_NAME, CONSTRUCTOR_NAME, "()V", false);
      mv.visitFieldInsn(PUTFIELD, proxyName, HOLDER, HOLDER_DESC);
    }
    // finish the constructor write
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // visit the getInstance() method
    mv = cw.visitMethod(ACC_PUBLIC, GET_INSTANCE, descToMethodDesc(INJ_CONTEXT_DESC, Object.class), null, null);
    mv.visitCode();
    // if this is a singleton check first if the instance is already loaded
    if (singleton) {
      visitSingletonHolder(mv, proxyName);
    }
    // check if the constructor does take arguments (if not that makes the life easier)
    if (target.getParameterCount() == 0) {
      // construct the class
      mv.visitTypeInsn(NEW, intName(ct));
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, intName(ct), CONSTRUCTOR_NAME, "()V", false);
      // if this is a singleton store the value in the AtomicReference
      if (singleton) {
        appendSingletonWrite(mv, proxyName);
      }
      // no types for the class init are required
      types = NO_TYPE;
    } else {
      // store all parameters to the stack
      types = storeParameters(target, proxyName, mv, singleton);
      // begin the instance creation
      mv.visitTypeInsn(NEW, intName(ct));
      mv.visitInsn(DUP);
      // load all elements from the stack
      loadParameters(types, mv);
      // instantiate the constructor with the parameters
      mv.visitMethodInsn(INVOKESPECIAL, intName(ct), CONSTRUCTOR_NAME, consDesc(target), false);
      // if this is a singleton store the value in the AtomicReference
      if (singleton) {
        appendSingletonWrite(mv, proxyName);
      }
    }
    // return the created value
    mv.visitInsn(ARETURN);
    // finish the class
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // finish & define the class
    cw.visitEnd();
    // construct
    return defineAndConstruct(cw, proxyName, ct, types);
  }

  static @NotNull Type unpackParameter(
    @NotNull String ot,
    @NotNull MethodVisitor mv,
    @NotNull Parameter parameter,
    @NotNull AtomicInteger typeWriterIndex,
    int index
  ) {
    // collect general information about the parameter we want to load
    String name = JakartaBridge.nameOf(parameter);
    // if the type is wrapped in a provider
    boolean provider = JakartaBridge.isProvider(parameter.getType());
    boolean jakartaProvider = JakartaBridge.needsProviderWrapping(parameter.getType());
    // the type of the parameter is important as we do need to consider either to push the real type of the super type later
    Type generic;
    Class<?> type;
    if (provider) {
      generic = ReflectionUtils.genericSuperType(parameter.getParameterizedType());
      type = ReflectionUtils.genericSuperTypeAsClass(parameter.getParameterizedType());
    } else {
      // just use the type of the parameter
      type = parameter.getType();
      generic = parameter.getParameterizedType();
    }

    // load the injection context to the stack
    mv.visitVarInsn(ALOAD, 1);
    // visit the name of the parameter
    if (name == null) {
      mv.visitInsn(ACONST_NULL);
    } else {
      mv.visitLdcInsn(name);
    }
    // read the type from the class intern array
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, ot, TYPES, TYPES_DEC);
    pushInt(mv, index);
    mv.visitInsn(AALOAD);
    // convert to an element
    mv.visitMethodInsn(INVOKESTATIC, ELEMENT_DESC, "named", methodDesc(Element.class, String.class, Type.class), true);
    // get its value from the injection context
    mv.visitMethodInsn(INVOKEINTERFACE, INJ_CONTEXT_NAME, FIND_INSTANCE, FIND_INSTANCE_DESC, true);
    // unwrap the primitive type if needed
    if (type.isPrimitive()) {
      typeWriterIndex.addAndGet(AsmPrimitives.storeUnbox(type, mv, 2 + typeWriterIndex.get()));
    } else {
      mv.visitTypeInsn(CHECKCAST, intName(type));
    }
    // wrap into a provider if needed
    if (provider) {
      // wrap the type into a provider
      mv.visitMethodInsn(INVOKESTATIC, PROV_NAME, "of", OF_PROV_DESC, true);
      // a jakarta provider needs special wrapping
      if (jakartaProvider) {
        mv.visitMethodInsn(INVOKESTATIC, JAKARTA_BRIDGE, "bridgeJakartaProvider", PROV_JAKARTA_DESC, false);
      }
    }
    if (!type.isPrimitive()) {
      // store relative to the index of the parameter
      mv.visitVarInsn(ASTORE, 2 + typeWriterIndex.getAndIncrement());
    }
    // return the extracted generic type
    return generic;
  }

  static @NotNull Type[] storeParameters(
    @NotNull Executable exec,
    @NotNull String name,
    @NotNull MethodVisitor mv,
    boolean singleton
  ) {
    // create an element for each parameter of the constructor
    Parameter[] parameters = exec.getParameters();
    // init the types directly while unboxing the parameters
    Type[] types = new Type[parameters.length];
    // stores the current writer index as some types need more space on the stack
    AtomicInteger writerIndex = new AtomicInteger(0);
    for (int i = 0; i < parameters.length; i++) {
      types[i] = unpackParameter(name, mv, parameters[i], writerIndex, i);
      // add a check if the singleton instance was created as a side effect after each parameter
      if (singleton) {
        visitSingletonHolder(mv, name);
      }
    }
    // return the types for later re-use
    return types;
  }

  static void loadParameters(@NotNull Type[] types, @NotNull MethodVisitor mv) {
    int readerIndex = 0;
    for (Type type : types) {
      // primitive types need to get loaded in another way from the stack than objects
      if (ReflectionUtils.isPrimitive(type)) {
        readerIndex += AsmPrimitives.load((Class<?>) type, mv, 2 + readerIndex);
      } else {
        mv.visitVarInsn(ALOAD, 2 + readerIndex++);
      }
    }
  }

  static @NotNull InstanceMaker defineAndConstruct(
    @NotNull ClassWriter cw,
    @NotNull String name,
    @NotNull Class<?> parent,
    @NotNull Type[] types
  ) {
    Class<?> defined = ClassDefiners.getDefiner().defineClass(name, parent, cw.toByteArray());
    // instantiate the class
    try {
      Constructor<?> ctx = defined.getDeclaredConstructor(Type[].class);
      ctx.setAccessible(true);

      return (InstanceMaker) ctx.newInstance((Object) types);
    } catch (ReflectiveOperationException exception) {
      throw new RuntimeException(exception);
    }
  }

  private static void appendSingletonWrite(@NotNull MethodVisitor mv, @NotNull String proxyName) {
    // temp store the previous return value
    mv.visitVarInsn(ASTORE, 2);
    // load the reference field to the stack
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, HOLDER, HOLDER_DESC);
    // load the constructed value
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(DUP2);
    // set the value in the reference
    mv.visitMethodInsn(INVOKEVIRTUAL, HOLDER_NAME, "set", descToMethodDesc(OBJECT_DESC, void.class), false);
  }

  private static void visitSingletonHolder(@NotNull MethodVisitor mv, @NotNull String proxyName) {
    Label nonNullDimension = new Label();
    // check if the singleton value is already there
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, HOLDER, HOLDER_DESC);
    mv.visitMethodInsn(INVOKEVIRTUAL, HOLDER_NAME, "get", "()" + OBJECT_DESC, false);
    mv.visitInsn(DUP);
    // if (value != null) then
    mv.visitJumpInsn(IFNULL, nonNullDimension);
    mv.visitInsn(ARETURN);
    mv.visitLabel(nonNullDimension);
  }
}
