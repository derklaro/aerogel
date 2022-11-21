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

package dev.derklaro.aerogel;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CircularDependencyTest {

  @Test
  void testCircularDependencyManagement() {
    Injector injector = Injector.newInjector();
    ApplicationMainClass mainClass = injector.instance(ApplicationMainClass.class);

    Assertions.assertNotNull(mainClass);
    Assertions.assertNotNull(mainClass.api1);
    Assertions.assertNotNull(mainClass.api1.main());
    Assertions.assertNotNull(mainClass.randomHolder);

    Assertions.assertNotSame(mainClass.api1, mainClass.api2);
    Assertions.assertNotSame(mainClass, mainClass.api1.main());
  }

  @Test
  void testProviderCircularDependencyManagement() {
    Injector injector = Injector.newInjector();
    ApplicationProviderMainClass mainClass = injector.instance(ApplicationProviderMainClass.class);

    Assertions.assertNotNull(mainClass);
    Assertions.assertNotNull(mainClass.apiProvider);
    Assertions.assertNotNull(mainClass.randomHolder);

    Assertions.assertNotSame(mainClass.apiProvider.get(), mainClass.apiProvider.get());
  }

  @ProvidedBy(ApplicationApiImpl.class)
  private interface ApplicationApi {

    default ApplicationMainClass main() {
      return null;
    }
  }

  private static class ApplicationMainClass {

    public final ApplicationApi api1;
    public final ApplicationApi api2;
    public final RandomHolder randomHolder;

    @Inject
    public ApplicationMainClass(ApplicationApi api1, ApplicationApi api2, RandomHolder randomHolder) {
      this.api1 = api1;
      this.api2 = api2;
      this.randomHolder = randomHolder;
    }
  }

  private static class ApplicationProviderMainClass {

    public final RandomHolder randomHolder;
    public final Provider<ApplicationApi> apiProvider;

    @Inject
    public ApplicationProviderMainClass(Provider<ApplicationApi> apiProvider, RandomHolder randomHolder) {
      this.randomHolder = randomHolder;
      this.apiProvider = apiProvider;
    }
  }

  private static final class RandomHolder {

    public final UUID uuid = UUID.randomUUID();
  }

  private static class ApplicationApiImpl implements ApplicationApi {

    public final ApplicationMainClass applicationMainClass;

    @Inject
    public ApplicationApiImpl(ApplicationMainClass applicationMainClass) {
      this.applicationMainClass = applicationMainClass;
    }

    @Override
    public ApplicationMainClass main() {
      return this.applicationMainClass;
    }
  }
}
