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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8

/**
 * Exercise the native bindings.
 */
internal class ZstdTest {
  private val helloWorld = "hello world".encodeUtf8()
  private val helloWorldZstd = "28b52ffd200b59000068656c6c6f20776f726c64".decodeHex()

  @Test
  fun getErrorName() {
    assertThat(getErrorName(ZSTD_error_no_error)).isNull()
    assertThat(getErrorName(ZSTD_error_GENERIC))
      .isEqualTo("Error (generic)")
  }

  @Test
  fun compress() {
    assertThat(oneShotCompress(helloWorld)).isEqualTo(helloWorldZstd)
  }

  @Test
  fun customOptions() {
    assertThat(
      oneShotCompress(
        original = helloWorld,
        compressionLevel = 7,
      ),
    ).isEqualTo("28b52ffd200b59000068656c6c6f20776f726c64".decodeHex())

    assertThat(
      oneShotCompress(
        original = helloWorld,
        checksumFlag = 1,
      ),
    ).isEqualTo("28b52ffd240b59000068656c6c6f20776f726c6468691eb2".decodeHex())

    assertThat(
      oneShotCompress(
        original = helloWorld,
        compressionLevel = 7,
        checksumFlag = 1,
      ),
    ).isEqualTo("28b52ffd240b59000068656c6c6f20776f726c6468691eb2".decodeHex())
  }

  @Test
  fun decompress() {
    assertThat(oneShotDecompress(helloWorldZstd)).isEqualTo(helloWorld)
  }

  @Test
  fun compressWithOffsets() {
    assertThat(
      oneShotCompress(
        original = helloWorld,
        inputOffset = 5,
        inputPadding = 7,
        outputOffset = 9,
      ),
    ).isEqualTo(helloWorldZstd)
  }

  @Test
  fun decompressWithOffsets() {
    assertThat(
      oneShotDecompress(
        compressed = helloWorldZstd,
        inputOffset = 5,
        inputPadding = 7,
        outputOffset = 9,
      ),
    ).isEqualTo(helloWorld)
  }
}
