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

import com.squareup.zstd.internal.ZSTD_getErrorName
import com.squareup.zstd.internal.ZSTD_isError
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

actual fun zstdCompressor(): ZstdCompressor = NativeZstdCompressor()

actual fun zstdDecompressor(): ZstdDecompressor = NativeZstdDecompressor()

actual fun getErrorName(code: Long): String? {
  if (ZSTD_isError(code.toULong()) == 0U) return null
  return ZSTD_getErrorName(code.toULong())?.toKString()
}
