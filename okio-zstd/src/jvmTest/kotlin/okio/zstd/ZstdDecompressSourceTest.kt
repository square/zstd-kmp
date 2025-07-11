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
package okio.zstd

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import com.github.luben.zstd.ZstdOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.ByteString
import okio.EOFException
import okio.IOException
import okio.Pipe
import okio.Source
import okio.buffer
import okio.sink

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
    val compressed = RandomSource(Random(1), byteCount)
      .lubenCompress()

    val decompressed = compressed.zstdDecompress().buffer().use {
      it.readByteString()
    }

    assertThat(decompressed)
      .isEqualTo(RandomSource(Random(1), byteCount).buffer().readByteString())
  }

  @Test
  fun flushedDataIsReadable() {
    val pipe = Pipe(1024L)

    pipe.source.zstdDecompress().buffer().use { source ->
      ZstdOutputStream(pipe.sink.buffer().outputStream()).sink().buffer().use { sink ->
        sink.writeUtf8("hello world")
        sink.flush()

        assertThat(source.readUtf8(11)).isEqualTo("hello world")

        sink.writeUtf8("hello again")
        sink.flush()

        assertThat(source.readUtf8(11)).isEqualTo("hello again")
      }
    }
  }

  /** Confirm we can clean up even if reads aren't working. */
  @Test
  fun readFailure() {
    val delegate = RandomSource(Random(1), 1024 * 1024)
      .lubenCompress()
    var sourceClosed = false
    var explode = false
    val source = object : Source by delegate {
      override fun read(sink: Buffer, byteCount: Long): Long {
        val result = delegate.read(sink, byteCount)
        if (explode) throw IOException("boom!")
        return result
      }

      override fun close() {
        sourceClosed = true
      }
    }

    val decompressor = JniZstdDecompressor()
    val zstdDecompressSource = ZstdDecompressSource(source.buffer(), decompressor)

    explode = true
    assertFailsWith<IOException> {
      zstdDecompressSource.buffer().require(1024L)
    }
    assertThat(zstdDecompressSource.closed).isFalse()
    assertThat(decompressor.dctxPointer).isNotEqualTo(0L)
    assertThat(sourceClosed).isFalse()

    zstdDecompressSource.close()
    assertThat(zstdDecompressSource.closed).isTrue()
    assertThat(decompressor.dctxPointer).isEqualTo(0L)
    assertThat(sourceClosed).isTrue()
  }

  @Test
  fun sourceIsTruncated() {
    val compressed = RandomSource(Random(1), 1024)
      .lubenCompress()
    val truncated = Buffer()
    truncated.write(compressed, compressed.size - 1L)

    truncated.zstdDecompress().buffer().use {
      assertFailsWith<EOFException> {
        it.readByteString()
      }
    }
  }

  @Test
  fun sourceIsEmpty() {
    Buffer().zstdDecompress().buffer().use {
      assertThat(it.readByteString()).isEqualTo(ByteString.EMPTY)
    }
  }
}
