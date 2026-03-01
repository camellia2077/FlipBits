cmake_minimum_required(VERSION 3.20)

project(BinaryAudioGeneratorCpp LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
enable_testing()

add_subdirectory(build/apps/audio_cli/cmake)
add_subdirectory(Test)
