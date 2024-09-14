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

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dev.derklaro.aerogel.auto.annotation.Factory;
import dev.derklaro.aerogel.auto.annotation.Provides;
import dev.derklaro.aerogel.auto.util.CompilationUtil;
import dev.derklaro.aerogel.auto.util.TestJavaClassBuilder;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MixedAutoEntryProcessingTest {

  @Test
  void testMixedAutoEntryProcessing() throws IOException {
    MethodSpec.Builder factoryMethod1 = MethodSpec.methodBuilder("factoryTest1")
      .returns(String.class)
      .addAnnotation(Factory.class)
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addParameter(String.class, "a")
      .addParameter(int.class, "b")
      .addParameter(boolean[][].class, "c")
      .addParameter(String[][][].class, "d")
      .addCode("return $S;", "Hello, World!");
    MethodSpec.Builder factoryMethod2 = MethodSpec.methodBuilder("factoryTest2")
      .returns(MethodHandle.class)
      .addAnnotation(Factory.class)
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addParameter(long[].class, "a")
      .addCode("return null;");

    AnnotationSpec providesAnnotation1 = AnnotationSpec.builder(Provides.class)
      .addMember(
        "value",
        "{$T.class, $T.class, $T.class}",
        boolean.class, String.class, long[].class)
      .build();
    AnnotationSpec providesAnnotation2 = AnnotationSpec.builder(Provides.class)
      .addMember("value", "{$T.class}", MethodHandle[][].class)
      .build();

    TypeSpec.Builder type1 = TypeSpec.classBuilder("MixedTest1").addAnnotation(providesAnnotation1);
    TypeSpec.Builder type2 = TypeSpec.classBuilder("MixedTest2").addAnnotation(providesAnnotation2);

    JavaFileObject sourceObject1 = TestJavaClassBuilder.from(type1)
      .withMethod(factoryMethod1)
      .withMethod(factoryMethod2)
      .build("some.testing.pkg");
    JavaFileObject sourceObject2 = TestJavaClassBuilder.from(type2)
      .withMethod(factoryMethod2)
      .build("some.testing.pkg");

    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor();
    Compilation compileResult = compiler.compile(sourceObject1, sourceObject2);
    Assertions.assertEquals(Compilation.Status.SUCCESS, compileResult.status());

    // order in generated file can be random
    Optional<JavaFileObject> bindings = compileResult.generatedFile(StandardLocation.CLASS_OUTPUT, "auto-config.aero");
    Assertions.assertTrue(bindings.isPresent());
    try (DataInputStream dataInput = new DataInputStream(bindings.get().openInputStream())) {
      Assertions.assertTrue(dataInput.available() > 0);
      Map<String, Integer> encounteredAutoEntriesPerCodec = new HashMap<>();
      while (dataInput.available() > 0) {
        String codec = dataInput.readUTF();
        encounteredAutoEntriesPerCodec.merge(codec, 1, Integer::sum);
        switch (codec) {
          case Provides.CODEC_ID:
            String annotatedType = dataInput.readUTF();
            switch (annotatedType) {
              case "some.testing.pkg.MixedTest1":
                Assertions.assertEquals(3, dataInput.readInt());
                Assertions.assertEquals("boolean", dataInput.readUTF());
                Assertions.assertEquals("java.lang.String", dataInput.readUTF());
                Assertions.assertEquals("[J", dataInput.readUTF());
                break;
              case "some.testing.pkg.MixedTest2":
                Assertions.assertEquals(1, dataInput.readInt());
                Assertions.assertEquals("[[Ljava.lang.invoke.MethodHandle;", dataInput.readUTF());
                break;
              default:
                Assertions.fail("Unexpected provides class " + annotatedType);
                break;
            }
            break;
          case Factory.CODEC_ID:
            String methodName = dataInput.readUTF();
            String definingClassName = dataInput.readUTF();
            Assertions.assertTrue(
              definingClassName.equals("some.testing.pkg.MixedTest1")
                || definingClassName.equals("some.testing.pkg.MixedTest2"));
            switch (methodName) {
              case "factoryTest1":
                Assertions.assertEquals(4, dataInput.readInt());
                Assertions.assertEquals("java.lang.String", dataInput.readUTF());
                Assertions.assertEquals("int", dataInput.readUTF());
                Assertions.assertEquals("[[Z", dataInput.readUTF());
                Assertions.assertEquals("[[[Ljava.lang.String;", dataInput.readUTF());
                break;
              case "factoryTest2":
                Assertions.assertEquals(1, dataInput.readInt());
                Assertions.assertEquals("[J", dataInput.readUTF());
                break;
              default:
                Assertions.fail("Unexpected factory method " + methodName);
                break;
            }
            break;
          default:
            Assertions.fail("Unexpected codec: " + codec);
            break;
        }
      }

      for (Map.Entry<String, Integer> entry : encounteredAutoEntriesPerCodec.entrySet()) {
        switch (entry.getKey()) {
          case Provides.CODEC_ID:
            Assertions.assertEquals(2, entry.getValue());
            break;
          case Factory.CODEC_ID:
            Assertions.assertEquals(3, entry.getValue());
            break;
          default:
            Assertions.fail("Found codec whose encountering count is not validated: " + entry.getKey());
            break;
        }
      }
    }
  }
}
