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
    FLIPBITS_WEB_WASM_OPT_LEVEL
    "O3"
    CACHE STRING
    "Emscripten/Binaryen optimization level used when linking the WebAssembly runtime."
)
set_property(
    CACHE FLIPBITS_WEB_WASM_OPT_LEVEL
    PROPERTY STRINGS O0 O1 O2 O3 Os Oz
)

set(
    FLIPBITS_WEB_MALLOC
    "dlmalloc"
    CACHE STRING
    "Emscripten malloc implementation for the WebAssembly runtime."
)
set_property(
    CACHE FLIPBITS_WEB_MALLOC
    PROPERTY STRINGS dlmalloc emmalloc mimalloc none
)

set(
    FLIPBITS_WEB_LTO
    OFF
    CACHE BOOL
    "Enable link-time optimization for the WebAssembly runtime."
)

set(
    FLIPBITS_WEB_ENABLE_SIMD
    OFF
    CACHE BOOL
    "Enable wasm simd128 code generation for the WebAssembly runtime."
)

set(
    FLIPBITS_WEB_ALLOW_MEMORY_GROWTH
    ON
    CACHE BOOL
    "Allow the WebAssembly heap to grow at runtime."
)

set(
    FLIPBITS_WEB_INITIAL_MEMORY
    ""
    CACHE STRING
    "Optional Emscripten INITIAL_MEMORY value in bytes for the WebAssembly runtime."
)

set(FLIPBITS_WEB_RELEASE_COMPILE_FLAGS "")
set(FLIPBITS_WEB_RELEASE_LINK_FLAGS "-${FLIPBITS_WEB_WASM_OPT_LEVEL}")

if(FLIPBITS_WEB_WASM_OPT_LEVEL MATCHES "^O[0-3]$")
    list(APPEND FLIPBITS_WEB_RELEASE_COMPILE_FLAGS "-${FLIPBITS_WEB_WASM_OPT_LEVEL}")
endif()

if(FLIPBITS_WEB_LTO)
    list(APPEND FLIPBITS_WEB_RELEASE_COMPILE_FLAGS "-flto")
    string(APPEND FLIPBITS_WEB_RELEASE_LINK_FLAGS " -flto")
endif()

if(FLIPBITS_WEB_ENABLE_SIMD)
    list(APPEND FLIPBITS_WEB_RELEASE_COMPILE_FLAGS "-msimd128")
    string(APPEND FLIPBITS_WEB_RELEASE_LINK_FLAGS " -msimd128")
endif()

if(FLIPBITS_WEB_MALLOC AND NOT FLIPBITS_WEB_MALLOC STREQUAL "dlmalloc")
    string(APPEND FLIPBITS_WEB_RELEASE_LINK_FLAGS " -sMALLOC=${FLIPBITS_WEB_MALLOC}")
endif()

if(FLIPBITS_WEB_ALLOW_MEMORY_GROWTH)
    string(APPEND FLIPBITS_WEB_RELEASE_LINK_FLAGS " -sALLOW_MEMORY_GROWTH=1")
else()
    string(APPEND FLIPBITS_WEB_RELEASE_LINK_FLAGS " -sALLOW_MEMORY_GROWTH=0")
endif()

if(FLIPBITS_WEB_INITIAL_MEMORY)
    string(APPEND FLIPBITS_WEB_RELEASE_LINK_FLAGS " -sINITIAL_MEMORY=${FLIPBITS_WEB_INITIAL_MEMORY}")
endif()

add_compile_options($<$<CONFIG:Release>:${FLIPBITS_WEB_RELEASE_COMPILE_FLAGS}>)

set(
    FLIPBITS_WEB_LINK_FLAGS
    "--no-entry \
    ${FLIPBITS_WEB_RELEASE_LINK_FLAGS} \
    -sWASM=1 \
    -sMODULARIZE=1 \
    -sEXPORT_ES6=1 \
    -sEXPORT_NAME=createFlipBitsRuntime \
    -sENVIRONMENT=web,worker \
    -sFILESYSTEM=0 \
    -sNO_EXIT_RUNTIME=1 \
    -sEXPORTED_FUNCTIONS=['_malloc','_free','_flipbits_web_begin_encode_operation','_flipbits_web_pump_encode_operation','_flipbits_web_take_encode_operation_result','_flipbits_web_abort_encode_operation','_flipbits_web_begin_voice_fx','_flipbits_web_apply_voice_fx_pcm_bytes','_flipbits_web_process_voice_fx_pcm_bytes','_flipbits_web_finish_voice_fx','_flipbits_web_abort_voice_fx','_flipbits_web_current_operation_state','_flipbits_web_current_operation_phase','_flipbits_web_current_operation_overall_progress','_flipbits_web_current_operation_phase_progress','_flipbits_web_current_operation_completed_work_units','_flipbits_web_current_operation_total_work_units','_flipbits_web_current_operation_phase_completed_work_units','_flipbits_web_current_operation_phase_total_work_units','_flipbits_web_current_operation_terminal_code','_flipbits_web_current_operation_estimated_pcm_sample_count','_flipbits_web_current_operation_payload_byte_count','_flipbits_web_current_operation_segment_count','_flipbits_web_current_operation_current_segment_index','_flipbits_web_current_operation_plan_preparing_work_units','_flipbits_web_current_operation_plan_rendering_work_units','_flipbits_web_current_operation_plan_postprocessing_work_units','_flipbits_web_current_operation_plan_finalizing_work_units','_flipbits_web_current_operation_plan_total_work_units','_flipbits_web_current_operation_plan_estimated_pcm_sample_count','_flipbits_web_current_operation_plan_payload_byte_count','_flipbits_web_current_operation_plan_segment_count','_flipbits_web_current_operation_diagnostics_flash_payload_prepare_ms','_flipbits_web_current_operation_diagnostics_flash_payload_sample_setup_ms','_flipbits_web_current_operation_diagnostics_flash_payload_envelope_ms','_flipbits_web_current_operation_diagnostics_flash_payload_articulation_ms','_flipbits_web_current_operation_diagnostics_flash_payload_harmonic_ms','_flipbits_web_current_operation_diagnostics_flash_payload_metallic_ms','_flipbits_web_current_operation_diagnostics_flash_payload_chant_resonance_ms','_flipbits_web_current_operation_diagnostics_flash_payload_chant_drone_ms','_flipbits_web_current_operation_diagnostics_flash_payload_mechanical_throat_ms','_flipbits_web_current_operation_diagnostics_flash_payload_standard_low_voice_ms','_flipbits_web_current_operation_diagnostics_flash_payload_hostility_edge_ms','_flipbits_web_current_operation_diagnostics_flash_payload_boundary_click_ms','_flipbits_web_current_operation_diagnostics_flash_payload_modulation_ms','_flipbits_web_current_operation_diagnostics_flash_payload_mix_shape_store_ms','_flipbits_web_current_operation_diagnostics_flash_payload_voiced_samples','_flipbits_web_current_operation_diagnostics_flash_payload_silence_samples','_flipbits_web_current_operation_diagnostics_flash_payload_profiled_samples','_flipbits_web_error_message_from_code','_flipbits_web_last_error_message','_flipbits_web_last_sample_count','_flipbits_web_last_sample_rate_hz','_flipbits_web_last_samples_ptr','_flipbits_web_last_sample_at'] \
    -sEXPORTED_RUNTIME_METHODS=['ccall','HEAP16','HEAPU8']"
)

set_property(TARGET flipbits_web_bridge PROPERTY LINK_FLAGS "${FLIPBITS_WEB_LINK_FLAGS}")
