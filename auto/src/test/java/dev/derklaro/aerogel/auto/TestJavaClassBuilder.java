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

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.tools.JavaFileObject;

final class TestJavaClassBuilder {

  private final TypeSpec.Builder typeSpec;

  public TestJavaClassBuilder(TypeSpec.Builder typeSpec) {
    this.typeSpec = typeSpec;
  }

  public static TestJavaClassBuilder of(String className) {
    return new TestJavaClassBuilder(TypeSpec.classBuilder(className));
  }

  public static TestJavaClassBuilder from(TypeSpec.Builder typeSpec) {
    return new TestJavaClassBuilder(typeSpec);
  }

  public TestJavaClassBuilder visitField(FieldSpec.Builder spec) {
    this.typeSpec.addField(spec.build());
    return this;
  }

  public TestJavaClassBuilder visitMethod(MethodSpec.Builder spec) {
    this.typeSpec.addMethod(spec.build());
    return this;
  }

  public JavaFileObject build() {
    return build("");
  }

  public JavaFileObject build(String packageName) {
    return JavaFile.builder(packageName, this.typeSpec.build()).build().toJavaFileObject();
  }
}
