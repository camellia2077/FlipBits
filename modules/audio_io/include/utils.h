#pragma once

#include <cstdint>
#include <filesystem>
#include <string>
#include <vector>

namespace utils {
void ensure_parent_dir(const std::filesystem::path& path);
std::vector<int> text_to_bits(const std::string& text);
std::string bits_to_text(const std::vector<int>& bits);
std::vector<float> generate_sine_wave(double frequency_hz,
                                      double duration_sec,
                                      int sample_rate_hz,
                                      double amplitude);
std::vector<int16_t> to_pcm16(const std::vector<float>& signal);
std::vector<float> to_mono(const std::vector<float>& interleaved, int channels);
int select_bit_from_peak(double peak_freq_hz);
}  // namespace utils
