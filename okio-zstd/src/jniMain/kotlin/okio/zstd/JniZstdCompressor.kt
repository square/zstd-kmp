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

internal class JniZstdCompressor : ZstdCompressor() {
  @JvmField
  var cctxPointer = createZstdCompressor()
    .also {
      if (it == 0L) throw OutOfMemoryError("createZstdCompressor failed")
    }

  override fun setParameter(param: Int, value: Int): Long = setParameter(cctxPointer, param, value)

  override fun compressStream2(
    output: UnsafeCursor,
    input: UnsafeCursor,
    mode: Int,
  ): Long = compressStream2(jniZstdPointer, cctxPointer, output, input, mode)

  override fun close() {
    val cctxPointerToClose = cctxPointer
    if (cctxPointerToClose != 0L) {
      cctxPointer = 0L
      close(cctxPointerToClose)
    }
  }

  private external fun setParameter(cctxPointer: Long, param: Int, value: Int): Long

  private external fun compressStream2(
    jniPointer: Long,
    cctxPointer: Long,
    output: UnsafeCursor,
    input: UnsafeCursor,
    mode: Int,
  ): Long

  private external fun close(cctxPointer: Long)
}
