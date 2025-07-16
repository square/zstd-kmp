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
@file:JvmName("OkioZstd")

package com.squareup.zstd.okio

import com.squareup.zstd.zstdCompressor
import com.squareup.zstd.zstdDecompressor
import kotlin.jvm.JvmName
import okio.Sink
import okio.Source
import okio.buffer

/** Returns a [Sink] that compresses its data with ZStandard before forwarding to this. */
fun Sink.zstdCompress(): Sink = ZstdCompressSink(this.buffer(), zstdCompressor())

/** Returns a [Source] that decompresses its data with ZStandard after reading from this. */
fun Source.zstdDecompress(): Source = ZstdDecompressSource(this.buffer(), zstdDecompressor())

internal val emptyByteArray = ByteArray(0)
