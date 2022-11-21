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

import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LSTORE;

import dev.derklaro.aerogel.internal.asm.primitive.DefaultPrimitiveEmitter;
import dev.derklaro.aerogel.internal.asm.primitive.PrimitiveEmitter;
import dev.derklaro.aerogel.internal.utility.MapUtil;
import java.util.Map;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * A utility class to work with primitive types during code generation.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal")
public final class AsmPrimitives {

  /**
   * A mapping of the primitive types to an emitter which can push the type to the operator stack.
   */
  public static final Map<Type, PrimitiveEmitter> EMITTER = MapUtil.staticMap(8, map -> {
    map.put(Type.BOOLEAN_TYPE, new DefaultPrimitiveEmitter(Boolean.class, boolean.class, ILOAD, ISTORE));
    map.put(Type.CHAR_TYPE, new DefaultPrimitiveEmitter(Character.class, char.class, ILOAD, ISTORE));
    map.put(Type.BYTE_TYPE, new DefaultPrimitiveEmitter(Byte.class, byte.class, ILOAD, ISTORE));
    map.put(Type.SHORT_TYPE, new DefaultPrimitiveEmitter(Short.class, short.class, ILOAD, ISTORE));
    map.put(Type.INT_TYPE, new DefaultPrimitiveEmitter(Integer.class, int.class, ILOAD, ISTORE));
    map.put(Type.FLOAT_TYPE, new DefaultPrimitiveEmitter(Float.class, float.class, FLOAD, FSTORE));
    map.put(Type.LONG_TYPE, new DefaultPrimitiveEmitter(Long.class, long.class, LLOAD, LSTORE));
    map.put(Type.DOUBLE_TYPE, new DefaultPrimitiveEmitter(Double.class, double.class, DLOAD, DSTORE));
  });

  private AsmPrimitives() {
    throw new UnsupportedOperationException();
  }

  /**
   * Stores a boxed version of the given class.
   *
   * @param clazz         the type which should get boxed and stored to the stack.
   * @param methodVisitor the method visitor which requested the boxing and storing.
   * @param index         the index to store the value on the operand stack.
   * @return the size of the primitive type.
   */
  public static int storeBox(@NotNull Class<?> clazz, @NotNull MethodVisitor methodVisitor, int index) {
    Type type = Type.getType(clazz);
    PrimitiveEmitter primitiveEmitter = EMITTER.get(type);

    if (primitiveEmitter != null) {
      primitiveEmitter.box(methodVisitor);
      primitiveEmitter.storeToStack(methodVisitor, index);
    }

    return type.getSize();
  }

  /**
   * Pushes a box of the given type to the operand stack.
   *
   * @param clazz         the type which should get boxed.
   * @param methodVisitor the method visitor which requested the boxing.
   */
  public static void pushBox(@NotNull Class<?> clazz, @NotNull MethodVisitor methodVisitor) {
    PrimitiveEmitter primitiveEmitter = EMITTER.get(Type.getType(clazz));
    if (primitiveEmitter != null) {
      primitiveEmitter.box(methodVisitor);
    }
  }

  /**
   * Stores an unboxed version of the given class.
   *
   * @param clazz         the type which should get unboxed and stored to the stack.
   * @param methodVisitor the method visitor which requested the unboxed and storing.
   * @param index         the index to store the value on the operand stack.
   * @return the size of the primitive type.
   */
  public static int storeUnbox(@NotNull Class<?> clazz, @NotNull MethodVisitor methodVisitor, int index) {
    Type type = Type.getType(clazz);
    PrimitiveEmitter primitiveEmitter = EMITTER.get(type);

    if (primitiveEmitter != null) {
      primitiveEmitter.unbox(methodVisitor);
      primitiveEmitter.storeToStack(methodVisitor, index);
    }

    return type.getSize();
  }

  /**
   * Pushes an unboxed version of the given type to the operand stack.
   *
   * @param clazz         the type which should get unboxed.
   * @param methodVisitor the method visitor which requested the unboxing.
   */
  public static void pushUnbox(@NotNull Class<?> clazz, @NotNull MethodVisitor methodVisitor) {
    PrimitiveEmitter primitiveEmitter = EMITTER.get(Type.getType(clazz));
    if (primitiveEmitter != null) {
      primitiveEmitter.unbox(methodVisitor);
    }
  }

  /**
   * Loads the given type to the stack if primitive.
   *
   * @param clazz the primitive type which should get loaded.
   * @param mv    the method visitor which requested the loading.
   * @param index the index of the value to load from the operand stack.
   * @return the size of the loaded type.
   */
  public static int load(@NotNull Class<?> clazz, @NotNull MethodVisitor mv, int index) {
    Type type = Type.getType(clazz);
    PrimitiveEmitter primitiveEmitter = EMITTER.get(type);

    if (primitiveEmitter != null) {
      primitiveEmitter.loadFromStack(mv, index);
    }

    return type.getSize();
  }
}
