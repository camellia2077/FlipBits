#include "test_std_support.h"
#include "test_framework.h"

import bag.flash.phy_clean;
import bag.flash.signal;
import bag.flash.voicing;

#include "flash_voicing_test_support.h"

namespace {

using namespace flash_voicing_test;

void TestLitanyPreambleUsesThreeEvenBellStrikes() {
    constexpr int kSampleRateHz = 44100;
    constexpr std::size_t kPreambleSampleCount =
        static_cast<std::size_t>(kSampleRateHz * 135 / 100);

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kLitany,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));

    const auto [strike1_begin, strike1_end] = SecondsRange(kSampleRateHz, 0.20, 0.26);
    const auto [gap1_begin, gap1_end] = SecondsRange(kSampleRateHz, 0.43, 0.48);
    const auto [strike2_begin, strike2_end] = SecondsRange(kSampleRateHz, 0.60, 0.66);
    const auto [gap2_begin, gap2_end] = SecondsRange(kSampleRateHz, 0.78, 0.81);
    const auto [strike3_begin, strike3_end] = SecondsRange(kSampleRateHz, 1.00, 1.06);

    constexpr double kStrike1Center = 0.20;
    constexpr double kStrike2Center = 0.60;
    constexpr double kStrike3Center = 1.00;
    test::AssertTrue(
        std::abs((kStrike2Center - kStrike1Center) -
                 (kStrike3Center - kStrike2Center)) < 0.0001,
        "litany preamble bell strike centers should be strictly evenly spaced.");

    const double strike1_energy = AverageAbsoluteSample(voiced.pcm, strike1_begin, strike1_end);
    const double gap1_energy = AverageAbsoluteSample(voiced.pcm, gap1_begin, gap1_end);
    const double strike2_energy = AverageAbsoluteSample(voiced.pcm, strike2_begin, strike2_end);
    const double gap2_energy = AverageAbsoluteSample(voiced.pcm, gap2_begin, gap2_end);
    const double strike3_energy = AverageAbsoluteSample(voiced.pcm, strike3_begin, strike3_end);
    const auto [strike1_tail_begin, strike1_tail_end] = SecondsRange(kSampleRateHz, 0.30, 0.40);
    const auto [strike3_tail_begin, strike3_tail_end] = SecondsRange(kSampleRateHz, 1.14, 1.26);
    const auto [terminal_quiet_begin, terminal_quiet_end] = SecondsRange(kSampleRateHz, 1.32, 1.345);
    const double strike1_tail_energy = AverageAbsoluteSample(voiced.pcm, strike1_tail_begin, strike1_tail_end);
    const double strike3_tail_energy = AverageAbsoluteSample(voiced.pcm, strike3_tail_begin, strike3_tail_end);
    const double terminal_quiet_energy =
        AverageAbsoluteSample(voiced.pcm, terminal_quiet_begin, terminal_quiet_end);

    test::AssertTrue(
        strike1_energy > gap1_energy * 1.15,
        "litany preamble should make the first bell attack stronger than its long tail.");
    test::AssertTrue(
        strike2_energy > strike1_tail_energy * 1.25,
        "litany preamble should ring the second bell strike after the first long tail.");
    test::AssertTrue(
        strike2_energy > gap2_energy * 1.15,
        "litany preamble should keep the second bell attack clear through its long tail.");
    test::AssertTrue(
        strike3_energy > gap2_energy * 1.15,
        "litany preamble should end with a third bell strike after the second long tail.");
    test::AssertTrue(
        strike3_tail_energy > terminal_quiet_energy * 2.0,
        "litany preamble bell should carry a long low-metal tail instead of a desk-tap transient.");
}

void TestLitanyEpilogueUsesLongThenShortBellStrikes() {
    constexpr int kSampleRateHz = 44100;
    constexpr std::size_t kEpilogueSampleCount =
        static_cast<std::size_t>(kSampleRateHz * 115 / 100);

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kLitany,
        MakeTrimEnabledConfig(static_cast<std::size_t>(0), kEpilogueSampleCount));

    const std::size_t epilogue_begin = voiced.descriptor.leading_nonpayload_samples +
                                       voiced.descriptor.payload_sample_count;
    const auto [long_begin_local, long_end_local] =
        SecondsRange(kSampleRateHz, 0.20, 0.30);
    const auto [long_tail_begin_local, long_tail_end_local] =
        SecondsRange(kSampleRateHz, 0.48, 0.66);
    const auto [gap_begin_local, gap_end_local] =
        SecondsRange(kSampleRateHz, 0.70, 0.78);
    const auto [short_begin_local, short_end_local] =
        SecondsRange(kSampleRateHz, 0.82, 0.90);
    const auto [short_tail_begin_local, short_tail_end_local] =
        SecondsRange(kSampleRateHz, 0.98, 1.10);

    const double long_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + long_begin_local,
        epilogue_begin + long_end_local);
    const double long_tail_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + long_tail_begin_local,
        epilogue_begin + long_tail_end_local);
    const double gap_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + gap_begin_local,
        epilogue_begin + gap_end_local);
    const double short_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + short_begin_local,
        epilogue_begin + short_end_local);
    const double short_tail_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + short_tail_begin_local,
        epilogue_begin + short_tail_end_local);

    test::AssertTrue(
        long_energy > gap_energy * 1.25,
        "litany epilogue should begin with a louder long closing bell strike.");
    test::AssertTrue(
        long_tail_energy > gap_energy * 0.55,
        "litany epilogue long bell should keep an audible low-metal tail before the short bell.");
    test::AssertTrue(
        short_energy > gap_energy * 1.25,
        "litany epilogue should answer with a short second bell strike after the pause.");
    test::AssertTrue(
        short_tail_energy < long_tail_energy * 0.82,
        "litany epilogue second bell should decay faster than the long closing bell.");
}

void TestSteadyPreambleUsesThreeHandshakeBursts() {
    constexpr std::size_t kPreambleSampleCount = 1600;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kSteady,
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
        "steady preamble should open with a strong short handshake burst.");
    test::AssertTrue(
        burst2_energy > gap1_energy * 3.0,
        "steady preamble should re-enter with a short confirmation burst after the first gap.");
    test::AssertTrue(
        burst2_energy > gap2_energy * 3.0,
        "steady preamble should leave a clear second gap before the sync burst.");
    test::AssertTrue(
        burst3_energy > gap2_energy * 3.0,
        "steady preamble should finish with a third sync burst that pushes directly into payload onset.");
}

void TestSteadyPreambleContrastsPayloadOnset() {
    constexpr std::size_t kPreambleSampleCount = 1600;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kSteady,
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
        "steady preamble sync burst should not sound like a continuation of payload onset.");
    test::AssertTrue(
        sync_brightness > payload_brightness * 1.08,
        "steady preamble sync burst should sound brighter than the payload start.");
    test::AssertTrue(
        seam_energy < sync_energy * 0.18,
        "steady preamble should leave a low-energy seam before payload begins.");
}

void TestSteadyEpilogueUsesClosingBurstAndAckChirp() {
    constexpr std::size_t kEpilogueSampleCount = 1200;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kSteady,
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
        "steady epilogue should begin with a short closing burst before the acknowledgement gap.");
    test::AssertTrue(
        ack_energy > gap_energy * 3.0,
        "steady epilogue should emit a distinct ack chirp after the main closing burst.");
    test::AssertTrue(
        tail_energy < ack_energy * 0.45,
        "steady epilogue should decay quickly after the ack chirp instead of trailing like litany.");
}

void TestSteadyEpilogueContrastsPayloadTailAndStopsHard() {
    constexpr std::size_t kPreambleSampleCount = 1600;
    constexpr std::size_t kEpilogueSampleCount = 1200;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto preamble_voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kSteady,
        MakeTrimEnabledConfig(kPreambleSampleCount, static_cast<std::size_t>(0)));
    const auto epilogue_voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kSteady,
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
        "steady closing burst should not sound like an extension of the payload tail.");
    test::AssertTrue(
        ack_brightness < preamble_sync_brightness * 0.92,
        "steady ack chirp should sound lower and less bright than the preamble sync burst.");
    test::AssertTrue(
        tail_energy < ack_energy * 0.12,
        "steady epilogue should hard-stop after the ack chirp.");
}

void TestEmotionShellProfilesAreDistinct() {
    constexpr std::size_t kShortPreambleSampleCount = 2400;
    constexpr std::size_t kShortEpilogueSampleCount = 1800;
    constexpr std::size_t kLitanyPreambleSampleCount = 44100 * 135 / 100;
    constexpr std::size_t kLitanyEpilogueSampleCount = 44100 * 115 / 100;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto short_config = MakeTrimEnabledConfig(kShortPreambleSampleCount, kShortEpilogueSampleCount);
    const auto litany_config = MakeTrimEnabledConfig(kLitanyPreambleSampleCount, kLitanyEpilogueSampleCount);

    const auto steady = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload, layout, bag::FlashVoicingFlavor::kSteady, short_config);
    const auto hostile = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload, layout, bag::FlashVoicingFlavor::kHostile, short_config);
    const auto litany = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload, layout, bag::FlashVoicingFlavor::kLitany, litany_config);
    const auto collapse = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload, layout, bag::FlashVoicingFlavor::kCollapse, short_config);

    const std::size_t epilogue_begin =
        steady.descriptor.leading_nonpayload_samples + steady.descriptor.payload_sample_count;
    const std::size_t epilogue_end = epilogue_begin + kShortEpilogueSampleCount;
    const auto [litany_attack_begin, litany_attack_end] = SecondsRange(44100, 0.20, 0.26);
    const std::size_t litany_epilogue_begin =
        litany.descriptor.leading_nonpayload_samples + litany.descriptor.payload_sample_count;

    test::AssertTrue(
        AverageAbsoluteDelta(steady.pcm, hostile.pcm, 0, kShortPreambleSampleCount) > 500.0,
        "hostile preamble should have a distinct aggressive shell instead of reusing steady.");
    test::AssertTrue(
        AverageAbsoluteSample(litany.pcm, litany_attack_begin, litany_attack_end) > 500.0,
        "litany preamble should have a distinct ceremonial shell instead of reusing steady.");
    test::AssertTrue(
        AverageAbsoluteDelta(steady.pcm, hostile.pcm, epilogue_begin, epilogue_end) > 0.0,
        "hostile epilogue should have a distinct aggressive shell instead of reusing steady.");
    test::AssertTrue(
        AverageAbsoluteSample(
            litany.pcm,
            litany_epilogue_begin + SecondsToSampleCount(44100, 0.20),
            litany_epilogue_begin + SecondsToSampleCount(44100, 0.30)) > 500.0,
        "litany epilogue should have a distinct ceremonial shell instead of reusing steady.");
    test::AssertTrue(
        AverageAbsoluteDelta(steady.pcm, collapse.pcm, epilogue_begin, epilogue_end) > 0.0,
        "collapse epilogue should have a distinct failure shell instead of reusing the old closure.");
}

void TestCollapseEpilogueUsesBrokenFailureCadence() {
    constexpr std::size_t kEpilogueSampleCount = 1800;

    const auto clean_payload = MakeCleanPayload("AB");
    const auto layout = MakePayloadLayout("AB");
    const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
        clean_payload,
        layout,
        bag::FlashVoicingFlavor::kCollapse,
        MakeTrimEnabledConfig(static_cast<std::size_t>(0), kEpilogueSampleCount));

    const std::size_t epilogue_begin =
        voiced.descriptor.leading_nonpayload_samples + voiced.descriptor.payload_sample_count;
    const auto [failure1_begin_local, failure1_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.06, 0.12);
    const auto [failure2_begin_local, failure2_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.20, 0.25);
    const auto [gap_begin_local, gap_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.29, 0.34);
    const auto [tail_begin_local, tail_end_local] =
        FractionalRange(kEpilogueSampleCount, 0.78, 0.94);

    const double failure1_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + failure1_begin_local,
        epilogue_begin + failure1_end_local);
    const double failure2_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + failure2_begin_local,
        epilogue_begin + failure2_end_local);
    const double gap_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + gap_begin_local,
        epilogue_begin + gap_end_local);
    const double tail_energy = AverageAbsoluteSample(
        voiced.pcm,
        epilogue_begin + tail_begin_local,
        epilogue_begin + tail_end_local);

    test::AssertTrue(
        failure1_energy > gap_energy * 2.5,
        "collapse epilogue should start with a broken failure fragment.");
    test::AssertTrue(
        failure2_energy > gap_energy * 2.0,
        "collapse epilogue should briefly re-enter after the first failure fragment.");
    test::AssertTrue(
        tail_energy < failure1_energy * 0.18,
        "collapse epilogue should fall away instead of keeping the old audible tail.");
}

void TestEmotionShellsStayOutsidePayloadDecode() {
    constexpr std::size_t kPreambleSampleCount = 2400;
    constexpr std::size_t kEpilogueSampleCount = 1800;

    const std::string text = "AB";
    const auto clean_payload = MakeCleanPayload(text);
    const auto layout = MakePayloadLayout(text);
    const auto config = MakeTrimEnabledConfig(kPreambleSampleCount, kEpilogueSampleCount);
    const bag::FlashVoicingFlavor flavors[] = {
        bag::FlashVoicingFlavor::kSteady,
        bag::FlashVoicingFlavor::kHostile,
        bag::FlashVoicingFlavor::kLitany,
        bag::FlashVoicingFlavor::kCollapse,
    };

    for (const bag::FlashVoicingFlavor flavor : flavors) {
        const auto voiced = bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload, layout, flavor, config);
        const auto trimmed_payload = bag::flash::TrimToPayloadPcm(voiced.pcm, voiced.descriptor);

        test::AssertEq(
            bag::flash::DecodePcm16ToBytes(trimmed_payload, MakeSignalConfig()),
            AsBytes(text),
            "emotion-specific preamble and epilogue shells must stay outside payload decode.");
    }
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

namespace flash_voicing_test {

void RegisterFlashVoicingShellTests(test::Runner& runner) {
    runner.Add("FlashVoicing.LitanyPreambleUsesThreeEvenBellStrikes",
               TestLitanyPreambleUsesThreeEvenBellStrikes);
    runner.Add("FlashVoicing.LitanyEpilogueUsesLongThenShortBellStrikes",
               TestLitanyEpilogueUsesLongThenShortBellStrikes);
    runner.Add("FlashVoicing.SteadyPreambleUsesThreeHandshakeBursts",
               TestSteadyPreambleUsesThreeHandshakeBursts);
    runner.Add("FlashVoicing.SteadyPreambleContrastsPayloadOnset",
               TestSteadyPreambleContrastsPayloadOnset);
    runner.Add("FlashVoicing.SteadyEpilogueUsesClosingBurstAndAckChirp",
               TestSteadyEpilogueUsesClosingBurstAndAckChirp);
    runner.Add("FlashVoicing.SteadyEpilogueContrastsPayloadTailAndStopsHard",
               TestSteadyEpilogueContrastsPayloadTailAndStopsHard);
    runner.Add("FlashVoicing.EmotionShellProfilesAreDistinct", TestEmotionShellProfilesAreDistinct);
    runner.Add("FlashVoicing.CollapseEpilogueUsesBrokenFailureCadence",
               TestCollapseEpilogueUsesBrokenFailureCadence);
    runner.Add("FlashVoicing.EmotionShellsStayOutsidePayloadDecode", TestEmotionShellsStayOutsidePayloadDecode);
    runner.Add("FlashVoicing.TrimDescriptorTracksNonpayloadSamples", TestTrimDescriptorTracksNonpayloadSamples);
    runner.Add("FlashVoicing.TrimToPayloadPcmExtractsPayloadForDecode", TestTrimToPayloadPcmExtractsPayloadForDecode);
    runner.Add("FlashVoicing.PreambleAndEpilogueSegmentsAreInserted", TestPreambleAndEpilogueSegmentsAreInserted);
    runner.Add("FlashVoicing.TrimRejectsDescriptorMismatch", TestTrimRejectsDescriptorMismatch);
}

}  // namespace flash_voicing_test
