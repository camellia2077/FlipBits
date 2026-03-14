#pragma once

#include <cstdint>
#include <vector>

namespace android_audio_io {

enum class WavDecodeStatus {
    kOk = 0,
    kInvalidArgument = 1,
    kInvalidHeader = 2,
    kUnsupportedFormat = 3,
    kTruncatedData = 4,
};

struct DecodedMonoPcm16WavData {
    WavDecodeStatus status = WavDecodeStatus::kOk;
    int sample_rate_hz = 0;
    int channels = 0;
    std::vector<std::int16_t> pcm_samples;
};

std::vector<std::uint8_t> EncodeMonoPcm16ToWavBytes(
    int sample_rate_hz,
    const std::vector<std::int16_t>& pcm_samples);
DecodedMonoPcm16WavData DecodeMonoPcm16WavBytes(const std::vector<std::uint8_t>& wav_bytes);

}  // namespace android_audio_io
