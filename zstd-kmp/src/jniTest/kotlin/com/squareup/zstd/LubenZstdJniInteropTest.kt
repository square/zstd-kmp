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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.luben.zstd.Zstd as LubenZstd
import kotlin.test.Test
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import okio.buffer

/**
 * Everyone else uses the Zstd-jni library. Confirm that we can interoperate with it and don't have
 * any runtime collisions on binary symbols.
 *
 * https://github.com/luben/zstd-jni
 */
internal class LubenZstdJniInteropTest {

  @Test
  fun lubenCompressKmpDecompress() {
    val original = "hello world".encodeUtf8()
    val compressed = LubenZstd.compress(original.toByteArray())
    val decompressed = oneShotDecompress(compressed.toByteString())
    assertThat(decompressed).isEqualTo(original)
  }

  @Test
  fun kmpCompressLubenDecompress() {
    val original = "hello world".encodeUtf8()
    val compressed = compress(original)
    val decompressed = compressed.referenceDecompress()
    assertThat(decompressed).isEqualTo(original)
  }

  private fun compress(original: ByteString): Buffer {
    val compressed = Buffer()
    compressed.zstdCompress().buffer().use { sink ->
      sink.write(original)
    }
    return compressed
  }
}
