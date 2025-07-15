cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 11)
set(ZSTD_BUILD_STATIC ON)
set(ZSTD_BUILD_SHARED OFF)

add_subdirectory("../../../zstd/build/cmake" "zstd")

project(zstd-kmp)

file(GLOB_RECURSE sources "../../native/*.cpp")

add_library(zstd-kmp SHARED ${sources})

target_include_directories(zstd-kmp PRIVATE "../../../zstd/lib")

target_link_libraries(zstd-kmp libzstd_static)

target_link_options(zstd-kmp PRIVATE "-Wl,-z,max-page-size=16384")
