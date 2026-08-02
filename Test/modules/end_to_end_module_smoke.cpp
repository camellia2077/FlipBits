#include "test_std_support.h"
#include "test_framework.h"
#include "test_utf8.h"
#include "test_vectors.h"

import bag.common.config;
import bag.common.error_code;
import bag.flash.codec;
import bag.flash.phy_clean;
import bag.pipeline;
import bag.pro.codec;
import bag.pro.phy_clean;
import bag.transport.facade;
import bag.ultra.codec;
import bag.ultra.phy_clean;

namespace {

bag::CoreConfig MakeCoreConfig(bag::TransportMode mode = bag::TransportMode::kFlash) {
    bag::CoreConfig config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 2205;
    config.enable_diagnostics = false;
    config.mode = mode;
    config.reserved = 0;
    return config;
}

std::unique_ptr<bag::IPipeline> MakePipeline(
    bag::TransportMode mode = bag::TransportMode::kFlash) {
    return bag::CreatePipeline(MakeCoreConfig(mode));
}

std::unique_ptr<bag::ITransportDecoder> MakeTransportDecoder(
    bag::TransportMode mode = bag::TransportMode::kFlash) {
    return bag::CreateTransportDecoder(MakeCoreConfig(mode));
}

std::vector<std::int16_t> EncodeForModeReference(bag::TransportMode mode, const std::string& text) {
    const auto config = MakeCoreConfig(mode);
    std::vector<std::int16_t> pcm;
    if (mode == bag::TransportMode::kFlash) {
        test::AssertEq(
            bag::flash::EncodeTextToPcm16(config, text, &pcm),
            bag::ErrorCode::kOk,
            "Flash clean module encode should succeed.");
        return pcm;
    }

    if (mode == bag::TransportMode::kPro) {
        test::AssertEq(
            bag::pro::EncodeTextToPcm16(config, text, &pcm),
            bag::ErrorCode::kOk,
            "Pro clean module encode should succeed.");
        return pcm;
    }

    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(config, text, &pcm),
        bag::ErrorCode::kOk,
        "Ultra clean module encode should succeed.");
    return pcm;
}

std::vector<std::int16_t> EncodeForModeFacade(bag::TransportMode mode, const std::string& text) {
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::EncodeTextToPcm16(MakeCoreConfig(mode), text, &pcm),
        bag::ErrorCode::kOk,
        "Transport facade module encode should succeed.");
    return pcm;
}

void PushAndPollExpectingText(std::unique_ptr<bag::IPipeline> pipeline,
                              const std::vector<std::int16_t>& pcm,
                              bag::TransportMode mode,
                              std::string_view text) {
    test::AssertTrue(pipeline != nullptr, "Pipeline module should return an instance.");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 123;

    test::AssertEq(
        pipeline->PushPcm(block),
        bag::ErrorCode::kOk,
        "Pipeline module push should succeed for encoded PCM.");

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Pipeline module poll should succeed after encoded PCM push.");
    test::AssertEq(result.text, std::string(text), "Pipeline module should recover the original text.");
    test::AssertTrue(result.complete, "Pipeline module result should be marked complete.");
    test::AssertEq(result.confidence, 1.0f, "Pipeline module confidence should remain simplified.");
    test::AssertEq(result.mode, mode, "Pipeline module should preserve the decoded transport mode.");
}

void PushAndPollViaTransportDecoderExpectingText(std::unique_ptr<bag::ITransportDecoder> decoder,
                                                 const std::vector<std::int16_t>& pcm,
                                                 bag::TransportMode mode,
                                                 std::string_view text) {
    test::AssertTrue(decoder != nullptr, "Transport facade module should return a decoder.");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 456;

    test::AssertEq(
        decoder->PushPcm(block),
        bag::ErrorCode::kOk,
        "Transport decoder module push should succeed for encoded PCM.");

    bag::TextResult result{};
    test::AssertEq(
        decoder->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Transport decoder module poll should succeed after encoded PCM push.");
    test::AssertEq(result.text, std::string(text), "Transport decoder module should recover original text.");
    test::AssertTrue(result.complete, "Transport decoder module result should be complete.");
    test::AssertEq(result.confidence, 1.0f, "Transport decoder module confidence should remain simplified.");
    test::AssertEq(result.mode, mode, "Transport decoder module should preserve configured mode.");
}

void TestFlashCodecRoundTrip() {
    const std::string text = test::Utf8Literal(u8"你好，FlipBits");
    std::vector<std::uint8_t> bytes;
    test::AssertEq(
        bag::flash::EncodeTextToBytes(text, &bytes),
        bag::ErrorCode::kOk,
        "Flash codec module should accept raw UTF-8 bytes.");
    test::AssertEq(
        bytes,
        std::vector<std::uint8_t>(text.begin(), text.end()),
        "Flash codec module should preserve the original raw bytes.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodeBytesToText(bytes, &decoded),
        bag::ErrorCode::kOk,
        "Flash codec module decode should succeed.");
    test::AssertEq(decoded, text, "Flash codec module should roundtrip raw UTF-8 text.");
}

void TestFlashPhyCleanRoundTrip() {
    const auto config = MakeCoreConfig(bag::TransportMode::kFlash);
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(config, test::Utf8Literal(u8"你好，FlipBits"), &pcm),
        bag::ErrorCode::kOk,
        "Flash clean module encode should succeed.");
    test::AssertTrue(!pcm.empty(), "Flash clean module should emit PCM for non-empty input.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Flash clean module decode should succeed.");
    test::AssertEq(
        decoded,
        test::Utf8Literal(u8"你好，FlipBits"),
        "Flash clean module should roundtrip UTF-8 text.");
}

void TestTransportFacadeEncodeMatchesReferenceModes() {
    const auto flash_pcm = EncodeForModeFacade(
        bag::TransportMode::kFlash,
        test::Utf8Literal(u8"你好，FlipBits"));
    test::AssertEq(
        flash_pcm,
        EncodeForModeReference(
            bag::TransportMode::kFlash,
            test::Utf8Literal(u8"你好，FlipBits")),
        "Flash transport facade module should delegate to the formal flash signal+voicing path.");

    const auto pro_pcm = EncodeForModeFacade(bag::TransportMode::kPro, "Hello-123");
    test::AssertEq(
        pro_pcm,
        EncodeForModeReference(bag::TransportMode::kPro, "Hello-123"),
        "Pro transport facade module should delegate to the pro clean path.");

    const auto ultra_pcm = EncodeForModeFacade(
        bag::TransportMode::kUltra,
        test::Utf8Literal(u8"FlipBits 超级模式 🚀"));
    test::AssertEq(
        ultra_pcm,
        EncodeForModeReference(
            bag::TransportMode::kUltra,
            test::Utf8Literal(u8"FlipBits 超级模式 🚀")),
        "Ultra transport facade module should delegate to the ultra clean path.");
}

void TestTransportFacadeValidation() {
    auto config = MakeCoreConfig();
    config.sample_rate_hz = 0;
    test::AssertEq(
        bag::ValidateEncodeRequest(config, "A"),
        bag::TransportValidationIssue::kInvalidSampleRate,
        "Transport facade module should reject zero sample rate.");

    config = MakeCoreConfig();
    config.frame_samples = 0;
    test::AssertEq(
        bag::ValidateEncodeRequest(config, "A"),
        bag::TransportValidationIssue::kInvalidFrameSamples,
        "Transport facade module should reject zero frame size.");

    config = MakeCoreConfig();
    config.mode = static_cast<bag::TransportMode>(99);
    test::AssertEq(
        bag::ValidateDecoderConfig(config),
        bag::TransportValidationIssue::kInvalidMode,
        "Transport facade module should reject unknown modes.");

    config = MakeCoreConfig(bag::TransportMode::kUltra);
    config.sample_rate_hz = 8;
    test::AssertEq(
        bag::ValidateEncodeRequest(config, "A"),
        bag::TransportValidationIssue::kInvalidSampleRate,
        "Transport facade module should reject Ultra rates that cannot provide one sample per symbol.");

    config = MakeCoreConfig(bag::TransportMode::kFlash);
    test::AssertEq(
        bag::ValidateEncodeRequest(config, std::string(513, 'F')),
        bag::TransportValidationIssue::kOk,
        "Flash facade validation should not inherit the old framed payload limit.");
    const auto flash_utf8 = test::Utf8Literal(u8"你好，FlipBits");
    test::AssertEq(
        bag::ValidateEncodeRequest(config, flash_utf8),
        bag::TransportValidationIssue::kOk,
        "Flash facade validation should continue to allow raw UTF-8 text.");

    config = MakeCoreConfig(bag::TransportMode::kPro);
    const auto pro_non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag::ValidateEncodeRequest(config, pro_non_ascii),
        bag::TransportValidationIssue::kProAsciiOnly,
        "Transport facade module should keep the pro ASCII-only rule.");
    test::AssertEq(
        bag::ValidateEncodeRequest(config, test::BuildTooLongProCorpus()),
        bag::TransportValidationIssue::kOk,
        "Pro facade validation should not inherit the old compat single-frame limit.");

    config = MakeCoreConfig(bag::TransportMode::kUltra);
    test::AssertEq(
        bag::ValidateEncodeRequest(config, test::BuildTooLongUltraCorpus()),
        bag::TransportValidationIssue::kOk,
        "Ultra facade validation should not inherit the old compat single-frame limit.");
}

void TestProPhyCleanRoundTrip() {
    const auto config = MakeCoreConfig(bag::TransportMode::kPro);
    const std::string text = "Hello-123";
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::pro::EncodeTextToPcm16(config, text, &pcm),
        bag::ErrorCode::kOk,
        "Pro clean module encode should succeed.");
    test::AssertEq(
        pcm.size(),
        text.size() * bag::pro::kSymbolsPerPayloadByte * static_cast<std::size_t>(config.frame_samples),
        "Pro clean module PCM length should be byte count * 2 symbols * frame size.");

    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Pro clean module decode should succeed.");
    test::AssertEq(decoded, text, "Pro clean module should roundtrip ASCII text.");
}

void TestProPayloadUsesRawAsciiBytes() {
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeTextToPayload(test::BuildMaxProCorpus(), &payload),
        bag::ErrorCode::kOk,
        "Pro codec module should accept the representative long ASCII corpus.");
    test::AssertEq(
        payload.size(),
        test::BuildMaxProCorpus().size(),
        "Pro codec module payload length should remain equal to the ASCII byte count.");

    test::AssertEq(
        bag::pro::EncodeTextToPayload(test::BuildTooLongProCorpus(), &payload),
        bag::ErrorCode::kOk,
        "Pro codec module should keep accepting ASCII text beyond the old compat limit.");
    test::AssertEq(
        payload.size(),
        test::BuildTooLongProCorpus().size(),
        "Pro codec module payload should stay as raw ASCII bytes for longer corpus inputs.");
}

void TestUltraTextCodecRoundTrip() {
    std::vector<std::uint8_t> payload;
    const std::string input = test::Utf8Literal(u8"FlipBits 超级模式 🚀");
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(input, &payload),
        bag::ErrorCode::kOk,
        "Ultra codec module payload encode should succeed.");

    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePayloadToText(payload, &decoded),
        bag::ErrorCode::kOk,
        "Ultra codec module payload decode should succeed.");
    test::AssertEq(decoded, input, "Ultra codec module payload decode should preserve UTF-8 bytes.");

    std::vector<std::uint8_t> bits;
    test::AssertEq(
        bag::ultra::EncodePayloadToVaricodeBits(payload, &bits),
        bag::ErrorCode::kOk,
        "Ultra codec module Varicode encode should succeed.");

    std::vector<std::uint8_t> decoded_payload;
    test::AssertEq(
        bag::ultra::DecodeVaricodeBits(bits, &decoded_payload),
        bag::ErrorCode::kOk,
        "Ultra codec module Varicode decode should succeed.");
    test::AssertEq(decoded_payload, payload, "Ultra Varicode decode should recover UTF-8 bytes.");

    std::vector<std::uint8_t> symbols;
    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols(
            payload, bag::ultra::MakeMfsk16Config(MakeCoreConfig(bag::TransportMode::kUltra)),
            &symbols),
        bag::ErrorCode::kOk,
        "Ultra symbol codec should encode MFSK16 symbols.");
    decoded_payload.clear();
    test::AssertEq(
        bag::ultra::DecodeSymbolsToPayload(symbols, &decoded_payload),
        bag::ErrorCode::kOk,
        "Ultra symbol codec should decode MFSK16 symbols.");
    test::AssertEq(decoded_payload, payload,
                   "Ultra symbol codec should roundtrip payload.");

    const std::string punctuation_input = "FlipBits: encode & decode!";
    std::vector<std::uint8_t> punctuation_payload;
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(punctuation_input,
                                        &punctuation_payload),
        bag::ErrorCode::kOk,
        "Ultra punctuation setup should encode payload.");
    std::vector<std::uint8_t> punctuation_symbols;
    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols(
            punctuation_payload,
            bag::ultra::MakeMfsk16Config(MakeCoreConfig(bag::TransportMode::kUltra)),
            &punctuation_symbols),
        bag::ErrorCode::kOk,
        "Ultra punctuation setup should encode symbols.");
    std::vector<std::uint8_t> punctuation_decoded;
    test::AssertEq(
        bag::ultra::DecodeSymbolsToPayload(punctuation_symbols,
                                           &punctuation_decoded),
        bag::ErrorCode::kOk,
        "Ultra punctuation symbols should decode.");
    std::string punctuation_text;
    test::AssertEq(
        bag::ultra::DecodePayloadToText(punctuation_decoded,
                                        &punctuation_text),
        bag::ErrorCode::kOk,
        "Ultra punctuation payload should decode to text.");
    test::AssertEq(punctuation_text, punctuation_input,
                   "Ultra punctuation should preserve the terminal exclamation mark.");
}

void TestUltraPhyCleanRoundTrip() {
    const auto config = MakeCoreConfig(bag::TransportMode::kUltra);
    const std::string input = test::Utf8Literal(u8"FlipBits 超级模式 🚀");
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Ultra clean module encode should succeed.");
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(input, &payload), bag::ErrorCode::kOk,
        "Ultra PCM length setup should encode payload.");
    std::vector<std::uint8_t> symbols;
    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols(
            payload, bag::ultra::MakeMfsk16Config(config), &symbols),
        bag::ErrorCode::kOk,
        "Ultra PCM length setup should encode MFSK16 symbols.");
    test::AssertEq(
        pcm.size(),
        bag::ultra::TotalSamplesForSymbols(44100, symbols.size()),
        "Ultra MFSK16 PCM length should match the cumulative symbol boundaries.");

    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Ultra clean module decode should succeed.");
    test::AssertEq(decoded, input, "Ultra clean module should roundtrip UTF-8 text.");
}

void TestUltraFractionalSymbolBoundaries() {
    test::AssertEq(
        bag::ultra::NominalSymbolSamples(44100), 2822,
        "Ultra nominal symbol width should remain an informational rounded value.");
    test::AssertEq(
        bag::ultra::SymbolBoundarySample(44100, 1), static_cast<std::size_t>(2822),
        "Ultra first 44.1 kHz symbol boundary should use the rational rate.");
    test::AssertEq(
        bag::ultra::SymbolBoundarySample(44100, 2), static_cast<std::size_t>(5645),
        "Ultra second 44.1 kHz symbol boundary should accumulate fractional samples.");
    test::AssertEq(
        bag::ultra::SymbolBoundarySample(44100, 3), static_cast<std::size_t>(8467),
        "Ultra third 44.1 kHz symbol boundary should accumulate fractional samples.");
    test::AssertEq(
        bag::ultra::TotalSamplesForSymbols(16000, 7), static_cast<std::size_t>(7168),
        "Ultra integral-rate boundaries should remain exactly one kilohertz-grid symbol wide.");

    const auto config = MakeCoreConfig(bag::TransportMode::kUltra);
    const std::string input = "fractional-boundary";
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Ultra fractional-boundary encode should succeed.");
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(input, &payload), bag::ErrorCode::kOk,
        "Ultra fractional-boundary payload setup should succeed.");
    std::vector<std::uint8_t> symbols;
    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols(
            payload, bag::ultra::MakeMfsk16Config(config), &symbols),
        bag::ErrorCode::kOk,
        "Ultra fractional-boundary symbol setup should succeed.");
    test::AssertEq(
        pcm.size(), bag::ultra::TotalSamplesForSymbols(44100, symbols.size()),
        "Ultra fractional-boundary PCM should end exactly at the final boundary.");
    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Ultra fractional-boundary decode should succeed.");
    test::AssertEq(
        decoded, input,
        "Ultra fractional-boundary PCM should roundtrip through the clean decoder.");
}

void TestUltra31_25BdRoundTrip() {
    const auto core_config = MakeCoreConfig(bag::TransportMode::kUltra);
    const auto speed = bag::ultra::Mfsk16Speed::k31_25Bd;
    const auto config = bag::ultra::MakeMfsk16Config(core_config, speed);
    test::AssertEq(config.symbol_rate_baud, 31.25,
                   "Ultra fast config should use 31.25 Bd.");
    test::AssertEq(config.freqs_hz[1], 1031.25,
                   "Ultra fast config should use 31.25 Hz tone spacing.");
    test::AssertEq(config.freqs_hz[15], 1468.75,
                   "Ultra fast config should generate all 16 tones on the fast grid.");
    test::AssertEq(config.symbol_samples, 1411,
                   "Ultra fast nominal symbol width should be rounded for 44.1 kHz.");
    test::AssertEq(
        bag::ultra::SymbolBoundarySample(44100, 1, 31.25),
        static_cast<std::size_t>(1411),
        "Ultra fast first symbol boundary should use the 31.25 Bd rate.");
    test::AssertEq(
        bag::ultra::SymbolBoundarySample(44100, 2, 31.25),
        static_cast<std::size_t>(2822),
        "Ultra fast second symbol boundary should accumulate exact samples.");
    test::AssertEq(
        bag::ultra::SymbolBoundarySample(44100, 3, 31.25),
        static_cast<std::size_t>(4234),
        "Ultra fast third symbol boundary should retain the fractional remainder.");

    const std::string input = "core-only 31.25 Bd";
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(core_config, input, &pcm, speed),
        bag::ErrorCode::kOk,
        "Ultra fast clean module encode should succeed.");
    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePcm16ToText(core_config, pcm, &decoded, speed),
        bag::ErrorCode::kOk,
        "Ultra fast clean module decode should succeed.");
    test::AssertEq(decoded, input,
                   "Ultra fast clean module should roundtrip core-only text.");

    auto transport_config = core_config;
    transport_config.frame_samples = config.symbol_samples;
    std::vector<std::int16_t> transport_pcm;
    test::AssertEq(
        bag::EncodeTextToPcm16(transport_config, input, &transport_pcm),
        bag::ErrorCode::kOk,
        "Ultra fast transport facade encode should use the selected frame rate.");
    test::AssertEq(transport_pcm.size(), pcm.size(),
                   "Ultra fast transport facade should preserve PCM duration.");
    auto transport_decoder = bag::CreateTransportDecoder(transport_config);
    test::AssertTrue(transport_decoder != nullptr,
                     "Ultra fast transport facade should create a decoder.");
    bag::PcmBlock block{};
    block.samples = transport_pcm.data();
    block.sample_count = transport_pcm.size();
    test::AssertEq(transport_decoder->PushPcm(block), bag::ErrorCode::kOk,
                   "Ultra fast transport decoder should accept complete PCM.");
    bag::TextResult transport_result{};
    test::AssertEq(transport_decoder->PollTextResult(&transport_result),
                   bag::ErrorCode::kOk,
                   "Ultra fast transport decoder should decode complete PCM.");
    test::AssertEq(transport_result.text, input,
                   "Ultra fast transport facade should roundtrip core-only text.");

    const auto auto_decode_config = [&]() {
        auto value = core_config;
        value.frame_samples = 0;
        return value;
    }();
    const auto assert_auto_decode = [&](const std::vector<std::int16_t>& audio,
                                        const std::string& expected,
                                        const char* label) {
        auto decoder = bag::CreateTransportDecoder(auto_decode_config);
        test::AssertTrue(decoder != nullptr, label);
        bag::PcmBlock auto_block{};
        auto_block.samples = audio.data();
        auto_block.sample_count = audio.size();
        test::AssertEq(decoder->PushPcm(auto_block), bag::ErrorCode::kOk, label);
        bag::TextResult auto_result{};
        test::AssertEq(decoder->PollTextResult(&auto_result), bag::ErrorCode::kOk,
                       label);
        test::AssertEq(auto_result.text, expected, label);
    };

    std::vector<std::int16_t> slow_pcm;
    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(core_config, input, &slow_pcm),
        bag::ErrorCode::kOk,
        "Ultra slow PCM should be available for automatic-rate decoding.");
    assert_auto_decode(
        slow_pcm, input,
        "Ultra decoder should infer 15.625 Bd from mode and sample rate only.");
    assert_auto_decode(
        transport_pcm, input,
        "Ultra decoder should infer 31.25 Bd from mode and sample rate only.");

    std::string direct_auto_decoded;
    test::AssertEq(
        bag::ultra::DecodePcm16ToText(auto_decode_config, slow_pcm,
                                       &direct_auto_decoded),
        bag::ErrorCode::kOk,
        "Direct Ultra decode should auto-select the slow rate.");
    test::AssertEq(direct_auto_decoded, input,
                   "Direct Ultra slow auto decode should preserve text.");
    direct_auto_decoded.clear();
    test::AssertEq(
        bag::ultra::DecodePcm16ToText(auto_decode_config, transport_pcm,
                                       &direct_auto_decoded),
        bag::ErrorCode::kOk,
        "Direct Ultra decode should auto-select the fast rate.");
    test::AssertEq(direct_auto_decoded, input,
                   "Direct Ultra fast auto decode should preserve text.");

    auto encode_operation = bag::CreateEncodeOperation(transport_config, input);
    test::AssertTrue(encode_operation != nullptr,
                     "Ultra fast encode operation should be created.");
    test::AssertEq(encode_operation->Run(), bag::ErrorCode::kOk,
                   "Ultra fast encode operation should complete.");
    bag::EncodedPcmFollowResult operation_result{};
    test::AssertEq(encode_operation->TakeResult(&operation_result),
                   bag::ErrorCode::kOk,
                   "Ultra fast encode operation should expose its result.");
    test::AssertEq(operation_result.pcm.size(), pcm.size(),
                   "Ultra fast encode operation should use fast symbol boundaries.");
}

void TestUltraRejectsInvalidInputs() {
    const auto config = MakeCoreConfig(bag::TransportMode::kUltra);
    const auto mfsk_config = bag::ultra::MakeMfsk16Config(config);
    std::vector<std::uint8_t> output_payload;
    std::vector<std::uint8_t> output_symbols;

    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols({}, mfsk_config, &output_symbols),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject an empty payload for symbol encoding.");
    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols({static_cast<std::uint8_t>('A')},
                                           mfsk_config, nullptr),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject a null symbol output.");
    test::AssertEq(
        bag::ultra::DecodeVaricodeBits({2}, &output_payload),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject Varicode bits outside 0 and 1.");
    test::AssertEq(
        bag::ultra::DecodeVaricodeBits({1, 0}, &output_payload),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject an incomplete Varicode codeword.");
    test::AssertEq(
        bag::ultra::DecodeVaricodeBits({1}, nullptr),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject a null Varicode output.");
    test::AssertEq(
        bag::ultra::DecodeSymbolsToPayload({}, &output_payload),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject an empty symbol stream.");

    const std::vector<std::uint8_t> payload = {'A'};
    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols(payload, mfsk_config,
                                           &output_symbols),
        bag::ErrorCode::kOk,
        "Ultra invalid-input setup should encode symbols.");
    auto bad_preamble = output_symbols;
    bad_preamble[0] = 1;
    test::AssertEq(
        bag::ultra::DecodeSymbolsToPayload(bad_preamble, &output_payload),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject a symbol stream with a bad preamble.");
    auto bad_tail = output_symbols;
    bad_tail.back() = 1;
    test::AssertEq(
        bag::ultra::DecodeSymbolsToPayload(bad_tail, &output_payload),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject a symbol stream with a bad tail.");
    test::AssertEq(
        bag::ultra::DecodeSymbolsToPayload({16}, &output_payload),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject a tone index outside the 16-tone range.");

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::ultra::EncodePayloadToPcm16(payload, mfsk_config, &pcm),
        bag::ErrorCode::kOk,
        "Ultra invalid-PCM setup should encode PCM.");
    std::vector<std::int16_t> truncated_pcm(pcm.begin(), pcm.end() - 1);
    test::AssertEq(
        bag::ultra::DecodePcm16ToSymbols(truncated_pcm, mfsk_config,
                                         &output_symbols),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject PCM truncated at a non-boundary sample.");
    auto extra_pcm = pcm;
    extra_pcm.push_back(0);
    test::AssertEq(
        bag::ultra::DecodePcm16ToText(config, extra_pcm, nullptr),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject PCM with an unaligned trailing sample.");
    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(config, std::string(), &pcm),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject empty text encoding.");

    auto invalid_rate_config = mfsk_config;
    invalid_rate_config.sample_rate_hz = 8;
    test::AssertEq(
        bag::ultra::DecodePcm16ToSymbols(pcm, invalid_rate_config,
                                          &output_symbols),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject a sample rate below the boundary contract.");

    auto unsupported_symbol_rate_config = mfsk_config;
    unsupported_symbol_rate_config.symbol_rate_baud = 20.0;
    test::AssertEq(
        bag::ultra::DecodePcm16ToSymbols(
            pcm, unsupported_symbol_rate_config, &output_symbols),
        bag::ErrorCode::kInvalidArgument,
        "Ultra should reject symbol rates outside the supported core speeds.");
}

void TestUltraDecoderRequiresWholeRecording() {
    const auto config = MakeCoreConfig(bag::TransportMode::kUltra);
    const std::string input = "whole-recording-only";
    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::ultra::EncodeTextToPcm16(config, input, &pcm),
        bag::ErrorCode::kOk,
        "Ultra whole-recording decoder setup should encode PCM.");

    auto decoder = bag::ultra::CreateDecoder(config);
    test::AssertTrue(decoder != nullptr,
                     "Ultra whole-recording decoder should be created.");
    const std::size_t first_block_samples =
        bag::ultra::SymbolBoundarySample(config.sample_rate_hz, 4);
    bag::PcmBlock first_block{
        .samples = pcm.data(),
        .sample_count = first_block_samples,
        .timestamp_ms = 0,
    };
    test::AssertEq(
        decoder->PushPcm(first_block), bag::ErrorCode::kOk,
        "Ultra decoder should accept the first recording block.");
    bag::TextResult result{};
    test::AssertEq(
        decoder->PollTextResult(&result), bag::ErrorCode::kNotReady,
        "Ultra decoder should not emit text from a partial recording.");
    test::AssertTrue(!result.complete,
                     "Ultra partial-recording result should not be complete.");

    bag::PcmBlock remaining_block{
        .samples = pcm.data() + first_block_samples,
        .sample_count = pcm.size() - first_block_samples,
        .timestamp_ms = static_cast<std::int64_t>(first_block_samples),
    };
    test::AssertEq(
        decoder->PushPcm(remaining_block), bag::ErrorCode::kOk,
        "Ultra decoder should accept the remaining recording block.");
    test::AssertEq(
        decoder->PollTextResult(&result), bag::ErrorCode::kOk,
        "Ultra decoder should decode after the whole recording arrives.");
    test::AssertEq(result.text, input,
                   "Ultra whole-recording decoder should recover the input.");
    test::AssertTrue(result.complete,
                     "Ultra whole-recording result should be complete.");
}

void TestUltraPayloadUsesUtf8Bytes() {
    std::vector<std::uint8_t> payload;
    const std::string max_input = test::BuildMaxUltraCorpus();
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(max_input, &payload),
        bag::ErrorCode::kOk,
        "Ultra codec module should accept representative large UTF-8 input.");
    test::AssertEq(
        payload.size(),
        static_cast<std::size_t>(512),
        "Ultra representative corpus should occupy exactly 512 UTF-8 bytes.");

    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePayloadToText(payload, &decoded),
        bag::ErrorCode::kOk,
        "Ultra representative payload decode should succeed.");
    test::AssertEq(decoded, max_input, "Ultra representative payload decode should preserve UTF-8 bytes.");

    const std::string too_long_input = test::BuildTooLongUltraCorpus();
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(too_long_input, &payload),
        bag::ErrorCode::kOk,
        "Ultra codec module should keep accepting UTF-8 input beyond the old compat limit.");
    test::AssertEq(
        payload.size(),
        static_cast<std::size_t>(513),
        "Extended ultra corpus should occupy 513 UTF-8 bytes.");
}

void TestPipelinePushPollLifecycle() {
    auto pipeline = MakePipeline();
    const auto pcm = EncodeForModeFacade(bag::TransportMode::kFlash, "PIPE");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();
    block.timestamp_ms = 123;

    test::AssertEq(
        pipeline->PushPcm(block),
        bag::ErrorCode::kOk,
        "Pipeline module push should succeed.");

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kOk,
        "Pipeline module poll should succeed after push.");
    test::AssertEq(result.text, std::string("PIPE"), "Pipeline module should decode the original text.");
    test::AssertTrue(result.complete, "Pipeline module result should be marked complete.");
    test::AssertEq(result.confidence, 1.0f, "Pipeline module confidence should match simplified value.");
    test::AssertEq(result.mode, bag::TransportMode::kFlash, "Flash pipeline module should report flash mode.");

    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kNotReady,
        "Pipeline module should report not ready after pending result is consumed.");
    test::AssertEq(result.text, std::string(), "Pipeline module should clear text on not ready.");
    test::AssertTrue(!result.complete, "Pipeline module complete flag should reset on not ready.");
}

void TestPipelineResetClearsPendingState() {
    auto pipeline = MakePipeline();
    const auto pcm = EncodeForModeFacade(bag::TransportMode::kFlash, "RESET");

    bag::PcmBlock block{};
    block.samples = pcm.data();
    block.sample_count = pcm.size();

    test::AssertEq(
        pipeline->PushPcm(block),
        bag::ErrorCode::kOk,
        "Pipeline module push before reset should succeed.");
    pipeline->Reset();

    bag::TextResult result{};
    test::AssertEq(
        pipeline->PollTextResult(&result),
        bag::ErrorCode::kNotReady,
        "Pipeline module reset should clear pending decode state.");
    test::AssertEq(result.text, std::string(), "Pipeline module reset should clear buffered text state.");
    test::AssertTrue(!result.complete, "Pipeline module reset should clear completion state.");
}

void TestPipelineFlashUtf8RoundTrip() {
    const auto flash_utf8 = test::Utf8Literal(u8"你好，FlipBits");
    PushAndPollExpectingText(
        MakePipeline(bag::TransportMode::kFlash),
        EncodeForModeReference(bag::TransportMode::kFlash, flash_utf8),
        bag::TransportMode::kFlash,
        flash_utf8);
}

void TestPipelineProRoundTrip() {
    PushAndPollExpectingText(
        MakePipeline(bag::TransportMode::kPro),
        EncodeForModeReference(bag::TransportMode::kPro, "Hello-123"),
        bag::TransportMode::kPro,
        "Hello-123");
}

void TestPipelineUltraRoundTrip() {
    const auto ultra_utf8 = test::Utf8Literal(u8"FlipBits 超级模式 🚀");
    PushAndPollExpectingText(
        MakePipeline(bag::TransportMode::kUltra),
        EncodeForModeReference(bag::TransportMode::kUltra, ultra_utf8),
        bag::TransportMode::kUltra,
        ultra_utf8);
}

void TestTransportDecoderRoundTripAcrossModes() {
    const auto flash_utf8 = test::Utf8Literal(u8"你好，FlipBits");
    PushAndPollViaTransportDecoderExpectingText(
        MakeTransportDecoder(bag::TransportMode::kFlash),
        EncodeForModeFacade(bag::TransportMode::kFlash, flash_utf8),
        bag::TransportMode::kFlash,
        flash_utf8);
    PushAndPollViaTransportDecoderExpectingText(
        MakeTransportDecoder(bag::TransportMode::kPro),
        EncodeForModeFacade(bag::TransportMode::kPro, "Hello-123"),
        bag::TransportMode::kPro,
        "Hello-123");
    const auto ultra_utf8 = test::Utf8Literal(u8"FlipBits 超级模式 🚀");
    PushAndPollViaTransportDecoderExpectingText(
        MakeTransportDecoder(bag::TransportMode::kUltra),
        EncodeForModeFacade(bag::TransportMode::kUltra, ultra_utf8),
        bag::TransportMode::kUltra,
        ultra_utf8);
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("ModulesEndToEnd.FlashCodecRoundTrip", TestFlashCodecRoundTrip);
    runner.Add("ModulesEndToEnd.FlashPhyCleanRoundTrip", TestFlashPhyCleanRoundTrip);
    runner.Add("ModulesEndToEnd.TransportFacadeEncodeMatchesReferenceModes", TestTransportFacadeEncodeMatchesReferenceModes);
    runner.Add("ModulesEndToEnd.TransportFacadeValidation", TestTransportFacadeValidation);
    runner.Add("ModulesEndToEnd.ProPhyCleanRoundTrip", TestProPhyCleanRoundTrip);
    runner.Add("ModulesEndToEnd.ProPayloadUsesRawAsciiBytes", TestProPayloadUsesRawAsciiBytes);
    runner.Add("ModulesEndToEnd.UltraTextCodecRoundTrip", TestUltraTextCodecRoundTrip);
    runner.Add("ModulesEndToEnd.UltraPhyCleanRoundTrip", TestUltraPhyCleanRoundTrip);
    runner.Add("ModulesEndToEnd.UltraFractionalSymbolBoundaries",
               TestUltraFractionalSymbolBoundaries);
    runner.Add("ModulesEndToEnd.Ultra31_25BdRoundTrip",
               TestUltra31_25BdRoundTrip);
    runner.Add("ModulesEndToEnd.UltraRejectsInvalidInputs",
               TestUltraRejectsInvalidInputs);
    runner.Add("ModulesEndToEnd.UltraDecoderRequiresWholeRecording",
               TestUltraDecoderRequiresWholeRecording);
    runner.Add("ModulesEndToEnd.UltraPayloadUsesUtf8Bytes", TestUltraPayloadUsesUtf8Bytes);
    runner.Add("ModulesEndToEnd.PipelinePushPollLifecycle", TestPipelinePushPollLifecycle);
    runner.Add("ModulesEndToEnd.PipelineResetClearsPendingState", TestPipelineResetClearsPendingState);
    runner.Add("ModulesEndToEnd.PipelineFlashUtf8RoundTrip", TestPipelineFlashUtf8RoundTrip);
    runner.Add("ModulesEndToEnd.PipelineProRoundTrip", TestPipelineProRoundTrip);
    runner.Add("ModulesEndToEnd.PipelineUltraRoundTrip", TestPipelineUltraRoundTrip);
    runner.Add("ModulesEndToEnd.TransportDecoderRoundTripAcrossModes", TestTransportDecoderRoundTripAcrossModes);
    return runner.Run();
}
