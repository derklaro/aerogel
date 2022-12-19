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

import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  id("build-logic")
  alias(libs.plugins.spotless)
  alias(libs.plugins.nexusPublish)
}

defaultTasks("build", "test")

allprojects {
  version = "2.0.0-SNAPSHOT"
  group = "dev.derklaro.aerogel"
  description = "A very lightweight jvm dependency injection library"

  apply(plugin = "signing")
  apply(plugin = "checkstyle")
  apply(plugin = "java-library")
  apply(plugin = "maven-publish")
  apply(plugin = "com.diffplug.spotless")

  repositories {
    mavenCentral()
  }

  dependencies {
    // exposed dependencies
    "api"(rootProject.libs.jakartaInjectApi)
    "compileOnlyApi"(rootProject.libs.apiGuardian)
    "compileOnlyApi"(rootProject.libs.annotations)

    // testing
    "testImplementation"(rootProject.libs.geantyref)
    "testImplementation"(rootProject.libs.bundles.junit)
    "testImplementation"(rootProject.libs.jakartaInjectTck)
  }

  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
      events("started", "passed", "skipped", "failed")
    }
  }

  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
    // options
    options.encoding = "UTF-8"
    options.isIncremental = true
  }

  tasks.withType<Checkstyle>().configureEach {
    maxErrors = 0
    maxWarnings = 0
    configFile = rootProject.file("checkstyle.xml")
  }

  tasks.withType<Javadoc>().configureEach {
    val options = options as? StandardJavadocDocletOptions ?: return@configureEach
    options.use()
    options.encoding = "UTF-8"
    options.memberLevel = JavadocMemberLevel.PRIVATE
    options.links(
      "https://javadoc.io/doc/org.jetbrains/annotations/",
      "https://javadoc.io/doc/org.apiguardian/apiguardian-api/",
      "https://javadoc.io/doc/jakarta.inject/jakarta.inject-api/"
    )
  }

  tasks.register<org.gradle.jvm.tasks.Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.getByName("javadoc"))
  }

  tasks.register<org.gradle.jvm.tasks.Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(project.the<JavaPluginExtension>().sourceSets["main"].allJava)
  }

  extensions.configure<SpotlessExtension> {
    java {
      licenseHeaderFile(rootProject.file("license_header.txt"))
    }

    kotlin {
      licenseHeaderFile(rootProject.file("license_header.txt"))
    }
  }

  extensions.configure<CheckstyleExtension> {
    toolVersion = "10.5.0"
  }
}

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

      username.set(rootProject.findProperty("ossrhUsername") as? String ?: "")
      password.set(rootProject.findProperty("ossrhPassword") as? String ?: "")
    }
  }

  useStaging.set(!rootProject.version.toString().endsWith("-SNAPSHOT"))
}

configurePublishing("java", true)
