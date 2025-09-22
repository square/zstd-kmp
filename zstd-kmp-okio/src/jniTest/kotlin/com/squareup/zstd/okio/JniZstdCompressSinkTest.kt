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
@file:Suppress(
  "CANNOT_OVERRIDE_INVISIBLE_MEMBER",
  "INVISIBLE_MEMBER",
  "INVISIBLE_REFERENCE",
)

package com.squareup.zstd.okio

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import com.github.luben.zstd.ZstdInputStream
import com.squareup.zstd.JniZstdCompressor
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.IOException
import okio.Pipe
import okio.Sink
import okio.buffer
import okio.source
import okio.use

class JniZstdCompressSinkTest {
  /** Confirm we can clean up even if writes aren't working. */
  @Test
  fun writeFailure() {
    val delegate = Buffer()
    var sinkClosed = false
    val sink = object : Sink by delegate {
      override fun write(source: Buffer, byteCount: Long) {
        delegate.write(source, byteCount)
        throw IOException("boom!")
      }

      override fun close() {
        delegate.close()
        sinkClosed = true
      }
    }

    val compressor = JniZstdCompressor()
    val zstdCompressSink = ZstdCompressSink(sink.buffer(), compressor)

    assertFailsWith<IOException> {
      zstdCompressSink.buffer().write(Random(1).nextBytes(1024 * 1024))
    }
    assertThat(zstdCompressSink.closed).isFalse()
    assertThat(compressor.cctxPointer).isNotEqualTo(0L)
    assertThat(sinkClosed).isFalse()

    assertFailsWith<IOException> {
      zstdCompressSink.close()
    }
    assertThat(zstdCompressSink.closed).isTrue()
    assertThat(compressor.cctxPointer).isEqualTo(0L)
    assertThat(sinkClosed).isTrue()
  }

  @Test
  fun flushMakesDataImmediatelyReadable() {
    val pipe = Pipe(1024L)
    pipe.source.timeout().timeout(250, TimeUnit.MILLISECONDS)

    ZstdInputStream(pipe.source.buffer().inputStream()).source().buffer().use { source ->
      pipe.sink.zstdCompress().buffer().use { sink ->
        sink.writeUtf8("hello world")
        sink.flush()

        assertThat(source.readUtf8(11)).isEqualTo("hello world")
      }
    }
  }

  @Test
  fun closeMakesDataImmediatelyReadable() {
    val pipe = Pipe(1024L)
    pipe.source.timeout().timeout(250, TimeUnit.MILLISECONDS)

    ZstdInputStream(pipe.source.buffer().inputStream()).source().buffer().use { source ->
      pipe.sink.zstdCompress().buffer().use { sink ->
        sink.writeUtf8("hello world")
      }

      assertThat(source.readUtf8(11)).isEqualTo("hello world")
    }
  }

  @Test
  fun dataIsNotReadableUntilFlush() {
    val pipe = Pipe(1024L)
    pipe.source.timeout().timeout(250, TimeUnit.MILLISECONDS)

    ZstdInputStream(pipe.source.buffer().inputStream()).source().buffer().use { source ->
      pipe.sink.zstdCompress().buffer().use { sink ->
        sink.writeUtf8("hello world")

        assertFailsWith<InterruptedIOException> {
          source.readUtf8(11)
        }

        sink.flush()
        assertThat(source.readUtf8(11)).isEqualTo("hello world")
      }
    }
  }

  @Test
  fun dataIsNotReadableUntilClose() {
    val pipe = Pipe(1024L)
    pipe.source.timeout().timeout(250, TimeUnit.MILLISECONDS)

    ZstdInputStream(pipe.source.buffer().inputStream()).source().buffer().use { source ->
      pipe.sink.zstdCompress().buffer().use { sink ->
        sink.writeUtf8("hello world")

        assertFailsWith<InterruptedIOException> {
          source.readUtf8(11)
        }
      }
      assertThat(source.readUtf8(11)).isEqualTo("hello world")
    }
  }
}
