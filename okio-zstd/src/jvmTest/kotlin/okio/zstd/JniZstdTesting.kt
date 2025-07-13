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

import com.github.luben.zstd.ZstdInputStream
import com.github.luben.zstd.ZstdOutputStream
import okio.Buffer
import okio.ByteString
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import okio.use

/**
 * Decompress this buffer using luben Zstd-jni and return the result.
 *
 * Note that this doesn't use [com.github.luben.zstd.Zstd.decompress] because those functions don't
 * work on data that was compressed in a stream.
 */
actual fun Buffer.referenceDecompress(): ByteString {
  val result = Buffer()
  ZstdInputStream(inputStream()).source().use {
    result.writeAll(it)
  }
  return result.readByteString()
}

/**
 * Compress using Zstd-jni.
 */
actual fun Source.referenceCompress(): Buffer {
  val result = Buffer()
  ZstdOutputStream(result.outputStream()).sink().buffer().use {
    it.writeAll(this@referenceCompress)
  }
  return result
}
