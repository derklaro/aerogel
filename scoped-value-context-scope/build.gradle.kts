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

description = "Extension for aerogel that uses scoped values for injection context scopes"

dependencies {
  api(projects.aerogel)
}

tasks.withType<Test> {
  jvmArgs("--enable-preview")
}

tasks.withType<JavaCompile> {
  sourceCompatibility = JavaVersion.VERSION_23.toString()
  targetCompatibility = JavaVersion.VERSION_23.toString()

  options.compilerArgs.add("-Xlint:-preview")
  options.compilerArgs.add("--enable-preview")
}

tasks.withType<Javadoc> {
  val options = options as? StandardJavadocDocletOptions ?: return@withType
  options.addStringOption("-release", "23")
  options.addBooleanOption("-enable-preview", true)
}

extensions.configure<JavaPluginExtension> {
  toolchain {
    vendor = JvmVendorSpec.AZUL
    languageVersion = JavaLanguageVersion.of(23)
  }
}

configurePublishing("java", true)
