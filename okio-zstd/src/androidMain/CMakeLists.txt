cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 11)
set(ZSTD_BUILD_STATIC ON)
set(ZSTD_BUILD_SHARED OFF)

add_subdirectory("../../../zstd/build/cmake" "zstd")

project(okio-zstd)

file(GLOB_RECURSE sources "../../native/*.cpp")

add_library(okio-zstd SHARED ${sources})

target_include_directories(okio-zstd PRIVATE "../../../zstd/lib")

target_link_libraries(okio-zstd libzstd_static)

target_link_options(okio-zstd PRIVATE "-Wl,-z,max-page-size=16384")
