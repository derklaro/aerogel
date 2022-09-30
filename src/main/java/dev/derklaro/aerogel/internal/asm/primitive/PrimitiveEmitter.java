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

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

/**
 * An emitter to store, load, box and unbox primitive types.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public interface PrimitiveEmitter {

  /**
   * Boxes the current top of the operand stack.
   *
   * @param mv the method visitor which requested the boxing.
   */
  void box(@NotNull MethodVisitor mv);

  /**
   * Unboxes the current top of the operand stack.
   *
   * @param mv the method visitor which requested the unboxing.
   */
  void unbox(@NotNull MethodVisitor mv);

  /**
   * Stores the current value to the operand stack at the given index using the appropriate opcode.
   *
   * @param mv         the method visitor which requested the storing.
   * @param stackIndex the index to store the value on the operand stack at.
   */
  void storeToStack(@NotNull MethodVisitor mv, int stackIndex);

  /**
   * Loads the value to the operand stack at the given index using the appropriate opcode.
   *
   * @param mv         the method visitor which requested the loading.
   * @param stackIndex the index to load the value on the operand stack from.
   */
  void loadFromStack(@NotNull MethodVisitor mv, int stackIndex);
}
