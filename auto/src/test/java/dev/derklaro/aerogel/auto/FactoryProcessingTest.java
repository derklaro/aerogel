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

package dev.derklaro.aerogel.auto;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import dev.derklaro.aerogel.AerogelException;
import com.google.testing.compile.Compilation;
import java.io.InputStream;
import java.nio.file.Files;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FactoryProcessingTest {

  @Test
  void testWarningEmitWhenFactoryMethodIsNotStatic() {
    JavaFileObject toCompile = TestJavaClassBuilder.of("Main")
      .visitMethod(methodBuilder("helloWorld").addAnnotation(Factory.class).addModifiers(PUBLIC))
      .build();

    Compilation compilation = CompilationUtils.javacWithProcessor().compile(toCompile);
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningCount(1);
    assertThat(compilation).hadWarningContaining("Factory method helloWorld must be static");
  }

  @Test
  void testWarningEmitWhenFactoryReturnsVoid() {
    JavaFileObject toCompile = TestJavaClassBuilder.of("Main")
      .visitMethod(methodBuilder("helloWorld").addAnnotation(Factory.class).addModifiers(PUBLIC, STATIC))
      .build();

    Compilation compilation = CompilationUtils.javacWithProcessor().compile(toCompile);
    assertThat(compilation).succeeded();
    assertThat(compilation).hadWarningCount(1);
    assertThat(compilation)
      .hadWarningContaining("Factory method helloWorld returns void but an actual type is expected");
  }

  @Test
  void testSuccessfulEmitOfFactoryMethod() throws Exception {
    JavaFileObject toCompile = TestJavaClassBuilder.of("Main")
      .visitMethod(methodBuilder("helloWorld")
        .returns(String.class)
        .addCode("return $S;", "Hello World")
        .addAnnotation(Factory.class)
        .addModifiers(PUBLIC, STATIC))
      .build();

    Compilation compilation = CompilationUtils.javacWithProcessor().compile(toCompile);
    assertThat(compilation).succeededWithoutWarnings();

    try (InputStream in = Files.newInputStream(CompilationUtils.outputFileOfProcessor(compilation))) {
      // the main class was only there during compile time - we expect the exception, but we then know that emitting
      // of the factory method works as expected
      Assertions.assertThrows(
        AerogelException.class,
        () -> AutoAnnotationRegistry.newInstance().makeConstructors(in));
    }
  }
}
