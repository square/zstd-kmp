# Change Log

## Unreleased

 * Fix: Don't ship an unused copy of `libzstd.so` in our Android APKs. We were inadvertently
   packaging our Android `.aar` libraries with both `libzstd-kmp.so` and `libzstd.so`, but only
   ever using the first one.


## Version 0.3.0

_2025-07-18_

 * Fix: Support JDK back to version 8.
 * Fix: Support Windows for JVM and Kotlin/Native.
 * Upgrade: [Okio 3.15.0][okio_3_15_0].


## Version 0.2.0

_2025-07-15_

 * Initial public release.


[okio_3_15_0]: https://square.github.io/okio/changelog/#version-3150
