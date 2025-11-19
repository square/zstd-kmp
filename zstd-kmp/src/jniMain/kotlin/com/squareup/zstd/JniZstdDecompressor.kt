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

internal class JniZstdDecompressor : ZstdDecompressor() {
  @JvmField
  var dctxPointer =
    createZstdDecompressor().also {
      if (it == 0L) throw OutOfMemoryError("createZstdDecompressor failed")
    }

  override fun decompressStream(
    outputByteArray: ByteArray,
    outputEnd: Int,
    outputStart: Int,
    inputByteArray: ByteArray,
    inputEnd: Int,
    inputStart: Int,
  ): Long =
    decompressStream(
      jniPointer = jniZstdPointer,
      dctxPointer = dctxPointer,
      outputByteArray = outputByteArray,
      outputEnd = outputEnd,
      outputStart = outputStart,
      inputByteArray = inputByteArray,
      inputEnd = inputEnd,
      inputStart = inputStart,
    )

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
    outputByteArray: ByteArray,
    outputEnd: Int,
    outputStart: Int,
    inputByteArray: ByteArray,
    inputEnd: Int,
    inputStart: Int,
  ): Long

  private external fun close(cctxPointer: Long)
}
