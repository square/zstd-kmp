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

/**
 * Support for operating on JVM objects from native code.
 *
 * Pass a pointer to this to all JNI functions that operate on JVM objects.
 */
class JniZstd {
public:
  JniZstd(JNIEnv *env, jclass zstdCompressorClass, jclass zstdDecompressorClass);

  jfieldID zstdCompressorOutputBytesProcessed;
  jfieldID zstdCompressorInputBytesProcessed;
  jfieldID zstdDecompressorOutputBytesProcessed;
  jfieldID zstdDecompressorInputBytesProcessed;
};

JniZstd::JniZstd(JNIEnv *env, jclass zstdCompressorClass, jclass zstdDecompressorClass)
  : zstdCompressorOutputBytesProcessed(env->GetFieldID(zstdCompressorClass, "outputBytesProcessed", "I")),
    zstdCompressorInputBytesProcessed(env->GetFieldID(zstdCompressorClass, "inputBytesProcessed", "I")),
    zstdDecompressorOutputBytesProcessed(env->GetFieldID(zstdDecompressorClass, "outputBytesProcessed", "I")),
    zstdDecompressorInputBytesProcessed(env->GetFieldID(zstdDecompressorClass, "inputBytesProcessed", "I")) {
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_squareup_zstd_JniZstdKt_jniGetErrorName(JNIEnv* env, jobject type, jlong code) {
  auto codeSizeT = static_cast<size_t>(code);
  if (!ZSTD_isError(codeSizeT)) return NULL;
  auto errorString = ZSTD_getErrorName(codeSizeT);
  return env->NewStringUTF(errorString);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_squareup_zstd_JniZstdKt_createJniZstd(JNIEnv* env, jclass type) {
  auto zstdCompressorClass = env->FindClass("com/squareup/zstd/ZstdCompressor");
  auto zstdDecompressorClass = env->FindClass("com/squareup/zstd/ZstdDecompressor");
  auto jniZstd = new JniZstd(env, zstdCompressorClass, zstdDecompressorClass);
  return reinterpret_cast<jlong>(jniZstd);
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

extern "C" JNIEXPORT jlong JNICALL
Java_com_squareup_zstd_JniZstdCompressor_compressStream2(JNIEnv* env, jobject type, jlong jniZstdPointer, jlong cctxPointer, jbyteArray outputByteArray, jint outputEnd, jint outputStart, jbyteArray inputByteArray, jint inputEnd, jint inputStart, jint mode) {
  auto jniZstd = reinterpret_cast<JniZstd*>(jniZstdPointer);
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

  env->SetIntField(type, jniZstd->zstdCompressorOutputBytesProcessed, static_cast<jint>(zstdOutput.pos) - outputStart);
  env->SetIntField(type, jniZstd->zstdCompressorInputBytesProcessed, static_cast<jint>(zstdInput.pos) - inputStart);

  env->ReleaseByteArrayElements(inputByteArray, inputByteArrayElements, JNI_ABORT);
  env->ReleaseByteArrayElements(outputByteArray, outputByteArrayElements, 0);

  return result;
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

extern "C" JNIEXPORT jlong JNICALL
Java_com_squareup_zstd_JniZstdDecompressor_decompressStream(JNIEnv* env, jobject type, jlong jniZstdPointer, jlong dctxPointer, jbyteArray outputByteArray, jint outputEnd, jint outputStart, jbyteArray inputByteArray, jint inputEnd, jint inputStart) {
  auto jniZstd = reinterpret_cast<JniZstd*>(jniZstdPointer);
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

  env->SetIntField(type, jniZstd->zstdDecompressorOutputBytesProcessed, static_cast<jint>(zstdOutput.pos) - outputStart);
  env->SetIntField(type, jniZstd->zstdDecompressorInputBytesProcessed, static_cast<jint>(zstdInput.pos) - inputStart);

  env->ReleaseByteArrayElements(inputByteArray, inputByteArrayElements, JNI_ABORT);
  env->ReleaseByteArrayElements(outputByteArray, outputByteArrayElements, 0);

  return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_squareup_zstd_JniZstdDecompressor_close(JNIEnv* env, jobject type, jlong dctxPointer) {
  auto dctx = reinterpret_cast<ZSTD_DCtx*>(dctxPointer);
  ZSTD_freeDCtx(dctx);
}
