cmake_minimum_required(VERSION 3.22.1)
project(audio_android_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(audio_core_fsk STATIC
    ../../../../../../apps/audio_core/core/src/common/version.cpp
    ../../../../../../apps/audio_core/core/src/pipeline/pipeline.cpp
    ../../../../../../apps/audio_core/core/src/fsk/fsk_codec.cpp
)
target_include_directories(audio_core_fsk
    PUBLIC
        ../../../../../../apps/audio_core/core/include
)

add_library(audio_core_api STATIC
    ../../../../../../apps/audio_core/api/src/bag_api.cpp
)
target_include_directories(audio_core_api
    PUBLIC
        ../../../../../../apps/audio_core/api/include
        ../../../../../../apps/audio_core/core/include
)
target_link_libraries(audio_core_api
    PUBLIC
        audio_core_fsk
)

add_library(audio_android_jni SHARED
    jni_bridge.cpp
)
target_include_directories(audio_android_jni
    PRIVATE
        ../../../../../../apps/audio_core/api/include
        ../../../../../../apps/audio_core/core/include
)
target_link_libraries(audio_android_jni
    PRIVATE
        audio_core_api
        android
        log
)
