# Keep classes with native methods so R8 doesn't rename them.
# JNI function names in the native library are derived from the fully-qualified
# class and method names, so renaming would break the JNI linkage.
-keepclasseswithmembers class com.squareup.zstd.** {
    native <methods>;
}

