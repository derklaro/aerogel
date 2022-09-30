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

package dev.derklaro.aerogel.internal.asm;

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

/**
 * A utility class for working with the asm library.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class AsmUtils {

  /**
   * The name of a constructor method.
   */
  public static final String CONSTRUCTOR_NAME = "<init>";
  /**
   * The name of the static initializer method.
   */
  public static final String STATIC_INITIALIZER_NAME = "<clinit>";

  /**
   * Accessor combination of public and final.
   */
  public static final int PUBLIC_FINAL = ACC_PUBLIC | ACC_FINAL;
  /**
   * Accessor combination of private and final.
   */
  public static final int PRIVATE_FINAL = ACC_PRIVATE | ACC_FINAL;

  /**
   * The internal name of the object class.
   */
  public static final String OBJECT = Type.getInternalName(Object.class);
  /**
   * The descriptor of the object class.
   */
  public static final String OBJECT_DESC = Type.getDescriptor(Object.class);

  private AsmUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Converts the given {@code desc} into a method descriptor by appending the return value to it.
   *
   * @param desc   the descriptor to convert.
   * @param rValue the return value of the method.
   * @return the method descriptor based on the given {@code desc} and {@code rValue}.
   */
  public static @NotNull String descToMethodDesc(@NotNull String desc, @NotNull Class<?> rValue) {
    return String.format("(%s)%s", desc, Type.getDescriptor(rValue));
  }

  /**
   * A shortcut method to get the internal name of a class. The internal name of a class is its fully qualified name, as
   * returned by Class.getName(), where {@code .} is replaced with {@code /}.
   *
   * @param clazz the clazz to get the internal name of.
   * @return the internal name of the given {@code clazz}.
   */
  public static @NotNull String intName(@NotNull Class<?> clazz) {
    return Type.getInternalName(clazz);
  }

  /**
   * A shortcut method to get the descriptor of a constructor.
   *
   * @param constructor the constructor to get the descriptor of.
   * @return the descriptor of the given {@code constructor}.
   */
  public static @NotNull String consDesc(@NotNull Constructor<?> constructor) {
    return Type.getConstructorDescriptor(constructor);
  }

  /**
   * Pushes an int to the stack choosing the best opcode for that.
   *
   * @param mv    the current method visitor onto which the int should get pushed.
   * @param value the int value to push onto the stack.
   */
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

  /**
   * Creates a method descriptor based on the given return type and the argument types.
   *
   * @param rType     the return type of the method.
   * @param arguments all argument types of the method.
   * @return the created method description based on the given arguments.
   */
  public static @NotNull String methodDesc(@NotNull Class<?> rType, @NotNull Class<?>... arguments) {
    StringBuilder builder = new StringBuilder("(");
    for (Class<?> argument : arguments) {
      builder.append(Type.getDescriptor(argument));
    }
    return builder.append(')').append(Type.getDescriptor(rType)).toString();
  }

  /**
   * A shortcut method to get the descriptor of a method.
   *
   * @param method the method to get the descriptor of.
   * @return the descriptor of the given {@code method}.
   */
  public static @NotNull String methodDesc(@NotNull Method method) {
    return Type.getMethodDescriptor(method);
  }

  /**
   * Gets the opcode to return the value of the given type.
   *
   * @param rt the type to get the return opcode for.
   * @return the opcode to return the given type.
   */
  public static int returnOpCode(@NotNull Class<?> rt) {
    return Type.getType(rt).getOpcode(IRETURN);
  }

  /**
   * Begins a method visitor in the given class for a constructor with the given descriptor.
   *
   * @param cw   the class visitor to write the constructor in.
   * @param desc the descriptor of the newly created constructor.
   * @return the method visitor to continue the constructor generation.
   */
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

  /**
   * Dumps the given class writer into a file at the given {@code target} path.
   *
   * @param target      the path of the file to write the class write to.
   * @param classWriter the class writer to dump into the file.
   */
  public static void dumpClassWriter(@NotNull Path target, @NotNull ClassWriter classWriter) {
    try {
      Files.write(target, classWriter.toByteArray());
    } catch (IOException ignored) {
    }
  }
}
