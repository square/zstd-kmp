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

package okio.zstd

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import okio.Buffer.UnsafeCursor
import okio.zstd.internal.ZSTD_createDCtx
import okio.zstd.internal.ZSTD_decompressStream
import okio.zstd.internal.ZSTD_freeDCtx
import okio.zstd.internal.ZSTD_inBuffer
import okio.zstd.internal.ZSTD_outBuffer

internal class NativeZstdDecompressor : ZstdDecompressor() {
  private var dctx = ZSTD_createDCtx()
    .also {
      if (it == null) throw OutOfMemoryError("ZSTD_createDCtx failed")
    }

  override fun decompressStream(
    output: UnsafeCursor,
    input: UnsafeCursor,
  ): Long {
    memScoped {
      output.data!!.usePinned { outputDataPinned ->
        input.data!!.usePinned { inputDataPinned ->
          val outputStart = output.start.toULong()
          val outputEnd = output.end.toULong()
          val zstdOutput = alloc<ZSTD_outBuffer>()
          zstdOutput.dst = outputDataPinned.addressOf(0)
          zstdOutput.pos = outputStart
          zstdOutput.size = outputEnd

          val inputStart = input.start.toULong()
          val inputEnd = input.end.toULong()
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
