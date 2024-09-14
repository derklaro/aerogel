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
import com.squareup.javapoet.MethodSpec;
import dev.derklaro.aerogel.auto.annotation.Factory;
import dev.derklaro.aerogel.auto.util.CompilationUtil;
import dev.derklaro.aerogel.auto.util.TestJavaClassBuilder;
import io.leangen.geantyref.TypeFactory;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FactoryAutoEntryProcessorTest {

  @Test
  void testFactoryEntriesEmittedSuccessfully() throws IOException {
    Type listStringType = TypeFactory.parameterizedClass(List.class, String.class);
    Type arrayListStringType = TypeFactory.arrayOf(listStringType);
    MethodSpec.Builder factoryMethodBuilder = MethodSpec.methodBuilder("factoryTest")
      .returns(String.class)
      .addAnnotation(Factory.class)
      .addAnnotation(Deprecated.class)
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addParameter(String.class, "a")
      .addParameter(int.class, "b")
      .addParameter(long[][].class, "c")
      .addParameter(boolean[].class, "d")
      .addParameter(String[].class, "e")
      .addParameter(Map[][][].class, "f")
      .addParameter(listStringType, "g")
      .addParameter(arrayListStringType, "h", Modifier.FINAL)
      .addParameter(String[][][][][][].class, "i")
      .addException(IOException.class)
      .addCode("return $S;", "Hello, World!");
    JavaFileObject sourceObject = TestJavaClassBuilder.of("FactoryTest")
      .withMethod(factoryMethodBuilder)
      .build("some.testing.pkg");

    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor();
    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.SUCCESS, compileResult.status());

    Optional<JavaFileObject> bindings = compileResult.generatedFile(StandardLocation.CLASS_OUTPUT, "auto-config.aero");
    Assertions.assertTrue(bindings.isPresent());
    try (DataInputStream dataInput = new DataInputStream(bindings.get().openInputStream())) {
      Assertions.assertTrue(dataInput.available() > 0);
      Assertions.assertEquals(Factory.CODEC_ID, dataInput.readUTF());
      Assertions.assertEquals("factoryTest", dataInput.readUTF());
      Assertions.assertEquals("some.testing.pkg.FactoryTest", dataInput.readUTF());
      Assertions.assertEquals(9, dataInput.readInt());
      Assertions.assertEquals("java.lang.String", dataInput.readUTF());
      Assertions.assertEquals("int", dataInput.readUTF());
      Assertions.assertEquals("[[J", dataInput.readUTF());
      Assertions.assertEquals("[Z", dataInput.readUTF());
      Assertions.assertEquals("[Ljava.lang.String;", dataInput.readUTF());
      Assertions.assertEquals("[[[Ljava.util.Map;", dataInput.readUTF());
      Assertions.assertEquals("java.util.List", dataInput.readUTF());
      Assertions.assertEquals("[Ljava.util.List;", dataInput.readUTF());
      Assertions.assertEquals("[[[[[[Ljava.lang.String;", dataInput.readUTF());
      Assertions.assertEquals(0, dataInput.available());
    }
  }

  @Test
  void testThrowsExceptionWhenFactoryMethodReturnsVoid() {
    MethodSpec.Builder factoryMethodBuilder = MethodSpec.methodBuilder("factoryTest")
      .returns(void.class)
      .addAnnotation(Factory.class)
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addParameter(String.class, "a")
      .addParameter(int.class, "b");
    JavaFileObject sourceObject = TestJavaClassBuilder.of("FactoryTest")
      .withMethod(factoryMethodBuilder)
      .build("some.testing.pkg");

    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor();
    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.FAILURE, compileResult.status());

    List<? extends Diagnostic<? extends JavaFileObject>> errors = compileResult.errors();
    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(Diagnostic.Kind.ERROR, errors.get(0).getKind());
    Assertions.assertTrue(errors.get(0).getMessage(null).contains("@Factory method is returning void"));
  }

  @Test
  void testThrowsExceptionWhenFactoryIsNotStatic() {
    MethodSpec.Builder factoryMethodBuilder = MethodSpec.methodBuilder("factoryTest")
      .returns(String.class)
      .addAnnotation(Factory.class)
      .addModifiers(Modifier.PUBLIC)
      .addParameter(String.class, "a")
      .addParameter(int.class, "b")
      .addCode("return $S;", "Hello, World!");
    JavaFileObject sourceObject = TestJavaClassBuilder.of("FactoryTest")
      .withMethod(factoryMethodBuilder)
      .build("some.testing.pkg");

    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor();
    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.FAILURE, compileResult.status());

    List<? extends Diagnostic<? extends JavaFileObject>> errors = compileResult.errors();
    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(Diagnostic.Kind.ERROR, errors.get(0).getKind());
    Assertions.assertTrue(errors.get(0).getMessage(null).contains("@Factory method is not static"));
  }

  @Test
  void testFactoryMethodHandledWithNoArguments() throws IOException {
    MethodSpec.Builder factoryMethodBuilder = MethodSpec.methodBuilder("factoryTest")
      .returns(String.class)
      .addAnnotation(Factory.class)
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addCode("return $S;", "Hello, World!");
    JavaFileObject sourceObject = TestJavaClassBuilder.of("FactoryTest")
      .withMethod(factoryMethodBuilder)
      .build("some.testing.pkg");

    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor();
    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.SUCCESS, compileResult.status());

    Optional<JavaFileObject> bindings = compileResult.generatedFile(StandardLocation.CLASS_OUTPUT, "auto-config.aero");
    Assertions.assertTrue(bindings.isPresent());
    try (DataInputStream dataInput = new DataInputStream(bindings.get().openInputStream())) {
      Assertions.assertTrue(dataInput.available() > 0);
      Assertions.assertEquals(Factory.CODEC_ID, dataInput.readUTF());
      Assertions.assertEquals("factoryTest", dataInput.readUTF());
      Assertions.assertEquals("some.testing.pkg.FactoryTest", dataInput.readUTF());
      Assertions.assertEquals(0, dataInput.readInt());
      Assertions.assertEquals(0, dataInput.available());
    }
  }
}
