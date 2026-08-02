#include "test_std_support.h"
#include "test_framework.h"
#include "test_utf8.h"

import bag.flash.signal;
import bag.flash.voicing;
import bag.pro.codec;
import bag.transport.compat.frame_codec;
import bag.ultra.codec;
import bag.ultra.phy_clean;

#include "leaf_module_smoke_support.h"

namespace {

void TestProCodecModule() {
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeTextToPayload("A", &payload),
        bag::ErrorCode::kOk,
        "Single-character pro payload encode should succeed.");
    test::AssertEq(
        payload,
        std::vector<std::uint8_t>{static_cast<std::uint8_t>('A')},
        "Pro codec module should preserve a single ASCII byte.");

    std::vector<std::uint8_t> symbols;
    test::AssertEq(
        bag::pro::EncodePayloadToSymbols(payload, &symbols),
        bag::ErrorCode::kOk,
        "Single-character pro symbol encode should succeed.");
    test::AssertEq(
        symbols,
        std::vector<std::uint8_t>{0x04, 0x01},
        "Pro codec module should map a byte to high and low nibbles.");

    payload.clear();
    test::AssertEq(
        bag::pro::EncodeTextToPayload("ASCII-123", &payload),
        bag::ErrorCode::kOk,
        "Pro codec module should encode ASCII payload.");
    test::AssertEq(
        payload,
        std::vector<std::uint8_t>{'A', 'S', 'C', 'I', 'I', '-', '1', '2', '3'},
        "Pro codec module should keep raw ASCII bytes.");

    test::AssertEq(
        bag::pro::EncodePayloadToSymbols(payload, &symbols),
        bag::ErrorCode::kOk,
        "Pro codec module should encode payload symbols.");
    test::AssertEq(
        symbols.size(),
        payload.size() * bag::pro::kSymbolsPerPayloadByte,
        "Pro codec module should emit two symbols per payload byte.");

    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePayloadToText(payload, &decoded),
        bag::ErrorCode::kOk,
        "Pro codec module should decode payload bytes back to text.");
    test::AssertEq(decoded, std::string("ASCII-123"), "Pro codec module should roundtrip ASCII text.");

    std::vector<std::uint8_t> decoded_payload;
    test::AssertEq(
        bag::pro::DecodeSymbolsToPayload(symbols, &decoded_payload),
        bag::ErrorCode::kOk,
        "Pro codec module should decode symbols back to payload.");
    test::AssertEq(decoded_payload, payload, "Pro codec module should roundtrip payload.");
}

void TestProCodecRejectsInvalidInput() {
    std::vector<std::uint8_t> payload;
    const auto non_ascii = test::Utf8Literal(u8"中文");
    test::AssertEq(
        bag::pro::EncodeTextToPayload(non_ascii, &payload),
        bag::ErrorCode::kInvalidArgument,
        "Pro codec module should reject non-ASCII input.");

    const std::vector<std::uint8_t> bad_payload = {0x80};
    std::string decoded;
    test::AssertEq(
        bag::pro::DecodePayloadToText(bad_payload, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Pro codec module should reject non-ASCII bytes.");

    const std::vector<std::uint8_t> odd_symbols = {0x04};
    test::AssertEq(
        bag::pro::DecodeSymbolsToPayload(odd_symbols, &payload),
        bag::ErrorCode::kInvalidArgument,
        "Pro codec module should reject odd nibble counts.");

    const std::vector<std::uint8_t> out_of_range_symbols = {0x04, 0x10};
    test::AssertEq(
        bag::pro::DecodeSymbolsToPayload(out_of_range_symbols, &payload),
        bag::ErrorCode::kInvalidArgument,
        "Pro codec module should reject nibble values outside 0x0..0xF.");
}

void TestUltraCodecModule() {
    const std::string text = test::Utf8Literal(u8"FlipBits 超级模式 🚀");
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(text, &payload),
        bag::ErrorCode::kOk,
        "Ultra codec module should encode UTF-8 payload.");

    std::vector<std::uint8_t> bits;
    test::AssertEq(
        bag::ultra::EncodePayloadToVaricodeBits(payload, &bits),
        bag::ErrorCode::kOk,
        "Ultra codec module should encode MFSK16 Varicode bits.");

    std::vector<std::uint8_t> decoded_payload;
    test::AssertEq(
        bag::ultra::DecodeVaricodeBits(bits, &decoded_payload),
        bag::ErrorCode::kOk,
        "Ultra codec module should decode MFSK16 Varicode bits.");
    test::AssertEq(decoded_payload, payload, "Ultra Varicode should roundtrip payload.");

    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePayloadToText(decoded_payload, &decoded),
        bag::ErrorCode::kOk,
        "Ultra codec module should decode UTF-8 payload bytes.");
    test::AssertEq(decoded, text, "Ultra codec module should roundtrip UTF-8 text.");
}

void TestUltraMfsk16StandardVectors() {
    // These expectations were calculated independently from the IZ8BLY table
    // and the MFSK16 NASA K=7 encoder/interleaver. Do not derive them from the
    // production encoder in this test: the purpose is to catch symmetric bugs
    // in the encode/decode round-trip.
    // Reference: https://www.qsl.net/zl1bpu/MFSK/Varicode.htm
    const std::vector<std::uint8_t> payload = {'A', ' ', 'e'};
    const std::vector<std::uint8_t> expected_varicode_bits = {
        1, 0, 1, 1, 1, 1, 0, 0,  // A: 10111100
        1, 0, 0,                    // space: 100
        1, 0, 0, 0,                 // e: 1000
        1                            // finite-record look-ahead bit
    };

    std::vector<std::uint8_t> actual_varicode_bits;
    test::AssertEq(
        bag::ultra::EncodePayloadToVaricodeBits(payload,
                                                &actual_varicode_bits),
        bag::ErrorCode::kOk,
        "Ultra MFSK16 vector Varicode encoding should succeed.");
    test::AssertEq(
        actual_varicode_bits, expected_varicode_bits,
        "Ultra MFSK16 Varicode bits should match the independent standard vector.");

    const std::vector<std::uint8_t> expected_symbols = {
        0, 0, 0, 0, 0, 0, 0, 0,
        15, 0, 15, 0, 15, 0, 15, 0,
        0, 0, 7, 0, 7, 0, 7, 0, 7, 0,
        0, 0, 0, 3, 3, 3, 0, 3,
        0, 0, 0, 0, 1, 1, 0, 0,
        0, 1, 1, 1, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,
        0, 0
    };

    bag::CoreConfig config{};
    config.sample_rate_hz = 16000;
    config.mode = bag::TransportMode::kUltra;
    const auto mfsk_config = bag::ultra::MakeMfsk16Config(config);
    std::vector<std::uint8_t> actual_symbols;
    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols(payload, mfsk_config,
                                           &actual_symbols),
        bag::ErrorCode::kOk,
        "Ultra MFSK16 vector symbol encoding should succeed.");
    test::AssertEq(
        actual_symbols, expected_symbols,
        "Ultra MFSK16 symbols should match the independent FEC/interleaver vector.");

    std::vector<std::uint8_t> decoded_payload;
    test::AssertEq(
        bag::ultra::DecodeSymbolsToPayload(expected_symbols, &decoded_payload),
        bag::ErrorCode::kOk,
        "Ultra MFSK16 standard symbol vector should decode.");
    test::AssertEq(
        decoded_payload, payload,
        "Ultra MFSK16 standard symbol vector should recover the payload.");
}

void TestCompatFrameCodecModule() {
    const std::vector<std::uint8_t> payload = {'W', 'B', '2'};
    std::vector<std::uint8_t> frame;
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kUltra, payload, &frame),
        bag::ErrorCode::kOk,
        "Compat frame module should encode ultra frame payload.");
    test::AssertEq(
        frame.size(),
        payload.size() + static_cast<std::size_t>(8),
        "Compat frame module should emit header plus CRC bytes around payload.");

    bag::transport::compat::DecodedFrame decoded{};
    test::AssertEq(
        bag::transport::compat::DecodeFrame(frame, &decoded),
        bag::ErrorCode::kOk,
        "Compat frame module should decode a valid frame.");
    test::AssertEq(decoded.mode, bag::TransportMode::kUltra, "Compat frame module should preserve transport mode.");
    test::AssertEq(decoded.payload, payload, "Compat frame module should preserve payload bytes.");
}

void TestCompatFrameCodecProRoundTrip() {
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::pro::EncodeTextToPayload("Frame", &payload),
        bag::ErrorCode::kOk,
        "Compat frame module setup should encode pro payload.");

    std::vector<std::uint8_t> frame;
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kPro, payload, &frame),
        bag::ErrorCode::kOk,
        "Compat frame module should encode pro frame payload.");

    bag::transport::compat::DecodedFrame decoded{};
    test::AssertEq(
        bag::transport::compat::DecodeFrame(frame, &decoded),
        bag::ErrorCode::kOk,
        "Compat frame module should decode a valid pro frame.");
    test::AssertEq(decoded.mode, bag::TransportMode::kPro, "Compat frame module should preserve pro transport mode.");
    test::AssertEq(decoded.payload, payload, "Compat frame module should preserve pro payload bytes.");
}

void TestCompatFrameCodecRejectsMalformedFrames() {
    const std::string utf8_text = test::Utf8Literal(u8"你好");
    const std::vector<std::uint8_t> payload(utf8_text.begin(), utf8_text.end());

    std::vector<std::uint8_t> frame;
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kUltra, payload, &frame),
        bag::ErrorCode::kOk,
        "Compat frame module malformed-frame setup should encode successfully.");

    bag::transport::compat::DecodedFrame decoded{};

    auto bad_preamble = frame;
    bad_preamble[0] = 0x00;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_preamble, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject bad preamble.");

    auto bad_version = frame;
    bad_version[2] = 0x02;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_version, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject bad version.");

    auto bad_mode = frame;
    bad_mode[3] = 0x00;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_mode, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject bad mode.");

    auto bad_length = frame;
    bad_length[5] = static_cast<std::uint8_t>(bad_length[5] + 1);
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_length, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject mismatched payload length.");

    auto bad_crc = frame;
    bad_crc.back() ^= 0x01;
    test::AssertEq(
        bag::transport::compat::DecodeFrame(bad_crc, &decoded),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject CRC mismatch.");

    std::vector<std::uint8_t> oversized_payload(
        bag::transport::compat::kMaxFramePayloadBytes + 1,
        static_cast<std::uint8_t>('A'));
    test::AssertEq(
        bag::transport::compat::EncodeFrame(bag::TransportMode::kPro, oversized_payload, &frame),
        bag::ErrorCode::kInvalidArgument,
        "Compat frame module should reject payloads above the single-frame limit.");
}

}  // namespace

namespace modules_leaf_smoke {

void RegisterLeafTransportTests(test::Runner& runner) {
    runner.Add("ModulesLeaf.ProCodecModule", TestProCodecModule);
    runner.Add("ModulesLeaf.ProCodecRejectsInvalidInput", TestProCodecRejectsInvalidInput);
    runner.Add("ModulesLeaf.UltraCodecModule", TestUltraCodecModule);
    runner.Add("ModulesLeaf.UltraMfsk16StandardVectors",
               TestUltraMfsk16StandardVectors);
    runner.Add("ModulesLeaf.CompatFrameCodecModule", TestCompatFrameCodecModule);
    runner.Add("ModulesLeaf.CompatFrameCodecProRoundTrip", TestCompatFrameCodecProRoundTrip);
    runner.Add("ModulesLeaf.CompatFrameCodecRejectsMalformedFrames",
               TestCompatFrameCodecRejectsMalformedFrames);
}

}  // namespace modules_leaf_smoke
