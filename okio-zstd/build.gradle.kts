import co.touchlab.cklib.gradle.CompileToBitcode.Language.C
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
  id("co.touchlab.cklib")
  id("com.github.gmazzo.buildconfig")
  id("binary-compatibility-validator")
}

kotlin {
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
      }
    }
    val jvmMain by getting {
      dependencies {
        api(libs.okio.core)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.assertk)
        implementation(libs.lubenZstdJni)
        implementation(kotlin("test"))
      }
    }

    targets.withType<KotlinNativeTarget> {
      val main by compilations.getting

      main.cinterops {
        create("zstd") {
          header(file("../zstd/lib/zstd.h"))
          packageName("okio.zstd.internal")
        }
      }
    }
  }
}

buildConfig {
  useKotlinOutput {
    internalVisibility = true
    topLevelConstants = true
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
configure<MavenPublishBaseExtension> {
  configure(
    KotlinMultiplatform(javadocJar = JavadocJar.Empty())
  )
}
