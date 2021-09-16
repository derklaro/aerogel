/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
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

package aerogel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CircularDependencyTest {

  @Test
  void testCircularDependencyManagement() {
    Injector injector = Injector.newInjector();
    injector.install(Bindings.fixed(Element.get(String.class).requireName("TestData"), "Very cool test"));
    injector.install(Bindings.constructing(Element.get(TestHolder.class), Element.get(TestImpl.class)));

    Root root = injector.instance(Root.class);

    Assertions.assertNotNull(root);
    Assertions.assertNotNull(root.test);
    Assertions.assertNotNull(root.data);
    Assertions.assertNotNull(root.data.test);
    Assertions.assertNotNull(root.data.test.root());
    Assertions.assertNotNull(root.test.rootProvider());

    Assertions.assertEquals("Very cool test", root.test.testData());
    Assertions.assertEquals("Very cool test", root.data.test.testData());

    Assertions.assertSame(root, root.test.root());
    Assertions.assertSame(root, root.data.test.root());
    Assertions.assertSame(root, root.test.rootProvider().get());
  }

  public interface TestHolder {

    String testData();

    Root root();

    Provider<Root> rootProvider();
  }

  @Singleton
  private static final class Root {

    private final TestHolder test;
    private final Data data;

    @Inject
    public Root(TestHolder test, Data data) {
      this.test = test;
      this.data = data;
    }
  }

  private static final class Data {

    private final TestHolder test;

    @Inject
    public Data(TestHolder test) {
      this.test = test;
    }
  }

  public static final class TestImpl implements TestHolder {

    private final String testData;
    private final Root root;
    private final Provider<Root> rootProvider;

    @Inject
    private TestImpl(@Name("TestData") String testData, Root root, Provider<Root> rootProvider) {
      this.testData = testData;
      this.root = root;
      this.rootProvider = rootProvider;
    }

    @Override
    public String testData() {
      return this.testData;
    }

    @Override
    public Root root() {
      return this.root;
    }

    @Override
    public Provider<Root> rootProvider() {
      return this.rootProvider;
    }
  }
}
