#pragma once

#include <filesystem>
#include <string>

std::string decode_wav_to_text(const std::filesystem::path& input_path);
