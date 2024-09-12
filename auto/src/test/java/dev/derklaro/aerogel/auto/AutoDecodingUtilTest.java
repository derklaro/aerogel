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

package dev.derklaro.aerogel.auto;

import dev.derklaro.aerogel.auto.internal.util.AutoDecodingUtil;
import io.leangen.geantyref.TypeFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AutoDecodingUtilTest {

  static DataInput serializeData(DataOutputWriter writer) throws IOException {
    try (
      ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
      DataOutputStream dataOutput = new DataOutputStream(dataStream)
    ) {
      writer.writeData(dataOutput);
      dataOutput.flush();

      byte[] serializedData = dataStream.toByteArray();
      return new DataInputStream(new ByteArrayInputStream(serializedData));
    }
  }

  @Test
  void testManualTypeLoading() throws IOException {
    Class<?>[] expectedTypes = new Class<?>[]{
      int.class,
      int[].class,
      String.class,
      String[].class,
      MethodHandles.Lookup.class,
      MethodHandles.Lookup[][][].class,
    };
    DataInput serializedTypeInput = serializeData(out -> {
      out.writeInt(6);
      out.writeUTF("int");
      out.writeUTF("[I");
      out.writeUTF("java.lang.String");
      out.writeUTF("[Ljava.lang.String;");
      out.writeUTF("java.lang.invoke.MethodHandles$Lookup");
      out.writeUTF("[[[Ljava.lang.invoke.MethodHandles$Lookup;");
    });

    ClassLoader cl = AutoDecodingUtilTest.class.getClassLoader();
    Assertions.assertArrayEquals(expectedTypes, AutoDecodingUtil.decodeTypes(serializedTypeInput, cl));
  }

  @Test
  void testPrimitiveTypeLoading() throws IOException {
    List<Class<?>> primitiveTypes = Arrays.asList(
      int.class,
      char.class,
      byte.class,
      long.class,
      float.class,
      short.class,
      double.class,
      boolean.class);
    for (Class<?> primitiveType : primitiveTypes) {
      ClassLoader cl = AutoDecodingUtilTest.class.getClassLoader();
      DataInput serializedTypeInput = serializeData(out -> out.writeUTF(primitiveType.getName()));
      Assertions.assertSame(primitiveType, AutoDecodingUtil.decodeType(serializedTypeInput, cl));
    }
  }

  @Test
  void testPrimitiveArrayTypeLoading() throws IOException {
    List<Class<?>> primitiveTypes = Arrays.asList(
      int.class,
      char.class,
      byte.class,
      long.class,
      float.class,
      short.class,
      double.class,
      boolean.class);
    for (Class<?> primitiveType : primitiveTypes) {
      ClassLoader cl = AutoDecodingUtilTest.class.getClassLoader();
      {
        Class<?> arrayType = (Class<?>) TypeFactory.arrayOf(primitiveType);
        DataInput serializedTypeInput = serializeData(out -> out.writeUTF(arrayType.getName()));
        Assertions.assertSame(arrayType, AutoDecodingUtil.decodeType(serializedTypeInput, cl));
      }
      {
        Class<?> arrayType = (Class<?>) TypeFactory.arrayOf(TypeFactory.arrayOf(primitiveType));
        DataInput serializedTypeInput = serializeData(out -> out.writeUTF(arrayType.getName()));
        Assertions.assertSame(arrayType, AutoDecodingUtil.decodeType(serializedTypeInput, cl));
      }
    }
  }

  @Test
  void testReferenceTypeLoading() throws IOException {
    List<Class<?>> referenceTypes = Arrays.asList(String.class, List.class, Math.class, DataOutputWriter.class);
    for (Class<?> referenceType : referenceTypes) {
      ClassLoader cl = AutoDecodingUtilTest.class.getClassLoader();
      DataInput serializedTypeInput = serializeData(out -> out.writeUTF(referenceType.getName()));
      Assertions.assertSame(referenceType, AutoDecodingUtil.decodeType(serializedTypeInput, cl));
    }
  }

  @Test
  void testReferenceArrayTypeLoading() throws IOException {
    List<Class<?>> referenceTypes = Arrays.asList(String.class, List.class, Math.class, DataOutputWriter.class);
    for (Class<?> referenceType : referenceTypes) {
      ClassLoader cl = AutoDecodingUtilTest.class.getClassLoader();
      {
        Class<?> arrayType = (Class<?>) TypeFactory.arrayOf(referenceType);
        DataInput serializedTypeInput = serializeData(out -> out.writeUTF(arrayType.getName()));
        Assertions.assertSame(arrayType, AutoDecodingUtil.decodeType(serializedTypeInput, cl));
      }
      {
        Class<?> arrayType = (Class<?>) TypeFactory.arrayOf(TypeFactory.arrayOf(referenceType));
        DataInput serializedTypeInput = serializeData(out -> out.writeUTF(arrayType.getName()));
        Assertions.assertSame(arrayType, AutoDecodingUtil.decodeType(serializedTypeInput, cl));
      }
    }
  }

  @Test
  void testThrowsIllegalStateExceptionOnUnknownType() throws IOException {
    ClassLoader cl = AutoDecodingUtilTest.class.getClassLoader();
    DataInput serializedTypeInput = serializeData(out -> out.writeUTF("testing.SomeNonExistentType"));
    IllegalStateException thrown = Assertions.assertThrows(
      IllegalStateException.class,
      () -> AutoDecodingUtil.decodeType(serializedTypeInput, cl));
    Assertions.assertTrue(thrown.getMessage().contains("Unable to locate class testing.SomeNonExistentType"));
  }

  @FunctionalInterface
  interface DataOutputWriter {

    void writeData(DataOutput out) throws IOException;
  }
}
