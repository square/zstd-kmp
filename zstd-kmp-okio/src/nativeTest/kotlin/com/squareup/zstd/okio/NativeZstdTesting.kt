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

import okio.Buffer
import okio.ByteString
import okio.Source
import okio.buffer
import okio.use

actual fun Buffer.referenceDecompress(): ByteString {
  zstdDecompress().buffer().use {
    return it.readByteString()
  }
}

actual fun Source.referenceCompress(): Buffer {
  val result = Buffer()
  result.zstdCompress().buffer().use {
    it.writeAll(this@referenceCompress)
  }
  return result
}
