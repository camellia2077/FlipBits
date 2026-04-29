#include "test_std_support.h"
#include "test_framework.h"

import bag.flash.phy_clean;
import bag.flash.signal;
import bag.flash.voicing;

#include "flash_voicing_test_support.h"

namespace {

using namespace flash_voicing_test;

void TestFormalLitanyPayloadTailStaysCloseToSteady() {
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

    std::vector<std::int16_t> steady_pcm;
    std::vector<std::int16_t> litany_pcm;
    const auto steady_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "A",
            signal_profile,
            bag::FlashVoicingFlavor::kSteady,
            &steady_pcm);
    const auto litany_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "A",
            signal_profile,
            bag::FlashVoicingFlavor::kLitany,
            &litany_pcm);

    test::AssertEq(steady_encode_code, bag::ErrorCode::kOk, "steady formal encode should succeed.");
    test::AssertEq(litany_encode_code, bag::ErrorCode::kOk, "litany formal encode should succeed.");

    const auto coded_trimmed = bag::flash::TrimToPayloadPcm(
        steady_pcm,
        bag::flash::DescribeVoicingOutput(
            steady_pcm.size(),
            bag::flash::MakeFormalVoicingConfigForFlavor(
                config,
                bag::FlashVoicingFlavor::kSteady)));
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
        "steady payload trimming should preserve the litany signal-profile payload length.");
    test::AssertEq(
        ritual_trimmed.size(),
        ritual_payload_layout.payload_sample_count,
        "litany payload trimming should preserve the litany signal-profile payload length plus chant pauses.");
    test::AssertTrue(
        ritual_trimmed != coded_trimmed,
        "litany payload should remain distinct from steady under the same signal profile.");

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
        "litany payload tail should stay close to steady instead of blooming into a long sustain.");
    test::AssertTrue(
        ritual_tail_brightness <= coded_tail_brightness * 1.15,
        "litany payload tail should remain close to steady brightness after the aggressive convergence tuning.");
}

void TestFormalLitanyHasLongerShellThanSteady() {
    auto steady_config = MakeAndroidSizedCoreConfig();
    auto litany_config = MakeAndroidSizedCoreConfig();
    litany_config.flash_signal_profile = bag::FlashSignalProfile::kLitany;
    litany_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
    std::vector<std::int16_t> steady_pcm;
    std::vector<std::int16_t> litany_pcm;

    const auto steady_code = bag::flash::EncodeTextToPcm16(
        steady_config,
        "Length",
        &steady_pcm);
    const auto litany_code = bag::flash::EncodeTextToPcm16(
        litany_config,
        "Length",
        &litany_pcm);

    test::AssertEq(steady_code, bag::ErrorCode::kOk, "steady formal encode should succeed.");
    test::AssertEq(litany_code, bag::ErrorCode::kOk, "litany formal encode should succeed.");
    test::AssertTrue(
        litany_pcm.size() > steady_pcm.size(),
        "litany should use a longer preamble and epilogue than steady.");
    test::AssertTrue(
        (litany_pcm.size() - steady_pcm.size()) > static_cast<std::size_t>(steady_config.sample_rate_hz),
        "litany should exceed steady by more than one second under the Android default frame size so coarse UI timers show a clear difference.");
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
    const auto signal_profile = bag::FlashSignalProfile::kSteady;
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

    std::vector<std::int16_t> steady_pcm;
    std::vector<std::int16_t> litany_pcm;
    const auto steady_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            text,
            signal_profile,
            bag::FlashVoicingFlavor::kSteady,
            &steady_pcm);
    const auto litany_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            text,
            signal_profile,
            bag::FlashVoicingFlavor::kLitany,
            &litany_pcm);

    test::AssertEq(
        steady_encode_code,
        bag::ErrorCode::kOk,
        "explicit steady encode should succeed with an explicit signal profile.");
    test::AssertEq(
        litany_encode_code,
        bag::ErrorCode::kOk,
        "explicit litany encode should succeed with an explicit signal profile.");
    test::AssertEq(
        steady_pcm.size(),
        expected_payload_sample_count +
            FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kSteady) +
            FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kSteady),
        "explicit steady encode should add only the steady shell on top of the shared payload timing.");
    test::AssertEq(
        litany_pcm.size(),
        expected_ritual_payload_sample_count +
            FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kLitany) +
            FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kLitany),
        "explicit litany encode should keep the explicit signal bit duration while adding litany payload pauses.");
    test::AssertEq(
        litany_pcm.size() - steady_pcm.size(),
        (expected_ritual_payload_sample_count - expected_payload_sample_count) +
        (FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kLitany) +
         FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kLitany)) -
            (FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kSteady) +
             FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kSteady)),
        "switching voicing flavor under one explicit signal profile should account for litany payload pauses and shell samples.");

    std::string steady_decoded;
    std::string litany_decoded;
    const auto steady_decode_code =
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            steady_pcm,
            signal_profile,
            bag::FlashVoicingFlavor::kSteady,
            &steady_decoded);
    const auto litany_decode_code =
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            litany_pcm,
            signal_profile,
            bag::FlashVoicingFlavor::kLitany,
            &litany_decoded);

    test::AssertEq(
        steady_decode_code,
        bag::ErrorCode::kOk,
        "explicit steady decode should succeed with the shared signal profile.");
    test::AssertEq(
        litany_decode_code,
        bag::ErrorCode::kOk,
        "explicit litany decode should succeed with the shared signal profile.");
    test::AssertEq(
        steady_decoded,
        text,
        "explicit steady decode should preserve the original text.");
    test::AssertEq(
        litany_decoded,
        text,
        "explicit litany decode should preserve the original text under the same signal profile.");
}

void TestExplicitSignalProfileAndFlavorMatchDefaultExplicitPath() {
    auto config = MakeAndroidSizedCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kSteady;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
    std::vector<std::int16_t> default_pcm;
    std::vector<std::int16_t> explicit_pcm;

    const auto default_encode_code =
        bag::flash::EncodeTextToPcm16(config, "FlavorPath", &default_pcm);
    const auto explicit_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "FlavorPath",
            bag::FlashSignalProfile::kSteady,
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
            bag::FlashSignalProfile::kSteady,
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

void TestDefaultFormalFlashRemainsSteadyBaseline() {
    std::vector<std::int16_t> default_pcm;
    std::vector<std::int16_t> steady_pcm;

    const auto default_encode_code = bag::flash::EncodeTextToPcm16(
        MakeCoreConfig(),
        "Baseline",
        &default_pcm);
    const auto steady_encode_code = bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
        MakeCoreConfig(),
        "Baseline",
        bag::FlashSignalProfile::kSteady,
        bag::FlashVoicingFlavor::kSteady,
        &steady_pcm);
    test::AssertEq(default_encode_code, bag::ErrorCode::kOk, "default formal flash encode should succeed.");
    test::AssertEq(steady_encode_code, bag::ErrorCode::kOk, "steady formal encode should succeed.");
    test::AssertEq(default_pcm, steady_pcm, "default formal flash should remain aligned with steady.");

    std::string decoded_text;
    const auto decode_code = bag::flash::DecodePcm16ToText(MakeCoreConfig(), default_pcm, &decoded_text);
    test::AssertEq(decode_code, bag::ErrorCode::kOk, "default formal flash decode should succeed.");
    test::AssertEq(decoded_text, std::string("Baseline"), "default formal flash decode should remain steady-compatible.");
}

}  // namespace

namespace flash_voicing_test {

void RegisterFlashVoicingFormalTests(test::Runner& runner) {
    runner.Add("FlashVoicing.FormalLitanyPayloadTailStaysCloseToSteady",
               TestFormalLitanyPayloadTailStaysCloseToSteady);
    runner.Add("FlashVoicing.FormalLitanyHasLongerShellThanSteady",
               TestFormalLitanyHasLongerShellThanSteady);
    runner.Add("FlashVoicing.FormalLitanyDecodesWithConfiguredTrim",
               TestFormalLitanyDecodesWithConfiguredTrim);
    runner.Add("FlashVoicing.ExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor",
               TestExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor);
    runner.Add("FlashVoicing.ExplicitSignalProfileAndFlavorMatchDefaultExplicitPath",
               TestExplicitSignalProfileAndFlavorMatchDefaultExplicitPath);
    runner.Add("FlashVoicing.DefaultFormalFlashRemainsSteadyBaseline",
               TestDefaultFormalFlashRemainsSteadyBaseline);
}

}  // namespace flash_voicing_test
