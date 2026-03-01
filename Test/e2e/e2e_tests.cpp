#include <algorithm>
#include <cstdint>
#include <filesystem>
#include <string>
#include <vector>

#include "bag/fsk/fsk_codec.h"
#include "test_framework.h"
#include "test_fs.h"
#include "wav_io.h"

namespace {
void TestPipelineRoundTripWithoutFile() {
    bag::fsk::FskConfig config{};
    const std::string input = "E2E-Direct";
    const auto pcm = bag::fsk::EncodeTextToPcm16(input, config);
    const auto decoded = bag::fsk::DecodePcm16ToText(pcm, config);
    test::AssertEq(decoded, input, "Core roundtrip should decode back to original text.");
}

void TestTextToWavToTextRoundTrip() {
    bag::fsk::FskConfig config{};
    const std::string input = "E2E-WAV-Chain";
    const auto pcm = bag::fsk::EncodeTextToPcm16(input, config);

    const auto dir = test::MakeTempDir("e2e");
    const auto wav_path = dir / "chain_roundtrip.wav";
    audio_io::WriteMonoPcm16Wav(wav_path, config.sample_rate_hz, pcm);

    const auto wav = audio_io::ReadMonoPcm16Wav(wav_path);
    const auto decoded = bag::fsk::DecodePcm16ToText(wav.mono_pcm, config);
    test::AssertEq(decoded, input, "WAV chain roundtrip should preserve text.");
}

void TestSnapshotFirstSamplesStable() {
    bag::fsk::FskConfig config{};
    const auto pcm = bag::fsk::EncodeTextToPcm16("A", config);
    const std::vector<int16_t> expected = {
        0, 1493, 2981, 4459, 5924, 7368, 8789, 10182,
        11541, 12863, 14143, 15377, 16561, 17692, 18765, 19777};

    test::AssertTrue(pcm.size() >= expected.size(), "PCM must contain enough samples for snapshot.");
    for (size_t i = 0; i < expected.size(); ++i) {
        if (pcm[i] != expected[i]) {
            test::Fail("Snapshot mismatch at sample index " + std::to_string(i));
        }
    }
}

void TestDecodeUnderGainDrop() {
    bag::fsk::FskConfig config{};
    const std::string input = "GAIN-TEST";
    auto pcm = bag::fsk::EncodeTextToPcm16(input, config);

    for (auto& sample : pcm) {
        sample = static_cast<int16_t>(sample / 2);
    }

    const auto decoded = bag::fsk::DecodePcm16ToText(pcm, config);
    test::AssertEq(decoded, input, "Decode should survive moderate amplitude attenuation.");
}
}  // namespace

int main() {
    test::Runner runner;
    runner.Add("E2E.PipelineRoundTripWithoutFile", TestPipelineRoundTripWithoutFile);
    runner.Add("E2E.TextToWavToTextRoundTrip", TestTextToWavToTextRoundTrip);
    runner.Add("E2E.SnapshotFirstSamplesStable", TestSnapshotFirstSamplesStable);
    runner.Add("E2E.DecodeUnderGainDrop", TestDecodeUnderGainDrop);
    return runner.Run();
}
