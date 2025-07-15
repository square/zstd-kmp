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
@file:Suppress("ktlint:standard:property-naming")
@file:JvmMultifileClass
@file:JvmName("Zstd")

package com.squareup.zstd

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

// From ZSTD_EndDirective in zstd.h.
const val ZSTD_e_continue = 0
const val ZSTD_e_flush = 1
const val ZSTD_e_end = 2

expect fun getErrorName(code: Long): String?

/** Returns a new compressor. The caller must close it. */
expect fun zstdCompressor(): ZstdCompressor

/** Returns a new decompressor. The caller must close it. */
expect fun zstdDecompressor(): ZstdDecompressor
