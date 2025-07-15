import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  kotlin("multiplatform")
  id("com.android.library")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
  id("binary-compatibility-validator")
}

kotlin {
  androidTarget {
    publishLibraryVariants("release")
  }

  jvm()

  linuxX64()
  macosX64()
  macosArm64()
  iosArm64()
  iosX64()
  iosSimulatorArm64()
  tvosArm64()
  tvosSimulatorArm64()
  tvosX64()

  applyDefaultHierarchyTemplate()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.okio.core)
        implementation(projects.zstdKmp)
      }
    }

    val commonTest by getting {
      dependencies {
        api(kotlin("test"))
        api(libs.assertk)
        api(projects.zstdKmp)
      }
    }
    val jvmTest by getting {
      // The jniTest directory depends on different lubenZstdJni artifacts for JVM vs. Android.
      // That library isn't Kotlin Multiplatform and doesn't have a common artifact. Including it as
      // a srcDir instead of as a sourceSet makes the IDE experience better.
      kotlin.srcDir("src/jniTest/kotlin")
      dependencies {
        implementation(libs.lubenZstdJni)
      }
    }
    val androidInstrumentedTest by getting {
      dependsOn(commonTest)
      kotlin.srcDir("src/jniTest/kotlin")
    }
  }
}

dependencies {
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(
    variantOf(libs.lubenZstdJni) {
      artifactType("aar")
    }
  )
}

android {
  namespace = "com.squareup.zstd.okio"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // TODO: Remove when https://issuetracker.google.com/issues/260059413 is resolved.
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinMultiplatform(javadocJar = JavadocJar.Empty())
  )
}
