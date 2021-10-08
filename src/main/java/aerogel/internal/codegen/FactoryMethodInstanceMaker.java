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

import static aerogel.internal.asm.AsmUtils.OBJECT;
import static aerogel.internal.asm.AsmUtils.PUBLIC_FINAL;
import static aerogel.internal.asm.AsmUtils.descToMethodDesc;
import static aerogel.internal.asm.AsmUtils.intName;
import static aerogel.internal.asm.AsmUtils.methodDesc;
import static aerogel.internal.codegen.ClassInstanceMaker.GET_INSTANCE;
import static aerogel.internal.codegen.ClassInstanceMaker.INJ_CONTEXT_DESC;
import static aerogel.internal.codegen.ClassInstanceMaker.INSTANCE_MAKER;
import static aerogel.internal.codegen.ClassInstanceMaker.NO_ELEMENT;
import static aerogel.internal.codegen.ClassInstanceMaker.appendSingletonWrite;
import static aerogel.internal.codegen.ClassInstanceMaker.checkForConstructedValue;
import static aerogel.internal.codegen.ClassInstanceMaker.defineAndConstruct;
import static aerogel.internal.codegen.ClassInstanceMaker.loadParameters;
import static aerogel.internal.codegen.ClassInstanceMaker.storeParameters;
import static aerogel.internal.codegen.ClassInstanceMaker.writeConstructor;
import static aerogel.internal.codegen.ClassInstanceMaker.writeFields;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.V1_8;

import aerogel.Element;
import aerogel.internal.asm.AsmPrimitives;
import java.lang.reflect.Method;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

/**
 * An instance maker generator which creates instances using factory methods.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class FactoryMethodInstanceMaker {

  private static final String PROXY_CLASS_NAME_FORMAT = "%s$Invoker_%s_%d";

  private FactoryMethodInstanceMaker() {
    throw new UnsupportedOperationException();
  }

  /**
   * Makes an instance maker for the given factory method.
   *
   * @param self              the element type for which the instance maker gets created.
   * @param method            the method to create the instance maker for.
   * @param shouldBeSingleton if the resulting object should be a singleton.
   * @return the created instance maker for the factory method based construction.
   * @throws aerogel.AerogelException if an exception occurs when defining and loading the class.
   */
  public static @NotNull InstanceMaker forMethod(
    @NotNull Element self,
    @NotNull Method method,
    boolean shouldBeSingleton
  ) {
    // extract the wrapping class of the method
    Class<?> ct = method.getDeclaringClass();
    // the types used for the class init
    Element[] elements;
    // make a proxy name for the class
    String proxyName = String.format(
      PROXY_CLASS_NAME_FORMAT,
      org.objectweb.asm.Type.getType(ct).getInternalName(),
      method.getName(),
      System.nanoTime());

    MethodVisitor mv;
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    // target Java 8 classes as the minimum requirement
    cw.visit(V1_8, PUBLIC_FINAL | ACC_SUPER, proxyName, null, OBJECT, INSTANCE_MAKER);
    // writes all necessary fields to the class
    writeFields(cw, shouldBeSingleton);
    // write the constructor
    writeConstructor(cw, proxyName, shouldBeSingleton);

    // visit the getInstance() method
    mv = cw.visitMethod(ACC_PUBLIC, GET_INSTANCE, descToMethodDesc(INJ_CONTEXT_DESC, Object.class), null, null);
    mv.visitCode();
    // if this is a singleton check first if the instance is already loaded
    if (shouldBeSingleton) {
      checkForConstructedValue(mv, proxyName, true);
    }
    // check if the constructor does take arguments (if not that makes the life easier)
    if (method.getParameterCount() == 0) {
      // just call the method (which must always be a static method)
      mv.visitMethodInsn(INVOKESTATIC, intName(ct), method.getName(), methodDesc(method), ct.isInterface());
      // no types needed for the invocation
      elements = NO_ELEMENT;
    } else {
      // store all parameters to the stack
      elements = storeParameters(method, proxyName, mv, shouldBeSingleton);
      // load all parameters
      loadParameters(elements, mv);
      // invoke the method with these arguments
      mv.visitMethodInsn(INVOKESTATIC, intName(ct), method.getName(), methodDesc(method), ct.isInterface());
      // if this is a singleton store the value in the AtomicReference
      if (shouldBeSingleton) {
        appendSingletonWrite(mv, proxyName);
      }
    }
    // if the return type of the class is primitive we need to box it as the next instance takes care
    // of the primitive / non-primitive conversion and the method signature indicates an object return type
    if (method.getReturnType().isPrimitive()) {
      AsmPrimitives.pushBox(method.getReturnType(), mv);
    }
    // return the last value of the stack
    mv.visitInsn(ARETURN);
    // finish the method
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    cw.visitEnd();
    // construct
    return defineAndConstruct(cw, proxyName, ct, self, elements);
  }
}
