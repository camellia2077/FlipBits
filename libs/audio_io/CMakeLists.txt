find_package(PkgConfig REQUIRED)
pkg_check_modules(SNDFILE REQUIRED IMPORTED_TARGET sndfile)

add_library(audio_io STATIC
    src/wav_io.cpp
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
