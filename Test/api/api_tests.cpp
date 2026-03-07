#include <string>
#include <vector>

#include "bag_api.h"
#include "test_framework.h"
#include "test_vectors.h"

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

std::string DecodeViaApi(const bag_decoder_config& config, const bag_pcm16_result& pcm) {
    bag_decoder* decoder = nullptr;
    const auto create_code = bag_create_decoder(&config, &decoder);
    test::AssertEq(create_code, BAG_OK, "Decoder creation should succeed.");
    test::AssertTrue(decoder != nullptr, "Decoder should not be null after creation.");

    const auto push_code = bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0);
    test::AssertEq(push_code, BAG_OK, "PCM push should succeed.");

    std::vector<char> text_buffer(4096, '\0');
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    const auto poll_code = bag_poll_result(decoder, &result);
    test::AssertEq(poll_code, BAG_OK, "Polling should succeed after valid push.");
    bag_destroy_decoder(decoder);
    return std::string(text_buffer.data(), result.text_size);
}

void TestApiEncodeDecodeRoundTripAcrossCorpusAndConfigs() {
    for (const auto& config_case : test::ConfigCases()) {
        const auto encoder_config = MakeEncoderConfig(config_case);
        const auto decoder_config = MakeDecoderConfig(config_case);

        for (const auto& corpus_case : test::CorpusCases()) {
            bag_pcm16_result pcm{};
            const auto encode_code =
                bag_encode_text(&encoder_config, corpus_case.text.c_str(), &pcm);
            test::AssertEq(
                encode_code, BAG_OK, "C API encode should succeed across corpus and configs.");

            const auto decoded = DecodeViaApi(decoder_config, pcm);
            test::AssertEq(decoded, corpus_case.text, "API roundtrip should preserve original text.");
            bag_free_pcm16_result(&pcm);
        }
    }
}

void TestApiEncodeRejectsInvalidArguments() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case);
    bag_pcm16_result pcm{};

    test::AssertEq(
        bag_encode_text(nullptr, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Null encoder config should be rejected.");
    test::AssertEq(
        bag_encode_text(&encoder_config, nullptr, &pcm),
        BAG_INVALID_ARGUMENT,
        "Null text should be rejected.");
    test::AssertEq(
        bag_encode_text(&encoder_config, "A", nullptr),
        BAG_INVALID_ARGUMENT,
        "Null output PCM result should be rejected.");

    auto invalid_config = encoder_config;
    invalid_config.sample_rate_hz = 0;
    test::AssertEq(
        bag_encode_text(&invalid_config, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Zero sample rate should be rejected.");

    invalid_config = encoder_config;
    invalid_config.frame_samples = 0;
    test::AssertEq(
        bag_encode_text(&invalid_config, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Zero frame size should be rejected.");
}

void TestApiCreateDecoderRejectsInvalidArguments() {
    const auto config_case = test::ConfigCases().front();
    const auto decoder_config = MakeDecoderConfig(config_case);
    bag_decoder* decoder = nullptr;

    test::AssertEq(
        bag_create_decoder(nullptr, &decoder),
        BAG_INVALID_ARGUMENT,
        "Null decoder config should be rejected.");
    test::AssertEq(
        bag_create_decoder(&decoder_config, nullptr),
        BAG_INVALID_ARGUMENT,
        "Null decoder output should be rejected.");

    auto invalid_config = decoder_config;
    invalid_config.sample_rate_hz = 0;
    test::AssertEq(
        bag_create_decoder(&invalid_config, &decoder),
        BAG_INVALID_ARGUMENT,
        "Zero sample rate should be rejected when creating decoder.");

    invalid_config = decoder_config;
    invalid_config.frame_samples = 0;
    test::AssertEq(
        bag_create_decoder(&invalid_config, &decoder),
        BAG_INVALID_ARGUMENT,
        "Zero frame size should be rejected when creating decoder.");
}

void TestApiPushRejectsInvalidArguments() {
    const auto config_case = test::ConfigCases().front();
    const auto decoder_config = MakeDecoderConfig(config_case);
    const auto encoder_config = MakeEncoderConfig(config_case);

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, "PUSH", &pcm),
        BAG_OK,
        "Encoding should succeed for push argument checks.");

    bag_decoder* decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &decoder),
        BAG_OK,
        "Decoder creation should succeed for push argument checks.");

    test::AssertEq(
        bag_push_pcm(nullptr, pcm.samples, pcm.sample_count, 0),
        BAG_INVALID_ARGUMENT,
        "Null decoder should be rejected for push.");
    test::AssertEq(
        bag_push_pcm(decoder, nullptr, pcm.sample_count, 0),
        BAG_INVALID_ARGUMENT,
        "Null sample buffer should be rejected for push.");
    test::AssertEq(
        bag_push_pcm(decoder, pcm.samples, 0, 0),
        BAG_INVALID_ARGUMENT,
        "Zero sample count should be rejected for push.");

    bag_destroy_decoder(decoder);
    bag_free_pcm16_result(&pcm);
}

void TestApiPollNotReadyAndResetLifecycle() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case);
    const auto decoder_config = MakeDecoderConfig(config_case);

    bag_decoder* decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &decoder),
        BAG_OK,
        "Decoder creation should succeed for poll lifecycle.");

    std::vector<char> buffer(256, '\0');
    bag_text_result result{};
    result.buffer = buffer.data();
    result.buffer_size = buffer.size();

    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling before push should return not ready.");
    test::AssertEq(result.text_size, static_cast<size_t>(0), "Text size should be zero when not ready.");
    test::AssertEq(std::string(result.buffer), std::string(), "Output buffer should be cleared.");

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, "RESET", &pcm),
        BAG_OK,
        "Encoding should succeed for reset lifecycle.");
    test::AssertEq(
        bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0),
        BAG_OK,
        "Push before reset should succeed.");

    bag_reset(decoder);
    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling after reset should return not ready.");

    bag_destroy_decoder(decoder);
    bag_free_pcm16_result(&pcm);
}

void TestApiPollRejectsInvalidArguments() {
    const auto config_case = test::ConfigCases().front();
    const auto decoder_config = MakeDecoderConfig(config_case);
    bag_decoder* decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &decoder),
        BAG_OK,
        "Decoder creation should succeed for poll argument checks.");

    bag_text_result result{};
    test::AssertEq(
        bag_poll_result(nullptr, &result),
        BAG_INVALID_ARGUMENT,
        "Null decoder should be rejected for poll.");
    test::AssertEq(
        bag_poll_result(decoder, nullptr),
        BAG_INVALID_ARGUMENT,
        "Null output result should be rejected for poll.");

    bag_destroy_decoder(decoder);
}

void TestApiFreePcmResultIsIdempotent() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case);
    bag_pcm16_result pcm{};

    test::AssertEq(
        bag_encode_text(&encoder_config, "free", &pcm),
        BAG_OK,
        "Encoding should succeed before freeing PCM.");
    test::AssertTrue(pcm.samples != nullptr, "PCM result should own a buffer before free.");

    bag_free_pcm16_result(&pcm);
    test::AssertTrue(pcm.samples == nullptr, "First free should clear PCM pointer.");
    test::AssertEq(pcm.sample_count, static_cast<size_t>(0), "First free should clear sample count.");

    bag_free_pcm16_result(&pcm);
    test::AssertTrue(pcm.samples == nullptr, "Second free should keep PCM pointer null.");
    test::AssertEq(pcm.sample_count, static_cast<size_t>(0), "Second free should keep sample count zero.");
}

void TestApiVersionMatchesRelease() {
    const char* version = bag_core_version();
    test::AssertTrue(version != nullptr, "Core version pointer should not be null.");
    test::AssertEq(
        std::string(version),
        std::string(test::kExpectedCoreVersion),
        "Core version should match the current release.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Api.EncodeDecodeRoundTripAcrossCorpusAndConfigs", TestApiEncodeDecodeRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.EncodeRejectsInvalidArguments", TestApiEncodeRejectsInvalidArguments);
    runner.Add("Api.CreateDecoderRejectsInvalidArguments", TestApiCreateDecoderRejectsInvalidArguments);
    runner.Add("Api.PushRejectsInvalidArguments", TestApiPushRejectsInvalidArguments);
    runner.Add("Api.PollNotReadyAndResetLifecycle", TestApiPollNotReadyAndResetLifecycle);
    runner.Add("Api.PollRejectsInvalidArguments", TestApiPollRejectsInvalidArguments);
    runner.Add("Api.FreePcmResultIsIdempotent", TestApiFreePcmResultIsIdempotent);
    runner.Add("Api.VersionMatchesRelease", TestApiVersionMatchesRelease);
    return runner.Run();
}
