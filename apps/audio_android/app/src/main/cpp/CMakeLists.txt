cmake_minimum_required(VERSION 3.22.1)
project(audio_android_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

get_filename_component(
    WAVEBITS_ROOT
    "${CMAKE_CURRENT_LIST_DIR}/../../../../../../"
    ABSOLUTE
)

add_subdirectory(
    "${WAVEBITS_ROOT}/libs/audio_core"
    "${CMAKE_CURRENT_BINARY_DIR}/deps/audio_core"
)
add_subdirectory(
    "${WAVEBITS_ROOT}/libs/audio_api"
    "${CMAKE_CURRENT_BINARY_DIR}/deps/audio_api"
)

add_library(audio_android_jni SHARED
    jni_bridge.cpp
)
target_include_directories(audio_android_jni
    PRIVATE
        "${WAVEBITS_ROOT}/libs/audio_api/include"
)
target_link_libraries(audio_android_jni
    PRIVATE
        bag_api
        android
        log
)
