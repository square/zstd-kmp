# Okio Zstd

Build the native libraries:

```
brew install zig
git submodule init
git submodule update
pushd okio-zstd
zig build -p src/jvmMain/resources/jni
popd
```

Test it:

```
./gradlew okio-zstd:jvmTest
```
