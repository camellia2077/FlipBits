#include "decoder.h"

#include <cmath>
#include <stdexcept>
#include <vector>

#include <kissfft/kiss_fft.h>
#include <kissfft/kiss_fftr.h>
#include <sndfile.h>

#include "config.h"
#include "utils.h"

std::string decode_wav_to_text(const std::filesystem::path& input_path) {
    SF_INFO info{};
    SNDFILE* file = sf_open(input_path.string().c_str(), SFM_READ, &info);
    if (!file) {
        throw std::runtime_error(sf_strerror(nullptr));
    }

    if (info.samplerate != config::kSampleRateHz) {
        sf_close(file);
        throw std::runtime_error("Sample rate mismatch.");
    }

    const sf_count_t total_samples = info.frames * info.channels;
    std::vector<float> interleaved(static_cast<size_t>(total_samples), 0.0f);
    sf_count_t read_count = sf_read_float(file, interleaved.data(), total_samples);
    sf_close(file);
    if (read_count != total_samples) {
        throw std::runtime_error("Failed to read full WAV data.");
    }

    auto mono = utils::to_mono(interleaved, info.channels);

    const int chunk_size = config::kChunkSize;
    const int freq_bins = chunk_size / 2 + 1;
    kiss_fftr_cfg cfg = kiss_fftr_alloc(chunk_size, 0, nullptr, nullptr);
    if (!cfg) {
        throw std::runtime_error("Failed to allocate KissFFT config.");
    }

    std::vector<kiss_fft_cpx> spectrum(static_cast<size_t>(freq_bins));
    std::vector<int> bits;
    for (size_t offset = 0; offset + static_cast<size_t>(chunk_size) <= mono.size();
         offset += static_cast<size_t>(chunk_size)) {
        const float* chunk = mono.data() + offset;
        kiss_fftr(cfg, chunk, spectrum.data());

        int peak_idx = 0;
        double peak_mag = 0.0;
        for (int i = 0; i < freq_bins; ++i) {
            double mag = std::sqrt(static_cast<double>(spectrum[i].r) * spectrum[i].r +
                                   static_cast<double>(spectrum[i].i) * spectrum[i].i);
            if (mag > peak_mag) {
                peak_mag = mag;
                peak_idx = i;
            }
        }

        double peak_freq = static_cast<double>(peak_idx) *
                           static_cast<double>(config::kSampleRateHz) /
                           static_cast<double>(chunk_size);
        bits.push_back(utils::select_bit_from_peak(peak_freq));
    }

    kiss_fft_free(cfg);

    return utils::bits_to_text(bits);
}
