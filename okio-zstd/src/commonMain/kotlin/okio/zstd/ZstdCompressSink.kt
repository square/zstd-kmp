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

import kotlin.jvm.JvmField
import okio.Buffer
import okio.Buffer.UnsafeCursor
import okio.BufferedSink
import okio.IOException
import okio.Sink
import okio.Timeout
import okio.use

/**
 * This stages all written bytes to [inputBuffer], and then emits to [sink] on these triggers:
 *
 *  * When [Buffer.completeSegmentByteCount] is greater than 0.
 *  * On [flush].
 *  * On [close].
 *
 * This emits directly to [BufferedSink.buffer]. To avoid that from exhausting memory, this always
 * calls [BufferedSink.emitCompleteSegments] after writing.
 */
internal class ZstdCompressSink internal constructor(
  /** The destination for our compressed data. We output directly to this sink's buffer. */
  @JvmField val sink: BufferedSink,
  private val compressor: ZstdCompressor,
) : Sink {
  /**
   * Raw data written to this sink, and not yet emitted. We emit on [flush], [close], and when
   * [Buffer.completeSegmentByteCount] is non-zero.
   */
  private val inputBuffer = Buffer()

  private val inputCursor = UnsafeCursor()
  private val outputCursor = UnsafeCursor()

  @JvmField var closed = false

  @Throws(IOException::class)
  override fun write(source: Buffer, byteCount: Long) {
    check(!closed) { "closed" }

    inputBuffer.write(source, byteCount)
    compress(ZSTD_e_continue)
  }

  @Throws(IOException::class)
  override fun flush() {
    check(!closed) { "closed" }

    compress(ZSTD_e_flush)
    sink.flush()
  }

  @Throws(IOException::class)
  override fun close() {
    if (closed) return
    closed = true

    sink.use {
      compressor.use {
        compress(ZSTD_e_end)
      }
    }
  }

  private fun compress(mode: Int) {
    // Decide how many bytes to write immediately.
    var inputRemaining = when (mode) {
      ZSTD_e_continue -> {
        inputBuffer.completeSegmentByteCount()
          .also { if (it == 0L) return@compress } // No bytes to write immediately.
      }

      else -> inputBuffer.size
    }

    do {
      var result: Long
      sink.buffer.readAndWriteUnsafe(outputCursor).use { outputCursor ->
        val outputSizeBefore = sink.buffer.size
        outputCursor.expandBuffer(1)

        if (inputRemaining > 0L) {
          // Compress some input. This might not produce any output!
          inputBuffer.readUnsafe(inputCursor).use { inputCursor ->
            inputCursor.next()
            result = compressor.compressStream2(outputCursor, inputCursor, mode)
          }
          inputRemaining -= compressor.inputBytesProcessed
          inputBuffer.skip(compressor.inputBytesProcessed.toLong())
        } else {
          // No more input, but possibly more output.
          inputCursor.data = emptyByteArray
          inputCursor.start = 0
          inputCursor.end = 0
          result = compressor.compressStream2(outputCursor, inputCursor, mode)
        }

        outputCursor.resizeBuffer(outputSizeBefore + compressor.outputBytesProcessed)
      }

      sink.emitCompleteSegments()
      result.checkError()

      val finished = when (mode) {
        ZSTD_e_continue -> inputRemaining == 0L
        else -> inputRemaining == 0L && result == 0L
      }
    } while (!finished)
  }

  override fun timeout(): Timeout = sink.timeout()
}
