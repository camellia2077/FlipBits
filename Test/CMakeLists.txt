add_executable(test_unit
    unit/unit_tests.cpp
)
target_include_directories(test_unit
    PRIVATE
        include
        ${WAVEBITS_CORE_GENERATED_INCLUDE_DIR}
)
target_link_libraries(test_unit
    PRIVATE
        bag_core
        audio_io
)
add_test(NAME unit_tests COMMAND test_unit)

add_executable(test_api
    api/api_tests.cpp
)
target_include_directories(test_api
    PRIVATE
        include
        ${WAVEBITS_CORE_GENERATED_INCLUDE_DIR}
)
target_link_libraries(test_api
    PRIVATE
        bag_api
)
add_test(NAME api_tests COMMAND test_api)

add_executable(test_artifact
    artifact/artifact_tests.cpp
)
target_include_directories(test_artifact
    PRIVATE
        include
        ${WAVEBITS_CORE_GENERATED_INCLUDE_DIR}
)
target_link_libraries(test_artifact
    PRIVATE
        bag_api
        audio_io
)
add_test(NAME artifact_tests COMMAND test_artifact)

add_executable(test_runtime
    runtime/runtime_tests.cpp
)
target_include_directories(test_runtime
    PRIVATE
        include
        ${WAVEBITS_CORE_GENERATED_INCLUDE_DIR}
)
target_link_libraries(test_runtime
    PRIVATE
        audio_runtime
)
add_test(NAME runtime_tests COMMAND test_runtime)

add_executable(test_cli_smoke
    cli/cli_smoke_tests.cpp
)
target_include_directories(test_cli_smoke
    PRIVATE
        include
        ${WAVEBITS_CORE_GENERATED_INCLUDE_DIR}
)
add_dependencies(test_cli_smoke binary_audio_cpp)
add_test(
    NAME cli_smoke_tests
    COMMAND test_cli_smoke $<TARGET_FILE:binary_audio_cpp>
)

function(wavebits_configure_module_test target_name)
    target_include_directories(${target_name}
        PRIVATE
            include
            ${WAVEBITS_CORE_GENERATED_INCLUDE_DIR}
    )
    target_compile_features(${target_name} PRIVATE cxx_std_23)
    target_compile_definitions(${target_name} PRIVATE WAVEBITS_TEST_IMPORT_STD=1)
    set_property(TARGET ${target_name} PROPERTY CXX_MODULE_STD ON)
endfunction()

add_executable(test_flash_voicing
    voicing/flash_voicing_tests.cpp
)
target_link_libraries(test_flash_voicing
    PRIVATE
        bag_core
)
wavebits_configure_module_test(test_flash_voicing)
add_test(NAME flash_voicing_tests COMMAND test_flash_voicing)

add_executable(test_modules_foundation
    modules/foundation_module_smoke.cpp
)
target_link_libraries(test_modules_foundation
    PRIVATE
        bag_core
        audio_io
)
wavebits_configure_module_test(test_modules_foundation)
add_test(NAME modules_foundation_smoke COMMAND test_modules_foundation)

add_executable(test_modules_leaf
    modules/leaf_module_smoke.cpp
)
target_link_libraries(test_modules_leaf
    PRIVATE
        bag_core
        audio_io
)
wavebits_configure_module_test(test_modules_leaf)
add_test(NAME modules_leaf_smoke COMMAND test_modules_leaf)

add_executable(test_modules_mid_layer
    modules/mid_layer_module_smoke.cpp
)
target_link_libraries(test_modules_mid_layer
    PRIVATE
        bag_core
)
wavebits_configure_module_test(test_modules_mid_layer)
add_test(NAME modules_mid_layer_smoke COMMAND test_modules_mid_layer)

add_executable(test_modules_facade_pipeline
    modules/facade_pipeline_smoke.cpp
)
target_link_libraries(test_modules_facade_pipeline
    PRIVATE
        bag_core
)
wavebits_configure_module_test(test_modules_facade_pipeline)
add_test(NAME modules_facade_pipeline_smoke COMMAND test_modules_facade_pipeline)

add_executable(test_modules_end_to_end
    modules/end_to_end_module_smoke.cpp
)
target_link_libraries(test_modules_end_to_end
    PRIVATE
        bag_core
)
wavebits_configure_module_test(test_modules_end_to_end)
add_test(NAME modules_end_to_end_smoke COMMAND test_modules_end_to_end)
