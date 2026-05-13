#include "test_std_support.h"
#include "test_framework.h"

import bag.flash.phy_clean;
import bag.flash.signal;
import bag.flash.voicing;

#include "flash_voicing_test_support.h"

namespace {

using namespace flash_voicing_test;

void TestFormalLitanyPayloadTailStaysCloseToStandard() {
    const auto config = MakeAndroidSizedCoreConfig();
    const auto signal_profile = bag::FlashSignalProfile::kLitany;
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(config, signal_profile);
    const auto payload_layout = bag::flash::BuildPayloadLayout(AsBytes("A"), signal_config);
    const auto ritual_payload_layout =
        bag::flash::BuildPayloadLayoutForVoicing(
            AsBytes("A"),
            signal_config,
            bag::FlashVoicingFlavor::kLitany);

    std::vector<std::int16_t> standard_pcm;
    std::vector<std::int16_t> litany_pcm;
    const auto standard_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "A",
            signal_profile,
            bag::FlashVoicingFlavor::kStandard,
            &standard_pcm);
    const auto litany_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "A",
            signal_profile,
            bag::FlashVoicingFlavor::kLitany,
            &litany_pcm);

    test::AssertEq(standard_encode_code, bag::ErrorCode::kOk, "standard formal encode should succeed.");
    test::AssertEq(litany_encode_code, bag::ErrorCode::kOk, "litany formal encode should succeed.");

    const auto coded_trimmed = bag::flash::TrimToPayloadPcm(
        standard_pcm,
        bag::flash::DescribeVoicingOutput(
            standard_pcm.size(),
            bag::flash::MakeFormalVoicingConfigForFlavor(
                config,
                bag::FlashVoicingFlavor::kStandard)));
    const auto ritual_trimmed = bag::flash::TrimToPayloadPcm(
        litany_pcm,
        bag::flash::DescribeVoicingOutput(
            litany_pcm.size(),
            bag::flash::MakeFormalVoicingConfigForFlavor(
                config,
                bag::FlashVoicingFlavor::kLitany)));

    test::AssertEq(
        coded_trimmed.size(),
        payload_layout.payload_sample_count,
        "standard payload trimming should preserve the litany signal-profile payload length.");
    test::AssertEq(
        ritual_trimmed.size(),
        ritual_payload_layout.payload_sample_count,
        "litany payload trimming should preserve the litany signal-profile payload length plus chant pauses.");
    test::AssertTrue(
        ritual_trimmed != coded_trimmed,
        "litany payload should remain distinct from standard under the same signal profile.");

    const auto& first_chunk = payload_layout.chunks.front();
    const auto [body_offset_begin, body_offset_end] =
        FractionalRange(first_chunk.sample_count, 0.25, 0.625);
    const auto [tail_offset_begin, tail_offset_end] =
        FractionalRange(first_chunk.sample_count, 0.875, 1.0);
    const std::size_t body_begin = first_chunk.sample_offset + body_offset_begin;
    const std::size_t body_end = first_chunk.sample_offset + body_offset_end;
    const std::size_t tail_begin = first_chunk.sample_offset + tail_offset_begin;
    const std::size_t tail_end = first_chunk.sample_offset + tail_offset_end;

    const double coded_body_energy =
        AverageAbsoluteSample(coded_trimmed, body_begin, body_end);
    const double ritual_body_energy =
        AverageAbsoluteSample(ritual_trimmed, body_begin, body_end);
    const double coded_tail_energy =
        AverageAbsoluteSample(coded_trimmed, tail_begin, tail_end);
    const double ritual_tail_energy =
        AverageAbsoluteSample(ritual_trimmed, tail_begin, tail_end);
    const double coded_tail_ratio =
        coded_tail_energy / std::max(coded_body_energy, 1.0);
    const double ritual_tail_ratio =
        ritual_tail_energy / std::max(ritual_body_energy, 1.0);
    const double coded_tail_brightness =
        AverageNormalizedFirstDifference(coded_trimmed, tail_begin, tail_end);
    const double ritual_tail_brightness =
        AverageNormalizedFirstDifference(ritual_trimmed, tail_begin, tail_end);

    test::AssertTrue(
        ritual_tail_ratio <= coded_tail_ratio * 1.25,
        "litany payload tail should stay close to standard instead of blooming into a long sustain.");
    test::AssertTrue(
        ritual_tail_brightness <= coded_tail_brightness * 1.15,
        "litany payload tail should remain close to standard brightness after the aggressive convergence tuning.");
}

void TestFormalLitanyHasLongerShellThanStandard() {
    auto standard_config = MakeAndroidSizedCoreConfig();
    auto litany_config = MakeAndroidSizedCoreConfig();
    litany_config.flash_signal_profile = bag::FlashSignalProfile::kLitany;
    litany_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
    std::vector<std::int16_t> standard_pcm;
    std::vector<std::int16_t> litany_pcm;

    const auto standard_code = bag::flash::EncodeTextToPcm16(
        standard_config,
        "Length",
        &standard_pcm);
    const auto litany_code = bag::flash::EncodeTextToPcm16(
        litany_config,
        "Length",
        &litany_pcm);

    test::AssertEq(standard_code, bag::ErrorCode::kOk, "standard formal encode should succeed.");
    test::AssertEq(litany_code, bag::ErrorCode::kOk, "litany formal encode should succeed.");
    test::AssertTrue(
        litany_pcm.size() > standard_pcm.size(),
        "litany should use a longer preamble and epilogue than standard.");
    test::AssertTrue(
        (litany_pcm.size() - standard_pcm.size()) > static_cast<std::size_t>(standard_config.sample_rate_hz),
        "litany should exceed standard by more than one second under the Android default frame size so coarse UI timers show a clear difference.");
}

void TestFormalLitanyDecodesWithConfiguredTrim() {
    auto litany_config = MakeCoreConfig();
    litany_config.flash_signal_profile = bag::FlashSignalProfile::kLitany;
    litany_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
    std::vector<std::int16_t> litany_pcm;
    const auto encode_code = bag::flash::EncodeTextToPcm16(
        litany_config,
        "Decode",
        &litany_pcm);
    test::AssertEq(encode_code, bag::ErrorCode::kOk, "litany formal encode should succeed.");

    std::string decoded_text;
    const auto decode_code = bag::flash::DecodePcm16ToText(
        litany_config,
        litany_pcm,
        &decoded_text);
    test::AssertEq(decode_code, bag::ErrorCode::kOk, "configured litany decode should succeed.");
    test::AssertEq(decoded_text, std::string("Decode"), "configured litany decode should roundtrip text.");
}

void TestExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor() {
    const auto config = MakeAndroidSizedCoreConfig();
    const auto signal_profile = bag::FlashSignalProfile::kStandard;
    const std::string text = "Decouple";
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(config, signal_profile);
    const auto payload_layout =
        bag::flash::BuildPayloadLayout(AsBytes(text), signal_config);
    const std::size_t expected_payload_sample_count = payload_layout.payload_sample_count;
    const auto ritual_payload_layout =
        bag::flash::BuildPayloadLayoutForVoicing(
            AsBytes(text), signal_config, bag::FlashVoicingFlavor::kLitany);
    const std::size_t expected_ritual_payload_sample_count =
        ritual_payload_layout.payload_sample_count;

    std::vector<std::int16_t> standard_pcm;
    std::vector<std::int16_t> litany_pcm;
    const auto standard_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            text,
            signal_profile,
            bag::FlashVoicingFlavor::kStandard,
            &standard_pcm);
    const auto litany_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            text,
            signal_profile,
            bag::FlashVoicingFlavor::kLitany,
            &litany_pcm);

    test::AssertEq(
        standard_encode_code,
        bag::ErrorCode::kOk,
        "explicit standard encode should succeed with an explicit signal profile.");
    test::AssertEq(
        litany_encode_code,
        bag::ErrorCode::kOk,
        "explicit litany encode should succeed with an explicit signal profile.");
    test::AssertEq(
        standard_pcm.size(),
        expected_payload_sample_count +
            FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kStandard) +
            FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kStandard),
        "explicit standard encode should add only the standard shell on top of the shared payload timing.");
    test::AssertEq(
        litany_pcm.size(),
        expected_ritual_payload_sample_count +
            FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kLitany) +
            FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kLitany),
        "explicit litany encode should keep the explicit signal bit duration while adding litany payload pauses.");
    test::AssertEq(
        litany_pcm.size() - standard_pcm.size(),
        (expected_ritual_payload_sample_count - expected_payload_sample_count) +
        (FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kLitany) +
         FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kLitany)) -
            (FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kStandard) +
             FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kStandard)),
        "switching voicing flavor under one explicit signal profile should account for litany payload pauses and shell samples.");

    std::string standard_decoded;
    std::string litany_decoded;
    const auto standard_decode_code =
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            standard_pcm,
            signal_profile,
            bag::FlashVoicingFlavor::kStandard,
            &standard_decoded);
    const auto litany_decode_code =
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            litany_pcm,
            signal_profile,
            bag::FlashVoicingFlavor::kLitany,
            &litany_decoded);

    test::AssertEq(
        standard_decode_code,
        bag::ErrorCode::kOk,
        "explicit standard decode should succeed with the shared signal profile.");
    test::AssertEq(
        litany_decode_code,
        bag::ErrorCode::kOk,
        "explicit litany decode should succeed with the shared signal profile.");
    test::AssertEq(
        standard_decoded,
        text,
        "explicit standard decode should preserve the original text.");
    test::AssertEq(
        litany_decoded,
        text,
        "explicit litany decode should preserve the original text under the same signal profile.");
}

void TestExplicitSignalProfileAndFlavorMatchDefaultExplicitPath() {
    auto config = MakeAndroidSizedCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kStandard;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
    std::vector<std::int16_t> default_pcm;
    std::vector<std::int16_t> explicit_pcm;

    const auto default_encode_code =
        bag::flash::EncodeTextToPcm16(config, "FlavorPath", &default_pcm);
    const auto explicit_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "FlavorPath",
            bag::FlashSignalProfile::kStandard,
            bag::FlashVoicingFlavor::kLitany,
            &explicit_pcm);

    test::AssertEq(default_encode_code, bag::ErrorCode::kOk, "default explicit-flavor encode should succeed.");
    test::AssertEq(explicit_encode_code, bag::ErrorCode::kOk, "explicit signal-profile-and-flavor encode should succeed.");
    test::AssertEq(
        default_pcm,
        explicit_pcm,
        "Default flash encode should match the explicit signal-profile-and-flavor path when explicit components are present.");

    std::string decoded_text;
    const auto decode_code =
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            explicit_pcm,
            bag::FlashSignalProfile::kStandard,
            bag::FlashVoicingFlavor::kLitany,
            &decoded_text);
    test::AssertEq(
        decode_code,
        bag::ErrorCode::kOk,
        "Explicit signal-profile-and-flavor decode should succeed.");
    test::AssertEq(
        decoded_text,
        std::string("FlavorPath"),
        "Explicit signal-profile-and-flavor decode should preserve the original text.");
}

void TestDefaultFormalFlashRemainsStandardBaseline() {
    std::vector<std::int16_t> default_pcm;
    std::vector<std::int16_t> standard_pcm;

    const auto default_encode_code = bag::flash::EncodeTextToPcm16(
        MakeCoreConfig(),
        "Baseline",
        &default_pcm);
    const auto standard_encode_code = bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
        MakeCoreConfig(),
        "Baseline",
        bag::FlashSignalProfile::kStandard,
        bag::FlashVoicingFlavor::kStandard,
        &standard_pcm);
    test::AssertEq(default_encode_code, bag::ErrorCode::kOk, "default formal flash encode should succeed.");
    test::AssertEq(standard_encode_code, bag::ErrorCode::kOk, "standard formal encode should succeed.");
    test::AssertEq(default_pcm, standard_pcm, "default formal flash should remain aligned with standard.");

    std::string decoded_text;
    const auto decode_code = bag::flash::DecodePcm16ToText(MakeCoreConfig(), default_pcm, &decoded_text);
    test::AssertEq(decode_code, bag::ErrorCode::kOk, "default formal flash decode should succeed.");
    test::AssertEq(decoded_text, std::string("Baseline"), "default formal flash decode should remain standard-compatible.");
}

}  // namespace

namespace flash_voicing_test {

void RegisterFlashVoicingFormalTests(test::Runner& runner) {
    runner.Add("FlashVoicing.FormalLitanyPayloadTailStaysCloseToStandard",
               TestFormalLitanyPayloadTailStaysCloseToStandard);
    runner.Add("FlashVoicing.FormalLitanyHasLongerShellThanStandard",
               TestFormalLitanyHasLongerShellThanStandard);
    runner.Add("FlashVoicing.FormalLitanyDecodesWithConfiguredTrim",
               TestFormalLitanyDecodesWithConfiguredTrim);
    runner.Add("FlashVoicing.ExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor",
               TestExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor);
    runner.Add("FlashVoicing.ExplicitSignalProfileAndFlavorMatchDefaultExplicitPath",
               TestExplicitSignalProfileAndFlavorMatchDefaultExplicitPath);
    runner.Add("FlashVoicing.DefaultFormalFlashRemainsStandardBaseline",
               TestDefaultFormalFlashRemainsStandardBaseline);
}

}  // namespace flash_voicing_test
