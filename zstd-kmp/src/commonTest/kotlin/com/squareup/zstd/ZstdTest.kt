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

package com.squareup.zstd

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import okio.Buffer.UnsafeCursor
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import okio.IOException

// From ZSTD_ErrorCode in zstd_errors.h. This is a small subset!
internal const val ZSTD_error_no_error = 0L
internal const val ZSTD_error_GENERIC = -1L

// From ZSTD_cParameter in zstd.h. This is a small subset!
internal const val ZSTD_c_compressionLevel = 100
internal const val ZSTD_c_checksumFlag = 201

/** Exercise the native bindings. */
internal class ZstdTest {
  private val helloWorld = "hello world".encodeUtf8()
  private val helloWorldZstd = "28b52ffd200b59000068656c6c6f20776f726c64".decodeHex()

  @Test
  fun getErrorName() {
    assertThat(getErrorName(ZSTD_error_no_error)).isNull()
    assertThat(getErrorName(ZSTD_error_GENERIC)).isEqualTo("Error (generic)")
  }

  @Test
  fun compress() {
    assertThat(oneShotCompress(helloWorld)).isEqualTo(helloWorldZstd)
  }

  @Test
  fun customOptions() {
    assertThat(oneShotCompress(original = helloWorld, compressionLevel = 7))
      .isEqualTo("28b52ffd200b59000068656c6c6f20776f726c64".decodeHex())

    assertThat(oneShotCompress(original = helloWorld, checksumFlag = 1))
      .isEqualTo("28b52ffd240b59000068656c6c6f20776f726c6468691eb2".decodeHex())

    assertThat(oneShotCompress(original = helloWorld, compressionLevel = 7, checksumFlag = 1))
      .isEqualTo("28b52ffd240b59000068656c6c6f20776f726c6468691eb2".decodeHex())
  }

  @Test
  fun decompress() {
    assertThat(oneShotDecompress(helloWorldZstd)).isEqualTo(helloWorld)
  }

  @Test
  fun compressWithOffsets() {
    assertThat(
        oneShotCompress(original = helloWorld, inputOffset = 5, inputPadding = 7, outputOffset = 9)
      )
      .isEqualTo(helloWorldZstd)
  }

  @Test
  fun decompressWithOffsets() {
    assertThat(
        oneShotDecompress(
          compressed = helloWorldZstd,
          inputOffset = 5,
          inputPadding = 7,
          outputOffset = 9,
        )
      )
      .isEqualTo(helloWorld)
  }
}

fun oneShotCompress(
  original: ByteString,
  inputOffset: Int = 0,
  inputPadding: Int = 0,
  outputOffset: Int = 0,
  compressionLevel: Int? = null,
  checksumFlag: Int? = null,
  outputArraySize: Int = 1024,
): ByteString {
  val compressor = zstdCompressor()
  try {
    compressionLevel?.let { compressor.setParameter(ZSTD_c_compressionLevel, it).checkError() }
    checksumFlag?.let { compressor.setParameter(ZSTD_c_checksumFlag, it).checkError() }

    val outputArray = ByteArray(outputArraySize)
    val inputArray = ByteArray(original.size + inputOffset + inputPadding)
    original.copyInto(0, inputArray, inputOffset, original.size)

    val input = UnsafeCursor()
    input.data = inputArray
    input.start = inputOffset
    input.end = inputOffset + original.size

    val output = UnsafeCursor()
    output.data = outputArray
    output.start = outputOffset
    output.end = outputArraySize

    val remaining =
      compressor
        .compressStream2(
          outputByteArray = outputArray,
          outputEnd = outputArraySize,
          outputStart = outputOffset,
          inputByteArray = inputArray,
          inputEnd = inputOffset + original.size,
          inputStart = inputOffset,
          mode = ZSTD_e_end,
        )
        .checkError()
    assertThat(remaining).isEqualTo(0)

    return outputArray.toByteString(outputOffset, compressor.outputBytesProcessed)
  } finally {
    compressor.close()
  }
}

fun oneShotDecompress(
  compressed: ByteString,
  inputOffset: Int = 0,
  inputPadding: Int = 0,
  outputOffset: Int = 0,
  outputArraySize: Int = 1024,
): ByteString {
  val decompressor = zstdDecompressor()
  try {
    val outputArray = ByteArray(outputArraySize)
    val inputArray = ByteArray(compressed.size + inputOffset + inputPadding)
    compressed.copyInto(0, inputArray, inputOffset, compressed.size)

    val remaining =
      decompressor
        .decompressStream(
          outputByteArray = outputArray,
          outputEnd = outputArraySize,
          outputStart = outputOffset,
          inputByteArray = inputArray,
          inputEnd = inputOffset + compressed.size,
          inputStart = inputOffset,
        )
        .checkError()
    assertThat(remaining).isEqualTo(0)

    return outputArray.toByteString(outputOffset, decompressor.outputBytesProcessed)
  } finally {
    decompressor.close()
  }
}

internal fun Long.checkError(): Long {
  val errorName = getErrorName(this) ?: return this
  throw IOException(errorName)
}
