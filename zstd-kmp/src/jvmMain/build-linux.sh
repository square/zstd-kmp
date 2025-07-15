#!/bin/bash

function echoRun() {
  echo "$ $*"
  "$@"
  echo ""
}

function build() {
  echoRun mkdir -p build/jni/amd64/
  echoRun mkdir -p zstd-kmp/src/jvmMain/resources/jni/amd64/
  echoRun cmake -S zstd-kmp/src/jvmMain/ \
    -B build/jni/amd64/ \
    -DCMAKE_SYSTEM_PROCESSOR=x86_64
  echoRun cmake --build build/jni/amd64/ --verbose
  echoRun cp -v build/jni/amd64/libzstd-kmp.* zstd-kmp/src/jvmMain/resources/jni/amd64/

  echo "Build complete."
  exit 0
}

# main
build

exit 1
