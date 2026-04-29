#include "test_std_support.h"
#include "test_framework.h"

import bag.flash.phy_clean;
import bag.flash.signal;
import bag.flash.voicing;

#include "flash_voicing_test_support.h"

namespace {

using namespace flash_voicing_test;

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
    const auto layout = MakePayloadLayout("FlipBits");
    const auto clean_payload = MakeCleanPayload("FlipBits");
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

void TestDefaultVoicingMatchesExplicitSteady() {
    const auto layout = MakePayloadLayout("FlipBits");
    const auto clean_payload = MakeCleanPayload("FlipBits");
    const auto default_voiced =
        bag::flash::ApplyVoicingToPayload(clean_payload, layout, MakeStyledConfig());
    const auto explicit_steady =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kSteady,
            MakeStyledConfig());

    test::AssertEq(
        default_voiced.pcm,
        explicit_steady.pcm,
        "Default flash voicing should remain identical to explicit steady style.");
    test::AssertEq(
        default_voiced.descriptor.payload_sample_count,
        explicit_steady.descriptor.payload_sample_count,
        "Default flash voicing should preserve the steady descriptor.");
}

void TestFlavorVoicingMatchesExplicitLitanyConfiguration() {
    const auto config = MakeStyledShellConfig();
    const auto layout = MakePayloadLayout("Flavor");
    const auto clean_payload = MakeCleanPayload("Flavor");
    const auto flavored =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kLitany,
            config);
    const auto repeated =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kLitany,
            config);

    test::AssertEq(
        flavored.pcm,
        repeated.pcm,
        "Flavor-based voicing should remain deterministic for litany.");
    test::AssertEq(
        flavored.descriptor.payload_sample_count,
        repeated.descriptor.payload_sample_count,
        "Flavor-based voicing should preserve the same descriptor payload size across repeated calls.");
}

void TestLitanyDiffersButDecodesLikeSteady() {
    const auto config = MakeStyledShellConfig();
    const auto layout = MakePayloadLayout("Command");
    const auto clean_payload = MakeCleanPayload("Command");
    const auto steady =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kSteady,
            config);
    const auto litany =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kLitany,
            config);

    test::AssertTrue(
        steady.pcm != litany.pcm,
        "Litany should sound different from steady for the same payload.");

    const auto steady_trimmed = bag::flash::TrimToPayloadPcm(steady.pcm, steady.descriptor);
    const auto litany_trimmed = bag::flash::TrimToPayloadPcm(litany.pcm, litany.descriptor);

    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(steady_trimmed, MakeSignalConfig()),
        AsBytes("Command"),
        "Steady should still decode to the original bytes.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(litany_trimmed, MakeSignalConfig()),
        AsBytes("Command"),
        "Litany should decode to the same bytes as steady.");
}

void TestLitanyAddsContinuousChantDroneWithoutBreakingDecode() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    const auto layout = MakePayloadLayout("Litany");
    const auto clean_payload = MakeCleanPayload("Litany");
    const auto steady =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kSteady,
            config);
    const auto litany =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kLitany,
            config);
    const auto& first_chunk = layout.chunks.front();
    const std::size_t begin =
        first_chunk.sample_offset + (first_chunk.sample_count * static_cast<std::size_t>(3)) /
                                        static_cast<std::size_t>(10);
    const std::size_t end =
        first_chunk.sample_offset + (first_chunk.sample_count * static_cast<std::size_t>(7)) /
                                        static_cast<std::size_t>(10);

    test::AssertTrue(
        AverageAbsoluteDelta(steady.pcm, clean_payload, begin, end) < 1.0,
        "Steady with no explicit texture should leave the payload midpoint untouched.");
    test::AssertTrue(
        AverageAbsoluteDelta(litany.pcm, clean_payload, begin, end) > 140.0,
        "Litany should add continuous low chant drone and mechanical throat texture across the payload body.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(litany.pcm, MakeSignalConfig()),
        AsBytes("Litany"),
        "Litany chant drone should keep the payload decodable.");
    AssertPcm16Range(litany.pcm, "Litany chant drone output");
}

void TestLitanyPhraseTailDipsWithoutBreakingDecode() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    const auto layout = MakePayloadLayout("Litany");
    const auto clean_payload = MakeCleanPayload("Litany");
    const auto litany =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kLitany,
            config);
    const auto& nibble_tail_chunk = layout.chunks[static_cast<std::size_t>(3)];
    const std::size_t mid_begin =
        nibble_tail_chunk.sample_offset +
        (nibble_tail_chunk.sample_count * static_cast<std::size_t>(35)) /
            static_cast<std::size_t>(100);
    const std::size_t mid_end =
        nibble_tail_chunk.sample_offset +
        (nibble_tail_chunk.sample_count * static_cast<std::size_t>(55)) /
            static_cast<std::size_t>(100);
    const std::size_t tail_begin =
        nibble_tail_chunk.sample_offset +
        (nibble_tail_chunk.sample_count * static_cast<std::size_t>(84)) /
            static_cast<std::size_t>(100);
    const std::size_t tail_end =
        nibble_tail_chunk.sample_offset +
        (nibble_tail_chunk.sample_count * static_cast<std::size_t>(98)) /
            static_cast<std::size_t>(100);

    test::AssertTrue(
        AverageAbsoluteSample(litany.pcm, tail_begin, tail_end) <
            AverageAbsoluteSample(litany.pcm, mid_begin, mid_end) * 0.74,
        "Litany should create a deeper chant-like closure at phrase tails.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(litany.pcm, MakeSignalConfig()),
        AsBytes("Litany"),
        "Litany phrase-tail dips should keep the payload decodable.");
}

void TestLitanyTextAwarePausesWithoutBreakingDecode() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    const std::string text = "A B.";
    const auto layout = MakePayloadLayout(text);
    const auto clean_payload = MakeCleanPayload(text);
    const auto litany =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kLitany,
            config);
    const auto pause_ratio_for_byte = [&](std::size_t byte_index) {
        const auto& chunk =
            layout.chunks[byte_index * static_cast<std::size_t>(8) +
                          static_cast<std::size_t>(7)];
        const std::size_t mid_begin =
            chunk.sample_offset +
            (chunk.sample_count * static_cast<std::size_t>(32)) /
                static_cast<std::size_t>(100);
        const std::size_t mid_end =
            chunk.sample_offset +
            (chunk.sample_count * static_cast<std::size_t>(48)) /
                static_cast<std::size_t>(100);
        const std::size_t tail_begin =
            chunk.sample_offset +
            (chunk.sample_count * static_cast<std::size_t>(82)) /
                static_cast<std::size_t>(100);
        const std::size_t tail_end =
            chunk.sample_offset +
            (chunk.sample_count * static_cast<std::size_t>(97)) /
                static_cast<std::size_t>(100);
        return AverageAbsoluteSample(litany.pcm, tail_begin, tail_end) /
               AverageAbsoluteSample(litany.pcm, mid_begin, mid_end);
    };

    test::AssertTrue(
        pause_ratio_for_byte(static_cast<std::size_t>(1)) < 0.74,
        "Litany should create a text-aware small pause after spaces.");
    test::AssertTrue(
        pause_ratio_for_byte(static_cast<std::size_t>(3)) < 0.42,
        "Litany should create a deeper text-aware pause after sentence punctuation.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(litany.pcm, MakeSignalConfig()),
        AsBytes(text),
        "Litany text-aware pauses should keep the payload decodable.");
}

void TestFormalSteadyAddsLowVoiceWithoutBreakingDecode() {
    auto core_config = MakeAndroidSizedCoreConfig();
    core_config.flash_signal_profile = bag::FlashSignalProfile::kSteady;
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(
            core_config,
            bag::FlashSignalProfile::kSteady);
    const auto bytes = AsBytes("Steady");
    const auto layout = bag::flash::BuildPayloadLayout(bytes, signal_config);
    const auto clean_payload = bag::flash::EncodeBytesToPcm16(bytes, signal_config);
    const auto steady =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kSteady,
            bag::flash::MakeFormalVoicingConfigForFlavor(
                core_config,
                bag::FlashVoicingFlavor::kSteady));
    const auto hostile =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kHostile,
            bag::flash::MakeFormalVoicingConfigForFlavor(
                core_config,
                bag::FlashVoicingFlavor::kHostile));
    const auto steady_trimmed = bag::flash::TrimToPayloadPcm(steady.pcm, steady.descriptor);
    const auto hostile_trimmed = bag::flash::TrimToPayloadPcm(hostile.pcm, hostile.descriptor);
    const auto& first_chunk = layout.chunks.front();
    const std::size_t begin =
        first_chunk.sample_offset +
        (first_chunk.sample_count * static_cast<std::size_t>(3)) /
            static_cast<std::size_t>(10);
    const std::size_t end =
        first_chunk.sample_offset +
        (first_chunk.sample_count * static_cast<std::size_t>(7)) /
            static_cast<std::size_t>(10);

    test::AssertTrue(
        AverageAbsoluteDelta(steady_trimmed, clean_payload, begin, end) > 80.0,
        "Formal steady should add a restrained low voice layer.");
    test::AssertTrue(
        AverageAbsoluteDelta(steady_trimmed, clean_payload, begin, end) <
            AverageAbsoluteDelta(hostile_trimmed, clean_payload, begin, end),
        "Formal steady should stay more restrained than hostile.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(steady_trimmed, signal_config),
        bytes,
        "Formal steady low voice should keep the payload decodable.");
    AssertPcm16Range(steady.pcm, "Formal steady low voice output");
}

void TestHostileAddsAggressiveEdgeWithoutBreakingDecode() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    const auto layout = MakePayloadLayout("Hostile");
    const auto clean_payload = MakeCleanPayload("Hostile");
    const auto steady =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kSteady,
            config);
    const auto hostile =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kHostile,
            config);
    const auto& first_chunk = layout.chunks.front();
    const std::size_t begin =
        first_chunk.sample_offset + (first_chunk.sample_count * static_cast<std::size_t>(2)) /
                                        static_cast<std::size_t>(10);
    const std::size_t end =
        first_chunk.sample_offset + (first_chunk.sample_count * static_cast<std::size_t>(8)) /
                                        static_cast<std::size_t>(10);

    test::AssertEq(
        steady.pcm,
        clean_payload,
        "Steady with no explicit texture should preserve the clean payload.");
    test::AssertTrue(
        AverageAbsoluteDelta(hostile.pcm, clean_payload, begin, end) > 300.0,
        "Hostile should add a sharp aggressive edge across the payload body.");
    test::AssertTrue(
        hostile.pcm != steady.pcm,
        "Hostile should produce a distinct aggressive payload from steady.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(hostile.pcm, MakeSignalConfig()),
        AsBytes("Hostile"),
        "Hostile edge should keep the payload decodable.");
    AssertPcm16Range(hostile.pcm, "Hostile edge output");
}

void TestCollapseAddsTremorWithoutBreakingDecode() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    const auto layout = MakePayloadLayout("Collapse");
    const auto clean_payload = MakeCleanPayload("Collapse");
    const auto steady =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kSteady,
            config);
    const auto collapse =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kCollapse,
            config);
    const auto& first_chunk = layout.chunks.front();
    const std::size_t begin =
        first_chunk.sample_offset + (first_chunk.sample_count * static_cast<std::size_t>(2)) /
                                        static_cast<std::size_t>(10);
    const std::size_t end =
        first_chunk.sample_offset + (first_chunk.sample_count * static_cast<std::size_t>(8)) /
                                        static_cast<std::size_t>(10);

    test::AssertEq(
        steady.pcm,
        clean_payload,
        "Steady with no explicit texture should preserve the clean payload.");
    test::AssertTrue(
        collapse.pcm != steady.pcm,
        "Collapse should produce a distinct trembling payload from steady.");
    test::AssertTrue(
        AverageAbsoluteDelta(collapse.pcm, clean_payload, begin, end) > 180.0,
        "Collapse should add a tremor layer across the payload body.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(collapse.pcm, MakeSignalConfig()),
        AsBytes("Collapse"),
        "Collapse tremor should keep the payload decodable.");
    AssertPcm16Range(collapse.pcm, "Collapse tremor output");
}

void TestCollapseAddsIrregularHesitationWithoutBreakingDecode() {
    bag::flash::FlashVoicingConfig config{};
    config.sample_rate_hz = 44100;
    const std::string text = "Collapse hesitation";
    const auto layout = MakePayloadLayout(text);
    const auto clean_payload = MakeCleanPayload(text);
    const auto collapse =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            layout,
            bag::FlashVoicingFlavor::kCollapse,
            config);
    std::size_t hesitating_chunks = 0;
    for (const auto& chunk : layout.chunks) {
        const std::size_t mid_begin =
            chunk.sample_offset +
            (chunk.sample_count * static_cast<std::size_t>(30)) /
                static_cast<std::size_t>(100);
        const std::size_t mid_end =
            chunk.sample_offset +
            (chunk.sample_count * static_cast<std::size_t>(48)) /
                static_cast<std::size_t>(100);
        const std::size_t tail_begin =
            chunk.sample_offset +
            (chunk.sample_count * static_cast<std::size_t>(86)) /
                static_cast<std::size_t>(100);
        const std::size_t tail_end =
            chunk.sample_offset +
            (chunk.sample_count * static_cast<std::size_t>(98)) /
                static_cast<std::size_t>(100);
        if (AverageAbsoluteSample(collapse.pcm, tail_begin, tail_end) <
            AverageAbsoluteSample(collapse.pcm, mid_begin, mid_end) * 0.45) {
            ++hesitating_chunks;
        }
    }

    test::AssertTrue(
        hesitating_chunks >= static_cast<std::size_t>(3),
        "Collapse should create several irregular near-silent hesitation tails.");
    test::AssertEq(
        bag::flash::DecodePcm16ToBytes(collapse.pcm, MakeSignalConfig()),
        AsBytes(text),
        "Collapse irregular hesitation should keep the payload decodable.");
}

void TestLitanyVariableSilenceRoundTripsText() {
    auto core_config = MakeAndroidSizedCoreConfig();
    core_config.flash_signal_profile = bag::FlashSignalProfile::kLitany;
    core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
    const std::string text = std::string("A B") + "\xEF" "\xBC" "\x8C" "C.";
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(
            core_config,
            bag::FlashSignalProfile::kLitany);
    const auto fixed_layout = bag::flash::BuildPayloadLayout(AsBytes(text), signal_config);
    const auto variable_layout =
        bag::flash::BuildPayloadLayoutForVoicing(
            AsBytes(text),
            signal_config,
            bag::FlashVoicingFlavor::kLitany);
    std::size_t silence_count = 0;
    bool has_bit_pause = false;
    bool has_space_pause = false;
    bool has_phrase_pause = false;
    bool has_sentence_pause = false;
    bool all_pauses_shorter_than_tone = true;
    for (const auto& chunk : variable_layout.chunks) {
        if (chunk.kind != bag::flash::FlashPayloadSegmentKind::kSilence) {
            continue;
        }
        ++silence_count;
        const std::size_t slot_count =
            chunk.sample_count / signal_config.samples_per_silence_slot;
        all_pauses_shorter_than_tone =
            all_pauses_shorter_than_tone &&
            chunk.sample_count < signal_config.samples_per_bit;
        has_bit_pause = has_bit_pause || slot_count == static_cast<std::size_t>(1);
        has_space_pause = has_space_pause || slot_count == static_cast<std::size_t>(3);
        has_phrase_pause = has_phrase_pause || slot_count == static_cast<std::size_t>(4);
        has_sentence_pause = has_sentence_pause || slot_count == static_cast<std::size_t>(8);
    }
    std::vector<std::int16_t> pcm;
    std::string decoded;

    test::AssertTrue(
        silence_count >= AsBytes(text).size() * static_cast<std::size_t>(8),
        "Litany variable layout should insert real silence after every payload bit.");
    test::AssertTrue(
        has_bit_pause && has_space_pause && has_phrase_pause && has_sentence_pause,
        "Litany variable layout should make spaces, phrase punctuation, and sentence punctuation larger than bit pauses.");
    test::AssertTrue(
        signal_config.samples_per_bit > signal_config.samples_per_silence_slot,
        "Litany low/high tone windows should be longer than silence slots.");
    test::AssertTrue(
        !all_pauses_shorter_than_tone,
        "Litany sentence pauses may still exceed a tone window for chant phrasing.");
    test::AssertTrue(
        variable_layout.payload_sample_count > fixed_layout.payload_sample_count,
        "Litany variable layout should be longer than the fixed bit grid.");
    test::AssertEq(
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            core_config,
            text,
            bag::FlashSignalProfile::kLitany,
            bag::FlashVoicingFlavor::kLitany,
            &pcm),
        bag::ErrorCode::kOk,
        "Litany variable silence encode should succeed.");
    test::AssertEq(
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            core_config,
            pcm,
            bag::FlashSignalProfile::kLitany,
            bag::FlashVoicingFlavor::kLitany,
            &decoded),
        bag::ErrorCode::kOk,
        "Litany gap-aware decode should skip variable silence.");
    test::AssertEq(decoded, text, "Litany variable silence should roundtrip text.");
}

void TestLitanyPeriodicChantPausesWithoutPunctuation() {
    auto core_config = MakeAndroidSizedCoreConfig();
    core_config.flash_signal_profile = bag::FlashSignalProfile::kLitany;
    core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
    const std::string text = "LitanyCadenceWithoutMarks";
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(
            core_config,
            bag::FlashSignalProfile::kLitany);
    const auto fixed_layout = bag::flash::BuildPayloadLayout(AsBytes(text), signal_config);
    const auto variable_layout =
        bag::flash::BuildPayloadLayoutForVoicing(
            AsBytes(text),
            signal_config,
            bag::FlashVoicingFlavor::kLitany);
    std::size_t silence_count = 0;
    bool has_bit_pause = false;
    bool has_periodic_breath_pause = false;
    bool bit_pause_is_shorter_than_tone = false;
    for (const auto& chunk : variable_layout.chunks) {
        if (chunk.kind != bag::flash::FlashPayloadSegmentKind::kSilence) {
            continue;
        }
        ++silence_count;
        const std::size_t slot_count =
            chunk.sample_count / signal_config.samples_per_silence_slot;
        has_bit_pause =
            has_bit_pause || slot_count == static_cast<std::size_t>(1);
        bit_pause_is_shorter_than_tone =
            bit_pause_is_shorter_than_tone ||
            (slot_count == static_cast<std::size_t>(1) &&
             chunk.sample_count < signal_config.samples_per_bit);
        has_periodic_breath_pause =
            has_periodic_breath_pause || slot_count == static_cast<std::size_t>(5);
    }
    std::vector<std::int16_t> pcm;
    std::string decoded;

    test::AssertTrue(
        silence_count >= AsBytes(text).size() * static_cast<std::size_t>(8),
        "Litany should add a chant pause after every low/high bit even without spaces or punctuation.");
    test::AssertTrue(
        has_bit_pause && has_periodic_breath_pause,
        "Litany periodic chant cadence should mix per-bit pauses with larger periodic breaths.");
    test::AssertTrue(
        bit_pause_is_shorter_than_tone,
        "Litany per-bit chant pauses should be shorter than each low/high tone.");
    test::AssertTrue(
        variable_layout.payload_sample_count > fixed_layout.payload_sample_count,
        "Litany periodic chant cadence should lengthen the payload timeline.");
    test::AssertEq(
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            core_config,
            text,
            bag::FlashSignalProfile::kLitany,
            bag::FlashVoicingFlavor::kLitany,
            &pcm),
        bag::ErrorCode::kOk,
        "Litany periodic chant pause encode should succeed.");
    test::AssertEq(
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            core_config,
            pcm,
            bag::FlashSignalProfile::kLitany,
            bag::FlashVoicingFlavor::kLitany,
            &decoded),
        bag::ErrorCode::kOk,
        "Litany gap-aware decode should skip periodic chant pauses.");
    test::AssertEq(decoded, text, "Litany periodic chant pauses should roundtrip text.");
}

void TestCollapseVariableSilenceIsDeterministicAndRoundTripsText() {
    auto core_config = MakeAndroidSizedCoreConfig();
    core_config.flash_signal_profile = bag::FlashSignalProfile::kSteady;
    core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kCollapse;
    const std::string text = "Collapse hesitation makes the signal stop and stutter.";
    const auto signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(
            core_config,
            bag::FlashSignalProfile::kSteady);
    const auto first_layout =
        bag::flash::BuildPayloadLayoutForVoicing(
            AsBytes(text),
            signal_config,
            bag::FlashVoicingFlavor::kCollapse);
    const auto second_layout =
        bag::flash::BuildPayloadLayoutForVoicing(
            AsBytes(text),
            signal_config,
            bag::FlashVoicingFlavor::kCollapse);
    const auto silence_count =
        std::count_if(first_layout.chunks.begin(), first_layout.chunks.end(), [](const auto& chunk) {
            return chunk.kind == bag::flash::FlashPayloadSegmentKind::kSilence;
        });
    std::size_t fixed_stutter_pause_count = 0;
    std::size_t adjacent_stutter_pause_count = 0;
    bool all_pauses_use_stutter_or_panic_slots = true;
    std::size_t previous_stutter_bit_position = static_cast<std::size_t>(-1);
    for (const auto& chunk : first_layout.chunks) {
        if (chunk.kind != bag::flash::FlashPayloadSegmentKind::kSilence) {
            continue;
        }
        const std::size_t slot_count = chunk.sample_count / signal_config.samples_per_bit;
        all_pauses_use_stutter_or_panic_slots =
            all_pauses_use_stutter_or_panic_slots &&
            (slot_count == static_cast<std::size_t>(2) ||
             slot_count == static_cast<std::size_t>(5));
        if (slot_count != static_cast<std::size_t>(2)) {
            continue;
        }
        ++fixed_stutter_pause_count;
        const std::size_t bit_position =
            chunk.byte_index * static_cast<std::size_t>(8) +
            static_cast<std::size_t>(chunk.bit_index_in_byte);
        if (previous_stutter_bit_position + static_cast<std::size_t>(1) == bit_position) {
            ++adjacent_stutter_pause_count;
        }
        previous_stutter_bit_position = bit_position;
    }
    std::vector<std::int16_t> pcm;
    std::string decoded;

    test::AssertEq(
        first_layout.payload_sample_count,
        second_layout.payload_sample_count,
        "Collapse variable silence should be deterministic for the same input.");
    test::AssertEq(
        first_layout.chunks.size(),
        second_layout.chunks.size(),
        "Collapse variable silence should produce a stable segment count.");
    test::AssertTrue(
        silence_count >= static_cast<std::size_t>(5),
        "Collapse stutter layout should insert several real hesitation silences.");
    test::AssertTrue(
        fixed_stutter_pause_count >= static_cast<std::size_t>(5),
        "Collapse stutter layout should mostly use fixed two-slot hesitation pauses.");
    test::AssertTrue(
        adjacent_stutter_pause_count >= static_cast<std::size_t>(2),
        "Collapse stutter layout should form local clusters of repeated fixed pauses.");
    test::AssertTrue(
        all_pauses_use_stutter_or_panic_slots,
        "Collapse stutter layout should avoid random short/medium/long pause lengths.");
    test::AssertEq(
        bag::flash::EncodeTextToPcm16WithSignalProfileAndFlavor(
            core_config,
            text,
            bag::FlashSignalProfile::kSteady,
            bag::FlashVoicingFlavor::kCollapse,
            &pcm),
        bag::ErrorCode::kOk,
        "Collapse variable silence encode should succeed.");
    test::AssertEq(
        bag::flash::DecodePcm16ToTextWithSignalProfileAndFlavor(
            core_config,
            pcm,
            bag::FlashSignalProfile::kSteady,
            bag::FlashVoicingFlavor::kCollapse,
            &decoded),
        bag::ErrorCode::kOk,
        "Collapse gap-aware decode should skip hesitation silence.");
    test::AssertEq(decoded, text, "Collapse variable silence should roundtrip text.");
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

}  // namespace

namespace flash_voicing_test {

void RegisterFlashVoicingPayloadTests(test::Runner& runner) {
    runner.Add("FlashVoicing.NoOpVoicingPreservesPayload", TestNoOpVoicingPreservesPayload);
    runner.Add("FlashVoicing.EnvelopeKeepsPayloadLength", TestEnvelopeKeepsPayloadLength);
    runner.Add("FlashVoicing.HarmonicVoicingStaysWithinPcm16Range", TestHarmonicVoicingStaysWithinPcm16Range);
    runner.Add("FlashVoicing.BoundaryClickVoicingIsDeterministic", TestBoundaryClickVoicingIsDeterministic);
    runner.Add("FlashVoicing.StyledVoicingOutputIsStable", TestStyledVoicingOutputIsStable);
    runner.Add("FlashVoicing.DefaultVoicingMatchesExplicitSteady", TestDefaultVoicingMatchesExplicitSteady);
    runner.Add("FlashVoicing.FlavorVoicingMatchesExplicitLitanyConfiguration",
               TestFlavorVoicingMatchesExplicitLitanyConfiguration);
    runner.Add("FlashVoicing.LitanyDiffersButDecodesLikeSteady",
               TestLitanyDiffersButDecodesLikeSteady);
    runner.Add("FlashVoicing.LitanyAddsContinuousChantDroneWithoutBreakingDecode",
               TestLitanyAddsContinuousChantDroneWithoutBreakingDecode);
    runner.Add("FlashVoicing.LitanyPhraseTailDipsWithoutBreakingDecode",
               TestLitanyPhraseTailDipsWithoutBreakingDecode);
    runner.Add("FlashVoicing.LitanyTextAwarePausesWithoutBreakingDecode",
               TestLitanyTextAwarePausesWithoutBreakingDecode);
    runner.Add("FlashVoicing.FormalSteadyAddsLowVoiceWithoutBreakingDecode",
               TestFormalSteadyAddsLowVoiceWithoutBreakingDecode);
    runner.Add("FlashVoicing.HostileAddsAggressiveEdgeWithoutBreakingDecode",
               TestHostileAddsAggressiveEdgeWithoutBreakingDecode);
    runner.Add("FlashVoicing.CollapseAddsTremorWithoutBreakingDecode",
               TestCollapseAddsTremorWithoutBreakingDecode);
    runner.Add("FlashVoicing.CollapseAddsIrregularHesitationWithoutBreakingDecode",
               TestCollapseAddsIrregularHesitationWithoutBreakingDecode);
    runner.Add("FlashVoicing.LitanyVariableSilenceRoundTripsText",
               TestLitanyVariableSilenceRoundTripsText);
    runner.Add("FlashVoicing.LitanyPeriodicChantPausesWithoutPunctuation",
               TestLitanyPeriodicChantPausesWithoutPunctuation);
    runner.Add("FlashVoicing.CollapseVariableSilenceIsDeterministicAndRoundTripsText",
               TestCollapseVariableSilenceIsDeterministicAndRoundTripsText);
    runner.Add("FlashVoicing.ByteBoundaryAccentRemainsStrongerThanNibbleAccent",
               TestByteBoundaryAccentRemainsStrongerThanNibbleAccent);
}

}  // namespace flash_voicing_test
