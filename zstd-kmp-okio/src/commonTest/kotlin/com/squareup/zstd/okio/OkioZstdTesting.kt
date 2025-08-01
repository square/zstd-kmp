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

import kotlin.random.Random
import okio.Buffer
import okio.ByteString
import okio.Source
import okio.Timeout

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
