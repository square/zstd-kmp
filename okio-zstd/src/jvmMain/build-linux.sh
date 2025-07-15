#!/bin/bash

function echoRun() {
  echo "$ $*"
  "$@"
  echo ""
}

function build() {
  echoRun mkdir -p build/jni/amd64/
  echoRun mkdir -p okio-zstd/src/jvmMain/resources/jni/amd64/
  echoRun cmake -S okio-zstd/src/jvmMain/ \
    -B build/jni/amd64/ \
    -DCMAKE_SYSTEM_PROCESSOR=x86_64
  echoRun cmake --build build/jni/amd64/ --verbose
  echoRun cp -v build/jni/amd64/libokio-zstd.* okio-zstd/src/jvmMain/resources/jni/amd64/

  echo "Build complete."
  exit 0
}

# main
build

exit 1
