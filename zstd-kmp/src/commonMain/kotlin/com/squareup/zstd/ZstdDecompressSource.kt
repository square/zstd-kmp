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

import kotlin.jvm.JvmField
import okio.Buffer
import okio.Buffer.UnsafeCursor
import okio.BufferedSource
import okio.EOFException
import okio.Source
import okio.Timeout
import okio.use

/**
 * This satisfies reads with the following process:
 *
 *  1. Decompresses at least 1 byte from [source] into [outputBuffer].
 *  2. Serves the read request from [outputBuffer].
 *
 * Each attempt to decompress returns 0 if the frame is complete, and non-zero otherwise. This
 * tracks that most recent result so it can throw [EOFException] if the source is exhausted
 * mid-frame.
 */
internal class ZstdDecompressSource internal constructor(
  @JvmField val source: BufferedSource,
  private val decompressor: ZstdDecompressor,
) : Source {
  /** Compressed input. */
  private val inputCursor = UnsafeCursor()

  /** Decompressed output. */
  private val outputBuffer = Buffer()
  private val outputCursor = UnsafeCursor()

  private var lastDecompressResult = 0L

  @JvmField
  var closed = false

  override fun read(sink: Buffer, byteCount: Long): Long {
    require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
    check(!closed) { "closed" }
    if (byteCount == 0L) return 0L

    refillIfNecessary()
    return outputBuffer.read(sink, byteCount)
  }

  override fun close() {
    if (closed) return
    closed = true

    outputBuffer.clear()

    source.use {
      decompressor.use {
      }
    }
  }

  private fun refillIfNecessary() {
    while (outputBuffer.exhausted()) {
      // Attempt to load more data into source.buffer.
      if (!source.request(1L)) {
        if (lastDecompressResult != 0L) throw EOFException("EOF before end of stream")
        return
      }

      // Decompress from source.buffer into outputBuffer.
      var result: Long
      outputBuffer.readAndWriteUnsafe(outputCursor).use { outputCursor ->
        val outputSizeBefore = outputBuffer.size
        outputCursor.expandBuffer(1)

        source.buffer.readUnsafe(inputCursor).use { inputCursor ->
          inputCursor.next()
          result = decompressor.decompressStream(
            outputByteArray = outputCursor.data!!,
            outputEnd = outputCursor.end,
            outputStart = outputCursor.start,
            inputByteArray = inputCursor.data!!,
            inputEnd = inputCursor.end,
            inputStart = inputCursor.start,
          )
        }
        source.skip(decompressor.inputBytesProcessed.toLong())

        outputCursor.resizeBuffer(outputSizeBefore + decompressor.outputBytesProcessed)
      }

      lastDecompressResult = result
      result.checkError()
    }
  }

  override fun timeout(): Timeout = source.timeout()
}
