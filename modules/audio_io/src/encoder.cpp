#include "encoder.h"

#include <stdexcept>
#include <vector>

#include <sndfile.h>

#include "config.h"
#include "utils.h"

namespace {
std::vector<float> synthesize_bits(const std::vector<int>& bits) {
    std::vector<float> signal;
    signal.reserve(bits.size() * static_cast<size_t>(config::kChunkSize));
    for (int bit : bits) {
        double freq = (bit == 1) ? config::kHighFreqHz : config::kLowFreqHz;
        auto wave = utils::generate_sine_wave(
            freq, config::kBitDurationSec, config::kSampleRateHz, config::kAmplitude);
        signal.insert(signal.end(), wave.begin(), wave.end());
    }
    return signal;
}
}  // namespace

void encode_text_to_wav(const std::string& text, const std::filesystem::path& output_path) {
    auto bits = utils::text_to_bits(text);
    auto signal = synthesize_bits(bits);
    auto pcm = utils::to_pcm16(signal);

    utils::ensure_parent_dir(output_path);

    SF_INFO info{};
    info.samplerate = config::kSampleRateHz;
    info.channels = 1;
    info.format = SF_FORMAT_WAV | SF_FORMAT_PCM_16;

    SNDFILE* file = sf_open(output_path.string().c_str(), SFM_WRITE, &info);
    if (!file) {
        throw std::runtime_error(sf_strerror(nullptr));
    }

    sf_count_t written = sf_write_short(file, pcm.data(), static_cast<sf_count_t>(pcm.size()));
    sf_close(file);
    if (written != static_cast<sf_count_t>(pcm.size())) {
        throw std::runtime_error("Failed to write full WAV data.");
    }
}
