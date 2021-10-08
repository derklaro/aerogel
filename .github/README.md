Aerogel ![Build Status](https://github.com/derklaro/aerogel/actions/workflows/build.yml/badge.svg) ![LGTM quality rating](https://img.shields.io/lgtm/grade/java/github/derklaro/aerogel) ![Central Release Version](https://img.shields.io/maven-central/v/io.github.derklaro/aerogel)
===========

A lightweight dependency injection framework for Java 8 - 17 which aims for stability, performance and reliability.
Aerogel is a fully implemented [JSR 330](https://jcp.org/en/jsr/detail?id=330) injector.

### How to (Core)

The api for aerogel is directly packaged with the internal core to run the framework. The following annotations are used
for the core injection framework:

- `@Inject`: the core annotation when trying to inject. Applied to a constructor it indicates which constructor should
  be used for class creating, applied to class members it indicates which members should be injected after a successful
  construction.
- `@Name`: a build-in qualifier annotation which - applied to a parameter or field - sets an extra name property in an
  element allowing multiple instances of a type to be distinguished (a name can be requested in an element
  using `Element#requireName(String)`).
- `@ProvidedBy`: an annotation which - applied to a type - signals the injector that the requested instance is not
  implemented by the current class but the class given as the annotation's value.
- `@Qualifier`: Identifies qualifier annotations. When constructing an element, qualifier annotations will be detected
  on fields and parameters and applied as special properties to an element. See down below for an example (In this
  case `@Greeting` and `@GoodBye`).
- `@Singleton`: Signals an injector that an instance of the class should only get created once per injector chain. The
  annotation is respected on all types as well as factory method binding types.

All libraries of aerogel are published to maven central:

```groovy
repositories {
  mavenCentral()
}

dependencies {
  implentation group: 'io.github.derklaro', name: 'aerogel', version: '<VERSION>'
}
```

You can now start building your application based on the input:

```java
import aerogel.Injector;
import aerogel.Bindings;
import aerogel.Element;

public final class Application {

  public static void main(String[] args) {
    // creates a new injector without any binding
    Injector injector = Injector.newInjector();
    // binds all types of 'int' to '1234'
    injector.install(Bindings.fixed(Element.get(int.class), 1234));
    // binds all types of int which are annotated as @Name("serverPort") to '25656'
    injector.install(Bindings.fixed(Element.get(int.class).requireName("serverPort"), 25656));
    // binds all types of String which are annotated as @Greeting to "Hello there :)"
    injector.install(Bindings.fixed(Element.get(String.class).requireAnnotations(Greeting.class), "Hello there :)"));
    // binds all types of String which are annotated as @GoodBye to a static factory method in this class
    injector.install(Bindings.factory(
      Element.get(String.class).requireAnnotations(GoodBye.class),
      Application.class.getDeclaredMethod("goodbye", String.class)));

    // dynamically creates the instance of 'ApplicationEntryPoint' using all previously installed bindings and creates
    // all bindings if possible dynamically when requested. A dynamic injection is only possible if no special needs
    // are supplied for example to a parameter. This means if a parameter is annotated as @GoodBye but there is no binding
    // for the type of the parameter in combination with @GoodBye the injection fails. If there are no special requirements
    // added to a parameter the injector tries to create a dynamic binding in the runtime.
    ApplicationEntryPoint aep = injector.instance(ApplicationEntryPoint.class);
    // Bootstrap our application!
    aep.bootstrap();
  }

  private static String goodbye(@Greeting String greetingMessage) {
    return greetingMessage + " but now you have to go :/";
  }
}
```

##### Circular proxies

Aerogel will create circular proxies of interfaces when necessary. These proxies will be used to break circular
dependencies (`Provider` can be used to reach the same goal but can be a bit annoying to use).

There are some limitations when proxies are used:

* Injecting the same type twice when the type is proxied will result in the same instance instead of two different
  instances.
* Injected proxies into classes are not available in the constructor are the type which got injected is required for the
  implementation of the proxied interface to be constructed. (This applies as well to all `Provider` methods when you
  try to prevent circular dependencies in a constructor)

### How to (Auto)

The auto module is used to generate binding data during compile time. This data is emitted to a file
called `auto-factories.txt` in the output directory and will be located in the jar after compile. This file can be
loaded using the build-in loader and will automatically create all bindings necessary. At the moment there are two
annotations supported by this: `@Factory` (which automatically creates factory bindings as showed above)
and `@Provides` (which automatically maps an implementation class to its source interface). The `AutoAnnotationRegistry`
also supports adding own annotation readers which were emitted by your own annotation processor. The data stream must
begin with the target factory name which should be used for reading the data (using `writeUTF`). That data will not be
there anymore when the `makeBinding` method gets called as it was used by the registry already. You can then write any
data you need to construct the binding in the runtime.

Add the auto module as follows (you still need to add the core module as shown above):

```groovy
repositories {
  mavenCentral()
}

dependencies {
  implentation group: 'io.github.derklaro', name: 'aerogel-auto', version: '<VERSION>'
  annotationProcessor group: 'io.github.derklaro', name: 'aerogel-auto', version: '<VERSION>'
}
```

We can for example do something like this (it's an example and not best practice tho - keep your code clean):

```java
import aerogel.auto.Factory;

public final class Bindings {

  /**
   * This method will now get registered as a factory method.
   */
  @Factory
  private static String greetingWithSmiley(@Greeting String greeting) {
    return greeting + " :)";
  }

  /**
   * This class will now be used for injecting the {@code Data} and {@code Info} class.
   */
  @Provides({Data.class, Info.class})
  public static final class DataImpl implements Data, Info {

  }
}
```

Bindings can now get loaded in the runtime:

```java
import aerogel.Injector;
import aerogel.auto.AutoAnnotationRegistry;

public final class Application {

  public static void main(String[] args) {
    // creates a new injector without any binding
    Injector injector = Injector.newInjector();
    // creates a new registry instance which by default supports @Factory and @Provides
    AutoAnnotationRegistry registry = AutoAnnotationRegistry.newInstance();
    // loads and installs all bindings from the file which was emitted to the class output
    registry.installBindings(Application.class.getClassLoader(), "auto-factories.txt", injector);
    // we can now add more bindings, start the application ...
  }
}
```

### How to (Kotlin Extensions)

The kotlin extension module is basically a nice-to-have collection of inline features mostly related to generics so that
there is no need for kotlin developers to actually convert the kotlin type to a java type. There are some restriction
when it comes to the kotlin type resolving as for primitive types most of the type the wrapper class is resolved instead
of the primitive type. This may lead to some weird behaviours when primitive types are requested but wrapper types were
bound.

Add the kotlin extensions module as follows (you still need to add the core module as shown above):

```groovy
repositories {
  mavenCentral()
}

dependencies {
  implentation group: 'io.github.derklaro', name: 'aerogel-kotlin-extensions', version: '<VERSION>'
}
```

There are now replacements for everything which works with generics. Here are some examples:

```kotlin
import aerogel.Injector
import aerogel.kotlin.fixed
import aerogel.kotlin.instance
import aerogel.kotlin.element

fun main() {
  val injector = Injector.newInjector()
  // instead of Bindings.fixed(Element.get(String::class.java), "Test2345")
  injector.install(fixed<String>("Test2345"))
  // instead of Element.get(String.class::java)
  val element = element<String>()
  // instead of injector.instance(String::class.java)
  val testString = injector.instance<String>()
}
```

### How to (Build from source)

To run the full build lifecycle you can just execute `./gradlew` or `gradlew.bat` depending on your current operating
system. The final jar, javadoc jar and sources jar are now located in `**/build/libs`. All tasks which are possible to
be executed can be listed by running `./gradlew tasks` or `gradlew.bat tasks`.

Publishing
--------

The library gets published on a regular basis to the sonatype snapshot repository if there are in-development changes.
You can get these versions by using the current snapshot version and adding the sonatype repository as a maven
repository to your build:

```groovy
repositories {
  maven {
    name 'sonatype-snapshots'
    url 'https://s01.oss.sonatype.org/content/repositories/snapshots/'
  }
}
```

Releases are published to the maven central repository and jitpack:

```groovy
repositories {
  // to use the releases from maven-central
  mavenCentral()
  // alternative to mavenCentral()
  maven {
    name 'central'
    url 'https://repo1.maven.org/maven2/'
  }
  // to use the releases from jitpack
  maven {
    name 'jitpack'
    url 'https://jitpack.io/'
  }
}
```

### GitHub Releases

Every release version of aerogel has a [release](https://github.com/derklaro/aerogel/releases) in the repository.
Release candidates gets tagged as `Pre-release`.

### Version naming

- `<version>-SNAPSHOT`: the current development snapshot, will be located in the sonatype snapshot repository.
- `<version->-RC<rc-version-count>`: the current pre-release. The development process of the next version ended but
  needs more testing before an actual release.
- `<version>`: A stable release version of aerogel.

JSR 330
--------

Aerogel is fully compatible with all requirements of an injector defined
in [JSR 330](https://jcp.org/en/jsr/detail?id=330). The Jakarta inject api is shaded into the final artifact of aerogel.
Here is a list of each jakarta type and its equivalents in aerogel:

| jakarta.inject | aerogel    |
| -------------- | ---------- |
| @Inject        | @Inject    |
| @Singleton     | @Singleton |
| @Qualifier     | @Qualifier |
| @Named         | @Name      |
| Provider       | Provider   |

Issue reporting and contributing
--------

Issues can be reported through the [GitHub issue tracker](https://github.com/derklaro/aerogel/issues/new/).
Contributions are always welcome - please make sure you've read the [contribution guidelines](../CONTRIBUTING.md). A
star is always appreciated.

License
---------

Aerogel is released under the terms of the MIT license. See [license.txt](../license.txt)
or https://opensource.org/licenses/MIT.

Alternatives
---------

There are some alternatives for dependency injection out there. Here are some popular ones:

- Google Guice: https://github.com/google/guice
- Spring: https://github.com/spring-projects/spring-framework
- Google Dagger (compile time): https://github.com/google/dagger
- Kodein (designed for kotlin): https://github.com/Kodein-Framework/Kodein-DI
