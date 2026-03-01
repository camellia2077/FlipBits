#include "utils.h"

#include <algorithm>
#include <cmath>
#include <stdexcept>

#include "config.h"

namespace utils {
void ensure_parent_dir(const std::filesystem::path& path) {
    auto parent = path.parent_path();
    if (!parent.empty()) {
        std::filesystem::create_directories(parent);
    }
}

std::vector<int> text_to_bits(const std::string& text) {
    std::vector<int> bits;
    bits.reserve(text.size() * 8);
    for (unsigned char ch : text) {
        for (int i = 7; i >= 0; --i) {
            bits.push_back((ch >> i) & 1);
        }
    }
    return bits;
}

std::string bits_to_text(const std::vector<int>& bits) {
    std::string text;
    if (bits.empty()) {
        return text;
    }
    text.reserve(bits.size() / 8);
    for (size_t i = 0; i + 7 < bits.size(); i += 8) {
        unsigned char value = 0;
        for (int bit = 0; bit < 8; ++bit) {
            value = static_cast<unsigned char>((value << 1) | (bits[i + bit] ? 1 : 0));
        }
        text.push_back(static_cast<char>(value));
    }
    return text;
}

std::vector<float> generate_sine_wave(double frequency_hz,
                                      double duration_sec,
                                      int sample_rate_hz,
                                      double amplitude) {
    const size_t sample_count = static_cast<size_t>(sample_rate_hz * duration_sec);
    std::vector<float> samples(sample_count, 0.0f);
    constexpr double kPi = 3.14159265358979323846;
    const double two_pi_f = 2.0 * kPi * frequency_hz;
    for (size_t i = 0; i < sample_count; ++i) {
        double t = static_cast<double>(i) / static_cast<double>(sample_rate_hz);
        samples[i] = static_cast<float>(amplitude * std::sin(two_pi_f * t));
    }
    return samples;
}

std::vector<int16_t> to_pcm16(const std::vector<float>& signal) {
    std::vector<int16_t> pcm;
    pcm.reserve(signal.size());
    for (float sample : signal) {
        float clamped = std::max(-1.0f, std::min(1.0f, sample));
        int16_t value = static_cast<int16_t>(clamped * 32767.0f);
        pcm.push_back(value);
    }
    return pcm;
}

std::vector<float> to_mono(const std::vector<float>& interleaved, int channels) {
    if (channels <= 1) {
        return interleaved;
    }
    const size_t frames = interleaved.size() / static_cast<size_t>(channels);
    std::vector<float> mono(frames, 0.0f);
    for (size_t frame = 0; frame < frames; ++frame) {
        double sum = 0.0;
        for (int ch = 0; ch < channels; ++ch) {
            sum += interleaved[frame * channels + ch];
        }
        mono[frame] = static_cast<float>(sum / static_cast<double>(channels));
    }
    return mono;
}

int select_bit_from_peak(double peak_freq_hz) {
    double dist_low = std::abs(peak_freq_hz - config::kLowFreqHz);
    double dist_high = std::abs(peak_freq_hz - config::kHighFreqHz);
    return dist_high < dist_low ? 1 : 0;
}
}  // namespace utils
