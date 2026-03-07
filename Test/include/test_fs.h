#pragma once

#include <atomic>
#include <filesystem>
#include <fstream>
#include <stdexcept>
#include <string>

namespace test {

inline std::filesystem::path MakeTempDir(const std::string& suite_name) {
    const auto root = std::filesystem::temp_directory_path() / "binary_audio_generator_tests";
    static std::atomic<unsigned long long> next_id{0};
    const auto dir = root / suite_name / std::to_string(next_id.fetch_add(1));
    std::filesystem::remove_all(dir);
    std::filesystem::create_directories(dir);
    return dir;
}

inline void WriteTextFile(const std::filesystem::path& path, const std::string& content) {
    const auto parent = path.parent_path();
    if (!parent.empty()) {
        std::filesystem::create_directories(parent);
    }

    std::ofstream file(path, std::ios::binary);
    if (!file) {
        throw std::runtime_error("Failed to write file: " + path.string());
    }
    file << content;
}

inline std::string ReadTextFile(const std::filesystem::path& path) {
    std::ifstream file(path, std::ios::binary);
    if (!file) {
        throw std::runtime_error("Failed to read file: " + path.string());
    }
    return std::string((std::istreambuf_iterator<char>(file)), std::istreambuf_iterator<char>());
}

}  // namespace test
