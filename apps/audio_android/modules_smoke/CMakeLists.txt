include("${FLIPBITS_ROOT}/cmake/flipbits_core_version.cmake")

set(CMAKE_CXX_SCAN_FOR_MODULES ON)

if(NOT DEFINED FLIPBITS_CORE_GENERATED_INCLUDE_DIR)
    set(
        FLIPBITS_ANDROID_MODULES_SMOKE_CORE_GENERATED_INCLUDE_DIR
        "${CMAKE_CURRENT_BINARY_DIR}/generated/include"
    )
    set(
        FLIPBITS_CORE_GENERATED_INCLUDE_DIR
        "${FLIPBITS_ANDROID_MODULES_SMOKE_CORE_GENERATED_INCLUDE_DIR}"
    )
endif()

file(MAKE_DIRECTORY "${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}/bag/common")
configure_file(
    "${FLIPBITS_ROOT}/libs/audio_core/include/bag/common/version_generated.h.in"
    "${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}/bag/common/version_generated.h"
    @ONLY
)

set(FLIPBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_LEVELS "unavailable")
if(DEFINED CMAKE_CXX_COMPILER_IMPORT_STD AND NOT CMAKE_CXX_COMPILER_IMPORT_STD STREQUAL "")
    string(
        REPLACE
        ";"
        ", "
        FLIPBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_LEVELS
        "${CMAKE_CXX_COMPILER_IMPORT_STD}"
    )
endif()

set(FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY "unavailable")
set(FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_EXISTS "0")
set(FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN "missing")
execute_process(
    COMMAND
        "${CMAKE_CXX_COMPILER}"
        ${CMAKE_CXX_COMPILER_ID_ARG1}
        "-print-file-name=libc++.modules.json"
    OUTPUT_VARIABLE FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY
    ERROR_QUIET
    RESULT_VARIABLE FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_RESULT
    OUTPUT_STRIP_TRAILING_WHITESPACE
)
if(
    FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_RESULT EQUAL 0
    AND EXISTS "${FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY}"
)
    set(FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_EXISTS "1")
    file(
        TO_CMAKE_PATH
        "${FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY}"
        FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY_NORMALIZED
    )
    file(TO_CMAKE_PATH "${CMAKE_ANDROID_NDK}" FLIPBITS_ANDROID_MODULES_SMOKE_ANDROID_NDK_NORMALIZED)
    string(
        FIND
        "${FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY_NORMALIZED}"
        "${FLIPBITS_ANDROID_MODULES_SMOKE_ANDROID_NDK_NORMALIZED}/"
        FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_UNDER_NDK
    )
    if(FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_UNDER_NDK EQUAL 0)
        set(FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN "android-ndk-local")
    else()
        set(FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN "foreign")
    endif()
endif()

set(
    FLIPBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_STATUS
    "blocked-missing-libcxx-modules-json"
)
if(FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN STREQUAL "android-ndk-local")
    set(FLIPBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_STATUS "candidate-available")
elseif(FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN STREQUAL "foreign")
    set(FLIPBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_STATUS "blocked-foreign-libcxx-modules-json")
endif()

set(FLIPBITS_ANDROID_MODULES_SMOKE_NDK_VERSION "unknown")
if(DEFINED CMAKE_ANDROID_NDK_VERSION)
    set(FLIPBITS_ANDROID_MODULES_SMOKE_NDK_VERSION "${CMAKE_ANDROID_NDK_VERSION}")
endif()

set(FLIPBITS_ANDROID_MODULES_SMOKE_PLATFORM "unknown")
if(DEFINED ANDROID_PLATFORM)
    set(FLIPBITS_ANDROID_MODULES_SMOKE_PLATFORM "${ANDROID_PLATFORM}")
endif()

set(FLIPBITS_ANDROID_MODULES_SMOKE_API "unknown")
if(DEFINED CMAKE_ANDROID_API)
    set(FLIPBITS_ANDROID_MODULES_SMOKE_API "${CMAKE_ANDROID_API}")
elseif(DEFINED ANDROID_NATIVE_API_LEVEL)
    set(FLIPBITS_ANDROID_MODULES_SMOKE_API "${ANDROID_NATIVE_API_LEVEL}")
endif()

set(
    FLIPBITS_ANDROID_MODULES_SMOKE_REPORT_PATH
    "${CMAKE_CURRENT_BINARY_DIR}/android-modules-smoke-report.txt"
)
file(
    WRITE
    "${FLIPBITS_ANDROID_MODULES_SMOKE_REPORT_PATH}"
    "flipbits_core_version=${FLIPBITS_CORE_VERSION}\n"
    "cmake_version=${CMAKE_VERSION}\n"
    "generator=${CMAKE_GENERATOR}\n"
    "system_name=${CMAKE_SYSTEM_NAME}\n"
    "android_abi=${CMAKE_ANDROID_ARCH_ABI}\n"
    "android_platform=${FLIPBITS_ANDROID_MODULES_SMOKE_PLATFORM}\n"
    "android_api=${FLIPBITS_ANDROID_MODULES_SMOKE_API}\n"
    "cxx_compiler_id=${CMAKE_CXX_COMPILER_ID}\n"
    "cxx_compiler_version=${CMAKE_CXX_COMPILER_VERSION}\n"
    "android_ndk=${CMAKE_ANDROID_NDK}\n"
    "android_ndk_version=${FLIPBITS_ANDROID_MODULES_SMOKE_NDK_VERSION}\n"
    "cxx_import_std=${FLIPBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_LEVELS}\n"
    "libcxx_modules_json_query=${FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_QUERY}\n"
    "libcxx_modules_json_exists=${FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_EXISTS}\n"
    "libcxx_modules_json_origin=${FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN}\n"
    "import_std_status=${FLIPBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_STATUS}\n"
    "mode=phase3a-direct-module-version-owner\n"
)

message(STATUS "FlipBits Android modules smoke enabled")
message(STATUS "  ABI: ${CMAKE_ANDROID_ARCH_ABI}")
message(STATUS "  compiler: ${CMAKE_CXX_COMPILER_ID} ${CMAKE_CXX_COMPILER_VERSION}")
message(STATUS "  import std levels: ${FLIPBITS_ANDROID_MODULES_SMOKE_IMPORT_STD_LEVELS}")
message(
    STATUS
    "  libc++.modules.json origin: ${FLIPBITS_ANDROID_MODULES_SMOKE_LIBCXX_MODULES_JSON_ORIGIN}"
)
message(STATUS "  report: ${FLIPBITS_ANDROID_MODULES_SMOKE_REPORT_PATH}")

add_library(flipbits_android_bag_common_version_owner STATIC
    "${FLIPBITS_ROOT}/libs/audio_core/src/common/version.cpp"
)

target_compile_features(flipbits_android_bag_common_version_owner PUBLIC cxx_std_23)
set_target_properties(
    flipbits_android_bag_common_version_owner
    PROPERTIES POSITION_INDEPENDENT_CODE ON
)

target_include_directories(flipbits_android_bag_common_version_owner
    PUBLIC
        "${FLIPBITS_ROOT}/libs/audio_core/include"
        "${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}"
)

target_sources(
    flipbits_android_bag_common_version_owner
    PUBLIC
        FILE_SET cxx_modules TYPE CXX_MODULES
        BASE_DIRS "${FLIPBITS_ROOT}/libs/audio_core/modules"
        FILES "${FLIPBITS_ROOT}/libs/audio_core/modules/bag/common/version.cppm"
)

add_library(flipbits_android_modules_smoke STATIC
    "${CMAKE_CURRENT_LIST_DIR}/android_modules_smoke.cpp"
)

target_compile_features(flipbits_android_modules_smoke PRIVATE cxx_std_23)
set_target_properties(flipbits_android_modules_smoke PROPERTIES POSITION_INDEPENDENT_CODE ON)

target_link_libraries(flipbits_android_modules_smoke
    PRIVATE
        flipbits_android_bag_common_version_owner
)
