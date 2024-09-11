/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2024 Pasqual K. and contributors
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

package dev.derklaro.aerogel.auto.processing.internal.util;

import java.util.EnumMap;
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class AutoTypeEncoder {

  private static final EnumMap<TypeKind, PrimitiveTypeDescriptor> PRIMITIVE_TYPE_DESCRIPTORS;

  static {
    PRIMITIVE_TYPE_DESCRIPTORS = new EnumMap<>(TypeKind.class);
    PRIMITIVE_TYPE_DESCRIPTORS.put(TypeKind.INT, new PrimitiveTypeDescriptor("int", "I"));
    PRIMITIVE_TYPE_DESCRIPTORS.put(TypeKind.CHAR, new PrimitiveTypeDescriptor("char", "C"));
    PRIMITIVE_TYPE_DESCRIPTORS.put(TypeKind.LONG, new PrimitiveTypeDescriptor("long", "J"));
    PRIMITIVE_TYPE_DESCRIPTORS.put(TypeKind.BYTE, new PrimitiveTypeDescriptor("byte", "B"));
    PRIMITIVE_TYPE_DESCRIPTORS.put(TypeKind.SHORT, new PrimitiveTypeDescriptor("short", "S"));
    PRIMITIVE_TYPE_DESCRIPTORS.put(TypeKind.FLOAT, new PrimitiveTypeDescriptor("float", "F"));
    PRIMITIVE_TYPE_DESCRIPTORS.put(TypeKind.DOUBLE, new PrimitiveTypeDescriptor("double", "D"));
    PRIMITIVE_TYPE_DESCRIPTORS.put(TypeKind.BOOLEAN, new PrimitiveTypeDescriptor("boolean", "Z"));
  }

  private final Types typeUtil;
  private final Elements elementUtil;

  public AutoTypeEncoder(@NotNull Types typeUtil, @NotNull Elements elementUtil) {
    this.typeUtil = typeUtil;
    this.elementUtil = elementUtil;
  }

  @Contract(value = "_ -> new", pure = true)
  public static @NotNull AutoTypeEncoder forProcessingEnvironment(@NotNull ProcessingEnvironment environment) {
    return new AutoTypeEncoder(environment.getTypeUtils(), environment.getElementUtils());
  }

  public @NotNull String getBinaryName(@NotNull TypeMirror type) {
    StringBuilder typeBuilder = new StringBuilder();
    TypeMirror erasedType = this.typeUtil.erasure(type);
    this.appendBinaryName(erasedType, typeBuilder, false);
    return typeBuilder.toString();
  }

  private void appendBinaryName(
    @NotNull TypeMirror type,
    @NotNull StringBuilder typeBuilder,
    boolean inArray
  ) {
    if (type instanceof ArrayType) {
      ArrayType arrayType = (ArrayType) type;
      TypeMirror componentType = arrayType.getComponentType();
      this.appendBinaryName(componentType, typeBuilder.append('['), true);
    } else if (type instanceof PrimitiveType) {
      PrimitiveTypeDescriptor primitiveTypeDesc = PRIMITIVE_TYPE_DESCRIPTORS.get(type.getKind());
      Objects.requireNonNull(primitiveTypeDesc, "Unexpected primitive type: " + type.getKind());
      String typeName = inArray ? primitiveTypeDesc.internalName : primitiveTypeDesc.binaryName;
      typeBuilder.append(typeName);
    } else if (type instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) type;
      TypeElement typeElement = (TypeElement) declaredType.asElement();
      Name binaryName = this.elementUtil.getBinaryName(typeElement);
      if (binaryName.length() == 0) {
        throw new IllegalStateException("Encountered unsupported unnamed class");
      }

      if (inArray) {
        typeBuilder.append('L').append(binaryName).append(';');
      } else {
        typeBuilder.append(binaryName);
      }
    } else {
      throw new IllegalStateException("Encountered unexpected erased type: " + type.getKind());
    }
  }

  private static final class PrimitiveTypeDescriptor {

    private final String binaryName;
    private final String internalName;

    public PrimitiveTypeDescriptor(@NotNull String binaryName, @NotNull String internalName) {
      this.binaryName = binaryName;
      this.internalName = internalName;
    }
  }
}
