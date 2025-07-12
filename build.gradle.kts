import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import java.net.URI
import java.net.URL
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

buildscript {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
  dependencies {
    classpath(libs.android.gradle.plugin)
    classpath(libs.binary.compatibility.validator.gradle.plugin)
    classpath(libs.mavenPublish.gradle.plugin)
    classpath(libs.kotlin.gradle.plugin)
    classpath(libs.dokka.gradle.plugin)
    classpath(libs.shadowJar.gradle.plugin)
    classpath(libs.cklib.gradle.plugin)
  }
}

plugins {
  id("com.github.gmazzo.buildconfig") version "5.6.7" apply false
  alias(libs.plugins.spotless)
}

apply(plugin = "org.jetbrains.dokka")

apply(plugin = "com.vanniktech.maven.publish.base")

configure<SpotlessExtension> {
  kotlin {
    target("**/*.kt")
    ktlint()
      .editorConfigOverride(
        mapOf(
          "ktlint_standard_comment-spacing" to "disabled", // TODO Re-enable
          "ktlint_standard_filename" to "disabled",
          "ktlint_standard_indent" to "disabled", // TODO Re-enable
        )
      )
  }
}

allprojects {
  group = "com.squareup.okio-zstd"
  version = project.property("VERSION_NAME") as String

  repositories {
    mavenCentral()
    google()
  }
}

subprojects {
  tasks.withType(Test::class).configureEach {
    testLogging {
      if (System.getenv("CI") == "true") {
        events = setOf(TestLogEvent.STARTED, TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
      }
      exceptionFormat = TestExceptionFormat.FULL
    }
  }
}

tasks.named("dokkaHtmlMultiModule", DokkaMultiModuleTask::class.java).configure {
  moduleName.set("Okio-Zstd")
}

allprojects {
  tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
      documentedVisibilities.set(setOf(
        Visibility.PUBLIC,
        Visibility.PROTECTED
      ))
      reportUndocumented.set(false)
      jdkVersion.set(11)

      sourceLink {
        localDirectory.set(rootProject.projectDir)
        remoteUrl.set(URL("https://github.com/square/okio-zstd/tree/main/"))
        remoteLineSuffix.set("#L")
      }
    }
  }

  // Workaround for https://github.com/Kotlin/dokka/issues/2977.
  // We disable the C Interop IDE metadata task when generating documentation using Dokka.
  tasks.withType<AbstractDokkaTask> {
    @Suppress("UNCHECKED_CAST")
    val taskClass = Class.forName("org.jetbrains.kotlin.gradle.targets.native.internal.CInteropMetadataDependencyTransformationTask") as Class<Task>
    parent?.subprojects?.forEach {
      dependsOn(it.tasks.withType(taskClass))
    }
  }

  // Don't attempt to sign anything if we don't have an in-memory key. Otherwise, the 'build' task
  // triggers 'signJsPublication' even when we aren't publishing (and so don't have signing keys).
  tasks.withType<Sign>().configureEach {
    enabled = project.findProperty("signingInMemoryKey") != null
  }

  plugins.withId("org.jetbrains.kotlin.multiplatform") {
    configure<KotlinMultiplatformExtension> {
      jvmToolchain(11)
    }
  }

  plugins.withId("org.jetbrains.kotlin.jvm") {
    configure<KotlinJvmProjectExtension> {
      jvmToolchain(11)
    }
  }

  plugins.withId("com.vanniktech.maven.publish.base") {
    configure<PublishingExtension> {
      repositories {
        maven {
          name = "testMaven"
          url = rootProject.layout.buildDirectory.dir("testMaven").get().asFile.toURI()
        }

        /*
         * Want to push to an internal repository for testing?
         * Set the following properties in ~/.gradle/gradle.properties.
         *
         * internalUrl=YOUR_INTERNAL_URL
         * internalUsername=YOUR_USERNAME
         * internalPassword=YOUR_PASSWORD
         *
         * Then run the following command to publish a new internal release:
         *
         * ./gradlew publishAllPublicationsToInternalRepository -DRELEASE_SIGNING_ENABLED=false
         */
        val internalUrl = providers.gradleProperty("internalUrl").orNull
        if (internalUrl != null) {
          maven {
            name = "internal"
            url = URI(internalUrl)
            credentials {
              username = providers.gradleProperty("internalUsername").get()
              password = providers.gradleProperty("internalPassword").get()
            }
          }
        }
      }
    }
    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
      signAllPublications()
      pom {
        description.set("ZStandard for Okio")
        name.set(project.name)
        url.set("https://github.com/square/okio-zstd/")
        licenses {
          license {
            name.set("The Apache Software License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
          }
        }
        developers {
          developer {
            id.set("square")
            name.set("Square, Inc.")
          }
        }
        scm {
          url.set("https://github.com/square/okio-zstd/")
          connection.set("scm:git:https://github.com/square/okio-zstd.git")
          developerConnection.set("scm:git:ssh://git@github.com/square/okio-zstd.git")
        }
      }
    }
  }
}
