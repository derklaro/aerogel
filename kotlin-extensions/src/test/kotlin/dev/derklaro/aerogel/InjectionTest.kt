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

package dev.derklaro.aerogel

import dev.derklaro.aerogel.binding.BindingBuilder
import dev.derklaro.aerogel.kotlin.element
import dev.derklaro.aerogel.kotlin.instance
import dev.derklaro.aerogel.util.Qualifiers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class InjectionTest {

  @Test
  fun `constructor injection`() {
    val injector = Injector.newInjector()
    injector.install(
      BindingBuilder.create()
        .bind(element<String>().requireAnnotation(Qualifiers.named("Hello World")))
        .toInstance("1234")
    )

    val depends = injector.instance<DependingOnTest>()
    Assertions.assertNotNull(depends)
    Assertions.assertEquals("1234", depends!!.theTestClass.theHelloWorldString)
  }

  @Test
  fun `constructor and member injection`() {
    val injector = Injector.newInjector()
    injector
      .install(
        BindingBuilder.create()
          .bind(element<String>().requireAnnotation(Qualifiers.named("Hello World")))
          .toInstance("1234")
      )
      .install(
        BindingBuilder.create()
          .bind(element<String>().requireAnnotation(Qualifiers.named("Hello there")))
          .toInstance("12345")
      )

    val two = injector.instance<TestClass2>()
    Assertions.assertNotNull(two)
    Assertions.assertEquals("12345", two!!.theHelloThereString)

    Assertions.assertNotNull(two.aField)
    Assertions.assertEquals("1234", two.aField!!.theHelloWorldString)
  }

  @Test
  fun `can even inject into val member`() {
    val injector = Injector.newInjector()
    injector
      .install(
        BindingBuilder.create()
          .bind(element<String>().requireAnnotation(Qualifiers.named("Hello World")))
          .toInstance("1234")
      )
      .install(
        BindingBuilder.create()
          .bind(element<String>().requireAnnotation(Qualifiers.named("Hello there")))
          .toInstance("12345")
      )

    Assertions.assertDoesNotThrow {
      injector.instance<TestClass3>()
    }
  }
}

data class DependingOnTest @Inject constructor(val theTestClass: TestClass)

data class TestClass @Inject constructor(@Name("Hello World") val theHelloWorldString: String)

class TestClass2 @Inject constructor(@Name("Hello there") val theHelloThereString: String) {
  @Inject
  var aField: TestClass? = null
}

class TestClass3 @Inject constructor(@Name("Hello there") val theHelloThereString: String) {
  @Inject
  val aField2: TestClass? = null
}
