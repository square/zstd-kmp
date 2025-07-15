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
package com.squareup.zstd.okio

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.random.Random
import kotlin.test.Test
import okio.Buffer
import okio.buffer
import okio.use

class ZstdCompressSinkTest {
  @Test
  fun compressEmpty() {
    testRoundTrip(0)
  }

  @Test
  fun compressSingleSegment() {
    testRoundTrip(1024)
  }

  @Test
  fun compressMultipleSegments() {
    testRoundTrip(1024 * 1024)
  }

  private fun testRoundTrip(byteCount: Int) {
    val compressed = Buffer()
    compressed.zstdCompress().buffer().use {
      it.writeAll(RandomSource(Random(1), byteCount))
    }
    assertThat(compressed.referenceDecompress())
      .isEqualTo(RandomSource(Random(1), byteCount).buffer().readByteString())
  }
}
