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

package dev.derklaro.aerogel.internal.asm.primitive;

import dev.derklaro.aerogel.internal.asm.AsmUtils;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A functional emitter which can be used for all primitive types.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal.asm")
public final class DefaultPrimitiveEmitter implements PrimitiveEmitter {

  private final String wrapperClass;
  private final String boxingDescriptor;
  private final String unboxingMethod;
  private final String unboxingDescriptor;

  private final int readOpCode;
  private final int storeOpCode;

  /**
   * Constructs a new default primitive emitter instance.
   *
   * @param box       the type of the boxed primitive.
   * @param primitive the primitive type.
   * @param rop       the load opcode.
   * @param sop       the store opcode.
   */
  public DefaultPrimitiveEmitter(Class<?> box, Class<?> primitive, int rop, int sop) {
    this.wrapperClass = Type.getInternalName(box);
    this.boxingDescriptor = AsmUtils.methodDesc(box, primitive);
    this.unboxingMethod = String.format("%sValue", primitive.getSimpleName());
    this.unboxingDescriptor = AsmUtils.methodDesc(primitive);
    this.readOpCode = rop;
    this.storeOpCode = sop;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void box(@NotNull MethodVisitor mv) {
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, this.wrapperClass, "valueOf", this.boxingDescriptor, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unbox(@NotNull MethodVisitor mv) {
    mv.visitTypeInsn(Opcodes.CHECKCAST, this.wrapperClass);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, this.wrapperClass, this.unboxingMethod, this.unboxingDescriptor, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void storeToStack(@NotNull MethodVisitor mv, int stackIndex) {
    mv.visitVarInsn(this.storeOpCode, stackIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loadFromStack(@NotNull MethodVisitor mv, int stackIndex) {
    mv.visitVarInsn(this.readOpCode, stackIndex);
  }
}
