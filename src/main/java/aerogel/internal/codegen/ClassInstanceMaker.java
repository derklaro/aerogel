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
import static aerogel.internal.asm.AsmUtils.OBJECT;
import static aerogel.internal.asm.AsmUtils.OBJECT_DESC;
import static aerogel.internal.asm.AsmUtils.PRIVATE_FINAL;
import static aerogel.internal.asm.AsmUtils.PUBLIC_FINAL;
import static aerogel.internal.asm.AsmUtils.beginConstructor;
import static aerogel.internal.asm.AsmUtils.consDesc;
import static aerogel.internal.asm.AsmUtils.descToMethodDesc;
import static aerogel.internal.asm.AsmUtils.intName;
import static aerogel.internal.asm.AsmUtils.methodDesc;
import static aerogel.internal.asm.AsmUtils.pushInt;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import aerogel.AerogelException;
import aerogel.BindingHolder;
import aerogel.Element;
import aerogel.InjectionContext;
import aerogel.Injector;
import aerogel.Provider;
import aerogel.internal.asm.AsmPrimitives;
import aerogel.internal.jakarta.JakartaBridge;
import aerogel.internal.reflect.ReflectionUtils;
import aerogel.internal.unsafe.ClassDefiners;
import aerogel.internal.utility.ElementHelper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * An instance maker generator which creates instances using constructor injection.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class ClassInstanceMaker {

  // the super interface all instance makers must implement
  static final String[] INSTANCE_MAKER = new String[]{org.objectweb.asm.Type.getInternalName(InstanceMaker.class)};
  // the holder for singleton instances
  static final String HOLDER = "holder";
  static final String HOLDER_DESC = org.objectweb.asm.Type.getDescriptor(AtomicReference.class);
  static final String HOLDER_NAME = org.objectweb.asm.Type.getInternalName(AtomicReference.class);
  // the holder of the information if the singleton instance was ever set
  static final String INSTANCE_THERE = "wasInstanceConstructed";
  static final String INSTANCE_THERE_DESC = org.objectweb.asm.Type.getDescriptor(AtomicBoolean.class);
  static final String INSTANCE_THERE_NAME = org.objectweb.asm.Type.getInternalName(AtomicBoolean.class);
  // the injection context
  static final String INJECTOR = "injector";
  static final String FIND_INSTANCE = "findInstance";
  static final String INJECTOR_DESC = methodDesc(Injector.class);
  static final String FIND_INSTANCE_DESC = methodDesc(Object.class, Element.class);
  static final String INJ_CONTEXT_DESC = org.objectweb.asm.Type.getDescriptor(InjectionContext.class);
  static final String INJ_CONTEXT_NAME = org.objectweb.asm.Type.getInternalName(InjectionContext.class);
  // the injector
  static final String INJECTOR_BINDING = "binding";
  static final String INJECTOR_NAME = org.objectweb.asm.Type.getInternalName(Injector.class);
  static final String INJECTOR_BINDING_DESC = methodDesc(BindingHolder.class, Element.class);
  // the provider wrapping stuff
  static final String PROVIDER_NAME = org.objectweb.asm.Type.getInternalName(Provider.class);
  static final String JAKARTA_BRIDGE = org.objectweb.asm.Type.getInternalName(JakartaBridge.class);
  static final String PROV_JAKARTA_DESC = methodDesc(jakarta.inject.Provider.class, Provider.class);
  // the element access stuff
  static final String ELEMENTS = "elements";
  static final Element[] NO_ELEMENT = new Element[0];
  static final String ELEMENT_DESC = org.objectweb.asm.Type.getDescriptor(Element[].class);
  // other stuff
  static final String GET_INSTANCE = "getInstance";
  static final String PROXY_CLASS_NAME_FORMAT = "%s$Invoker_%d";

  private ClassInstanceMaker() {
    throw new UnsupportedOperationException();
  }

  /**
   * Makes an instance maker for the given constructor.
   *
   * @param target    the target constructor to use for injection.
   * @param singleton if the resulting object should be a singleton.
   * @return the created instance maker for the constructor injection.
   * @throws AerogelException if an exception occurs when defining and loading the class.
   */
  public static @NotNull InstanceMaker forConstructor(@NotNull Constructor<?> target, boolean singleton) {
    // extract the wrapping class of the constructor
    Class<?> ct = target.getDeclaringClass();
    // the types used for the class init
    Element[] elements;
    // make a proxy name for the class
    String proxyName = String.format(
      PROXY_CLASS_NAME_FORMAT,
      org.objectweb.asm.Type.getType(ct).getInternalName(),
      System.nanoTime());

    MethodVisitor mv;
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    // target Java 8 classes as the minimum requirement
    cw.visit(V1_8, PUBLIC_FINAL | ACC_SUPER, proxyName, null, OBJECT, INSTANCE_MAKER);
    // writes all necessary fields to the class
    writeFields(cw, singleton);
    // write the constructor to the class
    writeConstructor(cw, proxyName, singleton);

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
      elements = NO_ELEMENT;
    } else {
      // store all parameters to the stack
      elements = storeParameters(target, proxyName, mv, singleton);
      // begin the instance creation
      mv.visitTypeInsn(NEW, intName(ct));
      mv.visitInsn(DUP);
      // load all elements from the stack
      loadParameters(elements, mv);
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
    return defineAndConstruct(cw, proxyName, ct, elements);
  }

  /**
   * Stores all parameters to the current object stack.
   *
   * @param exec      the executable for which the parameters should get stored.
   * @param name      the name of the proxy which holds the fields.
   * @param mv        the method visitor of the currently visiting method.
   * @param singleton if the resulting object should only have one instance per injector.
   * @return the elements of the stored parameters.
   */
  static @NotNull Element[] storeParameters(
    @NotNull Executable exec,
    @NotNull String name,
    @NotNull MethodVisitor mv,
    boolean singleton
  ) {
    // create an element for each parameter of the constructor
    Parameter[] parameters = exec.getParameters();
    // init the types directly while unboxing the parameters
    Element[] elements = new Element[parameters.length];
    // stores the current writer index as some types need more space on the stack
    AtomicInteger writerIndex = new AtomicInteger(0);
    for (int i = 0; i < parameters.length; i++) {
      elements[i] = unpackParameter(name, mv, parameters[i], writerIndex, parameters[i].getDeclaredAnnotations(), i);
      // add a check if the singleton instance was created as a side effect after each parameter
      if (singleton) {
        visitSingletonHolder(mv, name);
      }
    }
    // return the types for later re-use
    return elements;
  }

  /**
   * Write the default fields to a newly created class.
   *
   * @param cw        the class writer of the class.
   * @param singleton if the resulting object should only have one instance per injector.
   */
  static void writeFields(@NotNull ClassWriter cw, boolean singleton) {
    // adds the type[] fields to the class
    cw.visitField(PRIVATE_FINAL, ELEMENTS, ELEMENT_DESC, null, null).visitEnd();
    // if this is a singleton add the atomic reference field which will hold that instance later
    if (singleton) {
      cw.visitField(PRIVATE_FINAL, HOLDER, HOLDER_DESC, null, null).visitEnd();
      cw.visitField(PRIVATE_FINAL, INSTANCE_THERE, INSTANCE_THERE_DESC, null, null).visitEnd();
    }
  }

  /**
   * Writes the constructor with the required {@code Element[]} parameter to the class.
   *
   * @param cw        the class writer of the constructor.
   * @param proxyName the name of the proxy we are creating.
   * @param singleton if the resulting object should only have one instance per injector.
   */
  static void writeConstructor(@NotNull ClassWriter cw, @NotNull String proxyName, boolean singleton) {
    // visit the constructor
    MethodVisitor mv = beginConstructor(cw, descToMethodDesc(ELEMENT_DESC, void.class));
    // assign the type field
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, proxyName, ELEMENTS, ELEMENT_DESC);
    // assign the singleton AtomicReference field if this is a singleton
    if (singleton) {
      // create a new instance of the singleton holder
      mv.visitVarInsn(ALOAD, 0);
      mv.visitTypeInsn(NEW, HOLDER_NAME);
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, HOLDER_NAME, CONSTRUCTOR_NAME, "()V", false);
      mv.visitFieldInsn(PUTFIELD, proxyName, HOLDER, HOLDER_DESC);
      // create a new instance of the holder if the singleton instance was constructed
      mv.visitVarInsn(ALOAD, 0);
      mv.visitTypeInsn(NEW, INSTANCE_THERE_NAME);
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, INSTANCE_THERE_NAME, CONSTRUCTOR_NAME, "()V", false);
      mv.visitFieldInsn(PUTFIELD, proxyName, INSTANCE_THERE, INSTANCE_THERE_DESC);
    }
    // finish the constructor write
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Loads all previously stored parameters back to the stack.
   *
   * @param types the elements to load.
   * @param mv    the method visitor of the currently visiting method.
   */
  static void loadParameters(@NotNull Element[] types, @NotNull MethodVisitor mv) {
    int readerIndex = 0;
    for (Element element : types) {
      // primitive types need to get loaded in another way from the stack than objects
      if (ReflectionUtils.isPrimitive(element.componentType())) {
        readerIndex += AsmPrimitives.load((Class<?>) element.componentType(), mv, 2 + readerIndex);
      } else {
        mv.visitVarInsn(ALOAD, 2 + readerIndex++);
      }
    }
  }

  /**
   * Defines and construct the generated class.
   *
   * @param cw       the class writer used for construction of the type.
   * @param name     the name of the constructed class.
   * @param parent   the parent class of the constructed class (as we are generating anonymous classes)
   * @param elements the elements of the parameters used for injection.
   * @return the instance of the newly created instance maker.
   * @throws AerogelException if an exception occurs when defining and loading the class.
   */
  static @NotNull InstanceMaker defineAndConstruct(
    @NotNull ClassWriter cw,
    @NotNull String name,
    @NotNull Class<?> parent,
    @NotNull Element[] elements
  ) {
    Class<?> defined = ClassDefiners.getDefiner().defineClass(name, parent, cw.toByteArray());
    // instantiate the class
    try {
      Constructor<?> ctx = defined.getDeclaredConstructor(Element[].class);
      ctx.setAccessible(true);

      return (InstanceMaker) ctx.newInstance((Object) elements);
    } catch (ReflectiveOperationException exception) {
      throw AerogelException.forException(exception);
    }
  }

  /**
   * Writes the current stack top element into the singleton AtomicReference stored in the class.
   *
   * @param mv        the method visitor of the current method.
   * @param proxyName the name of the proxy owning the singleton reference field.
   */
  static void appendSingletonWrite(@NotNull MethodVisitor mv, @NotNull String proxyName) {
    // temp store the previous return value
    mv.visitVarInsn(ASTORE, 2);
    // inform the singleton holder that a value was computed
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, INSTANCE_THERE, INSTANCE_THERE_DESC);
    // set it to true
    mv.visitInsn(ICONST_1);
    mv.visitMethodInsn(
      INVOKEVIRTUAL,
      INSTANCE_THERE_NAME,
      "set",
      descToMethodDesc(org.objectweb.asm.Type.BOOLEAN_TYPE.getDescriptor(), void.class),
      false);

    // load the reference field to the stack
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, HOLDER, HOLDER_DESC);
    // load the constructed value
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(DUP2);
    // set the value in the reference
    mv.visitMethodInsn(INVOKEVIRTUAL, HOLDER_NAME, "set", descToMethodDesc(OBJECT_DESC, void.class), false);
  }

  /**
   * Loads the previously constructed element to stack and returns it if the construction was done before.
   *
   * @param mv        the method visitor of the current method.
   * @param proxyName the name of the proxy owning the singleton reference field.
   */
  static void visitSingletonHolder(@NotNull MethodVisitor mv, @NotNull String proxyName) {
    Label wasConstructedDimension = new Label();
    // check if the value was already constructed
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, INSTANCE_THERE, INSTANCE_THERE_DESC);
    mv.visitMethodInsn(
      INVOKEVIRTUAL,
      INSTANCE_THERE_NAME,
      "get",
      "()" + org.objectweb.asm.Type.BOOLEAN_TYPE.getDescriptor(),
      false);
    // if (this.wasInstanceConstructed.get()) then
    mv.visitJumpInsn(IFEQ, wasConstructedDimension);
    // load the singleton holder to the stack
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, HOLDER, HOLDER_DESC);
    // load & return the singleton value stored in the holder
    mv.visitMethodInsn(INVOKEVIRTUAL, HOLDER_NAME, "get", "()" + OBJECT_DESC, false);
    mv.visitInsn(ARETURN);
    mv.visitLabel(wasConstructedDimension);
  }

  /**
   * Ensures that the correct element gets loaded and pushed to the stack.
   *
   * @param ot              the proxy which the generator is generating.
   * @param mv              the method visitor of the current method.
   * @param parameter       the parameter which should get unboxed.
   * @param typeWriterIndex the current writer index of the stack.
   * @param annotations     the annotations of the parameter.
   * @param index           the current index for loading the element in the runtime from the class element array.
   * @return the element which will be used for loading the instance from the injection context.
   */
  private static @NotNull Element unpackParameter(
    @NotNull String ot,
    @NotNull MethodVisitor mv,
    @NotNull Parameter parameter,
    @NotNull AtomicInteger typeWriterIndex,
    @NotNull Annotation[] annotations,
    int index
  ) {
    // collect general information about the parameter we want to load
    String name = JakartaBridge.nameOf(parameter);
    // if the type is wrapped in a provider
    boolean provider = JakartaBridge.isProvider(parameter.getType());
    boolean jakartaProvider = JakartaBridge.needsProviderWrapping(parameter.getType());
    // filter out all qualifier annotations
    Annotation[] qualifiedAnnotations = ElementHelper.extractQualifierAnnotations(annotations);
    // the type of the parameter is important as we do need to consider either to push the real type of the super type later
    Type generic;
    Class<?> type;
    if (provider) {
      generic = ReflectionUtils.genericSuperType(parameter.getParameterizedType());
      type = ReflectionUtils.rawType(ReflectionUtils.genericSuperType(parameter.getParameterizedType()));
    } else {
      // just use the type of the parameter
      type = parameter.getType();
      generic = parameter.getParameterizedType();
    }

    // load the injection context to the stack
    mv.visitVarInsn(ALOAD, 1);
    // if we need a Provider we do need to call the binding(Element) method in the injector class - load the injector
    if (provider) {
      mv.visitMethodInsn(INVOKEINTERFACE, INJ_CONTEXT_NAME, INJECTOR, INJECTOR_DESC, true);
    }
    // read the element from the class intern array
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, ot, ELEMENTS, ELEMENT_DESC);
    pushInt(mv, index);
    mv.visitInsn(AALOAD);
    // get its value from the injection context, or just the binding if a Provider is requested
    if (provider) {
      // get the binding from the injector
      mv.visitMethodInsn(INVOKEINTERFACE, INJECTOR_NAME, INJECTOR_BINDING, INJECTOR_BINDING_DESC, true);
      mv.visitTypeInsn(CHECKCAST, PROVIDER_NAME);
      // bridge to a jakarta provider if needed
      if (jakartaProvider) {
        mv.visitMethodInsn(INVOKESTATIC, JAKARTA_BRIDGE, "bridgeJakartaProvider", PROV_JAKARTA_DESC, false);
      }
    } else {
      mv.visitMethodInsn(INVOKEINTERFACE, INJ_CONTEXT_NAME, FIND_INSTANCE, FIND_INSTANCE_DESC, true);
      // cast to the required type if the type is not primitive (that will be handled by AsmPrimitives.storeUnbox)
      if (!type.isPrimitive()) {
        mv.visitTypeInsn(CHECKCAST, intName(type));
      }
    }
    // unwrap the primitive type if needed - a provider could cause a type mismatch here so ignore that check
    if (!provider && type.isPrimitive()) {
      typeWriterIndex.addAndGet(AsmPrimitives.storeUnbox(type, mv, 2 + typeWriterIndex.get()));
    } else {
      mv.visitVarInsn(ASTORE, 2 + typeWriterIndex.getAndIncrement());
    }
    // return the extracted generic type
    return Element.get(generic)
      .requireName(name)
      .requireAnnotations(qualifiedAnnotations);
  }
}
