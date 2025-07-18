rootProject.name = "zstd-kmp-root"

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":zstd-kmp")
include(":zstd-kmp-okio")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
