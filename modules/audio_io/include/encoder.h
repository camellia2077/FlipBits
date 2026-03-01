#pragma once

#include <filesystem>
#include <string>

void encode_text_to_wav(const std::string& text, const std::filesystem::path& output_path);
