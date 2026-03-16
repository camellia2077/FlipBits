#include <algorithm>
#include <string>
#include <vector>

#include "bag_api.h"
#include "test_framework.h"
#include "test_utf8.h"
#include "test_vectors.h"

namespace {

struct DecodeResult {
    bag_error_code code = BAG_INTERNAL;
    std::string text;
    bag_transport_mode mode = BAG_TRANSPORT_FLASH;
};

bag_encoder_config MakeEncoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode = BAG_TRANSPORT_FLASH,
                                     bag_flash_signal_profile flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                                     bag_flash_voicing_flavor flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_CODED_BURST) {
    bag_encoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.flash_signal_profile = flash_signal_profile;
    config.flash_voicing_flavor = flash_voicing_flavor;
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig(const test::ConfigCase& config_case,
                                     bag_transport_mode mode = BAG_TRANSPORT_FLASH,
                                     bag_flash_signal_profile flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                                     bag_flash_voicing_flavor flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_CODED_BURST) {
    bag_decoder_config config{};
    config.sample_rate_hz = config_case.sample_rate_hz;
    config.frame_samples = config_case.frame_samples;
    config.enable_diagnostics = 0;
    config.mode = mode;
    config.flash_signal_profile = flash_signal_profile;
    config.flash_voicing_flavor = flash_voicing_flavor;
    config.reserved = 0;
    return config;
}

std::size_t RoundHalfUpFrameScale(int frame_samples, int numerator, int denominator) {
    return frame_samples > 0
               ? static_cast<std::size_t>((frame_samples * numerator + (denominator / 2)) / denominator)
               : static_cast<std::size_t>(0);
}

std::size_t ExpectedFlashSampleCount(const std::string& text,
                                     const test::ConfigCase& config_case,
                                     bag_flash_signal_profile flash_signal_profile,
                                     bag_flash_voicing_flavor flash_voicing_flavor) {
    const std::size_t frame_samples =
        config_case.frame_samples > 0 ? static_cast<std::size_t>(config_case.frame_samples) : static_cast<std::size_t>(0);
    const std::size_t payload_samples_per_bit =
        flash_signal_profile == BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT
            ? RoundHalfUpFrameScale(config_case.frame_samples, 3, 1)
            : frame_samples;
    const std::size_t leading_nonpayload_samples =
        flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT
            ? frame_samples * static_cast<std::size_t>(16)
            : frame_samples * static_cast<std::size_t>(3);
    const std::size_t trailing_nonpayload_samples =
        flash_voicing_flavor == BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT
            ? frame_samples * static_cast<std::size_t>(8)
            : frame_samples * static_cast<std::size_t>(3);
    return text.size() * static_cast<std::size_t>(8) * payload_samples_per_bit +
           leading_nonpayload_samples +
           trailing_nonpayload_samples;
}

bag_visualization_result AnalyzeVisualizationViaApi(const bag_decoder_config& config,
                                                    const bag_pcm16_result& pcm) {
    bag_visualization_result result{};
    const auto analyze_code = bag_analyze_visualization(&config, pcm.samples, pcm.sample_count, &result);
    test::AssertEq(analyze_code, BAG_OK, "Visualization analysis should succeed.");
    return result;
}

DecodeResult DecodeViaApi(const bag_decoder_config& config, const bag_pcm16_result& pcm) {
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
    bag_destroy_decoder(decoder);

    DecodeResult out{};
    out.code = poll_code;
    out.text.assign(text_buffer.data(), result.text_size);
    out.mode = result.mode;
    return out;
}

void AssertRoundTripAcrossCorpus(const std::vector<test::CorpusCase>& corpus,
                                 bag_transport_mode mode) {
    for (const auto& config_case : test::ConfigCases()) {
        const auto encoder_config = MakeEncoderConfig(config_case, mode);
        const auto decoder_config = MakeDecoderConfig(config_case, mode);

        for (const auto& corpus_case : corpus) {
            bag_pcm16_result pcm{};
            const auto encode_code =
                bag_encode_text(&encoder_config, corpus_case.text.c_str(), &pcm);
            test::AssertEq(encode_code, BAG_OK, "C API encode should succeed across corpus and configs.");

            const auto decoded = DecodeViaApi(decoder_config, pcm);
            test::AssertEq(decoded.code, BAG_OK, "Polling should succeed after valid push.");
            test::AssertEq(decoded.text, corpus_case.text, "API roundtrip should preserve original text.");
            test::AssertEq(decoded.mode, mode, "API result mode should match the configured mode.");
            bag_free_pcm16_result(&pcm);
        }
    }
}

void TestApiFlashRoundTripAcrossCorpusAndConfigs() {
    AssertRoundTripAcrossCorpus(test::FlashCorpusCases(), BAG_TRANSPORT_FLASH);
}

void TestApiProRoundTripAcrossCorpusAndConfigs() {
    AssertRoundTripAcrossCorpus(test::ProCorpusCases(), BAG_TRANSPORT_PRO);
}

void TestApiUltraRoundTripAcrossCorpusAndConfigs() {
    AssertRoundTripAcrossCorpus(test::UltraCorpusCases(), BAG_TRANSPORT_ULTRA);
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

    invalid_config = encoder_config;
    invalid_config.mode = static_cast<bag_transport_mode>(99);
    test::AssertEq(
        bag_encode_text(&invalid_config, "A", &pcm),
        BAG_INVALID_ARGUMENT,
        "Unknown encoder mode should be rejected.");
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
        "Zero decoder sample rate should be rejected.");

    invalid_config = decoder_config;
    invalid_config.frame_samples = 0;
    test::AssertEq(
        bag_create_decoder(&invalid_config, &decoder),
        BAG_INVALID_ARGUMENT,
        "Zero decoder frame size should be rejected.");

    invalid_config = decoder_config;
    invalid_config.mode = static_cast<bag_transport_mode>(99);
    test::AssertEq(
        bag_create_decoder(&invalid_config, &decoder),
        BAG_INVALID_ARGUMENT,
        "Unknown decoder mode should be rejected.");
}

void TestApiPollAndResetLifecycle() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
    const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, "RESET", &pcm),
        BAG_OK,
        "Encoding for lifecycle test should succeed.");

    bag_decoder* decoder = nullptr;
    test::AssertEq(
        bag_create_decoder(&decoder_config, &decoder),
        BAG_OK,
        "Decoder creation for lifecycle test should succeed.");

    std::vector<char> text_buffer(256, '\0');
    bag_text_result result{};
    result.buffer = text_buffer.data();
    result.buffer_size = text_buffer.size();

    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling before push should return not ready.");

    test::AssertEq(
        bag_push_pcm(decoder, pcm.samples, pcm.sample_count, 0),
        BAG_OK,
        "PCM push should succeed before reset.");
    bag_reset(decoder);
    test::AssertEq(
        bag_poll_result(decoder, &result),
        BAG_NOT_READY,
        "Polling after reset should return not ready.");

    bag_destroy_decoder(decoder);
    bag_free_pcm16_result(&pcm);
}

void TestApiModeSpecificValidation() {
    const auto config_case = test::ConfigCases().front();
    bag_pcm16_result pcm{};

    auto pro_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
    const auto pro_non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag_encode_text(&pro_config, pro_non_ascii.c_str(), &pcm),
        BAG_INVALID_ARGUMENT,
        "Pro mode should reject non-ASCII input.");
    test::AssertEq(
        bag_encode_text(&pro_config, test::BuildTooLongProCorpus().c_str(), &pcm),
        BAG_OK,
        "Pro mode should no longer inherit the compat single-frame limit.");
    bag_free_pcm16_result(&pcm);

    auto ultra_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
    test::AssertEq(
        bag_encode_text(&ultra_config, test::BuildTooLongUltraCorpus().c_str(), &pcm),
        BAG_OK,
        "Ultra mode should no longer inherit the compat single-frame limit.");
    bag_free_pcm16_result(&pcm);
}

void TestApiBoundarySuccessCases() {
    const auto config_case = test::ConfigCases().front();

    {
        const auto long_flash_text = std::string(513, 'F');
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, long_flash_text.c_str(), &pcm),
            BAG_OK,
            "Flash mode should not inherit the single-frame payload limit.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Flash long-text decode should succeed.");
        test::AssertEq(decoded.text, long_flash_text, "Flash long-text roundtrip should preserve text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_FLASH, "Flash long-text decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_PRO);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, test::BuildTooLongProCorpus().c_str(), &pcm),
            BAG_OK,
            "Pro mode should accept extended ASCII text beyond the old compat limit.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Pro boundary decode should succeed.");
        test::AssertEq(
            decoded.text,
            test::BuildTooLongProCorpus(),
            "Pro boundary roundtrip should preserve the extended ASCII text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_PRO, "Pro boundary decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, test::BuildTooLongUltraCorpus().c_str(), &pcm),
            BAG_OK,
            "Ultra mode should accept extended UTF-8 text beyond the old compat limit.");
        const auto decoded = DecodeViaApi(decoder_config, pcm);
        test::AssertEq(decoded.code, BAG_OK, "Ultra boundary decode should succeed.");
        test::AssertEq(
            decoded.text,
            test::BuildTooLongUltraCorpus(),
            "Ultra boundary roundtrip should preserve the extended UTF-8 text.");
        test::AssertEq(decoded.mode, BAG_TRANSPORT_ULTRA, "Ultra boundary decode should preserve mode.");
        bag_free_pcm16_result(&pcm);
    }
}

void TestApiFlashConfigAffectsLengthAndRoundTrip() {
    const std::string text = "Length";

    for (const auto& config_case : test::ConfigCases()) {
        const auto coded_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                BAG_FLASH_VOICING_FLAVOR_CODED_BURST);
        const auto coded_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                BAG_FLASH_VOICING_FLAVOR_CODED_BURST);
        const auto ritual_encoder =
            MakeEncoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
                BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);
        const auto ritual_decoder =
            MakeDecoderConfig(
                config_case,
                BAG_TRANSPORT_FLASH,
                BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
                BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);

        bag_pcm16_result coded_pcm{};
        bag_pcm16_result ritual_pcm{};
        test::AssertEq(
            bag_encode_text(&coded_encoder, text.c_str(), &coded_pcm),
            BAG_OK,
            "coded_burst flash encode should succeed through the C API.");
        test::AssertEq(
            bag_encode_text(&ritual_encoder, text.c_str(), &ritual_pcm),
            BAG_OK,
            "ritual_chant flash encode should succeed through the C API.");
        test::AssertEq(
            coded_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
                BAG_FLASH_VOICING_FLAVOR_CODED_BURST),
            "coded_burst C API flash length should stay on the baseline explicit configuration.");
        test::AssertEq(
            ritual_pcm.sample_count,
            ExpectedFlashSampleCount(
                text,
                config_case,
                BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
                BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT),
            "ritual_chant C API flash length should include the longer timing and shell configuration.");
        test::AssertTrue(
            ritual_pcm.sample_count > coded_pcm.sample_count,
            "ritual_chant flash output should be longer than coded_burst for the same text.");

        const auto coded_decoded = DecodeViaApi(coded_decoder, coded_pcm);
        const auto ritual_decoded = DecodeViaApi(ritual_decoder, ritual_pcm);
        test::AssertEq(coded_decoded.code, BAG_OK, "coded_burst flash decode should succeed.");
        test::AssertEq(ritual_decoded.code, BAG_OK, "ritual_chant flash decode should succeed.");
        test::AssertEq(coded_decoded.text, text, "coded_burst flash decode should preserve text.");
        test::AssertEq(ritual_decoded.text, text, "ritual_chant flash decode should preserve text.");

        bag_free_pcm16_result(&coded_pcm);
        bag_free_pcm16_result(&ritual_pcm);
    }
}

void TestApiFlashDecodeRequiresMatchingConfig() {
    const auto config_case = test::ConfigCases().front();
    const auto ritual_encoder =
        MakeEncoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT,
            BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT);
    const auto wrong_decoder =
        MakeDecoderConfig(
            config_case,
            BAG_TRANSPORT_FLASH,
            BAG_FLASH_SIGNAL_PROFILE_CODED_BURST,
            BAG_FLASH_VOICING_FLAVOR_CODED_BURST);
    const std::string text = "Mismatch";

    bag_pcm16_result ritual_pcm{};
    test::AssertEq(
        bag_encode_text(&ritual_encoder, text.c_str(), &ritual_pcm),
        BAG_OK,
        "ritual_chant flash encode should succeed before wrong-style decode validation.");

    const auto decoded = DecodeViaApi(wrong_decoder, ritual_pcm);
    test::AssertTrue(
        decoded.code != BAG_OK || decoded.text != text,
        "Decoding ritual flash PCM with coded_burst signal/flavor should not look like a valid roundtrip.");
    bag_free_pcm16_result(&ritual_pcm);
}

void TestApiFlashVisualizationProducesFrameTrack() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_FLASH);
    const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);
    const std::string text = "Visual";

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, text.c_str(), &pcm),
        BAG_OK,
        "Flash encode should succeed before visualization analysis.");

    auto visualization = AnalyzeVisualizationViaApi(decoder_config, pcm);
    test::AssertTrue(visualization.frames != nullptr, "Visualization frames should be allocated for flash PCM.");
    test::AssertTrue(visualization.frame_count > 0, "Visualization should produce at least one frame.");
    test::AssertEq(
        visualization.total_samples,
        static_cast<int>(pcm.sample_count),
        "Visualization total sample count should match the analyzed PCM.");
    test::AssertEq(
        visualization.sample_rate_hz,
        config_case.sample_rate_hz,
        "Visualization sample rate should mirror the decoder config.");
    test::AssertEq(
        visualization.frame_stride_samples,
        std::max(1, config_case.frame_samples / 4),
        "Visualization stride should use the fixed quarter-frame bucket.");
    test::AssertEq(
        visualization.frames[0].region_kind,
        BAG_VISUALIZATION_REGION_LEADING_SHELL,
        "Flash visualization should classify the first frame as leading shell.");

    bool has_payload_frame = false;
    for (std::size_t index = 0; index < visualization.frame_count; ++index) {
        const auto& frame = visualization.frames[index];
        if (frame.region_kind == BAG_VISUALIZATION_REGION_PAYLOAD) {
            has_payload_frame = true;
        }
        test::AssertTrue(frame.rms >= 0.0f && frame.rms <= 1.0f, "Visualization RMS must stay normalized.");
        test::AssertTrue(frame.peak >= 0.0f && frame.peak <= 1.0f, "Visualization peak must stay normalized.");
        test::AssertTrue(
            frame.brightness >= 0.0f && frame.brightness <= 1.0f,
            "Visualization brightness must stay normalized.");
    }
    test::AssertTrue(has_payload_frame, "Flash visualization should classify at least one frame as payload.");

    bag_free_visualization_result(&visualization);
    bag_free_pcm16_result(&pcm);
}

void TestApiVisualizationReturnsNotImplementedOutsideFlash() {
    const auto config_case = test::ConfigCases().front();

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_PRO);
        bag_pcm16_result pcm{};
        test::AssertEq(
            bag_encode_text(&encoder_config, "PRO", &pcm),
            BAG_OK,
            "Pro encode should succeed before visualization availability check.");
        bag_visualization_result visualization{};
        test::AssertEq(
            bag_analyze_visualization(&decoder_config, pcm.samples, pcm.sample_count, &visualization),
            BAG_NOT_IMPLEMENTED,
            "Visualization should report not implemented for pro mode.");
        bag_free_pcm16_result(&pcm);
    }

    {
        const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_ULTRA);
        bag_pcm16_result pcm{};
        const auto ultra_text = test::Utf8Literal(u8"超");
        test::AssertEq(
            bag_encode_text(&encoder_config, ultra_text.c_str(), &pcm),
            BAG_OK,
            "Ultra encode should succeed before visualization availability check.");
        bag_visualization_result visualization{};
        test::AssertEq(
            bag_analyze_visualization(&decoder_config, pcm.samples, pcm.sample_count, &visualization),
            BAG_NOT_IMPLEMENTED,
            "Visualization should report not implemented for ultra mode.");
        bag_free_pcm16_result(&pcm);
    }
}

void TestApiVisualizationRejectsInvalidArgumentsAndFreesSafely() {
    const auto config_case = test::ConfigCases().front();
    const auto decoder_config = MakeDecoderConfig(config_case, BAG_TRANSPORT_FLASH);
    bag_visualization_result visualization{};

    test::AssertEq(
        bag_analyze_visualization(nullptr, nullptr, 0, &visualization),
        BAG_INVALID_ARGUMENT,
        "Visualization should reject a null decoder config.");
    test::AssertEq(
        bag_analyze_visualization(&decoder_config, nullptr, 0, &visualization),
        BAG_INVALID_ARGUMENT,
        "Visualization should reject null samples.");
    test::AssertEq(
        bag_analyze_visualization(&decoder_config, nullptr, 0, nullptr),
        BAG_INVALID_ARGUMENT,
        "Visualization should reject a null output result.");

    visualization.frames = new bag_visualization_frame[3];
    visualization.frame_count = 3;
    visualization.total_samples = 12;
    visualization.sample_rate_hz = 44100;
    visualization.frame_stride_samples = 120;
    bag_free_visualization_result(&visualization);
    test::AssertTrue(visualization.frames == nullptr, "Visualization free should clear the frame pointer.");
    test::AssertEq(visualization.frame_count, static_cast<size_t>(0), "Visualization free should clear the frame count.");
    test::AssertEq(visualization.total_samples, 0, "Visualization free should clear total samples.");
    test::AssertEq(visualization.sample_rate_hz, 0, "Visualization free should clear sample rate.");
    test::AssertEq(visualization.frame_stride_samples, 0, "Visualization free should clear stride.");
    bag_free_visualization_result(&visualization);
}

void TestApiFreePcmResultIsIdempotent() {
    const auto config_case = test::ConfigCases().front();
    const auto encoder_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);

    bag_pcm16_result pcm{};
    test::AssertEq(
        bag_encode_text(&encoder_config, "free", &pcm),
        BAG_OK,
        "PCM result should be allocated for a successful encode.");
    test::AssertTrue(pcm.samples != nullptr, "PCM result buffer should be allocated.");
    test::AssertTrue(pcm.sample_count > 0, "PCM result sample count should be non-zero.");

    bag_free_pcm16_result(&pcm);
    test::AssertTrue(pcm.samples == nullptr, "PCM result samples should clear after free.");
    test::AssertEq(pcm.sample_count, static_cast<size_t>(0), "PCM result count should clear after free.");

    bag_free_pcm16_result(&pcm);
    test::AssertTrue(pcm.samples == nullptr, "PCM free should remain safe on a cleared result.");
    test::AssertEq(
        pcm.sample_count,
        static_cast<size_t>(0),
        "PCM sample count should remain zero on repeated free.");
}

void TestApiVersionMatchesRelease() {
    const char* version = bag_core_version();
    test::AssertTrue(version != nullptr, "Version pointer should not be null.");
    test::AssertEq(
        std::string(version),
        std::string(test::kExpectedCoreVersion),
        "C API should expose the current release version.");
}

void TestApiValidationHelpers() {
    bag_transport_mode mode = BAG_TRANSPORT_FLASH;
    test::AssertTrue(
        bag_try_parse_transport_mode("flash", &mode) != 0,
        "Mode parser should accept flash.");
    test::AssertEq(mode, BAG_TRANSPORT_FLASH, "Mode parser should map flash correctly.");
    test::AssertTrue(
        bag_try_parse_transport_mode("pro", &mode) != 0,
        "Mode parser should accept pro.");
    test::AssertEq(mode, BAG_TRANSPORT_PRO, "Mode parser should map pro correctly.");
    test::AssertTrue(
        bag_try_parse_transport_mode("ultra", &mode) != 0,
        "Mode parser should accept ultra.");
    test::AssertEq(mode, BAG_TRANSPORT_ULTRA, "Mode parser should map ultra correctly.");
    test::AssertTrue(
        bag_try_parse_transport_mode("warp", &mode) == 0,
        "Mode parser should reject unsupported values.");

    test::AssertEq(
        std::string(bag_transport_mode_name(BAG_TRANSPORT_PRO)),
        std::string("pro"),
        "Mode name helper should return the stable CLI spelling.");

    const auto config_case = test::ConfigCases().front();
    auto pro_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_PRO);
    const auto pro_non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag_validate_encode_request(&pro_config, pro_non_ascii.c_str()),
        BAG_VALIDATION_PRO_ASCII_ONLY,
        "Validation helper should expose the pro ASCII-only rule.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_PRO_ASCII_ONLY),
        "ASCII",
        "Validation helper message should explain the ASCII-only rule.");
    test::AssertEq(
        bag_validate_encode_request(&pro_config, test::BuildTooLongProCorpus().c_str()),
        BAG_VALIDATION_OK,
        "Validation helper should reflect that pro no longer inherits the compat frame limit.");

    auto ultra_config = MakeEncoderConfig(config_case, BAG_TRANSPORT_ULTRA);
    test::AssertEq(
        bag_validate_encode_request(&ultra_config, test::BuildTooLongUltraCorpus().c_str()),
        BAG_VALIDATION_OK,
        "Validation helper should reflect that ultra no longer inherits the compat frame limit.");

    auto invalid_decoder = MakeDecoderConfig(config_case, static_cast<bag_transport_mode>(99));
    test::AssertEq(
        bag_validate_decoder_config(&invalid_decoder),
        BAG_VALIDATION_INVALID_MODE,
        "Decoder validation helper should reject unknown modes.");

    auto invalid_flash_encoder = MakeEncoderConfig(config_case);
    invalid_flash_encoder.flash_signal_profile = static_cast<bag_flash_signal_profile>(99);
    test::AssertEq(
        bag_validate_encode_request(&invalid_flash_encoder, "flash-style"),
        BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE,
        "Validation helper should reject unsupported flash signal profiles.");
    auto invalid_flash_decoder = MakeDecoderConfig(config_case);
    invalid_flash_decoder.flash_voicing_flavor = static_cast<bag_flash_voicing_flavor>(99);
    test::AssertEq(
        bag_validate_decoder_config(&invalid_flash_decoder),
        BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR,
        "Decoder validation helper should reject unsupported flash voicing flavors.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE),
        "signal profile",
        "Validation helper message should explain the flash signal-profile failure.");
    test::AssertContains(
        bag_validation_issue_message(BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR),
        "voicing flavor",
        "Validation helper message should explain the flash voicing-flavor failure.");
    test::AssertContains(
        bag_error_code_message(BAG_INTERNAL),
        "Internal",
        "Error message helper should expose a stable internal-error prompt.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Api.FlashRoundTripAcrossCorpusAndConfigs", TestApiFlashRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.ProRoundTripAcrossCorpusAndConfigs", TestApiProRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.UltraRoundTripAcrossCorpusAndConfigs", TestApiUltraRoundTripAcrossCorpusAndConfigs);
    runner.Add("Api.EncodeRejectsInvalidArguments", TestApiEncodeRejectsInvalidArguments);
    runner.Add("Api.CreateDecoderRejectsInvalidArguments", TestApiCreateDecoderRejectsInvalidArguments);
    runner.Add("Api.PollAndResetLifecycle", TestApiPollAndResetLifecycle);
    runner.Add("Api.ModeSpecificValidation", TestApiModeSpecificValidation);
    runner.Add("Api.BoundarySuccessCases", TestApiBoundarySuccessCases);
    runner.Add("Api.FlashConfigAffectsLengthAndRoundTrip", TestApiFlashConfigAffectsLengthAndRoundTrip);
    runner.Add("Api.FlashDecodeRequiresMatchingConfig", TestApiFlashDecodeRequiresMatchingConfig);
    runner.Add("Api.FlashVisualizationProducesFrameTrack", TestApiFlashVisualizationProducesFrameTrack);
    runner.Add("Api.VisualizationReturnsNotImplementedOutsideFlash",
               TestApiVisualizationReturnsNotImplementedOutsideFlash);
    runner.Add("Api.VisualizationRejectsInvalidArgumentsAndFreesSafely",
               TestApiVisualizationRejectsInvalidArgumentsAndFreesSafely);
    runner.Add("Api.FreePcmResultIsIdempotent", TestApiFreePcmResultIsIdempotent);
    runner.Add("Api.VersionMatchesRelease", TestApiVersionMatchesRelease);
    runner.Add("Api.ValidationHelpers", TestApiValidationHelpers);
    return runner.Run();
}
