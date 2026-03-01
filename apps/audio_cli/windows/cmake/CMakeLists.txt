include(${CMAKE_CURRENT_LIST_DIR}/third_party.cmake)

set(core_source_files
    ../../../audio_core/core/src/common/version.cpp
    ../../../audio_core/core/src/pipeline/pipeline.cpp
)

add_library(bag_core STATIC ${core_source_files})
target_include_directories(bag_core
    PUBLIC
        ../../../audio_core/core/include
)

set(api_source_files
    ../../../audio_core/api/src/bag_api.cpp
)

add_library(bag_api STATIC ${api_source_files})
target_include_directories(bag_api
    PUBLIC
        ../../../audio_core/api/include
        ../../../audio_core/core/include
)
target_link_libraries(bag_api
    PUBLIC
        bag_core
)

set(source_files
    ../src/main.cpp
    ../../../../modules/audio_io/src/encoder.cpp
    ../../../../modules/audio_io/src/decoder.cpp
    ../../../../modules/audio_io/src/utils.cpp
    ../../../../modules/audio_io/include/config.h
    ../../../../modules/audio_io/include/encoder.h
    ../../../../modules/audio_io/include/decoder.h
    ../../../../modules/audio_io/include/utils.h
)

add_executable(binary_audio_cpp ${source_files})

target_include_directories(binary_audio_cpp
    PRIVATE
        ../../../../modules/audio_io/include
)

target_link_libraries(binary_audio_cpp
    PRIVATE
        ${third_party_libs}
)
