#pragma once

#include <filesystem>
#include <string>

namespace test {

inline std::filesystem::path MakeTempDir(const std::string& suite_name) {
    const auto root = std::filesystem::temp_directory_path() / "binary_audio_generator_tests";
    const auto dir = root / suite_name;
    std::filesystem::create_directories(dir);
    return dir;
}

}  // namespace test
