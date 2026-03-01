#pragma once

namespace config {
constexpr double kLowFreqHz = 400.0;
constexpr double kHighFreqHz = 800.0;
constexpr double kBitDurationSec = 0.05;
constexpr int kSampleRateHz = 44100;
constexpr int kChunkSize = static_cast<int>(kSampleRateHz * kBitDurationSec);
constexpr double kAmplitude = 0.8;
}  // namespace config
