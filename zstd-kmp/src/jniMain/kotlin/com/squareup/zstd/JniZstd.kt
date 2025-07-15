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
@file:JvmMultifileClass
@file:JvmName("Zstd")

package com.squareup.zstd

internal val jniZstdPointer: Long = run {
  loadNativeLibrary()
  createJniZstd()
}

actual fun zstdCompressor(): ZstdCompressor = JniZstdCompressor()

actual fun zstdDecompressor(): ZstdDecompressor = JniZstdDecompressor()

@JvmName("getErrorName")
actual external fun getErrorName(code: Long): String?

@JvmName("createJniZstd")
internal external fun createJniZstd(): Long

@JvmName("createZstdCompressor")
internal external fun createZstdCompressor(): Long

@JvmName("createZstdDecompressor")
internal external fun createZstdDecompressor(): Long

internal expect fun loadNativeLibrary()
