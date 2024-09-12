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
import dev.derklaro.aerogel.auto.util.CompilationUtil;
import dev.derklaro.aerogel.auto.util.TestJavaClassBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AutoEntryAnnotationProcessorTest {

  @Test
  void testEmptyAutoFileIsNotEmittedByDefault() {
    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor();
    JavaFileObject sourceObject = TestJavaClassBuilder.of("FactoryTest").build("some.testing.pkg");

    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.SUCCESS, compileResult.status());

    Optional<JavaFileObject> bindings = compileResult.generatedFile(StandardLocation.CLASS_OUTPUT, "auto-config.aero");
    Assertions.assertFalse(bindings.isPresent());
  }

  @Test
  void testEmptyAutoFileIsEmittedWithExplicitOptionEnabled() throws IOException {
    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor()
      .withOptions("-AaerogelEmitAutoFileIfEmpty=true");
    JavaFileObject sourceObject = TestJavaClassBuilder.of("FactoryTest").build("some.testing.pkg");

    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.SUCCESS, compileResult.status());

    Optional<JavaFileObject> bindings = compileResult.generatedFile(StandardLocation.CLASS_OUTPUT, "auto-config.aero");
    Assertions.assertTrue(bindings.isPresent());
    try (InputStream bindingsFileStream = bindings.get().openInputStream()) {
      Assertions.assertEquals(0, bindingsFileStream.available());
    }
  }

  @Test
  void testEmptyFileIsEmittedIfEnabledToSpecifiedOutputPath() throws IOException {
    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor().withOptions(
      "-AaerogelAutoFileName=testing.aero",
      "-AaerogelEmitAutoFileIfEmpty=true");
    JavaFileObject sourceObject = TestJavaClassBuilder.of("FactoryTest").build("some.testing.pkg");

    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.SUCCESS, compileResult.status());

    Optional<JavaFileObject> sb = compileResult.generatedFile(StandardLocation.CLASS_OUTPUT, "auto-config.aero");
    Assertions.assertFalse(sb.isPresent());

    Optional<JavaFileObject> bindings = compileResult.generatedFile(StandardLocation.CLASS_OUTPUT, "testing.aero");
    Assertions.assertTrue(bindings.isPresent());
    try (InputStream bindingsFileStream = bindings.get().openInputStream()) {
      Assertions.assertEquals(0, bindingsFileStream.available());
    }
  }

  @Test
  void testNotesArePrintedForLoadedProcessors() {
    Compiler compiler = CompilationUtil.javacCompilerWithAerogelProcessor();
    JavaFileObject sourceObject = TestJavaClassBuilder.of("FactoryTest").build("some.testing.pkg");

    Compilation compileResult = compiler.compile(sourceObject);
    Assertions.assertEquals(Compilation.Status.SUCCESS, compileResult.status());

    List<? extends Diagnostic<? extends JavaFileObject>> notes = compileResult.notes();
    Assertions.assertEquals(2, notes.size());
    Assertions.assertTrue(notes.stream().anyMatch(note -> {
      String message = note.getMessage(null);
      return message.contains("Loaded aerogel auto processor")
        && message.contains("@dev.derklaro.aerogel.auto.annotation.Factory")
        && message.contains("dev.derklaro.aerogel.auto.processing.internal.factory.FactoryAutoEntryProcessorFactory");
    }));
    Assertions.assertTrue(notes.stream().anyMatch(note -> {
      String message = note.getMessage(null);
      return message.contains("Loaded aerogel auto processor")
        && message.contains("@dev.derklaro.aerogel.auto.annotation.Provides")
        && message.contains("dev.derklaro.aerogel.auto.processing.internal.provides.ProvidesAutoEntryProcessorFactory");
    }));
  }
}
