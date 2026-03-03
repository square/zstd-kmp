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
package com.squareup.zstd

internal val jniLibraryLoaded: Unit by lazy { loadNativeLibrary() }

/**
 * Result of a native stream operation. The native function returns a [LongArray] with three
 * elements; this class provides named access to each.
 */
internal class StreamResult(private val values: LongArray) {
  /** The zstd result code. */
  val result: Long
    get() = values[0]

  /** The number of input bytes consumed. */
  val inputBytesProcessed: Int
    get() = values[1].toInt()

  /** The number of output bytes produced. */
  val outputBytesProcessed: Int
    get() = values[2].toInt()
}

@JvmName("jniGetErrorName") internal external fun jniGetErrorName(code: Long): String?

@JvmName("createZstdCompressor") internal external fun createZstdCompressor(): Long

@JvmName("createZstdDecompressor") internal external fun createZstdDecompressor(): Long

internal expect fun loadNativeLibrary()
