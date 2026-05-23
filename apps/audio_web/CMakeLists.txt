cmake_minimum_required(VERSION 3.28)

if(CMAKE_VERSION VERSION_GREATER_EQUAL 4.3)
    set(CMAKE_EXPERIMENTAL_CXX_IMPORT_STD "451f2fe2-a8a2-47c3-bc32-94786d8fc91b")
elseif(CMAKE_VERSION VERSION_GREATER_EQUAL 4.0)
    set(CMAKE_EXPERIMENTAL_CXX_IMPORT_STD "d0edc3af-4c50-42ea-a356-e2862fe7a444")
else()
    set(CMAKE_EXPERIMENTAL_CXX_IMPORT_STD "0e5b6991-d74f-4b3d-a41c-cf096e0b2508")
endif()

project(FlipBitsWebDemo LANGUAGES CXX)

if(NOT EMSCRIPTEN)
    message(FATAL_ERROR "apps/audio_web must be configured with Emscripten.")
endif()

include(${CMAKE_CURRENT_SOURCE_DIR}/../../cmake/flipbits_toolchain_capabilities.cmake)

if(POLICY CMP0155)
    cmake_policy(SET CMP0155 NEW)
endif()

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_SCAN_FOR_MODULES ON)

set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(FLIPBITS_CORE_GENERATED_INCLUDE_DIR "${CMAKE_BINARY_DIR}/generated/include")

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../libs/audio_core
                 ${CMAKE_BINARY_DIR}/audio_core)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../libs/audio_api
                 ${CMAKE_BINARY_DIR}/audio_api)

add_executable(flipbits_web_bridge
    src/flipbits_web_bridge.cpp
)

target_link_libraries(flipbits_web_bridge PRIVATE bag_api)
target_link_libraries(flipbits_web_bridge PRIVATE bag_core)
target_compile_features(flipbits_web_bridge PRIVATE cxx_std_23)

set_target_properties(
    flipbits_web_bridge
    PROPERTIES
        OUTPUT_NAME "flipbits_web_runtime"
        SUFFIX ".js"
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/site/wasm"
    )

set(
    FLIPBITS_WEB_LINK_FLAGS
    "--no-entry \
    -O3 \
    -sWASM=1 \
    -sMODULARIZE=1 \
    -sEXPORT_ES6=1 \
    -sEXPORT_NAME=createFlipBitsRuntime \
    -sENVIRONMENT=web,worker \
    -sALLOW_MEMORY_GROWTH=1 \
    -sFILESYSTEM=0 \
    -sNO_EXIT_RUNTIME=1 \
    -sEXPORTED_FUNCTIONS=['_flipbits_web_encode_text','_flipbits_web_last_error_message','_flipbits_web_last_sample_count','_flipbits_web_last_sample_rate_hz','_flipbits_web_last_samples_ptr','_flipbits_web_last_sample_at'] \
    -sEXPORTED_RUNTIME_METHODS=['ccall']"
)

set_property(TARGET flipbits_web_bridge PROPERTY LINK_FLAGS "${FLIPBITS_WEB_LINK_FLAGS}")
