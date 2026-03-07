#include "bag/fsk/fsk_codec.h"

#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <vector>

namespace bag::fsk {
namespace {
int ChunkSize(const FskConfig& config) {
    return static_cast<int>(config.sample_rate_hz * config.bit_duration_sec);
}

std::vector<int> TextToBits(const std::string& text) {
    std::vector<int> bits;
    bits.reserve(text.size() * 8);
    for (unsigned char ch : text) {
        for (int i = 7; i >= 0; --i) {
            bits.push_back((ch >> i) & 1);
        }
    }
    return bits;
}

std::string BitsToText(const std::vector<int>& bits) {
    std::string text;
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

double GoertzelPower(const int16_t* chunk,
                     int chunk_size,
                     int sample_rate_hz,
                     double target_freq_hz) {
    constexpr double kPi = 3.14159265358979323846;
    const double normalized_freq = target_freq_hz / static_cast<double>(sample_rate_hz);
    const double omega = 2.0 * kPi * normalized_freq;
    const double coeff = 2.0 * std::cos(omega);

    double q0 = 0.0;
    double q1 = 0.0;
    double q2 = 0.0;
    for (int i = 0; i < chunk_size; ++i) {
        const double sample = static_cast<double>(chunk[static_cast<size_t>(i)]) / 32767.0;
        q0 = coeff * q1 - q2 + sample;
        q2 = q1;
        q1 = q0;
    }
    return q1 * q1 + q2 * q2 - coeff * q1 * q2;
}
}  // namespace

std::vector<int16_t> EncodeTextToPcm16(const std::string& text, const FskConfig& config) {
    const int chunk_size = ChunkSize(config);
    if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
        throw std::invalid_argument("Invalid FSK config.");
    }

    const std::vector<int> bits = TextToBits(text);
    std::vector<int16_t> pcm;
    pcm.reserve(bits.size() * static_cast<size_t>(chunk_size));

    constexpr double kPi = 3.14159265358979323846;
    for (int bit : bits) {
        const double freq = bit == 1 ? config.high_freq_hz : config.low_freq_hz;
        const double two_pi_f = 2.0 * kPi * freq;
        for (int i = 0; i < chunk_size; ++i) {
            const double t = static_cast<double>(i) / static_cast<double>(config.sample_rate_hz);
            const double sample = config.amplitude * std::sin(two_pi_f * t);
            const double clamped = std::max(-1.0, std::min(1.0, sample));
            pcm.push_back(static_cast<int16_t>(clamped * 32767.0));
        }
    }

    return pcm;
}

std::string DecodePcm16ToText(const std::vector<int16_t>& pcm, const FskConfig& config) {
    const int chunk_size = ChunkSize(config);
    if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
        throw std::invalid_argument("Invalid FSK config.");
    }
    if (pcm.empty()) {
        return {};
    }

    std::vector<int> bits;

    for (size_t offset = 0; offset + static_cast<size_t>(chunk_size) <= pcm.size();
         offset += static_cast<size_t>(chunk_size)) {
        const int16_t* chunk = pcm.data() + offset;
        const double low_power = GoertzelPower(
            chunk, chunk_size, config.sample_rate_hz, config.low_freq_hz);
        const double high_power = GoertzelPower(
            chunk, chunk_size, config.sample_rate_hz, config.high_freq_hz);
        bits.push_back(high_power > low_power ? 1 : 0);
    }

    return BitsToText(bits);
}

}  // namespace bag::fsk
