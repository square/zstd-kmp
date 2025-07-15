# Zstd-KMP

The [ZStandard compression library (zstd)][zstd], packaged for [Kotlin multiplatform (kmp)][kmp].


## Building

# Git Submodules

```
git submodule init
git submodule update
```

# cmake

```
brew install cmake
```

# Build JNI libraries on macOS

```
./zstd-kmp/src/jvmMain/build-mac.sh
```

# Build JNI libraries on Linux

```
./zstd-kmp/src/jvmMain/build-linux.sh
```

# Test it:

```
./gradlew \
  zstd-kmp:jvmTest \
  zstd-kmp-okio:jvmTest \
  zstd-kmp:macosArm64Test \
  zstd-kmp-okio:macosArm64Test
```

[kmp]: https://kotlinlang.org/docs/multiplatform.html
[zstd]: https://github.com/facebook/zstd
