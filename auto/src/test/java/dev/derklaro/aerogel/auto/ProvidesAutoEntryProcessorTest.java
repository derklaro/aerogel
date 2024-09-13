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
import com.squareup.javapoet.TypeSpec;
import dev.derklaro.aerogel.auto.annotation.Provides;
import dev.derklaro.aerogel.auto.util.CompilationUtil;
import dev.derklaro.aerogel.auto.util.TestJavaClassBuilder;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProvidesAutoEntryProcessorTest {

  @Test
  void testProvidesEntriesEmittedSuccessfully() throws IOException {
    AnnotationSpec providesAnnotation = AnnotationSpec.builder(Provides.class)
      .addMember(
        "value",
        "{$T.class, $T.class, $T.class, $T.class}",
        long.class, char[].class, String.class, String[][][].class)
      .build();
    TypeSpec.Builder providesClassBuilder = TypeSpec.classBuilder("ProvidesTest").addAnnotation(providesAnnotation);
    JavaFileObject sourceObject = TestJavaClassBuilder.from(providesClassBuilder).build("some.testing.pkg");

    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor();
    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.SUCCESS, compileResult.status());

    Optional<JavaFileObject> bindings = compileResult.generatedFile(StandardLocation.CLASS_OUTPUT, "auto-config.aero");
    Assertions.assertTrue(bindings.isPresent());
    try (DataInputStream dataInput = new DataInputStream(bindings.get().openInputStream())) {
      Assertions.assertTrue(dataInput.available() > 0);
      Assertions.assertEquals(Provides.CODEC_ID, dataInput.readUTF());
      Assertions.assertEquals("some.testing.pkg.ProvidesTest", dataInput.readUTF());
      Assertions.assertEquals(4, dataInput.readInt());
      Assertions.assertEquals("long", dataInput.readUTF());
      Assertions.assertEquals("[C", dataInput.readUTF());
      Assertions.assertEquals("java.lang.String", dataInput.readUTF());
      Assertions.assertEquals("[[[Ljava.lang.String;", dataInput.readUTF());
      Assertions.assertEquals(0, dataInput.available());
    }
  }

  @Test
  void testThrowsExceptionIfProvidedTypesIsEmpty() {
    AnnotationSpec providesAnnotation = AnnotationSpec.builder(Provides.class).addMember("value", "{}").build();
    TypeSpec.Builder providesClassBuilder = TypeSpec.classBuilder("ProvidesTest").addAnnotation(providesAnnotation);
    JavaFileObject sourceObject = TestJavaClassBuilder.from(providesClassBuilder).build("some.testing.pkg");

    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor();
    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.FAILURE, compileResult.status());

    List<? extends Diagnostic<? extends JavaFileObject>> errors = compileResult.errors();
    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(Diagnostic.Kind.ERROR, errors.get(0).getKind());
    Assertions.assertTrue(errors.get(0).getMessage(null).contains("Provided types in @Provides annotation is empty"));
  }

  @Test
  void testCanHandleProvideAnnotationWithSingleProvidedType() throws IOException {
    AnnotationSpec providesAnnotation = AnnotationSpec.builder(Provides.class)
      .addMember("value", "{$T.class}", long[][].class)
      .build();
    TypeSpec.Builder providesClassBuilder = TypeSpec.classBuilder("ProvidesTest").addAnnotation(providesAnnotation);
    JavaFileObject sourceObject = TestJavaClassBuilder.from(providesClassBuilder).build("some.testing.pkg");

    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor();
    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.SUCCESS, compileResult.status());

    Optional<JavaFileObject> bindings = compileResult.generatedFile(StandardLocation.CLASS_OUTPUT, "auto-config.aero");
    Assertions.assertTrue(bindings.isPresent());
    try (DataInputStream dataInput = new DataInputStream(bindings.get().openInputStream())) {
      Assertions.assertTrue(dataInput.available() > 0);
      Assertions.assertEquals(Provides.CODEC_ID, dataInput.readUTF());
      Assertions.assertEquals("some.testing.pkg.ProvidesTest", dataInput.readUTF());
      Assertions.assertEquals(1, dataInput.readInt());
      Assertions.assertEquals("[[J", dataInput.readUTF());
      Assertions.assertEquals(0, dataInput.available());
    }
  }
}
