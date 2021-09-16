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

package aerogel.internal.asm;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public final class AsmUtils {

  public static final String CONSTRUCTOR_NAME = "<init>";
  public static final String STATIC_INITIALIZER_NAME = "<clinit>";

  public static final int PUBLIC_FINAL = ACC_PUBLIC | ACC_FINAL;
  public static final int PRIVATE_FINAL = ACC_PRIVATE | ACC_FINAL;

  public static final String OBJECT = Type.getInternalName(Object.class);
  public static final String OBJECT_DESC = Type.getDescriptor(Object.class);

  private AsmUtils() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull String descToMethodDesc(@NotNull String desc, @NotNull Class<?> rValue) {
    return String.format("(%s)%s", desc, Type.getDescriptor(rValue));
  }

  public static @NotNull String intName(@NotNull Class<?> clazz) {
    return Type.getInternalName(clazz);
  }

  public static @NotNull String consDesc(@NotNull Constructor<?> constructor) {
    return Type.getConstructorDescriptor(constructor);
  }

  public static void pushInt(@NotNull MethodVisitor mv, int value) {
    if (value < -1) {
      mv.visitLdcInsn(value);
    } else if (value <= 5) {
      mv.visitInsn(ICONST_0 + value);
    } else if (value <= Byte.MAX_VALUE) {
      mv.visitIntInsn(BIPUSH, value);
    } else if (value <= Short.MAX_VALUE) {
      mv.visitIntInsn(SIPUSH, value);
    } else {
      mv.visitLdcInsn(value);
    }
  }

  public static @NotNull String methodDesc(@NotNull Class<?> rType, @NotNull Class<?>... arguments) {
    StringBuilder builder = new StringBuilder("(");
    for (Class<?> argument : arguments) {
      builder.append(Type.getDescriptor(argument));
    }
    return builder.append(')').append(Type.getDescriptor(rType)).toString();
  }

  public static @NotNull String methodDesc(@NotNull Method method) {
    return Type.getMethodDescriptor(method);
  }

  public static int returnOpCode(@NotNull Class<?> rt) {
    return Type.getType(rt).getOpcode(IRETURN);
  }

  public static @NotNull MethodVisitor beginConstructor(@NotNull ClassVisitor cw, @NotNull String desc) {
    // add the constructor to the class
    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, CONSTRUCTOR_NAME, desc, null, null);
    mv.visitCode();
    // super() call
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, OBJECT, CONSTRUCTOR_NAME, "()V", false);
    // only the common code - more may be needed
    return mv;
  }

  public static void dumpClassWriter(@NotNull Path target, @NotNull ClassWriter classWriter) {
    try {
      Files.write(target, classWriter.toByteArray());
    } catch (IOException ignored) {
    }
  }
}
