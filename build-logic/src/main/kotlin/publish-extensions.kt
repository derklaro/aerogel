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

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

fun Project.configurePublishing(publishedComponent: String, withJavadocAndSource: Boolean = false) {
  extensions.configure<PublishingExtension> {
    publications.apply {
      create("maven", MavenPublication::class.java).apply {
        from(components.getByName(publishedComponent))

        if (withJavadocAndSource) {
          artifact(tasks.getByName("sourcesJar"))
          artifact(tasks.getByName("javadocJar"))
        }

        pom.apply {
          name.set(project.name)
          description.set(project.description)
          url.set("https://github.com/derklaro/aerogel")

          developers {
            developer {
              id.set("derklaro")
              email.set("git@derklaro.dev")
              timezone.set("Europe/Berlin")
              name.set("Pasqual Koschmieder")
            }
          }

          licenses {
            license {
              name.set("MIT")
              url.set("https://opensource.org/licenses/MIT")
            }
          }

          scm {
            tag.set("HEAD")
            url.set("git@github.com:derklaro/aerogel.git")
            connection.set("scm:git:git@github.com:derklaro/aerogel.git")
            developerConnection.set("scm:git:git@github.com:derklaro/aerogel.git")
          }

          issueManagement {
            system.set("GitHub Issues")
            url.set("https://github.com/derklaro/aerogel/issues")
          }

          ciManagement {
            system.set("GitHub Actions")
            url.set("https://github.com/derklaro/aerogel/actions")
          }

          withXml {
            val repositories = asNode().appendNode("repositories")
            project.repositories.forEach {
              if (it is MavenArtifactRepository && it.url.toString().startsWith("https://")) {
                val repo = repositories.appendNode("repository")
                repo.appendNode("id", it.name)
                repo.appendNode("url", it.url.toString())
              }
            }
          }
        }
      }
    }
  }

  extensions.configure<SigningExtension> {
    useGpgCmd()
    sign(extensions.getByType(PublishingExtension::class.java).publications.getByName("maven"))
  }

  tasks.withType(Sign::class) {
    onlyIf {
      !rootProject.version.toString().endsWith("-SNAPSHOT")
    }
  }
}
