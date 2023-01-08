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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.assertion.InjectAssert;
import dev.derklaro.aerogel.binding.BindingBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IndirectConstructionTest {

  private static int constructorCalls = 0;

  @Test
  void testIndirectConstructionRespectsStoredValue() {
    Injector injector = Injector.newInjector();
    injector.install(BindingBuilder.create().bind(SomeProvider.class).toConstructing(SomeProviderImpl.class));

    // get the provider instance & check how often the constructor was called
    SomeProviderImpl provider = (SomeProviderImpl) injector.instance(SomeProvider.class);
    Assertions.assertEquals(1, IndirectConstructionTest.constructorCalls);

    // ensure that the providers were injected
    Assertions.assertNotNull(provider.dependA);
    Assertions.assertNotNull(provider.dependB);

    // ensure that the provider is a singleton
    InjectAssert.assertSameProxySafe(provider, provider.dependA.provider);
    InjectAssert.assertSameProxySafe(provider, provider.dependB.provider);
  }

  private interface SomeProvider {

  }

  @Singleton
  private static final class SomeProviderImpl implements SomeProvider {

    private final SomeClassDependingOnProviderA dependA;
    private final SomeClassDependingOnProviderB dependB;

    @Inject
    public SomeProviderImpl(SomeClassDependingOnProviderA dependA, SomeClassDependingOnProviderB dependB) {
      this.dependA = dependA;
      this.dependB = dependB;

      // count the calls
      IndirectConstructionTest.constructorCalls++;
    }
  }

  @Singleton
  private static final class SomeClassDependingOnProviderA {

    private final SomeProvider provider;

    @Inject
    public SomeClassDependingOnProviderA(SomeProvider provider) {
      this.provider = provider;
    }
  }

  @Singleton
  private static final class SomeClassDependingOnProviderB {

    private final SomeProvider provider;

    @Inject
    public SomeClassDependingOnProviderB(SomeProvider provider) {
      this.provider = provider;
    }
  }
}
