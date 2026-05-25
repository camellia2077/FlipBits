find_package(PkgConfig REQUIRED)
pkg_check_modules(SNDFILE REQUIRED IMPORTED_TARGET sndfile)

add_library(audio_io STATIC
    src/audio_io_api.cpp
    src/wav_io.cpp
    src/wav_io_backend.cpp
    src/wav_metadata_parse_rules.cpp
)

target_compile_features(audio_io PUBLIC cxx_std_17)
target_include_directories(audio_io
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/../audio_core/include
        ${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}
)
target_link_libraries(audio_io
    PUBLIC
        $<LINK_ONLY:PkgConfig::SNDFILE>
)

target_compile_features(audio_io PUBLIC cxx_std_20)
# Header consumers and module importers keep separate front-ends while sharing
# the same private backend implementation.
target_sources(
    audio_io
    PRIVATE
        modules/audio_io/wav_impl.cpp
    PUBLIC
        FILE_SET cxx_modules TYPE CXX_MODULES
        BASE_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/modules
        FILES
            modules/audio_io/wav.cppm
)

target_compile_features(audio_io PRIVATE cxx_std_23)
flipbits_apply_toolchain_capabilities(audio_io)

if(BUILD_TESTING)
    flipbits_add_host_test(test_unit
        NAME unit_tests
        SOURCES tests/unit_tests.cpp
        LIBS bag_core audio_io
    )
endif()
