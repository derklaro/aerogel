/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
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

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.reflect.TypeToken;
import com.google.testing.compile.Compilation;
import com.squareup.javapoet.ParameterSpec;
import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.auto.runtime.AutoAnnotationRegistry;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import lombok.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FactoryProcessingTest {

  private static final Type COLLECTION_STRING_TYPE = new TypeToken<Collection<String>>() {
  }.getType();
  private static final Type MAP_STRING_STRING_TYPE = new TypeToken<Map<String, String>>() {
  }.getType();
  private static final Type COLLECTION_STRING_ARRAY_TYPE = new TypeToken<Collection<String>[]>() {
  }.getType();

  @Test
  void testWarningEmitWhenFactoryMethodIsNotStatic() {
    JavaFileObject toCompile = TestJavaClassBuilder.of("Main")
      .visitMethod(methodBuilder("helloWorld").addAnnotation(Factory.class).addModifiers(PUBLIC))
      .build();

    Compilation compilation = CompilationUtil.javacWithProcessor().compile(toCompile);
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningCount(5);
    assertThat(compilation).hadWarningContaining("Factory method helloWorld must be static");
  }

  @Test
  void testWarningEmitWhenFactoryReturnsVoid() {
    JavaFileObject toCompile = TestJavaClassBuilder.of("Main")
      .visitMethod(methodBuilder("helloWorld").addAnnotation(Factory.class).addModifiers(PUBLIC, STATIC))
      .build();

    Compilation compilation = CompilationUtil.javacWithProcessor().compile(toCompile);
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningCount(5);
    assertThat(compilation)
      .hadWarningContaining("Factory method helloWorld returns void but an actual type is expected");
  }

  @Test
  void testSuccessfulEmitOfFactoryMethod() throws Throwable {
    JavaFileObject toCompile = TestJavaClassBuilder.of("Main")
      .visitMethod(methodBuilder("helloWorld")
        .returns(String.class)
        .addParameter(ParameterSpec.builder(COLLECTION_STRING_TYPE, "test").addAnnotation(NonNull.class).build())
        .addParameter(ParameterSpec.builder(String[].class, "array").addAnnotation(NonNull.class).build())
        .addParameter(MAP_STRING_STRING_TYPE, "map")
        .addParameter(COLLECTION_STRING_TYPE, "another", Modifier.FINAL)
        .addParameter(int.class, "i")
        .addParameter(int[][][].class, "intArray")
        .addParameter(COLLECTION_STRING_ARRAY_TYPE, "colArray")
        .addCode("return $S;", "Hello World")
        .addAnnotation(Factory.class)
        .addModifiers(PUBLIC, STATIC))
      .build("testing");

    Compilation compilation = CompilationUtil.javacWithProcessor().compile(toCompile);
    assertThat(compilation).hadWarningCount(4);
    assertThat(compilation).generatedFile(StandardLocation.CLASS_OUTPUT, "auto-config.aero");

    //noinspection OptionalGetWithoutIsPresent
    JavaFileObject file = compilation.generatedFile(StandardLocation.CLASS_OUTPUT, "auto-config.aero").get();
    try (InputStream in = file.openInputStream()) {
      // the main class was only there during compile time - we expect the exception, but we then know that emitting
      // of the factory method works as expected
      Exception thrown = Assertions.assertThrows(
        AerogelException.class,
        () -> AutoAnnotationRegistry.newRegistry().makeConstructors(ClassLoader.getSystemClassLoader(), in));
      // Ensures that:
      //   - the classes were found correctly
      //   - the parameters are in the correct order
      //   - parameterized type information was emitted
      //   - annotations were emitted
      //   - primitive types are correctly shown
      //   - the array flag is present on array types
      Assertions.assertTrue(thrown.getMessage().contains(
        "testing.Main.helloWorld(java.util.Collection, java.lang.String[], java.util.Map, java.util.Collection, int,"
          + " int[][][], java.util.Collection[])"));

      // ensure that the reason for the exception came because of the dynamically generated class
      ClassNotFoundException cause = Assertions.assertInstanceOf(ClassNotFoundException.class, thrown.getCause());
      Assertions.assertNull(cause.getException());
      Assertions.assertEquals("testing.Main", cause.getMessage());
    }
  }
}
