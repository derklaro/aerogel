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

import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LSTORE;

import aerogel.internal.asm.primitive.DefaultPrimitiveEmitter;
import aerogel.internal.asm.primitive.PrimitiveEmitter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public final class AsmPrimitives {

  public static final Map<Type, PrimitiveEmitter> EMITTER = new ConcurrentHashMap<>();

  static {
    EMITTER.put(Type.BOOLEAN_TYPE, new DefaultPrimitiveEmitter(Boolean.class, boolean.class, ILOAD, ISTORE));
    EMITTER.put(Type.CHAR_TYPE, new DefaultPrimitiveEmitter(Character.class, char.class, ILOAD, ISTORE));
    EMITTER.put(Type.BYTE_TYPE, new DefaultPrimitiveEmitter(Byte.class, byte.class, ILOAD, ISTORE));
    EMITTER.put(Type.SHORT_TYPE, new DefaultPrimitiveEmitter(Short.class, short.class, ILOAD, ISTORE));
    EMITTER.put(Type.INT_TYPE, new DefaultPrimitiveEmitter(Integer.class, int.class, ILOAD, ISTORE));
    EMITTER.put(Type.FLOAT_TYPE, new DefaultPrimitiveEmitter(Float.class, float.class, FLOAD, FSTORE));
    EMITTER.put(Type.LONG_TYPE, new DefaultPrimitiveEmitter(Long.class, long.class, LLOAD, LSTORE));
    EMITTER.put(Type.DOUBLE_TYPE, new DefaultPrimitiveEmitter(Double.class, double.class, DLOAD, DSTORE));
  }

  private AsmPrimitives() {
    throw new UnsupportedOperationException();
  }

  public static int storeBox(@NotNull Class<?> clazz, @NotNull MethodVisitor methodVisitor, int index) {
    Type type = Type.getType(clazz);
    PrimitiveEmitter primitiveEmitter = EMITTER.get(type);

    if (primitiveEmitter != null) {
      primitiveEmitter.box(methodVisitor);
      primitiveEmitter.storeToStack(methodVisitor, index);
    }

    return type.getSize();
  }

  public static void pushBox(@NotNull Class<?> clazz, @NotNull MethodVisitor methodVisitor) {
    PrimitiveEmitter primitiveEmitter = EMITTER.get(Type.getType(clazz));
    if (primitiveEmitter != null) {
      primitiveEmitter.box(methodVisitor);
    }
  }

  public static int storeUnbox(@NotNull Class<?> clazz, @NotNull MethodVisitor methodVisitor, int index) {
    Type type = Type.getType(clazz);
    PrimitiveEmitter primitiveEmitter = EMITTER.get(type);

    if (primitiveEmitter != null) {
      primitiveEmitter.unbox(methodVisitor);
      primitiveEmitter.storeToStack(methodVisitor, index);
    }

    return type.getSize();
  }

  public static void pushUnbox(@NotNull Class<?> clazz, @NotNull MethodVisitor methodVisitor) {
    PrimitiveEmitter primitiveEmitter = EMITTER.get(Type.getType(clazz));
    if (primitiveEmitter != null) {
      primitiveEmitter.unbox(methodVisitor);
    }
  }

  public static int load(@NotNull Class<?> clazz, @NotNull MethodVisitor mv, int index) {
    Type type = Type.getType(clazz);
    PrimitiveEmitter primitiveEmitter = EMITTER.get(type);

    if (primitiveEmitter != null) {
      primitiveEmitter.loadFromStack(mv, index);
    }

    return type.getSize();
  }
}
