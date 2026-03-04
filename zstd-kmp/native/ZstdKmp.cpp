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
#include <jni.h>
#include <zstd.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_squareup_zstd_JniZstdKt_jniGetErrorName(JNIEnv* env, jobject type, jlong code) {
  auto codeSizeT = static_cast<size_t>(code);
  if (!ZSTD_isError(codeSizeT)) return NULL;
  auto errorString = ZSTD_getErrorName(codeSizeT);
  return env->NewStringUTF(errorString);
}


extern "C" JNIEXPORT jlong JNICALL
Java_com_squareup_zstd_JniZstdKt_createZstdCompressor(JNIEnv* env, jclass type) {
  ZSTD_CCtx* cctx = ZSTD_createCCtx(); // Could be NULL.
  return reinterpret_cast<jlong>(cctx);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_squareup_zstd_JniZstdCompressor_setParameter(JNIEnv* env, jobject type, jlong cctxPointer, jint param, jint value) {
  auto cctx = reinterpret_cast<ZSTD_CCtx*>(cctxPointer);
  return ZSTD_CCtx_setParameter(cctx, static_cast<ZSTD_cParameter>(param), value);
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_squareup_zstd_JniZstdCompressor_compressStream2(JNIEnv* env, jobject type, jlong cctxPointer, jbyteArray outputByteArray, jint outputEnd, jint outputStart, jbyteArray inputByteArray, jint inputEnd, jint inputStart, jint mode) {
  auto cctx = reinterpret_cast<ZSTD_CCtx*>(cctxPointer);

  auto inputByteArrayElements = env->GetByteArrayElements(inputByteArray, NULL);
  ZSTD_inBuffer zstdInput = { inputByteArrayElements, static_cast<size_t>(inputEnd), static_cast<size_t>(inputStart) };

  auto outputByteArrayElements = env->GetByteArrayElements(outputByteArray, NULL);
  ZSTD_outBuffer zstdOutput = { outputByteArrayElements, static_cast<size_t>(outputEnd), static_cast<size_t>(outputStart) };

  size_t result;
  if (inputByteArrayElements != NULL && outputByteArrayElements != NULL) {
    result = ZSTD_compressStream2(cctx, &zstdOutput, &zstdInput,
      static_cast<ZSTD_EndDirective>(mode));
  } else {
    result = -ZSTD_error_GENERIC;
  }

  env->ReleaseByteArrayElements(inputByteArray, inputByteArrayElements, JNI_ABORT);
  env->ReleaseByteArrayElements(outputByteArray, outputByteArrayElements, 0);

  jlong results[3];
  results[0] = static_cast<jlong>(result);
  results[1] = static_cast<jlong>(zstdInput.pos) - inputStart;
  results[2] = static_cast<jlong>(zstdOutput.pos) - outputStart;
  jlongArray resultArray = env->NewLongArray(3);
  env->SetLongArrayRegion(resultArray, 0, 3, results);
  return resultArray;
}

extern "C" JNIEXPORT void JNICALL
Java_com_squareup_zstd_JniZstdCompressor_close(JNIEnv* env, jobject type, jlong cctxPointer) {
  auto cctx = reinterpret_cast<ZSTD_CCtx*>(cctxPointer);
  ZSTD_freeCCtx(cctx);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_squareup_zstd_JniZstdKt_createZstdDecompressor(JNIEnv* env, jclass type) {
  ZSTD_DCtx* dctx = ZSTD_createDCtx(); // Could be NULL.
  return reinterpret_cast<jlong>(dctx);
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_squareup_zstd_JniZstdDecompressor_decompressStream(JNIEnv* env, jobject type, jlong dctxPointer, jbyteArray outputByteArray, jint outputEnd, jint outputStart, jbyteArray inputByteArray, jint inputEnd, jint inputStart) {
  auto dctx = reinterpret_cast<ZSTD_DCtx*>(dctxPointer);

  auto inputByteArrayElements = env->GetByteArrayElements(inputByteArray, NULL);
  ZSTD_inBuffer zstdInput = { inputByteArrayElements, static_cast<size_t>(inputEnd), static_cast<size_t>(inputStart) };

  auto outputByteArrayElements = env->GetByteArrayElements(outputByteArray, NULL);
  ZSTD_outBuffer zstdOutput = { outputByteArrayElements, static_cast<size_t>(outputEnd), static_cast<size_t>(outputStart) };

  size_t result;
  if (inputByteArrayElements != NULL && outputByteArrayElements != NULL) {
    result = ZSTD_decompressStream(dctx, &zstdOutput, &zstdInput);
  } else {
    result = -ZSTD_error_GENERIC;
  }

  env->ReleaseByteArrayElements(inputByteArray, inputByteArrayElements, JNI_ABORT);
  env->ReleaseByteArrayElements(outputByteArray, outputByteArrayElements, 0);

  jlong results[3];
  results[0] = static_cast<jlong>(result);
  results[1] = static_cast<jlong>(zstdInput.pos) - inputStart;
  results[2] = static_cast<jlong>(zstdOutput.pos) - outputStart;
  jlongArray resultArray = env->NewLongArray(3);
  env->SetLongArrayRegion(resultArray, 0, 3, results);
  return resultArray;
}

extern "C" JNIEXPORT void JNICALL
Java_com_squareup_zstd_JniZstdDecompressor_close(JNIEnv* env, jobject type, jlong dctxPointer) {
  auto dctx = reinterpret_cast<ZSTD_DCtx*>(dctxPointer);
  ZSTD_freeDCtx(dctx);
}
