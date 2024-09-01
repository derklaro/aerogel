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

import dev.derklaro.aerogel.binding.UninstalledBinding;
import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MemberInjectorTest {

  @Test
  void testMemberInjectionRespectsOrder() throws AnnotationFormatException {
    Injector injector = Injector.newInjector();
    for (char c = 'A'; c <= 'D'; c++) {
      Named named = TypeFactory.annotation(Named.class, Collections.singletonMap("value", "test" + c));
      UninstalledBinding<String> binding = injector.createBindingBuilder()
        .bind(String.class)
        .qualifiedWith(named)
        .toInstance(String.valueOf(c));
      injector.installBinding(binding);
    }

    MemberInjectableClass instance = new MemberInjectableClass();
    MemberInjector<MemberInjectableClass> memberInjector = injector.memberInjector(MemberInjectableClass.class);
    Assertions.assertSame(injector, memberInjector.injector());
    Assertions.assertEquals(MemberInjectableClass.class, memberInjector.target());
    Assertions.assertDoesNotThrow(() -> memberInjector.injectMembers(instance));

    // order is as follows:
    //  - static before instance
    //  - ordered by Order annotation
    //  - ordered by name
    Assertions.assertEquals("A", MemberInjectableClass.fieldTestA);
    Assertions.assertEquals("B", MemberInjectableClass.fieldTestB);
    Assertions.assertEquals("C", instance.fieldTestC);
    Assertions.assertEquals("D", instance.fieldTestD);

    Assertions.assertEquals(0, MemberInjectableClass.methodTestB);
    Assertions.assertEquals(1, MemberInjectableClass.methodTestA);
    Assertions.assertEquals(2, instance.methodTestD);
    Assertions.assertEquals(3, instance.methodTestC);
    Assertions.assertEquals(4, instance.methodTestF);
    Assertions.assertEquals(5, instance.methodTestE);
  }

  // @formatter:off
  public static class MemberInjectableClass {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    @Inject @Named("testB") public static String fieldTestB;
    public static int methodTestA;
    public static int methodTestB;
    @Inject @Named("testA") private static String fieldTestA;
    @Inject @Named("testD") protected String fieldTestD;
    @Inject @Named("testC") String fieldTestC;
    private int methodTestC;
    private int methodTestD;
    private int methodTestE;
    private int methodTestF;
    @Inject @Order(100) private static void methodA() { methodTestA = COUNTER.getAndIncrement(); }
    @Inject @Order(0) public static void methodB() { methodTestB = COUNTER.getAndIncrement(); }
    @Inject @Order(100) protected void methodC() { this.methodTestC = COUNTER.getAndIncrement(); }
    @Inject @Order(-100) void methodD() { this.methodTestD = COUNTER.getAndIncrement(); }
    @Inject public void methodE() { this.methodTestE = COUNTER.getAndIncrement(); }
    @Inject private void aMethodF() { this.methodTestF = COUNTER.getAndIncrement(); }
  }
  // @formatter:on
}
