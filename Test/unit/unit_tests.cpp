#include <algorithm>
#include <cmath>
#include <cstdint>
#include <filesystem>
#include <string>
#include <vector>

#include "bag/fsk/fsk_codec.h"
#include "test_framework.h"
#include "test_fs.h"
#include "wav_io.h"

namespace {
void TestEncodeLengthMatchesExpected() {
    bag::fsk::FskConfig config{};
    const std::string text = "A";
    const auto pcm = bag::fsk::EncodeTextToPcm16(text, config);
    const size_t chunk_size =
        static_cast<size_t>(config.sample_rate_hz * config.bit_duration_sec);
    test::AssertEq(
        pcm.size(),
        static_cast<size_t>(8) * chunk_size,
        "Encoded sample count should be 8 bits * chunk size for one byte.");
}

void TestEncodeAmplitudeInRange() {
    bag::fsk::FskConfig config{};
    const auto pcm = bag::fsk::EncodeTextToPcm16("Hello", config);
    const auto [min_it, max_it] = std::minmax_element(pcm.begin(), pcm.end());
    test::AssertTrue(min_it != pcm.end(), "PCM should not be empty for non-empty input.");
    test::AssertTrue(*min_it >= static_cast<int16_t>(-32767), "PCM min out of range.");
    test::AssertTrue(*max_it <= static_cast<int16_t>(32767), "PCM max out of range.");
}

void TestDecodeEmptyInputReturnsEmptyText() {
    bag::fsk::FskConfig config{};
    const std::vector<int16_t> pcm;
    const std::string decoded = bag::fsk::DecodePcm16ToText(pcm, config);
    test::AssertEq(decoded, std::string(), "Decoding empty input should return empty string.");
}

void TestWavIoMonoRoundTrip() {
    bag::fsk::FskConfig config{};
    const std::string text = "Unit";
    const auto pcm = bag::fsk::EncodeTextToPcm16(text, config);

    const auto dir = test::MakeTempDir("unit");
    const auto path = dir / "mono_roundtrip.wav";
    audio_io::WriteMonoPcm16Wav(path, config.sample_rate_hz, pcm);

    const auto read_back = audio_io::ReadMonoPcm16Wav(path);
    test::AssertEq(
        read_back.sample_rate_hz,
        config.sample_rate_hz,
        "Sample rate should be preserved in wav read/write.");
    test::AssertEq(
        read_back.mono_pcm.size(),
        pcm.size(),
        "Sample count should be preserved in mono wav read/write.");
    test::AssertEq(read_back.mono_pcm, pcm, "PCM content should be identical after roundtrip.");
}
}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Unit.EncodeLengthMatchesExpected", TestEncodeLengthMatchesExpected);
    runner.Add("Unit.EncodeAmplitudeInRange", TestEncodeAmplitudeInRange);
    runner.Add("Unit.DecodeEmptyInputReturnsEmptyText", TestDecodeEmptyInputReturnsEmptyText);
    runner.Add("Unit.WavIoMonoRoundTrip", TestWavIoMonoRoundTrip);
    return runner.Run();
}
