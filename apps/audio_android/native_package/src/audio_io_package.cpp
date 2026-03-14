#include "android_audio_io/audio_io_package.h"

#include "wav_io.h"

#include "../../../../libs/audio_io/src/wav_io_bytes_impl.inc"

namespace android_audio_io {

std::vector<std::uint8_t> EncodeMonoPcm16ToWavBytes(
    int sample_rate_hz,
    const std::vector<std::int16_t>& pcm_samples
) {
    return audio_io::detail::bytes_impl::SerializeMonoPcm16WavBytes(sample_rate_hz, pcm_samples);
}

DecodedMonoPcm16WavData DecodeMonoPcm16WavBytes(const std::vector<std::uint8_t>& wav_bytes) {
    const auto parsed = audio_io::detail::bytes_impl::ParseMonoPcm16WavBytes(
        wav_bytes.data(),
        wav_bytes.size());
    return {
        static_cast<WavDecodeStatus>(parsed.status),
        parsed.wav.sample_rate_hz,
        parsed.wav.channels,
        parsed.wav.mono_pcm
    };
}

}  // namespace android_audio_io
