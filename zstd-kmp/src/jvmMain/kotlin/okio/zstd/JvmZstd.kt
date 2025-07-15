/*
 * Copyright (C) 2025 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("JvmZstd")

package okio.zstd

import java.nio.file.Files
import java.util.Locale.US
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath

@Suppress("UnsafeDynamicallyLoadedCode") // Only loading from our own JAR contents.
internal actual fun loadNativeLibrary() {
  val osName = System.getProperty("os.name").lowercase(US)
  val osArch = System.getProperty("os.arch").lowercase(US)
  val resourcePath = when {
    osName.contains("linux") -> "/jni/$osArch/libzstd-kmp.so".toPath()
    osName.contains("mac") -> "/jni/$osArch/libzstd-kmp.dylib".toPath()
    else -> error("Unsupported OS: $osName")
  }

  // File-based deleteOnExit() uses a special internal shutdown hook that always runs last.
  val tempFile = Files.createTempFile("zstd-kmp", null)
  tempFile.toFile().deleteOnExit()

  FileSystem.RESOURCES.read(resourcePath) {
    FileSystem.SYSTEM.write(tempFile.toOkioPath()) {
      writeAll(this@read)
    }
  }

  System.load(tempFile.toAbsolutePath().toString())
}
