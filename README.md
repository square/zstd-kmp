# Okio Zstd

Build the native libraries:

# Git Submodules

```
git submodule init
git submodule update
```

# cmake

```
brew install cmake
```

# macOS

```
./zstd-kmp/src/jvmMain/build-mac.sh
```

# Linux

```
./zstd-kmp/src/jvmMain/build-linux.sh
```

# Test it:

```
./gradlew zstd-kmp:jvmTest
```
