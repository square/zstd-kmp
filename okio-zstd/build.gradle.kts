import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
  id("com.github.gmazzo.buildconfig")
  id("binary-compatibility-validator")
}

kotlin {
  jvm()

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
  }
}

buildConfig {
  useKotlinOutput {
    internalVisibility = true
    topLevelConstants = true
  }
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinMultiplatform(javadocJar = JavadocJar.Empty())
  )
}
