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

import dev.derklaro.aerogel.registry.Registry;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RegistryWithKeyMappingTest {

  @Test
  void testParentRegistryGet() {
    Registry.WithKeyMapping<String, String> registry = Registry.createRegistryWithKeys();
    Registry<String, String> childRegistry = registry.createChildRegistry();
    Assertions.assertFalse(registry.parent().isPresent());
    Assertions.assertTrue(childRegistry.parent().isPresent());
    Assertions.assertSame(registry, childRegistry.parent().get());
  }

  @Test
  void testGetFromRegistry() {
    Registry.WithKeyMapping<String, String> registry = Registry.createRegistryWithKeys();
    registry.register("hello", "world");
    Assertions.assertFalse(registry.get("test").isPresent());
    Assertions.assertTrue(registry.get("hello").isPresent());
    Assertions.assertEquals("world", registry.get("hello").get());
  }

  @Test
  void testDuplicateRegistrationThrowsError() {
    Registry.WithKeyMapping<String, String> registry = Registry.createRegistryWithKeys();
    Assertions.assertDoesNotThrow(() -> registry.register("test", "world"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> registry.register("test", "lol"));
    Assertions.assertDoesNotThrow(() -> registry.register("Test", "world"));
  }

  @Test
  void testChildRegistryCanAccessParentRegistry() {
    Registry.WithKeyMapping<String, String> registry = Registry.createRegistryWithKeys();
    registry.register("hello", "world");
    registry.register("test", "hello");

    Registry.WithKeyMapping<String, String> childRegistry = registry.createChildRegistry();
    childRegistry.register("test", "child");
    childRegistry.register("child", "parent");
    childRegistry.register("dependency", "injection");

    Assertions.assertFalse(registry.get("dependency").isPresent());
    Assertions.assertFalse(childRegistry.get("registry").isPresent());

    Optional<String> testFromChild = childRegistry.get("test");
    Assertions.assertTrue(testFromChild.isPresent());
    Assertions.assertEquals("child", testFromChild.get());

    Optional<String> testFromParent = registry.get("test");
    Assertions.assertTrue(testFromParent.isPresent());
    Assertions.assertEquals("hello", testFromParent.get());

    Optional<String> helloFromChild = childRegistry.get("hello");
    Assertions.assertTrue(helloFromChild.isPresent());
    Assertions.assertEquals("world", helloFromChild.get());

    Optional<String> helloFromParent = registry.get("hello");
    Assertions.assertTrue(helloFromParent.isPresent());
    Assertions.assertEquals("world", helloFromParent.get());

    Optional<String> testDirectFromChild = childRegistry.getDirect("hello");
    Assertions.assertFalse(testDirectFromChild.isPresent());

    Optional<String> testDirectFromParent = registry.getDirect("hello");
    Assertions.assertTrue(testDirectFromParent.isPresent());
    Assertions.assertEquals("world", testDirectFromParent.get());
  }

  @Test
  void testUnregisterByKey() {
    Registry.WithKeyMapping<String, String> registry = Registry.createRegistryWithKeys();
    registry.register("hello", "world");
    registry.register("test", "hello");

    Assertions.assertTrue(registry.get("hello").isPresent());
    Assertions.assertTrue(registry.get("test").isPresent());

    registry.unregisterByKey("hello");
    Assertions.assertFalse(registry.get("hello").isPresent());
    Assertions.assertTrue(registry.get("test").isPresent());
  }

  @Test
  void testUnregisterByValue() {
    Registry.WithKeyMapping<String, String> registry = Registry.createRegistryWithKeys();
    registry.register("hello", "world");
    registry.register("dependency", "world");
    registry.register("test", "hello");
    registry.register("return", "World");

    Assertions.assertTrue(registry.get("hello").isPresent());
    Assertions.assertTrue(registry.get("dependency").isPresent());
    Assertions.assertTrue(registry.get("test").isPresent());
    Assertions.assertTrue(registry.get("return").isPresent());

    registry.unregisterByValue("world");
    Assertions.assertFalse(registry.get("hello").isPresent());
    Assertions.assertFalse(registry.get("dependency").isPresent());
    Assertions.assertTrue(registry.get("test").isPresent());
    Assertions.assertTrue(registry.get("return").isPresent());

    Registry.WithKeyMapping<String, String> childRegistry = registry.createChildRegistry();
    childRegistry.register("hello", "world");
    childRegistry.unregisterByValue("hello");
    Assertions.assertTrue(childRegistry.get("hello").isPresent());
    Assertions.assertTrue(childRegistry.get("test").isPresent());
    Assertions.assertTrue(registry.get("test").isPresent());
  }

  @Test
  void testUnregisterWithFilter() {
    Registry.WithKeyMapping<String, String> registry = Registry.createRegistryWithKeys();
    registry.register("hello", "world");
    registry.register("test", "hello");

    Assertions.assertTrue(registry.get("hello").isPresent());
    Assertions.assertTrue(registry.get("test").isPresent());

    registry.unregister(value -> value.equals("world"));
    Assertions.assertFalse(registry.get("hello").isPresent());
    Assertions.assertTrue(registry.get("test").isPresent());

    Registry.WithKeyMapping<String, String> childRegistry = registry.createChildRegistry();
    childRegistry.register("dependency", "child");
    childRegistry.unregister(value -> value.equals("world"));
    Assertions.assertTrue(registry.get("test").isPresent());
    Assertions.assertTrue(childRegistry.get("test").isPresent());
    Assertions.assertTrue(childRegistry.get("dependency").isPresent());
  }

  @Test
  void testEntryCount() {
    Registry.WithKeyMapping<String, String> registry = Registry.createRegistryWithKeys();
    registry.register("hello", "world");
    registry.register("test", "hello");
    Assertions.assertEquals(2, registry.entryCount());

    Registry.WithKeyMapping<String, String> childRegistry = registry.createChildRegistry();
    childRegistry.register("test", "child");
    Assertions.assertEquals(1, childRegistry.entryCount());
  }

  @Test
  void testCopyReturnsNewRegistryWithSameMappings() {
    Registry.WithKeyMapping<String, String> registry = Registry.createRegistryWithKeys();
    registry.register("hello", "world");
    registry.register("test", "hello");

    Registry.WithKeyMapping<String, String> copy = registry.copy();
    Assertions.assertNotSame(registry, copy);

    Assertions.assertTrue(registry.get("hello").isPresent());
    Assertions.assertTrue(registry.get("test").isPresent());

    registry.register("dependency", "injection");
    Assertions.assertFalse(copy.get("dependency").isPresent());
  }

  @Test
  void testFrozenRegistryHasAccessToKeysButCannotModify() {
    Registry.WithKeyMapping<String, String> registry = Registry.createRegistryWithKeys();
    registry.register("hello", "world");
    registry.register("test", "hello");

    Registry.WithKeyMapping<String, String> frozen = registry.freeze();
    Assertions.assertThrows(UnsupportedOperationException.class, () -> frozen.register("test", "test"));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> frozen.unregisterByKey("test"));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> frozen.unregisterByValue("test"));

    Assertions.assertTrue(registry.get("hello").isPresent());
    Assertions.assertTrue(registry.get("test").isPresent());

    Registry.WithKeyMapping<String, String> child = frozen.createChildRegistry();
    Assertions.assertDoesNotThrow(() -> child.register("test", "test"));
  }
}
