#include <algorithm>
#include <cstdint>
#include <string>
#include <vector>

#include "bag_api.h"
#include "test_framework.h"
#include "test_fs.h"
#include "test_vectors.h"
#include "wav_io.h"

namespace {

bag_encoder_config MakeEncoderConfig(const test::ConfigCase& config_case) {
    bag_encoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig(const test::ConfigCase& config_case) {
    bag_decoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.reserved = 0;
    return config;
}

std::vector<int16_t> EncodeToVector(const bag_encoder_config& config, const std::string& text) {
    bag_pcm16_result pcm{};
    const auto encode_code = bag_encode_text(&config, text.c_str(), &pcm);
    test::AssertEq(encode_code, BAG_OK, "Artifact encode should succeed.");

    std::vector<int16_t> output;
    if (pcm.sample_count > 0) {
        output.assign(pcm.samples, pcm.samples + pcm.sample_count);
    }
    bag_free_pcm16_result(&pcm);
    return output;
}

std::string DecodeFromVector(const bag_decoder_config& config, const std::vector<int16_t>& pcm) {
    bag_decoder* decoder = nullptr;
    const auto create_code = bag_create_decoder(&config, &decoder);
    test::AssertEq(create_code, BAG_OK, "Artifact decoder creation should succeed.");

    const auto push_code = bag_push_pcm(decoder, pcm.data(), pcm.size(), 0);
    test::AssertEq(push_code, BAG_OK, "Artifact PCM push should succeed.");

    std::vector<char> text_buffer(4096, '\0');
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    const auto poll_code = bag_poll_result(decoder, &result);
    test::AssertEq(poll_code, BAG_OK, "Artifact poll should succeed after push.");
    bag_destroy_decoder(decoder);
    return std::string(text_buffer.data(), result.text_size);
}

void AssertPcmProperties(const std::vector<int16_t>& pcm,
                         const std::string& text,
                         const test::ConfigCase& config_case) {
    const auto expected_length = text.size() * 8 * static_cast<size_t>(config_case.frame_samples);
    test::AssertEq(pcm.size(), expected_length, "PCM length should match bytes * 8 * frame size.");

    const auto [min_it, max_it] = std::minmax_element(pcm.begin(), pcm.end());
    test::AssertTrue(min_it != pcm.end(), "PCM should not be empty for non-empty artifact corpus.");
    test::AssertTrue(*min_it >= static_cast<int16_t>(-32767), "Artifact PCM min out of range.");
    test::AssertTrue(*max_it <= static_cast<int16_t>(32767), "Artifact PCM max out of range.");
}

void TestArtifactDirectRoundTripAcrossCorpusAndConfigs() {
    for (const auto& config_case : test::ConfigCases()) {
        const auto encoder_config = MakeEncoderConfig(config_case);
        const auto decoder_config = MakeDecoderConfig(config_case);

        for (const auto& corpus_case : test::CorpusCases()) {
            const auto pcm = EncodeToVector(encoder_config, corpus_case.text);
            AssertPcmProperties(pcm, corpus_case.text, config_case);

            const auto decoded = DecodeFromVector(decoder_config, pcm);
            test::AssertEq(
                decoded, corpus_case.text, "Direct PCM roundtrip should preserve original text.");
        }
    }
}

void TestArtifactWavRoundTripAcrossCorpusAndConfigs() {
    for (const auto& config_case : test::ConfigCases()) {
        const auto encoder_config = MakeEncoderConfig(config_case);
        const auto decoder_config = MakeDecoderConfig(config_case);

        for (const auto& corpus_case : test::CorpusCases()) {
            const auto pcm = EncodeToVector(encoder_config, corpus_case.text);
            AssertPcmProperties(pcm, corpus_case.text, config_case);

            const auto dir = test::MakeTempDir("artifact");
            const auto wav_path = dir / (config_case.name + "_" + corpus_case.name + ".wav");
            audio_io::WriteMonoPcm16Wav(wav_path, config_case.sample_rate_hz, pcm);

            const auto wav = audio_io::ReadMonoPcm16Wav(wav_path);
            test::AssertEq(
                wav.sample_rate_hz,
                config_case.sample_rate_hz,
                "WAV sample rate should match the encoding configuration.");

            const auto decoded = DecodeFromVector(decoder_config, wav.mono_pcm);
            test::AssertEq(decoded, corpus_case.text, "WAV roundtrip should preserve original text.");
        }
    }
}

void TestArtifactDecodeUnderGainDrop() {
    const std::string input = "GAIN-TEST";
    for (const auto& config_case : test::ConfigCases()) {
        const auto encoder_config = MakeEncoderConfig(config_case);
        const auto decoder_config = MakeDecoderConfig(config_case);
        auto pcm = EncodeToVector(encoder_config, input);

        for (auto& sample : pcm) {
            sample = static_cast<int16_t>(sample / 2);
        }

        const auto decoded = DecodeFromVector(decoder_config, pcm);
        test::AssertEq(decoded, input, "Decode should survive moderate amplitude attenuation.");
    }
}

void TestArtifactVersionMatchesRelease() {
    const char* version = bag_core_version();
    test::AssertTrue(version != nullptr, "Artifact version pointer should not be null.");
    test::AssertEq(
        std::string(version),
        std::string(test::kExpectedCoreVersion),
        "Artifact layer should observe the current release version.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Artifact.DirectRoundTripAcrossCorpusAndConfigs", TestArtifactDirectRoundTripAcrossCorpusAndConfigs);
    runner.Add("Artifact.WavRoundTripAcrossCorpusAndConfigs", TestArtifactWavRoundTripAcrossCorpusAndConfigs);
    runner.Add("Artifact.DecodeUnderGainDrop", TestArtifactDecodeUnderGainDrop);
    runner.Add("Artifact.VersionMatchesRelease", TestArtifactVersionMatchesRelease);
    return runner.Run();
}
