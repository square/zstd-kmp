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
import okio.zstd.internal.ZSTD_CCtx_setParameter
import okio.zstd.internal.ZSTD_compressStream2
import okio.zstd.internal.ZSTD_createCCtx
import okio.zstd.internal.ZSTD_freeCCtx
import okio.zstd.internal.ZSTD_inBuffer
import okio.zstd.internal.ZSTD_outBuffer

internal class NativeZstdCompressor : ZstdCompressor() {
  private var cctx = ZSTD_createCCtx()
    .also {
      if (it == null) throw OutOfMemoryError("ZSTD_createCCtx failed")
    }

  override fun setParameter(param: Int, value: Int): Long = ZSTD_CCtx_setParameter(cctx, param.toUInt(), value).toLong()

  override fun compressStream2(
    output: UnsafeCursor,
    input: UnsafeCursor,
    mode: Int,
  ): Long {
    memScoped {
      output.data!!.usePinned { outputDataPinned ->
        input.data!!.usePinned { inputDataPinned ->
          val outputStart = output.start.toULong()
          val outputEnd = output.end.toULong()
          val zstdOutput = alloc<ZSTD_outBuffer>()
          zstdOutput.dst = when {
            outputStart < outputEnd -> outputDataPinned.addressOf(0)
            else -> null
          }
          zstdOutput.pos = outputStart
          zstdOutput.size = outputEnd

          val inputStart = input.start.toULong()
          val inputEnd = input.end.toULong()
          val zstdInput = alloc<ZSTD_inBuffer>()
          zstdInput.src = when {
            inputStart < inputEnd -> inputDataPinned.addressOf(0)
            else -> null
          }
          zstdInput.pos = inputStart
          zstdInput.size = inputEnd

          val result = ZSTD_compressStream2(
            cctx = cctx,
            output = zstdOutput.ptr,
            input = zstdInput.ptr,
            endOp = mode.toUInt(),
          ).toLong()

          outputBytesProcessed = (zstdOutput.pos - outputStart).toInt()
          inputBytesProcessed = (zstdInput.pos - inputStart).toInt()

          return result
        }
      }
    }
  }

  override fun close() {
    val cctxToClose = cctx ?: return
    cctx = null
    ZSTD_freeCCtx(cctxToClose)
  }
}
