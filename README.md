ZSTD-KMP
========

The [Zstandard compression library (zstd)][zstd], packaged for [Kotlin multiplatform (kmp)][kmp].

This is intended to be a small library that supports common Zstandard use-cases with zero additional
dependencies. For more advanced features, consider using [zstd] directly.

This library is packaged as two modules:

 * `zstd-kmp`: direct access to Zstandard APIs.
 * `zstd-kmp-okio`: [Okio] integration.


Usage with Okio
---------------

### Gradle Setup

In `gradle/libs.versions.toml`:

```toml
[libraries]
okio-core = { module = "com.squareup.okio:okio", version = "3.15.0" }
zstd-kmp-okio = { module = "com.squareup.zstd:zstd-kmp-okio", version = "0.2.0" }
```

In `build.gradle.kts`:

```kotlin
kotlin {
  // ...
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.okio.core)
        implementation(libs.zstd.kmp.okio)
      }
    }
  }
}
```

### Compressing a stream of data

Turn a `Sink` into a zstd-compressing sink with `zstdCompress()`. All data written to that sink will
be compressed and forwarded on. Use `buffer()` to enable buffering for better performance and a more
capable API.

Use `flush()` to force all data to be emitted immediately.

Be sure to call `close()` when you're done, both to emit the last frame and to release the native
resources held by the zstd context. This example uses `use()` to call `close()` even if something
throws an exception.

```kotlin
fun compressOneFile(fileSystem: FileSystem, path: Path) {
  val zstdPath = "$path.zst".toPath()
  fileSystem.source(path).use { fileSource ->
    fileSystem.sink(zstdPath).zstdCompress().buffer().use { zstdSink ->
      zstdSink.writeAll(fileSource)
    }
  }
}
```

### Decompressing a stream of data

Turn a `Source` of zstd-compressed data into its original form with `zstdDecompress()`.

Don't forget to close this source to release resources held by the zstd context. This example also
uses `use()` to call `close()`.

```kotlin
fun decompressOneFile(fileSystem: FileSystem, path: Path) {
  require(path.name.endsWith(".zst")) { "unexpected path: $path"}
  val filePath = path.toString().removeSuffix(".zst").toPath()
  fileSystem.source(path).zstdDecompress().use { zstdSource ->
    fileSystem.sink(filePath).buffer().use { fileSink ->
      fileSink.writeAll(zstdSource)
    }
  }
}
```

Supported Platforms
-------------------

 * Android (API 21+)
 * JVM (JDK 11+)
 * Kotlin/Native
   * linuxX64
   * macosX64
   * macosArm64
   * iosArm64
   * iosX64
   * iosSimulatorArm64
   * tvosArm64
   * tvosSimulatorArm64
   * tvosX64


Building
--------

### Git Submodules

```
git submodule init
git submodule update
```

### Build JNI libraries

```
pushd zstd-kmp
zig build --verbose -p src/jvmMain/resources/jni
popd
```

### Test it:

```
./gradlew \
  zstd-kmp:jvmTest \
  zstd-kmp-okio:jvmTest \
  zstd-kmp:macosArm64Test \
  zstd-kmp-okio:macosArm64Test
```


License
-------

    Copyright 2025 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[Okio]: https://github.com/square/okio/
[kmp]: https://kotlinlang.org/docs/multiplatform.html
[zstd]: https://github.com/facebook/zstd
