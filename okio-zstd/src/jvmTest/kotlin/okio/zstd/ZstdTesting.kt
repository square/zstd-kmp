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
import com.github.luben.zstd.ZstdInputStream
import com.github.luben.zstd.ZstdOutputStream
import kotlin.random.Random
import okio.Buffer
import okio.Buffer.UnsafeCursor
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.Source
import okio.Timeout
import okio.buffer
import okio.sink
import okio.source

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

/**
 * Decompress this buffer using Zstd-jni and return the result.
 *
 * Note that this doesn't use [com.github.luben.zstd.Zstd.decompress] because those functions don't
 * work on data that was compressed in a stream.
 */
fun Buffer.lubenDecompress(): ByteString {
  val result = Buffer()
  ZstdInputStream(inputStream()).source().use {
    result.writeAll(it)
  }
  return result.readByteString()
}

/**
 * Compress this source using Zstd-jni and return the result.
 */
fun Source.lubenCompress(): Buffer {
  val result = Buffer()
  ZstdOutputStream(result.outputStream()).sink().buffer().use {
    it.writeAll(this@lubenCompress)
  }
  return result
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
  JniZstdCompressor().use { compressor ->
    compressionLevel?.let {
      compressor.setParameter(ZSTD_c_compressionLevel, it).checkError()
    }
    checksumFlag?.let {
      compressor.setParameter(ZSTD_c_checksumFlag, it).checkError()
    }

    val inputArray = ByteArray(original.size + inputOffset + inputPadding)
    original.copyInto(0, inputArray, inputOffset, original.size)

    val input = UnsafeCursor()
    input.data = inputArray
    input.start = inputOffset
    input.end = inputOffset + original.size

    val output = UnsafeCursor()
    output.data = ByteArray(outputArraySize)
    output.start = outputOffset
    output.end = outputArraySize

    val remaining = compressor.compressStream2(output, input, ZSTD_e_end).checkError()
    assertThat(remaining).isEqualTo(0)

    return output.data!!.toByteString(outputOffset, compressor.outputBytesProcessed)
  }
}

fun oneShotDecompress(
  compressed: ByteString,
  inputOffset: Int = 0,
  inputPadding: Int = 0,
  outputOffset: Int = 0,
  outputArraySize: Int = 1024,
): ByteString {
  JniZstdDecompressor().use { decompressor ->
    val inputArray = ByteArray(compressed.size + inputOffset + inputPadding)
    compressed.copyInto(0, inputArray, inputOffset, compressed.size)

    val input = UnsafeCursor()
    input.data = inputArray
    input.start = inputOffset
    input.end = inputOffset + compressed.size

    val output = UnsafeCursor()
    output.data = ByteArray(outputArraySize)
    output.start = outputOffset
    output.end = outputArraySize

    val remaining = decompressor.decompressStream(output, input).checkError()
    assertThat(remaining).isEqualTo(0)

    return output.data!!.toByteString(outputOffset, decompressor.outputBytesProcessed)
  }
}
