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
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.EOFException
import okio.IOException
import okio.buffer
import okio.use

class ZstdDecompressSourceTest {
  @Test
  fun decompressEmpty() {
    testRoundTrip(0)
  }

  @Test
  fun decompressSingleSegment() {
    testRoundTrip(1024)
  }

  @Test
  fun decompressMultipleSegments() {
    testRoundTrip(1024 * 1024)
  }

  private fun testRoundTrip(byteCount: Int) {
    val compressed = Random(1).nextBytes(byteCount)
      .referenceCompress()

    val decompressed = compressed.zstdDecompress().buffer().use {
      it.readByteString()
    }

    assertThat(decompressed)
      .isEqualTo(Random(1).nextBytes(byteCount).toByteString())
  }

  @Test
  fun sourceIsTruncated() {
    val compressed = Random(1).nextBytes(1024)
      .referenceCompress()
    val truncated = Buffer()
    truncated.write(compressed, compressed.size - 1L)

    truncated.zstdDecompress().buffer().use {
      assertFailsWith<EOFException> {
        it.readByteString()
      }
    }
  }

  @Test
  fun sourceIsMalformed() {
    val truncated = Buffer()
    truncated.writeUtf8("this is not zstd data")

    truncated.zstdDecompress().buffer().use {
      val e = assertFailsWith<IOException> {
        it.readByteString()
      }
      assertThat(e).hasMessage("zstd decompress failed: Unknown frame descriptor")
    }
  }

  @Test
  fun sourceIsEmpty() {
    Buffer().zstdDecompress().buffer().use {
      assertThat(it.readByteString()).isEqualTo(ByteString.EMPTY)
    }
  }
}
