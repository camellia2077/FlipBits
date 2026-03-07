add_executable(binary_audio_cpp
    ../src/main.cpp
)

target_link_libraries(binary_audio_cpp
    PRIVATE
        bag_api
        audio_io
)
