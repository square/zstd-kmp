import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import java.net.URI
import java.net.URL
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
    classpath(libs.testDistributionGradlePlugin)
  }
}

plugins {
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
  group = "com.squareup.zstd"
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
  moduleName.set("Zstd-Kmp")
}

allprojects {
  tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
      documentedVisibilities.set(setOf(
        Visibility.PUBLIC,
        Visibility.PROTECTED
      ))
      reportUndocumented.set(false)
      jdkVersion.set(8)

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

  val javaVersion = JavaVersion.VERSION_1_8
  tasks.withType(KotlinJvmCompile::class.java).configureEach {
    compilerOptions {
      freeCompilerArgs.add("-Xjdk-release=$javaVersion")
      jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
    }
  }
  tasks.withType(JavaCompile::class.java).configureEach {
    sourceCompatibility = javaVersion.toString()
    targetCompatibility = javaVersion.toString()
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
      publishToMavenCentral(automaticRelease = true)
      signAllPublications()
      pom {
        description.set("Zstandard for Kotlin Multiplatform")
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
