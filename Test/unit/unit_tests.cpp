#include <algorithm>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "bag/common/config.h"
#include "bag/common/error_code.h"
#include "bag/common/types.h"
#include "bag/fsk/fsk_codec.h"
#include "bag/pipeline/pipeline.h"
#include "test_framework.h"
#include "test_fs.h"
#include "wav_io.h"

namespace {

bag::CoreConfig MakeCoreConfig() {
    bag::CoreConfig config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 2205;
    config.enable_diagnostics = false;
    config.reserved = 0;
    return config;
}

bag::fsk::FskConfig MakeFskConfig() {
    bag::fsk::FskConfig config{};
    config.sample_rate_hz = 44100;
    config.bit_duration_sec = 0.05;
    return config;
}

std::unique_ptr<bag::IPipeline> MakePipeline() {
    return bag::CreatePipeline(MakeCoreConfig());
}

void TestEncodeLengthMatchesExpected() {
    const auto config = MakeFskConfig();
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
    const auto config = MakeFskConfig();
    const auto pcm = bag::fsk::EncodeTextToPcm16("Hello", config);
    const auto [min_it, max_it] = std::minmax_element(pcm.begin(), pcm.end());
    test::AssertTrue(min_it != pcm.end(), "PCM should not be empty for non-empty input.");
    test::AssertTrue(*min_it >= static_cast<int16_t>(-32767), "PCM min out of range.");
    test::AssertTrue(*max_it <= static_cast<int16_t>(32767), "PCM max out of range.");
}

void TestDecodeEmptyInputReturnsEmptyText() {
    const auto config = MakeFskConfig();
    const std::vector<int16_t> pcm;
    const std::string decoded = bag::fsk::DecodePcm16ToText(pcm, config);
    test::AssertEq(decoded, std::string(), "Decoding empty input should return empty string.");
}

void TestWavIoMonoRoundTrip() {
    const auto config = MakeFskConfig();
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

void TestPipelinePushPollLifecycle() {
    auto pipeline = MakePipeline();
    const auto config = MakeFskConfig();
    const auto pcm = bag::fsk::EncodeTextToPcm16("PIPE", config);

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 123;

    const auto push_code = pipeline->PushPcm(block);
    test::AssertEq(push_code, bag::ErrorCode::kOk, "Pipeline push should succeed.");

    bag::TextResult result{};
    const auto poll_code = pipeline->PollTextResult(&result);
    test::AssertEq(poll_code, bag::ErrorCode::kOk, "Pipeline poll should succeed after push.");
    test::AssertEq(result.text, std::string("PIPE"), "Pipeline should decode the original text.");
    test::AssertTrue(result.complete, "Pipeline result should be marked complete.");
    test::AssertEq(result.confidence, 1.0f, "Pipeline confidence should match simplified value.");

    const auto second_poll_code = pipeline->PollTextResult(&result);
    test::AssertEq(
        second_poll_code,
        bag::ErrorCode::kNotReady,
        "Pipeline should report not ready after pending result is consumed.");
    test::AssertEq(result.text, std::string(), "Pipeline should clear text on not ready.");
    test::AssertTrue(!result.complete, "Pipeline complete flag should reset on not ready.");
}

void TestPipelineResetClearsPendingState() {
    auto pipeline = MakePipeline();
    const auto config = MakeFskConfig();
    const auto pcm = bag::fsk::EncodeTextToPcm16("RESET", config);

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();

    test::AssertEq(
        pipeline->PushPcm(block), bag::ErrorCode::kOk, "Pipeline push before reset should succeed.");
    pipeline->Reset();

    bag::TextResult result{};
    const auto poll_after_reset = pipeline->PollTextResult(&result);
    test::AssertEq(
        poll_after_reset,
        bag::ErrorCode::kNotReady,
        "Pipeline reset should clear pending decode state.");
    test::AssertEq(result.text, std::string(), "Reset should clear buffered text state.");
    test::AssertTrue(!result.complete, "Reset should clear completion state.");
}

void TestSnapshotFirstSamplesStable() {
    const auto config = MakeFskConfig();
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

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Unit.EncodeLengthMatchesExpected", TestEncodeLengthMatchesExpected);
    runner.Add("Unit.EncodeAmplitudeInRange", TestEncodeAmplitudeInRange);
    runner.Add("Unit.DecodeEmptyInputReturnsEmptyText", TestDecodeEmptyInputReturnsEmptyText);
    runner.Add("Unit.WavIoMonoRoundTrip", TestWavIoMonoRoundTrip);
    runner.Add("Unit.PipelinePushPollLifecycle", TestPipelinePushPollLifecycle);
    runner.Add("Unit.PipelineResetClearsPendingState", TestPipelineResetClearsPendingState);
    runner.Add("Unit.SnapshotFirstSamplesStable", TestSnapshotFirstSamplesStable);
    return runner.Run();
}
