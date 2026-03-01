add_executable(test_unit
    unit/unit_tests.cpp
)
target_include_directories(test_unit
    PRIVATE
        include
        ../apps/audio_core/core/include
        ../modules/audio_io/include
)
target_link_libraries(test_unit
    PRIVATE
        audio_core_fsk
        audio_io
)
add_test(NAME unit_tests COMMAND test_unit)

add_executable(test_e2e
    e2e/e2e_tests.cpp
)
target_include_directories(test_e2e
    PRIVATE
        include
        ../apps/audio_core/core/include
        ../modules/audio_io/include
)
target_link_libraries(test_e2e
    PRIVATE
        audio_core_fsk
        audio_io
)
add_test(
    NAME e2e_tests
    COMMAND test_e2e
)
