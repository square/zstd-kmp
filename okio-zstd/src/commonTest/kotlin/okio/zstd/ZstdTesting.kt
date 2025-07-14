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
import kotlin.random.Random
import okio.Buffer
import okio.Buffer.UnsafeCursor
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.Source
import okio.Timeout
import okio.use

/** Returns [byteCount] bytes from [random]. */
class RandomSource(
  val random: Random,
  byteCount: Int,
) : Source {
  private var remaining = byteCount.toLong()

  override fun read(sink: Buffer, byteCount: Long): Long {
    val toRead = minOf(byteCount, remaining)
    if (toRead == 0L) return -1L
    sink.write(random.nextBytes(toRead.toInt()))
    remaining -= toRead
    return toRead
  }

  override fun timeout() = Timeout.Companion.NONE

  override fun close() {
  }
}

/** Decompress using a different implementation, where available. */
expect fun Buffer.referenceDecompress(): ByteString

/** Compress using a different implementation, where available. */
expect fun Source.referenceCompress(): Buffer

fun oneShotCompress(
  original: ByteString,
  inputOffset: Int = 0,
  inputPadding: Int = 0,
  outputOffset: Int = 0,
  compressionLevel: Int? = null,
  checksumFlag: Int? = null,
  outputArraySize: Int = 1024,
): ByteString {
  zstdCompressor().use { compressor ->
    compressionLevel?.let {
      compressor.setParameter(ZSTD_c_compressionLevel, it).checkError()
    }
    checksumFlag?.let {
      compressor.setParameter(ZSTD_c_checksumFlag, it).checkError()
    }

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

    val remaining = compressor.compressStream2(
      outputByteArray = outputArray,
      outputEnd = outputArraySize,
      outputStart = outputOffset,
      inputByteArray = inputArray,
      inputEnd = inputOffset + original.size,
      inputStart = inputOffset,
      mode = ZSTD_e_end,
    ).checkError()
    assertThat(remaining).isEqualTo(0)

    return outputArray.toByteString(outputOffset, compressor.outputBytesProcessed)
  }
}

fun oneShotDecompress(
  compressed: ByteString,
  inputOffset: Int = 0,
  inputPadding: Int = 0,
  outputOffset: Int = 0,
  outputArraySize: Int = 1024,
): ByteString {
  zstdDecompressor().use { decompressor ->
    val outputArray = ByteArray(outputArraySize)
    val inputArray = ByteArray(compressed.size + inputOffset + inputPadding)
    compressed.copyInto(0, inputArray, inputOffset, compressed.size)

    val remaining = decompressor.decompressStream(
      outputByteArray = outputArray,
      outputEnd = outputArraySize,
      outputStart = outputOffset,
      inputByteArray = inputArray,
      inputEnd = inputOffset + compressed.size,
      inputStart = inputOffset,
    ).checkError()
    assertThat(remaining).isEqualTo(0)

    return outputArray.toByteString(outputOffset, decompressor.outputBytesProcessed)
  }
}
