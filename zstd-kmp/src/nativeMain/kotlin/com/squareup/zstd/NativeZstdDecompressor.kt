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
@file:OptIn(ExperimentalForeignApi::class)

package com.squareup.zstd

import com.squareup.zstd.internal.ZSTD_createDCtx
import com.squareup.zstd.internal.ZSTD_decompressStream
import com.squareup.zstd.internal.ZSTD_freeDCtx
import com.squareup.zstd.internal.ZSTD_inBuffer
import com.squareup.zstd.internal.ZSTD_outBuffer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned

internal class NativeZstdDecompressor : ZstdDecompressor() {
  private var dctx = ZSTD_createDCtx()
    .also {
      if (it == null) throw OutOfMemoryError("ZSTD_createDCtx failed")
    }

  override fun decompressStream(
    outputByteArray: ByteArray,
    outputEnd: Int,
    outputStart: Int,
    inputByteArray: ByteArray,
    inputEnd: Int,
    inputStart: Int,
  ): Long {
    memScoped {
      outputByteArray.usePinned { outputDataPinned ->
        inputByteArray.usePinned { inputDataPinned ->
          val outputStart = outputStart.toULong()
          val outputEnd = outputEnd.toULong()
          val zstdOutput = alloc<ZSTD_outBuffer>()
          zstdOutput.dst = outputDataPinned.addressOf(0)
          zstdOutput.pos = outputStart
          zstdOutput.size = outputEnd

          val inputStart = inputStart.toULong()
          val inputEnd = inputEnd.toULong()
          val zstdInput = alloc<ZSTD_inBuffer>()
          zstdInput.src = inputDataPinned.addressOf(0)
          zstdInput.pos = inputStart
          zstdInput.size = inputEnd

          val result = ZSTD_decompressStream(
            zds = dctx,
            output = zstdOutput.ptr,
            input = zstdInput.ptr,
          ).toLong()

          outputBytesProcessed = (zstdOutput.pos - outputStart).toInt()
          inputBytesProcessed = (zstdInput.pos - inputStart).toInt()

          return result
        }
      }
    }
  }

  override fun close() {
    val dctxToClose = dctx ?: return
    dctx = null
    ZSTD_freeDCtx(dctxToClose)
  }
}
