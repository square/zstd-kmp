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

import okio.Buffer.UnsafeCursor

internal class JniZstdDecompressor : ZstdDecompressor() {
  @JvmField
  var dctxPointer = createZstdDecompressor()
    .also {
      if (it == 0L) throw OutOfMemoryError("createZstdDecompressor failed")
    }

  override fun decompressStream(
    output: UnsafeCursor,
    input: UnsafeCursor,
  ): Long = decompressStream(jniZstdPointer, dctxPointer, output, input)

  override fun close() {
    val cctxPointerToClose = dctxPointer
    if (cctxPointerToClose != 0L) {
      dctxPointer = 0L
      close(cctxPointerToClose)
    }
  }

  private external fun decompressStream(
    jniPointer: Long,
    dctxPointer: Long,
    output: UnsafeCursor,
    input: UnsafeCursor,
  ): Long

  private external fun close(cctxPointer: Long)
}
