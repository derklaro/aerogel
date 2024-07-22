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

package dev.derklaro.aerogel;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

public class SecondCircularDependencyTest {

  @Test
  void test() {
    Injector injector = Injector.newInjector();
    SomeService someService = injector.instance(SomeService.class);
  }

  @ProvidedBy(SomeProviderImpl.class)
  private interface SomeProvider {

    SomeService someService();
  }

  @ProvidedBy(SomeOtherProviderImpl.class)
  private interface SomeOtherProvider {

    SomeProvider someProvider();
  }

  private static final class SomeService {

    private final SomeProvider someProvider;
    private final SomeOtherService someOtherService;

    @Inject
    public SomeService(SomeProvider someProvider, SomeOtherService someOtherService) {
      this.someProvider = someProvider;
      this.someOtherService = someOtherService;
    }
  }

  private static final class SomeOtherService {

    private final SomeProvider someProvider;
    private final SomeOtherProvider someOtherProvider;

    @Inject
    public SomeOtherService(SomeProvider someProvider, SomeOtherProvider someOtherProvider) {
      this.someProvider = someProvider;
      this.someOtherProvider = someOtherProvider;
    }
  }

  private static final class SomeProviderImpl implements SomeProvider {

    private final SomeService someService;

    @Inject
    public SomeProviderImpl(SomeService someService) {
      this.someService = someService;
    }

    @Override
    public SomeService someService() {
      return this.someService;
    }
  }

  private static final class SomeOtherProviderImpl implements SomeOtherProvider {

    private final SomeProvider someProvider;

    @Inject
    public SomeOtherProviderImpl(SomeProvider someProvider) {
      this.someProvider = someProvider;
    }

    @Override
    public SomeProvider someProvider() {
      return this.someProvider;
    }
  }
}
