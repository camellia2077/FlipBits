set(bag_core_sources)

file(MAKE_DIRECTORY "${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}/bag/common")
configure_file(
    "${CMAKE_CURRENT_SOURCE_DIR}/include/bag/common/build_features_generated.h.in"
    "${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}/bag/common/build_features_generated.h"
    @ONLY
)
configure_file(
    "${CMAKE_CURRENT_SOURCE_DIR}/include/bag/common/version_generated.h.in"
    "${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}/bag/common/version_generated.h"
    @ONLY
)

set(bag_core_sources
    src/common/version.cpp
    src/flash/codec.cpp
    src/flash/phy_decode.cpp
    src/flash/phy_encode.cpp
    src/flash/phy_rules.cpp
    src/flash/signal.cpp
    src/flash/signal_decode.cpp
    src/flash/signal_layout.cpp
    src/flash/signal_rules.cpp
    src/flash/voicing.cpp
    src/fsk/fsk_codec.cpp
    src/mini/codec.cpp
    src/mini/morse_rules.cpp
    src/mini/phy_decode.cpp
    src/mini/phy_encode.cpp
    src/mini/tone_renderer.cpp
    src/pro/codec.cpp
    src/pro/phy_decode.cpp
    src/pro/phy_encode.cpp
    src/pro/phy_rules.cpp
    src/pro/tone_renderer.cpp
    src/pro/phy_compat.cpp
    src/transport/compat/frame_codec.cpp
    src/transport/encode_operation_steps.cpp
    src/transport/encode_work_plan.cpp
    src/transport/follow.cpp
    src/transport/transport.cpp
    src/transport/transport_encode_operation.cpp
    src/ultra/codec.cpp
    src/ultra/phy_decode.cpp
    src/ultra/phy_encode.cpp
    src/ultra/phy_rules.cpp
    src/ultra/tone_renderer.cpp
    src/voice/fx.cpp
    src/pipeline/pipeline.cpp
)

add_library(bag_core STATIC
    ${bag_core_sources}
)

target_compile_features(bag_core PUBLIC cxx_std_20)
target_include_directories(bag_core
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${FLIPBITS_CORE_GENERATED_INCLUDE_DIR}
)

target_sources(
    bag_core
    PUBLIC
        FILE_SET cxx_modules TYPE CXX_MODULES
        BASE_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/modules
        FILES
            modules/bag/common/config.cppm
            modules/bag/common/error_code.cppm
            modules/bag/common/types.cppm
            modules/bag/common/version.cppm
            modules/bag/flash/codec.cppm
            modules/bag/flash/facade.cppm
            modules/bag/flash/phy_clean.cppm
            modules/bag/flash/phy_decode.cppm
            modules/bag/flash/phy_encode.cppm
            modules/bag/flash/phy_rules.cppm
            modules/bag/flash/signal.cppm
            modules/bag/flash/signal_decode.cppm
            modules/bag/flash/signal_layout.cppm
            modules/bag/flash/signal_rules.cppm
            modules/bag/flash/voicing.cppm
            modules/bag/fsk/codec.cppm
            modules/bag/mini/codec.cppm
            modules/bag/mini/morse_rules.cppm
            modules/bag/mini/phy_clean.cppm
            modules/bag/mini/phy_decode.cppm
            modules/bag/mini/phy_encode.cppm
            modules/bag/mini/tone_renderer.cppm
            modules/bag/pro/codec.cppm
            modules/bag/pro/phy_clean.cppm
            modules/bag/pro/phy_decode.cppm
            modules/bag/pro/phy_encode.cppm
            modules/bag/pro/phy_compat.cppm
            modules/bag/pro/phy_rules.cppm
            modules/bag/pro/tone_renderer.cppm
            modules/bag/transport/decoder.cppm
            modules/bag/transport/compat/frame_codec.cppm
            modules/bag/transport/encode_operation_steps.cppm
            modules/bag/transport/encode_work_plan.cppm
            modules/bag/transport/follow.cppm
            modules/bag/transport/facade.cppm
            modules/bag/ultra/codec.cppm
            modules/bag/ultra/phy_clean.cppm
            modules/bag/ultra/phy_decode.cppm
            modules/bag/ultra/phy_encode.cppm
            modules/bag/ultra/phy_rules.cppm
            modules/bag/ultra/tone_renderer.cppm
            modules/bag/voice/fx.cppm
            modules/bag/pipeline/pipeline.cppm
)

target_compile_features(bag_core PRIVATE cxx_std_23)
flipbits_apply_toolchain_capabilities(bag_core)
