#include "test_std_support.h"
#include "test_framework.h"

import bag.flash.phy_clean;
import bag.flash.signal;
import bag.flash.voicing;

namespace {

bag::flash::BfskConfig MakeSignalConfig() {
    bag::flash::BfskConfig config{};
    config.sample_rate_hz = 44100;
    config.samples_per_bit = 2205;
    config.bit_duration_sec = 0.05;
    return config;
}

bag::CoreConfig MakeCoreConfig() {
    bag::CoreConfig config{};
    config.sample_rate_hz = 44100;
    config.frame_samples = 480;
    config.mode = bag::TransportMode::kFlash;
    return config;
}

bag::CoreConfig MakeAndroidSizedCoreConfig() {
    auto config = MakeCoreConfig();
    config.frame_samples = 2205;
    return config;
}

std::size_t FormalPreambleSampleCountForFlavor(const bag::CoreConfig& config,
                                               bag::FlashVoicingFlavor flavor) {
    switch (flavor) {
    case bag::FlashVoicingFlavor::kRitualChant:
        return config.frame_samples > 0
                   ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(16)
                   : static_cast<std::size_t>(0);
    case bag::FlashVoicingFlavor::kCodedBurst:
    default:
        return config.frame_samples > 0
                   ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(3)
                   : static_cast<std::size_t>(0);
    }
}

std::size_t FormalEpilogueSampleCountForFlavor(const bag::CoreConfig& config,
                                               bag::FlashVoicingFlavor flavor) {
    switch (flavor) {
    case bag::FlashVoicingFlavor::kRitualChant:
        return config.frame_samples > 0
                   ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(8)
                   : static_cast<std::size_t>(0);
    case bag::FlashVoicingFlavor::kCodedBurst:
    default:
        return config.frame_samples > 0
                   ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(3)
                   : static_cast<std::size_t>(0);
    }
}

bag::flash::FlashVoicingConfig MakeEnvelopeOnlyConfig() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.attack_ratio = 0.10;
    config.release_ratio = 0.10;
    return config;
}

bag::flash::FlashVoicingConfig MakeHarmonicOnlyConfig() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.second_harmonic_gain = 0.12;
    config.third_harmonic_gain = 0.08;
    return config;
}

bag::flash::FlashVoicingConfig MakeClickOnlyConfig() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.boundary_click_gain = 0.02;
    return config;
}

bag::flash::FlashVoicingConfig MakeStyledConfig() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.attack_ratio = 0.08;
    config.release_ratio = 0.08;
    config.second_harmonic_gain = 0.10;
    config.third_harmonic_gain = 0.03;
    config.boundary_click_gain = 0.02;
    return config;
}

bag::flash::FlashVoicingConfig MakeStyledShellConfig() {
    auto config = MakeStyledConfig();
    config.enable_preamble = true;
    config.enable_epilogue = true;
    config.preamble_sample_count = static_cast<std::size_t>(480);
    config.epilogue_sample_count = static_cast<std::size_t>(240);
    return config;
}

bag::flash::FlashVoicingConfig MakeTrimEnabledConfig(std::size_t preamble_sample_count,
                                                     std::size_t epilogue_sample_count) {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    config.enable_preamble = preamble_sample_count > 0;
    config.enable_epilogue = epilogue_sample_count > 0;
    config.preamble_sample_count = preamble_sample_count;
    config.epilogue_sample_count = epilogue_sample_count;
    return config;
}

std::vector<std::uint8_t> AsBytes(const std::string& text) {
    return std::vector<std::uint8_t>(text.begin(), text.end());
}

bag::flash::FlashPayloadLayout MakePayloadLayout(const std::string& text) {
    return bag::flash::BuildPayloadLayout(AsBytes(text), MakeSignalConfig());
}

std::vector<std::int16_t> MakeCleanPayload(const std::string& text) {
    return bag::flash::EncodeBytesToPcm16(AsBytes(text), MakeSignalConfig());
}

void AssertPcm16Range(const std::vector<std::int16_t>& pcm, const std::string& context) {
    test::AssertTrue(!pcm.empty(), context + " should not be empty.");
    const auto [min_it, max_it] = std::minmax_element(pcm.begin(), pcm.end());
    test::AssertTrue(
        *min_it >= static_cast<std::int16_t>(-32767),
        context + " min sample should remain in PCM16 range.");
    test::AssertTrue(
        *max_it <= static_cast<std::int16_t>(32767),
        context + " max sample should remain in PCM16 range.");
}

double AverageAbsoluteSample(const std::vector<std::int16_t>& pcm,
                             std::size_t begin,
                             std::size_t end) {
    test::AssertTrue(begin < end, "AverageAbsoluteSample requires a non-empty range.");
    test::AssertTrue(end <= pcm.size(), "AverageAbsoluteSample range must stay within PCM.");

    double sum = 0.0;
    for (std::size_t index = begin; index < end; ++index) {
        sum += std::abs(static_cast<double>(pcm[index]));
    }
    return sum / static_cast<double>(end - begin);
}

double AverageAbsoluteDelta(const std::vector<std::int16_t>& first,
                            const std::vector<std::int16_t>& second,
                            std::size_t begin,
                            std::size_t end) {
    test::AssertEq(first.size(), second.size(), "AverageAbsoluteDelta inputs must have the same size.");
    test::AssertTrue(begin < end, "AverageAbsoluteDelta requires a non-empty range.");
    test::AssertTrue(end <= first.size(), "AverageAbsoluteDelta range must stay within PCM.");

    double sum = 0.0;
    for (std::size_t index = begin; index < end; ++index) {
        sum += std::abs(static_cast<double>(first[index]) - static_cast<double>(second[index]));
    }
    return sum / static_cast<double>(end - begin);
}

double AverageAbsoluteRangeDelta(const std::vector<std::int16_t>& pcm,
                                 std::size_t first_begin,
                                 std::size_t first_end,
                                 std::size_t second_begin,
                                 std::size_t second_end) {
    test::AssertTrue(first_begin < first_end, "AverageAbsoluteRangeDelta requires a non-empty first range.");
    test::AssertTrue(second_begin < second_end, "AverageAbsoluteRangeDelta requires a non-empty second range.");
    test::AssertTrue(first_end <= pcm.size(), "AverageAbsoluteRangeDelta first range must stay within PCM.");
    test::AssertTrue(second_end <= pcm.size(), "AverageAbsoluteRangeDelta second range must stay within PCM.");
    test::AssertEq(
        first_end - first_begin,
        second_end - second_begin,
        "AverageAbsoluteRangeDelta ranges must have the same length.");

    double sum = 0.0;
    const std::size_t length = first_end - first_begin;
    for (std::size_t index = 0; index < length; ++index) {
        sum += std::abs(
            static_cast<double>(pcm[first_begin + index]) -
            static_cast<double>(pcm[second_begin + index]));
    }
    return sum / static_cast<double>(length);
}

double AverageNormalizedFirstDifference(const std::vector<std::int16_t>& pcm,
                                        std::size_t begin,
                                        std::size_t end) {
    test::AssertTrue(begin < end, "AverageNormalizedFirstDifference requires a non-empty range.");
    test::AssertTrue(end <= pcm.size(), "AverageNormalizedFirstDifference range must stay within PCM.");
    if (end - begin < static_cast<std::size_t>(2)) {
        return 0.0;
    }

    double diff_sum = 0.0;
    double amplitude_sum = 0.0;
    for (std::size_t index = begin + static_cast<std::size_t>(1); index < end; ++index) {
        diff_sum += std::abs(
            static_cast<double>(pcm[index]) -
            static_cast<double>(pcm[index - static_cast<std::size_t>(1)]));
        amplitude_sum += std::abs(static_cast<double>(pcm[index]));
    }
    return diff_sum / std::max(amplitude_sum, 1.0);
}

std::pair<std::size_t, std::size_t> FractionalRange(std::size_t sample_count,
                                                    double begin_ratio,
                                                    double end_ratio) {
    const std::size_t begin = static_cast<std::size_t>(
        std::floor(static_cast<double>(sample_count) * begin_ratio));
    const std::size_t end = static_cast<std::size_t>(
        std::ceil(static_cast<double>(sample_count) * end_ratio));
    return {
        std::min(begin, sample_count),
        std::clamp(end, begin + static_cast<std::size_t>(1), sample_count)
    };
}

void TestNoOpVoicingPreservesPayload() {
    const auto layout = MakePayloadLayout("Hi");
    const auto clean_payload = MakeCleanPayload("Hi");
    const auto voiced = bag::flash::ApplyVoicingToPayload(clean_payload, layout);

    test::AssertEq(voiced.pcm, clean_payload, "No-op voicing should preserve payload PCM.");
    test::AssertEq(
        voiced.descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(0),
        "No-op voicing should report zero leading non-payload samples.");
    test::AssertEq(
        voiced.descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(0),
        "No-op voicing should report zero trailing non-payload samples.");
    test::AssertEq(
        voiced.descriptor.payload_sample_count,
        clean_payload.size(),
        "No-op voicing should preserve payload sample count.");
}

void TestEnvelopeKeepsPayloadLength() {
    const auto layout = MakePayloadLayout("Envelope");
    const auto clean_payload = MakeCleanPayload("Envelope");
    const auto voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeEnvelopeOnlyConfig());

    test::AssertEq(
        voiced.pcm.size(),
        clean_payload.size(),
        "Envelope voicing should preserve total payload length.");
    test::AssertEq(
        voiced.descriptor.payload_sample_count,
        clean_payload.size(),
        "Envelope voicing should preserve descriptor payload length.");
    test::AssertEq(
        voiced.descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Envelope voicing should keep zero leading non-payload samples.");
    test::AssertEq(
        voiced.descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(0),
        "Envelope voicing should keep zero trailing non-payload samples.");
    test::AssertTrue(
        voiced.pcm != clean_payload,
        "Envelope voicing should alter payload shape without changing length.");
}

void TestHarmonicVoicingStaysWithinPcm16Range() {
    const auto layout = MakePayloadLayout("Harmonic");
    const auto clean_payload = MakeCleanPayload("Harmonic");
    const auto voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeHarmonicOnlyConfig());

    test::AssertEq(
        voiced.pcm.size(),
        clean_payload.size(),
        "Harmonic voicing should preserve total payload length.");
    test::AssertTrue(
        voiced.pcm != clean_payload,
        "Harmonic voicing should differ from the clean payload.");
    AssertPcm16Range(voiced.pcm, "Harmonic voicing output");
}

void TestBoundaryClickVoicingIsDeterministic() {
    const auto layout = MakePayloadLayout("AB");
    const auto clean_payload = MakeCleanPayload("AB");
    const auto first =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeClickOnlyConfig());
    const auto second =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeClickOnlyConfig());

    test::AssertEq(
        first.pcm,
        second.pcm,
        "Boundary click voicing should be deterministic for the same input.");
    test::AssertTrue(
        first.pcm != clean_payload,
        "Boundary click voicing should alter payload samples at byte boundaries.");
}

void TestStyledVoicingOutputIsStable() {
    const auto layout = MakePayloadLayout("WaveBits");
    const auto clean_payload = MakeCleanPayload("WaveBits");
    const auto first =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeStyledConfig());
    const auto second =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeStyledConfig());

    test::AssertEq(
        first.pcm,
        second.pcm,
        "Styled voicing output should remain identical across repeated runs.");
    test::AssertEq(
        first.descriptor.payload_sample_count,
        clean_payload.size(),
        "Styled voicing should preserve descriptor payload length.");
    AssertPcm16Range(first.pcm, "Styled voicing output");
}

void TestDefaultVoicingMatchesExplicitCodedBurst() {
    const auto layout = MakePayloadLayout("WaveBits");
    const auto clean_payload = MakeCleanPayload("WaveBits");
    const auto default_voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeStyledConfig());
    const auto explicit_coded_burst =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kCodedBurst,
            MakeStyledConfig());

    test::AssertEq(
        default_voiced.pcm,
        explicit_coded_burst.pcm,
        "Default flash voicing should remain identical to explicit coded_burst style.");
    test::AssertEq(
        default_voiced.descriptor.payload_sample_count,
        explicit_coded_burst.descriptor.payload_sample_count,
        "Default flash voicing should preserve the coded_burst descriptor.");
}

void TestFlavorVoicingMatchesExplicitRitualConfiguration() {
    const auto config = MakeStyledShellConfig();
    const auto layout = MakePayloadLayout("Flavor");
    const auto clean_payload = MakeCleanPayload("Flavor");
    const auto flavored =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kRitualChant,
            config);
    const auto repeated =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kRitualChant,
            config);

    test::AssertEq(
        flavored.pcm,
        repeated.pcm,
        "Flavor-based voicing should remain deterministic for ritual_chant.");
    test::AssertEq(
        flavored.descriptor.payload_sample_count,
        repeated.descriptor.payload_sample_count,
        "Flavor-based voicing should preserve the same descriptor payload size across repeated calls.");
}

void TestRitualChantDiffersButDecodesLikeCodedBurst() {
    const auto config = MakeStyledShellConfig();
    const auto layout = MakePayloadLayout("Command");
    const auto clean_payload = MakeCleanPayload("Command");
    const auto coded_burst =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kCodedBurst,
            config);
    const auto ritual_chant =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kRitualChant,
            config);

    test::AssertTrue(
        coded_burst.pcm != ritual_chant.pcm,
        "Ritual chant should sound different from coded_burst for the same payload.");

    const auto coded_trimmed = bag::flash::TrimToPayloadPcm(coded_burst.pcm, coded_burst.descriptor);
    const auto ritual_trimmed = bag::flash::TrimToPayloadPcm(ritual_chant.pcm, ritual_chant.descriptor);

    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(coded_trimmed, MakeSignalConfig()),
        AsBytes("Command"),
        "Coded burst should still decode to the original bytes.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(ritual_trimmed, MakeSignalConfig()),
        AsBytes("Command"),
        "Ritual chant should decode to the same bytes as coded_burst.");
}

void TestRitualChantKeepsMoreTailEnergyThanCodedBurst() {
    const auto config = MakeStyledConfig();
    const auto layout = MakePayloadLayout("A");
    const auto clean_payload = MakeCleanPayload("A");
    const auto coded_burst =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kCodedBurst,
            config);
    const auto ritual_chant =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kRitualChant,
            config);

    const auto coded_trimmed =
        bag::flash::TrimToPayloadPcm(coded_burst.pcm, coded_burst.descriptor);
    const auto ritual_trimmed =
        bag::flash::TrimToPayloadPcm(ritual_chant.pcm, ritual_chant.descriptor);
    const auto& first_chunk = layout.chunks.front();
    const std::size_t tail_begin =
        first_chunk.sample_offset + (first_chunk.sample_count * static_cast<std::size_t>(3)) / static_cast<std::size_t>(4);
    const std::size_t tail_end = first_chunk.sample_offset + first_chunk.sample_count;

    const double coded_tail_energy =
        AverageAbsoluteSample(coded_trimmed, tail_begin, tail_end);
    const double ritual_tail_energy =
        AverageAbsoluteSample(ritual_trimmed, tail_begin, tail_end);

    test::AssertTrue(
        ritual_tail_energy > coded_tail_energy,
        "Ritual chant should keep more tail energy so adjacent bits feel less abruptly separated.");
}

void TestFormalRitualChantHasLongerShellThanCodedBurst() {
    auto coded_config = MakeAndroidSizedCoreConfig();
    auto ritual_config = MakeAndroidSizedCoreConfig();
    ritual_config.flash_signal_profile = bag::FlashSignalProfile::kRitualChant;
    ritual_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
    std::vector<std::int16_t> coded_burst_pcm;
    std::vector<std::int16_t> ritual_chant_pcm;

    const auto coded_burst_code = bag::flash::EncodeTextToPcm16(
        coded_config,
        "Length",
        &coded_burst_pcm);
    const auto ritual_chant_code = bag::flash::EncodeTextToPcm16(
        ritual_config,
        "Length",
        &ritual_chant_pcm);

    test::AssertEq(coded_burst_code, bag::ErrorCode::kOk, "coded_burst formal encode should succeed.");
    test::AssertEq(ritual_chant_code, bag::ErrorCode::kOk, "ritual_chant formal encode should succeed.");
    test::AssertTrue(
        ritual_chant_pcm.size() > coded_burst_pcm.size(),
        "ritual_chant should use a longer preamble and epilogue than coded_burst.");
    test::AssertTrue(
        (ritual_chant_pcm.size() - coded_burst_pcm.size()) > static_cast<std::size_t>(coded_config.sample_rate_hz),
        "ritual_chant should exceed coded_burst by more than one second under the Android default frame size so coarse UI timers show a clear difference.");
}

void TestFormalRitualChantDecodesWithConfiguredTrim() {
    auto ritual_config = MakeCoreConfig();
    ritual_config.flash_signal_profile = bag::FlashSignalProfile::kRitualChant;
    ritual_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
    std::vector<std::int16_t> ritual_chant_pcm;
    const auto encode_code = bag::flash::EncodeTextToPcm16(
        ritual_config,
        "Decode",
        &ritual_chant_pcm);
    test::AssertEq(encode_code, bag::ErrorCode::kOk, "ritual_chant formal encode should succeed.");

    std::string decoded_text;
    const auto decode_code = bag::flash::DecodePcm16ToText(
        ritual_config,
        ritual_chant_pcm,
        &decoded_text);
    test::AssertEq(decode_code, bag::ErrorCode::kOk, "configured ritual_chant decode should succeed.");
    test::AssertEq(decoded_text, std::string("Decode"), "configured ritual_chant decode should roundtrip text.");
}

void TestExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor() {
    const auto config = MakeAndroidSizedCoreConfig();
    const auto signal_profile = bag::FlashSignalProfile::kCodedBurst;
    const std::string text = "Decouple";
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(config, signal_profile);
    const auto payload_layout =
        bag::flash::BuildPayloadLayout(AsBytes(text), signal_config);
    const std::size_t expected_payload_sample_count = payload_layout.payload_sample_count;

    std::vector<std::int16_t> coded_burst_pcm;
    std::vector<std::int16_t> ritual_chant_pcm;
    const auto coded_burst_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            text,
            signal_profile,
            bag::FlashVoicingFlavor::kCodedBurst,
            &coded_burst_pcm);
    const auto ritual_chant_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            text,
            signal_profile,
            bag::FlashVoicingFlavor::kRitualChant,
            &ritual_chant_pcm);

    test::AssertEq(
        coded_burst_encode_code,
        bag::ErrorCode::kOk,
        "explicit coded_burst encode should succeed with an explicit signal profile.");
    test::AssertEq(
        ritual_chant_encode_code,
        bag::ErrorCode::kOk,
        "explicit ritual_chant encode should succeed with an explicit signal profile.");
    test::AssertEq(
        coded_burst_pcm.size(),
        expected_payload_sample_count +
            FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kCodedBurst) +
            FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kCodedBurst),
        "explicit coded_burst encode should add only the coded_burst shell on top of the shared payload timing.");
    test::AssertEq(
        ritual_chant_pcm.size(),
        expected_payload_sample_count +
            FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kRitualChant) +
            FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kRitualChant),
        "explicit ritual_chant encode should reuse the same payload timing and only change the shell.");
    test::AssertEq(
        ritual_chant_pcm.size() - coded_burst_pcm.size(),
        (FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kRitualChant) +
         FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kRitualChant)) -
            (FormalPreambleSampleCountForFlavor(config, bag::FlashVoicingFlavor::kCodedBurst) +
             FormalEpilogueSampleCountForFlavor(config, bag::FlashVoicingFlavor::kCodedBurst)),
        "switching voicing flavor under one explicit signal profile should change only shell samples, not payload timing.");

    std::string coded_burst_decoded;
    std::string ritual_chant_decoded;
    const auto coded_burst_decode_code =
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            coded_burst_pcm,
            signal_profile,
            bag::FlashVoicingFlavor::kCodedBurst,
            &coded_burst_decoded);
    const auto ritual_chant_decode_code =
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            config,
            ritual_chant_pcm,
            signal_profile,
            bag::FlashVoicingFlavor::kRitualChant,
            &ritual_chant_decoded);

    test::AssertEq(
        coded_burst_decode_code,
        bag::ErrorCode::kOk,
        "explicit coded_burst decode should succeed with the shared signal profile.");
    test::AssertEq(
        ritual_chant_decode_code,
        bag::ErrorCode::kOk,
        "explicit ritual_chant decode should succeed with the shared signal profile.");
    test::AssertEq(
        coded_burst_decoded,
        text,
        "explicit coded_burst decode should preserve the original text.");
    test::AssertEq(
        ritual_chant_decoded,
        text,
        "explicit ritual_chant decode should preserve the original text under the same signal profile.");
}

void TestExplicitSignalProfileAndFlavorMatchDefaultExplicitPath() {
    auto config = MakeAndroidSizedCoreConfig();
    config.flash_signal_profile = bag::FlashSignalProfile::kCodedBurst;
    config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
    std::vector<std::int16_t> default_pcm;
    std::vector<std::int16_t> explicit_pcm;

    const auto default_encode_code =
        bag::flash::EncodeTextToPcm16(config, "FlavorPath", &default_pcm);
    const auto explicit_encode_code =
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            config,
            "FlavorPath",
            bag::FlashSignalProfile::kCodedBurst,
            bag::FlashVoicingFlavor::kRitualChant,
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
            bag::FlashSignalProfile::kCodedBurst,
            bag::FlashVoicingFlavor::kRitualChant,
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

void TestRitualChantPreambleUsesThreePhrasedBursts() {
    constexpr std::size_t kPreambleSampleCount = 3000;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kRitualChant,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));

    const auto [phrase1_begin, phrase1_end] = FractionalRange(kPreambleSampleCount, 0.08, 0.18);
    const auto [gap1_begin, gap1_end] = FractionalRange(kPreambleSampleCount, 0.25, 0.29);
    const auto [phrase2_begin, phrase2_end] = FractionalRange(kPreambleSampleCount, 0.40, 0.50);
    const auto [gap2_begin, gap2_end] = FractionalRange(kPreambleSampleCount, 0.61, 0.65);
    const auto [phrase3_begin, phrase3_end] = FractionalRange(kPreambleSampleCount, 0.76, 0.86);

    const double phrase1_energy = AverageAbsoluteSample(voiced.pcm, phrase1_begin, phrase1_end);
    const double gap1_energy = AverageAbsoluteSample(voiced.pcm, gap1_begin, gap1_end);
    const double phrase2_energy = AverageAbsoluteSample(voiced.pcm, phrase2_begin, phrase2_end);
    const double gap2_energy = AverageAbsoluteSample(voiced.pcm, gap2_begin, gap2_end);
    const double phrase3_energy = AverageAbsoluteSample(voiced.pcm, phrase3_begin, phrase3_end);

    test::AssertTrue(
        phrase1_energy > gap1_energy * 3.0,
        "ritual_chant preamble should leave a clear short pause between phrase one and phrase two.");
    test::AssertTrue(
        phrase2_energy > gap1_energy * 3.0,
        "ritual_chant preamble should re-enter strongly after the first short pause.");
    test::AssertTrue(
        phrase2_energy > gap2_energy * 3.0,
        "ritual_chant preamble should leave a second clear short pause before phrase three.");
    test::AssertTrue(
        phrase3_energy > gap2_energy * 3.0,
        "ritual_chant preamble should end with a third voiced phrase after the second short pause.");
}

void TestRitualChantEpilogueUsesTwoPhrasedBursts() {
    constexpr std::size_t kEpilogueSampleCount = 1800;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kRitualChant,
        MakeTrimEnabledConfig(static_cast<std::size_t>(0), kEpilogueSampleCount));

    const std::size_t epilogue_begin = voiced.descriptor.leading_nonpayload_samples +
                                       voiced.descriptor.payload_sample_count;
    const auto [phrase1_begin_local, phrase1_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.16, 0.32);
    const auto [gap_begin_local, gap_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.48, 0.54);
    const auto [phrase2_begin_local, phrase2_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.70, 0.84);

    const double phrase1_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + phrase1_begin_local,
        epilogue_begin + phrase1_end_local);
    const double gap_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + gap_begin_local,
        epilogue_begin + gap_end_local);
    const double phrase2_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + phrase2_begin_local,
        epilogue_begin + phrase2_end_local);

    test::AssertTrue(
        phrase1_energy > gap_energy * 3.0,
        "ritual_chant epilogue should insert a short pause after the first closure phrase.");
    test::AssertTrue(
        phrase2_energy > gap_energy * 3.0,
        "ritual_chant epilogue should resume with a second closure phrase after the short pause.");
}

void TestCodedBurstPreambleUsesThreeHandshakeBursts() {
    constexpr std::size_t kPreambleSampleCount = 1600;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCodedBurst,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));

    const auto [burst1_begin, burst1_end] = FractionalRange(kPreambleSampleCount, 0.05, 0.18);
    const auto [gap1_begin, gap1_end] = FractionalRange(kPreambleSampleCount, 0.21, 0.27);
    const auto [burst2_begin, burst2_end] = FractionalRange(kPreambleSampleCount, 0.30, 0.43);
    const auto [gap2_begin, gap2_end] = FractionalRange(kPreambleSampleCount, 0.47, 0.54);
    const auto [burst3_begin, burst3_end] = FractionalRange(kPreambleSampleCount, 0.58, 0.70);

    const double burst1_energy = AverageAbsoluteSample(voiced.pcm, burst1_begin, burst1_end);
    const double gap1_energy = AverageAbsoluteSample(voiced.pcm, gap1_begin, gap1_end);
    const double burst2_energy = AverageAbsoluteSample(voiced.pcm, burst2_begin, burst2_end);
    const double gap2_energy = AverageAbsoluteSample(voiced.pcm, gap2_begin, gap2_end);
    const double burst3_energy = AverageAbsoluteSample(voiced.pcm, burst3_begin, burst3_end);

    test::AssertTrue(
        burst1_energy > gap1_energy * 3.0,
        "coded_burst preamble should open with a strong short handshake burst.");
    test::AssertTrue(
        burst2_energy > gap1_energy * 3.0,
        "coded_burst preamble should re-enter with a short confirmation burst after the first gap.");
    test::AssertTrue(
        burst2_energy > gap2_energy * 3.0,
        "coded_burst preamble should leave a clear second gap before the sync burst.");
    test::AssertTrue(
        burst3_energy > gap2_energy * 3.0,
        "coded_burst preamble should finish with a third sync burst that pushes directly into payload onset.");
}

void TestCodedBurstPreambleContrastsPayloadOnset() {
    constexpr std::size_t kPreambleSampleCount = 1600;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCodedBurst,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));

    const auto [sync_begin, sync_end] = FractionalRange(kPreambleSampleCount, 0.58, 0.70);
    const std::size_t sync_length = sync_end - sync_begin;
    const std::size_t payload_begin = voiced.descriptor.leading_nonpayload_samples;
    const std::size_t payload_end = payload_begin + sync_length;
    const auto [seam_begin, seam_end] = FractionalRange(kPreambleSampleCount, 0.78, 0.94);

    const double sync_delta = AverageAbsoluteRangeDelta(
        voiced.pcm,
        sync_begin,
        sync_end,
        payload_begin,
        payload_end);
    const double sync_brightness = AverageNormalizedFirstDifference(voiced.pcm, sync_begin, sync_end);
    const double payload_brightness =
        AverageNormalizedFirstDifference(voiced.pcm, payload_begin, payload_end);
    const double sync_energy = AverageAbsoluteSample(voiced.pcm, sync_begin, sync_end);
    const double seam_energy = AverageAbsoluteSample(voiced.pcm, seam_begin, seam_end);

    test::AssertTrue(
        sync_delta > 5500.0,
        "coded_burst preamble sync burst should not sound like a continuation of payload onset.");
    test::AssertTrue(
        sync_brightness > payload_brightness * 1.08,
        "coded_burst preamble sync burst should sound brighter than the payload start.");
    test::AssertTrue(
        seam_energy < sync_energy * 0.18,
        "coded_burst preamble should leave a low-energy seam before payload begins.");
}

void TestCodedBurstEpilogueUsesClosingBurstAndAckChirp() {
    constexpr std::size_t kEpilogueSampleCount = 1200;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCodedBurst,
        MakeTrimEnabledConfig(static_cast<std::size_t>(0), kEpilogueSampleCount));

    const std::size_t epilogue_begin = voiced.descriptor.leading_nonpayload_samples +
                                       voiced.descriptor.payload_sample_count;
    const auto [burst_begin_local, burst_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.12, 0.32);
    const auto [gap_begin_local, gap_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.40, 0.50);
    const auto [ack_begin_local, ack_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.54, 0.68);
    const auto [tail_begin_local, tail_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.82, 0.96);

    const double burst_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + burst_begin_local,
        epilogue_begin + burst_end_local);
    const double gap_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + gap_begin_local,
        epilogue_begin + gap_end_local);
    const double ack_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + ack_begin_local,
        epilogue_begin + ack_end_local);
    const double tail_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + tail_begin_local,
        epilogue_begin + tail_end_local);

    test::AssertTrue(
        burst_energy > gap_energy * 3.0,
        "coded_burst epilogue should begin with a short closing burst before the acknowledgement gap.");
    test::AssertTrue(
        ack_energy > gap_energy * 3.0,
        "coded_burst epilogue should emit a distinct ack chirp after the main closing burst.");
    test::AssertTrue(
        tail_energy < ack_energy * 0.45,
        "coded_burst epilogue should decay quickly after the ack chirp instead of trailing like ritual_chant.");
}

void TestCodedBurstEpilogueContrastsPayloadTailAndStopsHard() {
    constexpr std::size_t kPreambleSampleCount = 1600;
    constexpr std::size_t kEpilogueSampleCount = 1200;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto preamble_voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCodedBurst,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));
    const auto epilogue_voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCodedBurst,
        MakeTrimEnabledConfig(static_cast<std::size_t>(0), kEpilogueSampleCount));

    const auto [sync_begin, sync_end] = FractionalRange(kPreambleSampleCount, 0.58, 0.70);
    const double preamble_sync_brightness =
        AverageNormalizedFirstDifference(preamble_voiced.pcm, sync_begin, sync_end);

    const std::size_t epilogue_begin = epilogue_voiced.descriptor.leading_nonpayload_samples +
                                       epilogue_voiced.descriptor.payload_sample_count;
    const auto [closing_begin_local, closing_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.12, 0.32);
    const auto [ack_begin_local, ack_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.54, 0.68);
    const auto [tail_begin_local, tail_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.82, 0.96);

    const std::size_t closing_length = closing_end_local - closing_begin_local;
    const std::size_t payload_tail_end = epilogue_begin;
    const std::size_t payload_tail_begin = payload_tail_end - closing_length;

    const double closing_delta = AverageAbsoluteRangeDelta(
        epilogue_voiced.pcm,
        epilogue_begin + closing_begin_local,
        epilogue_begin + closing_end_local,
        payload_tail_begin,
        payload_tail_end);
    const double ack_brightness = AverageNormalizedFirstDifference(
        epilogue_voiced.pcm,
        epilogue_begin + ack_begin_local,
        epilogue_begin + ack_end_local);
    const double ack_energy = AverageAbsoluteSample(
        epilogue_voiced.pcm,
        epilogue_begin + ack_begin_local,
        epilogue_begin + ack_end_local);
    const double tail_energy = AverageAbsoluteSample(
        epilogue_voiced.pcm,
        epilogue_begin + tail_begin_local,
        epilogue_begin + tail_end_local);

    test::AssertTrue(
        closing_delta > 4200.0,
        "coded_burst closing burst should not sound like an extension of the payload tail.");
    test::AssertTrue(
        ack_brightness < preamble_sync_brightness * 0.92,
        "coded_burst ack chirp should sound lower and less bright than the preamble sync burst.");
    test::AssertTrue(
        tail_energy < ack_energy * 0.12,
        "coded_burst epilogue should hard-stop after the ack chirp.");
}

void TestDefaultFormalFlashRemainsCodedBurstBaseline() {
    std::vector<std::int16_t> default_pcm;
    std::vector<std::int16_t> coded_burst_pcm;

    const auto default_encode_code = bag::flash::EncodeTextToPcm16(
        MakeCoreConfig(),
        "Baseline",
        &default_pcm);
    const auto coded_burst_encode_code = bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
        MakeCoreConfig(),
        "Baseline",
        bag::FlashSignalProfile::kCodedBurst,
        bag::FlashVoicingFlavor::kCodedBurst,
        &coded_burst_pcm);
    test::AssertEq(default_encode_code, bag::ErrorCode::kOk, "default formal flash encode should succeed.");
    test::AssertEq(coded_burst_encode_code, bag::ErrorCode::kOk, "coded_burst formal encode should succeed.");
    test::AssertEq(default_pcm, coded_burst_pcm, "default formal flash should remain aligned with coded_burst.");

    std::string decoded_text;
    const auto decode_code = bag::flash::DecodePcm16ToText(MakeCoreConfig(), default_pcm, &decoded_text);
    test::AssertEq(decode_code, bag::ErrorCode::kOk, "default formal flash decode should succeed.");
    test::AssertEq(decoded_text, std::string("Baseline"), "default formal flash decode should remain coded_burst-compatible.");
}

void TestTrimDescriptorTracksNonpayloadSamples() {
    const auto descriptor = bag::flash::DescribeVoicingOutput(
        static_cast<std::size_t>(24),
        MakeTrimEnabledConfig(static_cast<std::size_t>(5), static_cast<std::size_t>(7)));

    test::AssertEq(
        descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(5),
        "Trim descriptor should report configured leading non-payload samples.");
    test::AssertEq(
        descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(7),
        "Trim descriptor should report configured trailing non-payload samples.");
    test::AssertEq(
        descriptor.payload_sample_count,
        static_cast<std::size_t>(12),
        "Trim descriptor should report the remaining payload sample count.");
}

void TestTrimToPayloadPcmExtractsPayloadForDecode() {
    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayload(
        clean_payload,
        layout,
        MakeTrimEnabledConfig(static_cast<std::size_t>(6), static_cast<std::size_t>(4)));
    const auto trimmed_payload = bag::flash::TrimToPayloadPcm(voiced.pcm, voiced.descriptor);

    test::AssertEq(
        trimmed_payload,
        clean_payload,
        "Trim helper should return the original payload PCM after removing non-payload samples.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(trimmed_payload, MakeSignalConfig()),
        AsBytes("AB"),
        "Trimmed payload should remain decodable through the flash signal layer.");
}

void TestPreambleAndEpilogueSegmentsAreInserted() {
    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayload(
        clean_payload,
        layout,
        MakeTrimEnabledConfig(static_cast<std::size_t>(6), static_cast<std::size_t>(4)));

    test::AssertEq(
        voiced.descriptor.leading_nonpayload_samples,
        static_cast<std::size_t>(6),
        "Voicing should report inserted preamble sample count.");
    test::AssertEq(
        voiced.descriptor.trailing_nonpayload_samples,
        static_cast<std::size_t>(4),
        "Voicing should report inserted epilogue sample count.");
    test::AssertEq(
        voiced.pcm.size(),
        clean_payload.size() + static_cast<std::size_t>(10),
        "Voicing should grow the PCM length by preamble plus epilogue sample counts.");

    bool has_nonzero_preamble = false;
    for (std::size_t index = 0; index < voiced.descriptor.leading_nonpayload_samples; ++index) {
        has_nonzero_preamble = has_nonzero_preamble || voiced.pcm[index] != 0;
    }

    bool has_nonzero_epilogue = false;
    const std::size_t epilogue_begin =
        voiced.descriptor.leading_nonpayload_samples + voiced.descriptor.payload_sample_count;
    for (std::size_t index = epilogue_begin; index < voiced.pcm.size(); ++index) {
        has_nonzero_epilogue = has_nonzero_epilogue || voiced.pcm[index] != 0;
    }

    test::AssertTrue(has_nonzero_preamble, "Inserted preamble should contain audible non-zero samples.");
    test::AssertTrue(has_nonzero_epilogue, "Inserted epilogue should contain audible non-zero samples.");
}

void TestByteBoundaryAccentRemainsStrongerThanNibbleAccent() {
    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeClickOnlyConfig());

    const auto window_for_chunk = [&](std::size_t chunk_index) {
        const auto& chunk = layout.chunks[chunk_index];
        const std::size_t window =
            std::clamp(chunk.sample_count / static_cast<std::size_t>(96),
                       static_cast<std::size_t>(6),
                       static_cast<std::size_t>(24));
        return AverageAbsoluteDelta(
            voiced.pcm,
            clean_payload,
            chunk.sample_offset,
            chunk.sample_offset + window);
    };

    const double nibble_accent = window_for_chunk(static_cast<std::size_t>(4));
    const double byte_accent = window_for_chunk(static_cast<std::size_t>(8));

    test::AssertTrue(
        nibble_accent > 0.0,
        "Nibble grouping should introduce a detectable accent at four-bit boundaries.");
    test::AssertTrue(
        byte_accent > nibble_accent,
        "Byte boundary accent should remain stronger than nibble boundary accent.");
}

void TestTrimRejectsDescriptorMismatch() {
    const std::vector<std::int16_t> pcm = {1, 2, 3, 4, 5, 6};
    bag::flash::FlashVoicingDescriptor descriptor{};
    descriptor.leading_nonpayload_samples = 2;
    descriptor.payload_sample_count = 3;
    descriptor.trailing_nonpayload_samples = 2;

    test::AssertThrows(
        [&] {
            (void)bag::flash::TrimToPayloadPcm(pcm, descriptor);
        },
        "Trim helper should reject descriptors whose total sample count does not match the PCM.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("FlashVoicing.NoOpVoicingPreservesPayload", TestNoOpVoicingPreservesPayload);
    runner.Add("FlashVoicing.EnvelopeKeepsPayloadLength", TestEnvelopeKeepsPayloadLength);
    runner.Add("FlashVoicing.HarmonicVoicingStaysWithinPcm16Range", TestHarmonicVoicingStaysWithinPcm16Range);
    runner.Add("FlashVoicing.BoundaryClickVoicingIsDeterministic", TestBoundaryClickVoicingIsDeterministic);
    runner.Add("FlashVoicing.StyledVoicingOutputIsStable", TestStyledVoicingOutputIsStable);
    runner.Add("FlashVoicing.DefaultVoicingMatchesExplicitCodedBurst", TestDefaultVoicingMatchesExplicitCodedBurst);
    runner.Add("FlashVoicing.FlavorVoicingMatchesExplicitRitualConfiguration",
               TestFlavorVoicingMatchesExplicitRitualConfiguration);
    runner.Add("FlashVoicing.RitualChantDiffersButDecodesLikeCodedBurst",
               TestRitualChantDiffersButDecodesLikeCodedBurst);
    runner.Add("FlashVoicing.RitualChantKeepsMoreTailEnergyThanCodedBurst",
               TestRitualChantKeepsMoreTailEnergyThanCodedBurst);
    runner.Add("FlashVoicing.FormalRitualChantHasLongerShellThanCodedBurst",
               TestFormalRitualChantHasLongerShellThanCodedBurst);
    runner.Add("FlashVoicing.FormalRitualChantDecodesWithConfiguredTrim",
               TestFormalRitualChantDecodesWithConfiguredTrim);
    runner.Add("FlashVoicing.ExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor",
               TestExplicitSignalProfileDecouplesPayloadTimingFromVoicingFlavor);
    runner.Add("FlashVoicing.ExplicitSignalProfileAndFlavorMatchDefaultExplicitPath",
               TestExplicitSignalProfileAndFlavorMatchDefaultExplicitPath);
    runner.Add("FlashVoicing.RitualChantPreambleUsesThreePhrasedBursts",
               TestRitualChantPreambleUsesThreePhrasedBursts);
    runner.Add("FlashVoicing.RitualChantEpilogueUsesTwoPhrasedBursts",
               TestRitualChantEpilogueUsesTwoPhrasedBursts);
    runner.Add("FlashVoicing.CodedBurstPreambleUsesThreeHandshakeBursts",
               TestCodedBurstPreambleUsesThreeHandshakeBursts);
    runner.Add("FlashVoicing.CodedBurstPreambleContrastsPayloadOnset",
               TestCodedBurstPreambleContrastsPayloadOnset);
    runner.Add("FlashVoicing.CodedBurstEpilogueUsesClosingBurstAndAckChirp",
               TestCodedBurstEpilogueUsesClosingBurstAndAckChirp);
    runner.Add("FlashVoicing.CodedBurstEpilogueContrastsPayloadTailAndStopsHard",
               TestCodedBurstEpilogueContrastsPayloadTailAndStopsHard);
    runner.Add("FlashVoicing.DefaultFormalFlashRemainsCodedBurstBaseline",
               TestDefaultFormalFlashRemainsCodedBurstBaseline);
    runner.Add("FlashVoicing.TrimDescriptorTracksNonpayloadSamples", TestTrimDescriptorTracksNonpayloadSamples);
    runner.Add("FlashVoicing.TrimToPayloadPcmExtractsPayloadForDecode", TestTrimToPayloadPcmExtractsPayloadForDecode);
    runner.Add("FlashVoicing.PreambleAndEpilogueSegmentsAreInserted", TestPreambleAndEpilogueSegmentsAreInserted);
    runner.Add("FlashVoicing.ByteBoundaryAccentRemainsStrongerThanNibbleAccent",
               TestByteBoundaryAccentRemainsStrongerThanNibbleAccent);
    runner.Add("FlashVoicing.TrimRejectsDescriptorMismatch", TestTrimRejectsDescriptorMismatch);
    return runner.Run();
}
