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
@file:Suppress("ktlint:standard:property-naming")
@file:JvmName("Zstd")

package com.squareup.zstd

import kotlin.jvm.JvmName
import okio.IOException
import okio.Sink
import okio.Source
import okio.buffer

/** Returns a [Sink] that compresses its data with ZStandard before forwarding to this. */
fun Sink.zstdCompress(): Sink = ZstdCompressSink(this.buffer(), zstdCompressor())

/** Returns a [Source] that decompresses its data with ZStandard after reading from this. */
fun Source.zstdDecompress(): Source = ZstdDecompressSource(this.buffer(), zstdDecompressor())

// From ZSTD_ErrorCode in zstd_errors.h. This is a small subset!
internal const val ZSTD_error_no_error = 0L
internal const val ZSTD_error_GENERIC = -1L

// From ZSTD_cParameter in zstd.h. This is a small subset!
internal const val ZSTD_c_compressionLevel = 100
internal const val ZSTD_c_checksumFlag = 201

// From ZSTD_EndDirective in zstd.h.
internal const val ZSTD_e_continue = 0
internal const val ZSTD_e_flush = 1
internal const val ZSTD_e_end = 2

@Throws(IOException::class)
internal fun Long.checkError(): Long {
  val errorName = getErrorName(this) ?: return this
  throw IOException(errorName)
}

internal expect fun getErrorName(code: Long): String?

/** Returns a new compressor. The caller must close it. */
internal expect fun zstdCompressor(): ZstdCompressor

/** Returns a new decompressor. The caller must close it. */
internal expect fun zstdDecompressor(): ZstdDecompressor

internal val emptyByteArray = ByteArray(0)
