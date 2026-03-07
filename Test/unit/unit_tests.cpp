#include <algorithm>
#include <cmath>
#include <cstdint>
#include <filesystem>
#include <string>
#include <vector>

#include "bag_api.h"
#include "bag/fsk/fsk_codec.h"
#include "test_framework.h"
#include "test_fs.h"
#include "wav_io.h"

namespace {
bag_encoder_config MakeEncoderConfig() {
    bag_encoder_config config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 2205;
    config.enable_diagnostics = 0;
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig() {
    bag_decoder_config config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 2205;
    config.enable_diagnostics = 0;
    config.reserved = 0;
    return config;
}

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

void TestApiEncodeProducesExpectedLength() {
    const bag_encoder_config config = MakeEncoderConfig();
    bag_pcm16_result pcm{};

    const bag_error_code code = bag_encode_text(&config, "A", &pcm);
    test::AssertEq(code, BAG_OK, "C API encode should succeed.");
    test::AssertEq(
        pcm.sample_count,
        static_cast<size_t>(8 * config.frame_samples),
        "C API encode should emit one frame per bit.");

    bag_free_pcm16_result(&pcm);
}

void TestApiEncodeDecodeRoundTrip() {
    const bag_encoder_config encoder_config = MakeEncoderConfig();
    const bag_decoder_config decoder_config = MakeDecoderConfig();
    bag_pcm16_result pcm{};

    const bag_error_code encode_code = bag_encode_text(&encoder_config, "API-ROUNDTRIP", &pcm);
    test::AssertEq(encode_code, BAG_OK, "C API encode should succeed for roundtrip.");

    bag_decoder* decoder = nullptr;
    const bag_error_code create_code = bag_create_decoder(&decoder_config, &decoder);
    test::AssertEq(create_code, BAG_OK, "Decoder creation should succeed.");
    test::AssertTrue(decoder != nullptr, "Decoder instance should be created.");

    const bag_error_code push_code = bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0);
    test::AssertEq(push_code, BAG_OK, "PCM push should succeed.");

    std::vector<char> text_buffer(1024, '\0');
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    const bag_error_code poll_code = bag_poll_result(decoder, &result);
    test::AssertEq(poll_code, BAG_OK, "Polling result should succeed after push.");
    test::AssertEq(
        std::string(text_buffer.data(), result.text_size),
        std::string("API-ROUNDTRIP"),
        "API encode/decode roundtrip should preserve text.");

    bag_destroy_decoder(decoder);
    bag_free_pcm16_result(&pcm);
}

void TestApiFreePcm16ResultClearsStruct() {
    const bag_encoder_config config = MakeEncoderConfig();
    bag_pcm16_result pcm{};

    const bag_error_code code = bag_encode_text(&config, "free", &pcm);
    test::AssertEq(code, BAG_OK, "C API encode should succeed before free.");
    test::AssertTrue(pcm.samples != nullptr, "PCM result should own a buffer before free.");

    bag_free_pcm16_result(&pcm);
    test::AssertTrue(pcm.samples == nullptr, "Free should clear PCM pointer.");
    test::AssertEq(pcm.sample_count, static_cast<size_t>(0), "Free should clear sample count.");
}

void TestApiVersionAvailable() {
    const char* version = bag_core_version();
    test::AssertTrue(version != nullptr, "Core version pointer should not be null.");
    test::AssertEq(std::string(version), std::string("0.1.1"), "Core version should match release.");
}
}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Unit.EncodeLengthMatchesExpected", TestEncodeLengthMatchesExpected);
    runner.Add("Unit.EncodeAmplitudeInRange", TestEncodeAmplitudeInRange);
    runner.Add("Unit.DecodeEmptyInputReturnsEmptyText", TestDecodeEmptyInputReturnsEmptyText);
    runner.Add("Unit.WavIoMonoRoundTrip", TestWavIoMonoRoundTrip);
    runner.Add("Unit.ApiEncodeProducesExpectedLength", TestApiEncodeProducesExpectedLength);
    runner.Add("Unit.ApiEncodeDecodeRoundTrip", TestApiEncodeDecodeRoundTrip);
    runner.Add("Unit.ApiFreePcm16ResultClearsStruct", TestApiFreePcm16ResultClearsStruct);
    runner.Add("Unit.ApiVersionAvailable", TestApiVersionAvailable);
    return runner.Run();
}
