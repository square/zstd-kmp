import co.touchlab.cklib.gradle.CompileToBitcode.Language.C
import com.android.build.api.variant.HasUnitTestBuilder
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME

plugins {
  kotlin("multiplatform")
  id("com.android.library")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
  id("co.touchlab.cklib")
  id("binary-compatibility-validator")
  id("com.jakewharton.test-distribution")
}

kotlin {
  androidTarget {
    publishLibraryVariants("release")
  }

  jvm()

  linuxArm64()
  linuxX64()
  macosArm64()
  macosX64()
  mingwX64()
  iosArm64()
  iosX64()
  iosSimulatorArm64()
  tvosArm64()
  tvosSimulatorArm64()
  tvosX64()

  applyDefaultHierarchyTemplate()

  sourceSets {
    val commonMain by getting {
    }
    val jniMain by creating {
      dependsOn(commonMain)
    }
    val jvmMain by getting {
      dependsOn(jniMain)
    }
    val androidMain by getting {
      dependsOn(jniMain)
    }

    targets.withType<KotlinNativeTarget> {
      val main by compilations.getting

      main.cinterops {
        create("zstd") {
          header(file("../zstd/lib/zstd.h"))
          packageName("com.squareup.zstd.internal")
        }
      }
    }

    val commonTest by getting {
      dependencies {
        api(kotlin("test"))
        api(libs.assertk)
        api(libs.okio.core)
      }
    }
  }

  val linkNativeDebugTests = tasks.register("linkNativeDebugTests")
  targets.withType<KotlinNativeTarget> {
    linkNativeDebugTests.configure {
      dependsOn(compilations.getByName(TEST_COMPILATION_NAME).binariesTaskName)
    }
  }
}

cklib {
  config.kotlinVersion = libs.versions.kotlin.get()
  create("zstd") {
    language = C
    srcDirs = project.files(
      "../zstd/lib/common",
      "../zstd/lib/compress",
      "../zstd/lib/decompress",
    )
    compilerArgs.addAll(
      listOf(
        //"-DDUMP_LEAKS=1", // For local testing ONLY!
        "-DZSTD_DISABLE_ASM=1", // We haven't hooked up X86 assembly in Kotlin/Native builds.
        "-DKONAN_MI_MALLOC=1",
        "-Wno-unknown-pragmas",
        "-ftls-model=initial-exec",
        "-Wno-unused-function",
        "-Wno-error=atomic-alignment",
        "-Wno-sign-compare",
        "-Wno-unused-parameter" /* for windows 32 */
      )
    )
  }
}

// Disable host-side unit tests. Testing is done with device instrumentation tests.
androidComponents {
  beforeVariants {
    (it as HasUnitTestBuilder).enableUnitTest = false
  }
}

android {
  namespace = "com.squareup.zstd"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    multiDexEnabled = true

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    ndk {
      abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
    }

    externalNativeBuild {
      cmake {
        arguments("-DANDROID_TOOLCHAIN=clang", "-DANDROID_STL=c++_static")
        cFlags("-fstrict-aliasing")
        cppFlags("-fstrict-aliasing")
      }
    }
  }

  // TODO: Remove when https://issuetracker.google.com/issues/260059413 is resolved.
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildTypes {
    val release by getting {
      externalNativeBuild {
        cmake {
          arguments("-DCMAKE_BUILD_TYPE=MinSizeRel")
          cFlags("-g0", "-Os", "-fomit-frame-pointer", "-DNDEBUG", "-fvisibility=hidden")
          cppFlags("-g0", "-Os", "-fomit-frame-pointer", "-DNDEBUG", "-fvisibility=hidden")
        }
      }
    }
    val debug by getting {
      externalNativeBuild {
        cmake {
          cFlags("-g", "-DDEBUG", "-DDUMP_LEAKS")
          cppFlags("-g", "-DDEBUG", "-DDUMP_LEAKS")
        }
      }
    }
  }

  externalNativeBuild {
    cmake {
      path = file("src/androidMain/CMakeLists.txt")
    }
  }
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinMultiplatform(javadocJar = JavadocJar.Empty())
  )
}
