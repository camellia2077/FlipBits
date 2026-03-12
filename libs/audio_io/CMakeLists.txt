find_package(PkgConfig REQUIRED)
pkg_check_modules(SNDFILE REQUIRED IMPORTED_TARGET sndfile)

add_library(audio_io STATIC
    src/wav_io.cpp
    src/wav_io_backend.cpp
)

target_compile_features(audio_io PUBLIC cxx_std_17)
target_include_directories(audio_io
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)
target_link_libraries(audio_io
    PUBLIC
        $<LINK_ONLY:PkgConfig::SNDFILE>
)

if(WAVEBITS_HOST_MODULES)
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

    if(WAVEBITS_HOST_IMPORT_STD_PILOT_ENABLED)
        target_compile_features(audio_io PRIVATE cxx_std_23)
        target_compile_definitions(audio_io PRIVATE WAVEBITS_AUDIO_IO_IMPORT_STD=1)
        set_property(TARGET audio_io PROPERTY CXX_MODULE_STD ON)
    endif()
endif()
