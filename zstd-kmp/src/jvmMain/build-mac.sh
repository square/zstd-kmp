#!/bin/bash

MACOS_ARCH=""
CMAKE_ARCH=""

function autoDetect() {
  RAW_ARCH=$(uname -m)
  if [[ "$RAW_ARCH" == "arm64" ]]; then
    MACOS_ARCH="aarch64"
    CMAKE_ARCH="arm64"
  elif [[ "$RAW_ARCH" == "x86_64" ]]; then
    MACOS_ARCH="x86_64"
    CMAKE_ARCH="x86_64"
  else
    echo "Unable to detect Mac architecture."
    exit 1
  fi
}

function echoRun() {
  echo "$ $*"
  "$@"
  echo ""
}

function build() {
  echo "MACOS_ARCH=${MACOS_ARCH}"
  echo "CMAKE_ARCH=${CMAKE_ARCH}"
  echo ""

  # Clean the build/jni directory to prevent confusing failure cases
  echoRun rm -rf build/jni/$MACOS_ARCH

  # Build commands extracted from Github Actions
  echoRun mkdir -p build/jni/$MACOS_ARCH/
  echoRun mkdir -p zstd-kmp/src/jvmMain/resources/jni/$MACOS_ARCH/
  echoRun cmake -S zstd-kmp/src/jvmMain/ -B build/jni/$MACOS_ARCH/ \
    -DCMAKE_OSX_ARCHITECTURES=$CMAKE_ARCH
  echoRun cmake --build build/jni/$MACOS_ARCH/ --verbose
  echoRun cp -v build/jni/$MACOS_ARCH/libzstd-kmp.* zstd-kmp/src/jvmMain/resources/jni/$MACOS_ARCH/

  echo "Build complete."
  exit 0
}

# main
autoDetect
build

exit 1
