#include "test_std_support.h"
#include "test_audio_io.h"
#include "test_fs.h"
#include "test_framework.h"
#include "test_vectors.h"
#include "test_utf8.h"

import audio_io.wav;
import bag.common.error_code;
import bag.common.version;
import bag.flash.codec;
import bag.flash.signal;
import bag.flash.voicing;
import bag.flash.phy_clean;
import bag.pro.codec;
import bag.transport.compat.frame_codec;
import bag.ultra.codec;

namespace {

bag::flash::BfskConfig MakeBfskConfig() {
    bag::flash::BfskConfig config{};
    config.sample_rate_hz = 44100;
    config.samples_per_bit = 2205;
    config.bit_duration_sec = 0.05;
    return config;
}

bag::flash::FlashVoicingConfig MakeStyledVoicingConfig() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.attack_ratio = 0.08;
    config.release_ratio = 0.08;
    config.second_harmonic_gain = 0.10;
    config.third_harmonic_gain = 0.03;
    config.boundary_click_gain = 0.02;
    return config;
}

bag::CoreConfig MakeFlashCoreConfig() {
    bag::CoreConfig config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 2205;
    config.mode = bag::TransportMode::kFlash;
    return config;
}

bag::CoreConfig MakeRitualFlashCoreConfig() {
    auto config = MakeFlashCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kRitualChant;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
    return config;
}

bag::CoreConfig MakeExplicitDecoupledFlashCoreConfig() {
    auto config = MakeFlashCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kCodedBurst;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
    return config;
}

std::size_t FormalFlashLeadingSamples(const bag::CoreConfig& config) {
    return config.frame_samples > 0
               ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(3)
               : static_cast<std::size_t>(0);
}

std::size_t FormalFlashTrailingSamples(const bag::CoreConfig& config) {
    return config.frame_samples > 0
               ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(3)
               : static_cast<std::size_t>(0);
}

std::vector<std::uint8_t> AsBytes(const std::string& text) {
    return std::vector<std::uint8_t>(text.begin(), text.end());
}

void TestVersionModule() {
    test::AssertEq(
        std::string(bag::CoreVersion()),
        std::string(test::kExpectedCoreVersion),
        "Version module should expose core version.");
}

void TestAudioIoModuleRoundTripContract() {
    const auto dir = test::MakeTempDir("modules_leaf");
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        const auto path = dir / (std::string(test_case.name) + ".wav");
        audio_io::WriteMonoPcm16Wav(path, test_case.sample_rate_hz, test_case.mono_pcm);
        const auto wav = audio_io::ReadMonoPcm16Wav(path);
        test::AssertAudioIoRoundTripResult(wav, test_case, "Module audio_io boundary");
    }
}

void TestAudioIoModuleBytesRoundTripContract() {
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        const auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
        const auto parsed = audio_io::ParseMonoPcm16Wav(wav_bytes);
        test::AssertEq(
            parsed.status,
            audio_io::WavPcm16Status::kOk,
            "Module bytes route should parse canonical mono PCM16 WAV bytes.");
        test::AssertAudioIoRoundTripResult(parsed.wav, test_case, "Module audio_io bytes boundary");
    }
}

void TestAudioIoModuleReadMissingFileFails() {
    const auto missing_path = test::MakeTempDir("modules_leaf") / "missing.wav";
    test::AssertThrows(
        [&] {
            (void)audio_io::ReadMonoPcm16Wav(missing_path);
        },
        "Module audio_io boundary should throw when the input file does not exist.");
}

void TestAudioIoModuleRejectsInvalidBytes() {
    const std::vector<std::uint8_t> bad_header = {'N', 'O', 'T', 'W', 'A', 'V', 'E'};
    const auto parsed = audio_io::ParseMonoPcm16Wav(bad_header);
    test::AssertEq(
        parsed.status,
        audio_io::WavPcm16Status::kInvalidHeader,
        "Module bytes route should reject invalid RIFF/WAVE bytes.");
}

void TestFlashCodecModule() {
    const std::string text = test::Utf8Literal(u8"你好，WaveBits");
    std::vector<std::uint8_t> bytes;
    test::AssertEq(
        bag::flash::EncodeTextToBytes(text, &bytes),
        bag::ErrorCode::kOk,
        "Flash codec module should encode UTF-8 bytes.");
    test::AssertEq(
        bytes,
        std::vector<std::uint8_t>(text.begin(), text.end()),
        "Flash codec module should preserve raw byte payload.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodeBytesToText(bytes, &decoded),
        bag::ErrorCode::kOk,
        "Flash codec module should decode raw bytes.");
    test::AssertEq(decoded, text, "Flash codec module should roundtrip UTF-8 text.");
}

void TestFlashSignalLayoutMatchesExpected() {
    const auto config = MakeBfskConfig();
    const auto layout = bag::flash::BuildPayloadLayout(AsBytes("A"), config);
    const std::size_t chunk_size = config.samples_per_bit;

    test::AssertEq(
        layout.chunks.size(),
        static_cast<std::size_t>(8),
        "Flash signal module should emit one payload chunk per bit.");
    test::AssertEq(
        layout.payload_sample_count,
        static_cast<std::size_t>(8) * chunk_size,
        "Flash signal module payload layout should match the PCM sample budget.");
    test::AssertEq(
        layout.chunks.front().bit_value,
        static_cast<std::uint8_t>(0),
        "Flash signal module should keep the first bit for 'A' as 0.");
    test::AssertEq(
        layout.chunks[1].bit_value,
        static_cast<std::uint8_t>(1),
        "Flash signal module should keep the second bit for 'A' as 1.");
    test::AssertEq(
        layout.chunks[1].sample_offset,
        chunk_size,
        "Flash signal module should advance payload chunk offsets by one chunk size.");
    test::AssertEq(
        layout.chunks.front().carrier_freq_hz,
        config.low_freq_hz,
        "Flash signal module should map 0 bits to the low carrier.");
    test::AssertEq(
        layout.chunks[1].carrier_freq_hz,
        config.high_freq_hz,
        "Flash signal module should map 1 bits to the high carrier.");
}

void TestFlashSignalEncodeLengthMatchesExpected() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("A"), config);
    const std::size_t chunk_size = config.samples_per_bit;
    test::AssertEq(
        pcm.size(),
        static_cast<std::size_t>(8) * chunk_size,
        "Flash signal module should emit 8 bits times chunk size for one byte.");
}

void TestFlashSignalStyleAwareChunkSizeMatchesConfig() {
    const auto coded_signal = bag::flash::MakeBfskConfig(MakeFlashCoreConfig());
    const auto ritual_signal = bag::flash::MakeBfskConfig(MakeRitualFlashCoreConfig());

    test::AssertEq(
        coded_signal.samples_per_bit,
        static_cast<std::size_t>(2205),
        "coded_burst flash signal should keep one frame per bit.");
    test::AssertEq(
        ritual_signal.samples_per_bit,
        static_cast<std::size_t>(6615),
        "ritual_chant flash signal should use the longer 3x bit timing profile.");
    test::AssertTrue(
        ritual_signal.samples_per_bit > coded_signal.samples_per_bit,
        "ritual_chant flash signal should use more samples per bit than coded_burst.");
}

void TestFlashSignalExplicitProfileOverridesLegacyStyleTiming() {
    const auto ritual_config = MakeRitualFlashCoreConfig();
    const auto explicit_coded_signal =
        bag::flash::MakeBfskConfigForSignalProfile(
            ritual_config,
            bag::FlashSignalProfile::kCodedBurst);
    const auto explicit_ritual_signal =
        bag::flash::MakeBfskConfigForSignalProfile(
            ritual_config,
            bag::FlashSignalProfile::kRitualChant);

    test::AssertEq(
        explicit_coded_signal.samples_per_bit,
        static_cast<std::size_t>(2205),
        "Explicit coded signal profile should keep coded timing even when ritual config is in scope.");
    test::AssertEq(
        explicit_ritual_signal.samples_per_bit,
        static_cast<std::size_t>(6615),
        "Explicit ritual signal profile should keep the ritual timing when requested.");
}

void TestFlashSignalAmplitudeInRange() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("Hello"), config);
    test::AssertTrue(!pcm.empty(), "Flash signal module PCM should not be empty for non-empty input.");

    std::int16_t min_sample = pcm.front();
    std::int16_t max_sample = pcm.front();
    for (std::int16_t sample : pcm) {
        if (sample < min_sample) {
            min_sample = sample;
        }
        if (sample > max_sample) {
            max_sample = sample;
        }
    }

    test::AssertTrue(min_sample >= static_cast<std::int16_t>(-32767), "Flash signal module PCM min out of range.");
    test::AssertTrue(max_sample <= static_cast<std::int16_t>(32767), "Flash signal module PCM max out of range.");
}

void TestFlashSignalDecodeEmptyInputReturnsEmptyPayload() {
    const auto config = MakeBfskConfig();
    const std::vector<std::int16_t> pcm;
    const auto decoded_bytes = bag::flash::DecodePcm16ToBytes(pcm, config);
    test::AssertEq(
        decoded_bytes,
        std::vector<std::uint8_t>{},
        "Flash signal module should decode empty PCM to an empty byte payload.");
}

void TestFlashSignalSnapshotFirstSamplesStable() {
    const auto config = MakeBfskConfig();
    const auto pcm = bag::flash::EncodeBytesToPcm16(AsBytes("A"), config);
    const std::vector<std::int16_t> expected = {
        0, 1493, 2981, 4459, 5924, 7368, 8789, 10182,
        11541, 12863, 14143, 15377, 16561, 17692, 18765, 19777};

    test::AssertTrue(
        pcm.size() >= expected.size(),
        "Flash signal module PCM must contain enough samples for snapshot coverage.");
    for (std::size_t index = 0; index < expected.size(); ++index) {
        if (pcm[index] != expected[index]) {
            test::Fail("Flash signal module snapshot mismatch at sample index " + std::to_string(index));
        }
    }
}

void TestFlashPhyCleanTextRoundTrip() {
    const auto config = MakeFlashCoreConfig();
    const std::string text = test::Utf8Literal(u8"你好，WaveBits");

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(config, text, &pcm),
        bag::ErrorCode::kOk,
        "Flash PHY facade should encode UTF-8 text through the extracted signal layer.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Flash PHY facade should decode PCM through the extracted signal layer.");
    test::AssertEq(decoded, text, "Flash PHY facade should preserve roundtrip text behavior.");
}

void TestFlashPhyCleanFormalOutputIncludesPredictableNonpayloadSegments() {
    const auto config = MakeFlashCoreConfig();
    const std::string text = test::Utf8Literal(u8"组合输出");

    std::vector<std::uint8_t> bytes;
    test::AssertEq(
        bag::flash::EncodeTextToBytes(text, &bytes),
        bag::ErrorCode::kOk,
        "Flash codec setup should encode UTF-8 bytes for formal flash output.");

    const auto clean_payload_pcm = bag::flash::EncodeBytesToPcm16(bytes, MakeBfskConfig());

    std::vector<std::int16_t> formal_pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(config, text, &formal_pcm),
        bag::ErrorCode::kOk,
        "Flash PHY facade should encode text through the formal signal+voicing chain.");
    test::AssertEq(
        formal_pcm.size(),
        clean_payload_pcm.size() + FormalFlashLeadingSamples(config) + FormalFlashTrailingSamples(config),
        "Flash PHY facade should add predictable preamble and epilogue sample counts.");
    test::AssertTrue(
        formal_pcm != clean_payload_pcm,
        "Flash PHY facade should apply safe voicing so the formal output differs from the clean signal.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(config, formal_pcm, &decoded),
        bag::ErrorCode::kOk,
        "Flash PHY facade should decode formal output after trimming non-payload segments.");
    test::AssertEq(decoded, text, "Flash PHY facade formal output should preserve roundtrip text behavior.");
}

void TestFlashPhyCleanRitualChantUsesLongerTimingAndStillDecodes() {
    const auto coded_config = MakeFlashCoreConfig();
    const auto ritual_config = MakeRitualFlashCoreConfig();
    const std::string text = "Ritual";

    std::vector<std::int16_t> coded_pcm;
    std::vector<std::int16_t> ritual_pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(coded_config, text, &coded_pcm),
        bag::ErrorCode::kOk,
        "coded_burst flash encode should succeed for timing comparison.");
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(ritual_config, text, &ritual_pcm),
        bag::ErrorCode::kOk,
        "ritual_chant flash encode should succeed for timing comparison.");
    test::AssertTrue(
        ritual_pcm.size() > coded_pcm.size(),
        "ritual_chant flash encode should be longer than coded_burst after signal timing expansion.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(ritual_config, ritual_pcm, &decoded),
        bag::ErrorCode::kOk,
        "ritual_chant flash decode should succeed when the matching style is configured.");
    test::AssertEq(decoded, text, "ritual_chant flash decode should preserve the original text.");
}

void TestFlashPhyCleanWrongStyleDoesNotRoundTrip() {
    const auto coded_config = MakeFlashCoreConfig();
    const auto ritual_config = MakeRitualFlashCoreConfig();
    const std::string text = "Mismatch";

    std::vector<std::int16_t> ritual_pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(ritual_config, text, &ritual_pcm),
        bag::ErrorCode::kOk,
        "ritual_chant flash encode should succeed before wrong-style decode validation.");

    std::string decoded;
    const auto decode_code = bag::flash::DecodePcm16ToText(coded_config, ritual_pcm, &decoded);
    test::AssertTrue(
        decode_code != bag::ErrorCode::kOk || decoded != text,
        "Decoding ritual_chant flash PCM with coded_burst style should not look like a valid roundtrip.");
}

void TestFlashPhyCleanExplicitSignalProfileKeepsPayloadTimingWhenVoicingChanges() {
    const auto config = MakeFlashCoreConfig();
    const auto signal_profile = bag::FlashSignalProfile::kCodedBurst;
    const std::string text = "Decouple";
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(config, signal_profile);
    const auto payload_layout =
        bag::flash::BuildPayloadLayout(AsBytes(text), signal_config);
    const std::size_t expected_payload_sample_count = payload_layout.payload_sample_count;
    const std::size_t expected_ritual_shell =
        static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(24);

    std::vector<std::int16_t> ritual_pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            text,
            signal_profile,
            bag::FlashVoicingFlavor::kRitualChant,
            &ritual_pcm),
        bag::ErrorCode::kOk,
        "Explicit signal profile encode should succeed when ritual voicing is layered over coded timing.");
    test::AssertEq(
        ritual_pcm.size(),
        expected_payload_sample_count + expected_ritual_shell,
        "Explicit signal profile encode should reuse coded payload timing and add only the ritual shell.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            ritual_pcm,
            signal_profile,
            bag::FlashVoicingFlavor::kRitualChant,
            &decoded),
        bag::ErrorCode::kOk,
        "Explicit signal profile decode should succeed with the matching ritual shell.");
    test::AssertEq(
        decoded,
        text,
        "Explicit signal profile decode should preserve the original text.");
}

void TestFlashPhyCleanSignalProfileAndFlavorApiMatchesConfiguredDefaultPath() {
    auto config = MakeFlashCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kCodedBurst;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
    std::vector<std::int16_t> flavor_pcm;
    std::vector<std::int16_t> default_pcm;

    test::AssertEq(
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "FlavorApi",
            bag::FlashSignalProfile::kCodedBurst,
            bag::FlashVoicingFlavor::kRitualChant,
            &flavor_pcm),
        bag::ErrorCode::kOk,
        "Signal-profile-and-flavor encode should succeed.");
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(
            config,
            "FlavorApi",
            &default_pcm),
        bag::ErrorCode::kOk,
        "Configured default flash encode should succeed.");
    test::AssertEq(
        flavor_pcm,
        default_pcm,
        "Explicit signal-profile-and-flavor encode should match the configured default path.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            flavor_pcm,
            bag::FlashSignalProfile::kCodedBurst,
            bag::FlashVoicingFlavor::kRitualChant,
            &decoded),
        bag::ErrorCode::kOk,
        "Signal-profile-and-flavor decode should succeed.");
    test::AssertEq(
        decoded,
        std::string("FlavorApi"),
        "Signal-profile-and-flavor decode should preserve the original text.");
}

void TestFlashPhyCleanDefaultPathUsesExplicitFlashComponentsWhenPresent() {
    const auto config = MakeExplicitDecoupledFlashCoreConfig();
    const std::string text = "DefaultPath";
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(
            config,
            bag::FlashSignalProfile::kCodedBurst);
    const auto payload_layout =
        bag::flash::BuildPayloadLayout(AsBytes(text), signal_config);
    const std::size_t expected_payload_sample_count = payload_layout.payload_sample_count;
    const std::size_t expected_total_size =
        expected_payload_sample_count +
        static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(16) +
        static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(8);

    std::vector<std::int16_t> pcm;
    test::AssertEq(
        bag::flash::EncodeTextToPcm16(config, text, &pcm),
        bag::ErrorCode::kOk,
        "Default flash encode should succeed when explicit flash components are present.");
    test::AssertEq(
        pcm.size(),
        expected_total_size,
        "Default flash encode should reuse the explicit coded payload timing and the explicit ritual shell.");

    std::string decoded;
    test::AssertEq(
        bag::flash::DecodePcm16ToText(config, pcm, &decoded),
        bag::ErrorCode::kOk,
        "Default flash decode should succeed when explicit flash components are present.");
    test::AssertEq(
        decoded,
        text,
        "Default flash decode should preserve the original text when explicit flash components are present.");
}

void TestFlashVoicingNoOpPreservesPayload() {
    const auto signal_config = MakeBfskConfig();
    const auto payload_layout = bag::flash::BuildPayloadLayout(AsBytes("Hi"), signal_config);
    const auto clean_payload_pcm = bag::flash::EncodeBytesToPcm16(AsBytes("Hi"), signal_config);
    const auto voiced = bag::flash::ApplyVoicingToPayload(clean_payload_pcm, payload_layout);

    test::AssertEq(
        voiced.pcm,
        clean_payload_pcm,
        "Flash voicing module should preserve payload PCM in no-op mode.");
    test::AssertEq(
        voiced.descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Flash voicing module should report zero leading non-payload samples in no-op mode.");
    test::AssertEq(
        voiced.descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Flash voicing module should report zero trailing non-payload samples in no-op mode.");
    test::AssertEq(
        voiced.descriptor.payload_sample_count,
        clean_payload_pcm.size(),
        "Flash voicing module should preserve payload sample count in no-op mode.");
}

void TestFlashVoicingStyledOutputKeepsPayloadShape() {
    const auto signal_config = MakeBfskConfig();
    const auto payload_layout = bag::flash::BuildPayloadLayout(AsBytes("Hi"), signal_config);
    const auto clean_payload_pcm = bag::flash::EncodeBytesToPcm16(AsBytes("Hi"), signal_config);
    const auto voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload_pcm, payload_layout, MakeStyledVoicingConfig());

    test::AssertEq(
        voiced.pcm.size(),
        clean_payload_pcm.size(),
        "Flash voicing styled output should keep the payload sample count unchanged.");
    test::AssertEq(
        voiced.descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Flash voicing styled output should keep zero leading non-payload samples.");
    test::AssertEq(
        voiced.descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Flash voicing styled output should keep zero trailing non-payload samples.");
    test::AssertEq(
        voiced.descriptor.payload_sample_count,
        clean_payload_pcm.size(),
        "Flash voicing styled output should report the original payload sample count.");
    test::AssertTrue(
        voiced.pcm != clean_payload_pcm,
        "Flash voicing styled output should differ from the clean payload PCM.");

    const auto [min_it, max_it] = std::minmax_element(voiced.pcm.begin(), voiced.pcm.end());
    test::AssertTrue(min_it != voiced.pcm.end(), "Flash voicing styled output should not be empty.");
    test::AssertTrue(
        *min_it >= static_cast<std::int16_t>(-32767),
        "Flash voicing styled output min sample should remain in PCM16 range.");
    test::AssertTrue(
        *max_it <= static_cast<std::int16_t>(32767),
        "Flash voicing styled output max sample should remain in PCM16 range.");
}

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
    const std::string text = test::Utf8Literal(u8"WaveBits 超级模式 🚀");
    std::vector<std::uint8_t> payload;
    test::AssertEq(
        bag::ultra::EncodeTextToPayload(text, &payload),
        bag::ErrorCode::kOk,
        "Ultra codec module should encode UTF-8 payload.");

    std::vector<std::uint8_t> symbols;
    test::AssertEq(
        bag::ultra::EncodePayloadToSymbols(payload, &symbols),
        bag::ErrorCode::kOk,
        "Ultra codec module should encode payload symbols.");

    std::vector<std::uint8_t> decoded_payload;
    test::AssertEq(
        bag::ultra::DecodeSymbolsToPayload(symbols, &decoded_payload),
        bag::ErrorCode::kOk,
        "Ultra codec module should decode symbols back to payload.");
    test::AssertEq(decoded_payload, payload, "Ultra codec module should roundtrip payload symbols.");

    std::string decoded;
    test::AssertEq(
        bag::ultra::DecodePayloadToText(decoded_payload, &decoded),
        bag::ErrorCode::kOk,
        "Ultra codec module should decode UTF-8 payload.");
    test::AssertEq(decoded, text, "Ultra codec module should roundtrip UTF-8 text.");
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

int main() {
    test::Runner runner;
    runner.Add("ModulesLeaf.VersionModule", TestVersionModule);
    runner.Add("ModulesLeaf.AudioIoModuleRoundTripContract", TestAudioIoModuleRoundTripContract);
    runner.Add("ModulesLeaf.AudioIoModuleBytesRoundTripContract", TestAudioIoModuleBytesRoundTripContract);
    runner.Add("ModulesLeaf.AudioIoModuleReadMissingFileFails", TestAudioIoModuleReadMissingFileFails);
    runner.Add("ModulesLeaf.AudioIoModuleRejectsInvalidBytes", TestAudioIoModuleRejectsInvalidBytes);
    runner.Add("ModulesLeaf.FlashCodecModule", TestFlashCodecModule);
    runner.Add("ModulesLeaf.FlashSignalLayoutMatchesExpected", TestFlashSignalLayoutMatchesExpected);
    runner.Add("ModulesLeaf.FlashSignalEncodeLengthMatchesExpected", TestFlashSignalEncodeLengthMatchesExpected);
    runner.Add("ModulesLeaf.FlashSignalStyleAwareChunkSizeMatchesConfig",
               TestFlashSignalStyleAwareChunkSizeMatchesConfig);
    runner.Add("ModulesLeaf.FlashSignalExplicitProfileOverridesLegacyStyleTiming",
               TestFlashSignalExplicitProfileOverridesLegacyStyleTiming);
    runner.Add("ModulesLeaf.FlashSignalAmplitudeInRange", TestFlashSignalAmplitudeInRange);
    runner.Add(
        "ModulesLeaf.FlashSignalDecodeEmptyInputReturnsEmptyPayload",
        TestFlashSignalDecodeEmptyInputReturnsEmptyPayload);
    runner.Add(
        "ModulesLeaf.FlashSignalSnapshotFirstSamplesStable",
        TestFlashSignalSnapshotFirstSamplesStable);
    runner.Add("ModulesLeaf.FlashPhyCleanTextRoundTrip", TestFlashPhyCleanTextRoundTrip);
    runner.Add(
        "ModulesLeaf.FlashPhyCleanFormalOutputIncludesPredictableNonpayloadSegments",
        TestFlashPhyCleanFormalOutputIncludesPredictableNonpayloadSegments);
    runner.Add("ModulesLeaf.FlashPhyCleanRitualChantUsesLongerTimingAndStillDecodes",
               TestFlashPhyCleanRitualChantUsesLongerTimingAndStillDecodes);
    runner.Add("ModulesLeaf.FlashPhyCleanWrongStyleDoesNotRoundTrip",
               TestFlashPhyCleanWrongStyleDoesNotRoundTrip);
    runner.Add("ModulesLeaf.FlashPhyCleanExplicitSignalProfileKeepsPayloadTimingWhenVoicingChanges",
               TestFlashPhyCleanExplicitSignalProfileKeepsPayloadTimingWhenVoicingChanges);
    runner.Add("ModulesLeaf.FlashPhyCleanSignalProfileAndFlavorApiMatchesConfiguredDefaultPath",
               TestFlashPhyCleanSignalProfileAndFlavorApiMatchesConfiguredDefaultPath);
    runner.Add("ModulesLeaf.FlashPhyCleanDefaultPathUsesExplicitFlashComponentsWhenPresent",
               TestFlashPhyCleanDefaultPathUsesExplicitFlashComponentsWhenPresent);
    runner.Add("ModulesLeaf.FlashVoicingNoOpPreservesPayload", TestFlashVoicingNoOpPreservesPayload);
    runner.Add(
        "ModulesLeaf.FlashVoicingStyledOutputKeepsPayloadShape",
        TestFlashVoicingStyledOutputKeepsPayloadShape);
    runner.Add("ModulesLeaf.ProCodecModule", TestProCodecModule);
    runner.Add("ModulesLeaf.ProCodecRejectsInvalidInput", TestProCodecRejectsInvalidInput);
    runner.Add("ModulesLeaf.UltraCodecModule", TestUltraCodecModule);
    runner.Add("ModulesLeaf.CompatFrameCodecModule", TestCompatFrameCodecModule);
    runner.Add("ModulesLeaf.CompatFrameCodecProRoundTrip", TestCompatFrameCodecProRoundTrip);
    runner.Add(
        "ModulesLeaf.CompatFrameCodecRejectsMalformedFrames",
        TestCompatFrameCodecRejectsMalformedFrames);
    return runner.Run();
}
