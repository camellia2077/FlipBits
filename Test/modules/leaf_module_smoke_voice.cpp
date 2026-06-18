#include "test_std_support.h"
#include "test_framework.h"

import bag.common.error_code;
import bag.voice.fx;

namespace {

constexpr double kPi = 3.14159265358979323846;

std::vector<std::int16_t> BuildInputPcm() {
    constexpr int kSampleRateHz = 44100;
    const std::size_t sample_count = static_cast<std::size_t>(kSampleRateHz / 4);
    std::vector<std::int16_t> pcm(sample_count, 0);
    for (std::size_t index = 0; index < sample_count; ++index) {
        const double time =
            static_cast<double>(index) / static_cast<double>(kSampleRateHz);
        const double sample =
            0.52 * std::sin(2.0 * kPi * 220.0 * time) +
            0.26 * std::sin(2.0 * kPi * 440.0 * time) +
            0.1 * std::sin(2.0 * kPi * 880.0 * time);
        pcm[index] = static_cast<std::int16_t>(
            std::lround(std::clamp(sample, -1.0, 1.0) * 32767.0));
    }
    return pcm;
}

std::vector<std::int16_t> BuildEventfulInputPcm() {
    constexpr int kSampleRateHz = 44100;
    constexpr std::size_t kSampleCount = static_cast<std::size_t>(kSampleRateHz / 2);
    constexpr std::size_t kBurstCount = 7;
    constexpr std::size_t kBurstLength = static_cast<std::size_t>(kSampleRateHz / 28);
    std::vector<std::int16_t> pcm(kSampleCount, 0);
    for (std::size_t burst = 0; burst < kBurstCount; ++burst) {
        const std::size_t start = burst * (kSampleCount / (kBurstCount + 1));
        for (std::size_t index = 0; index < kBurstLength &&
                                    start + index < pcm.size();
             ++index) {
            const double burst_phase =
                static_cast<double>(index) /
                static_cast<double>(std::max<std::size_t>(1, kBurstLength - 1));
            const double envelope = std::sin(burst_phase * kPi);
            const double time =
                static_cast<double>(start + index) /
                static_cast<double>(kSampleRateHz);
            const double sample =
                envelope *
                (0.58 * std::sin(2.0 * kPi * 180.0 * time) +
                 0.21 * std::sin(2.0 * kPi * 620.0 * time));
            pcm[start + index] = static_cast<std::int16_t>(
                std::lround(std::clamp(sample, -1.0, 1.0) * 32767.0));
        }
    }
    return pcm;
}

int PeakAbs(const std::vector<std::int16_t>& pcm) {
    int max_abs = 0;
    for (const std::int16_t sample : pcm) {
        max_abs = std::max(max_abs, std::abs(static_cast<int>(sample)));
    }
    return max_abs;
}

bool Differs(const std::vector<std::int16_t>& lhs,
             const std::vector<std::int16_t>& rhs) {
    if (lhs.size() != rhs.size()) {
        return true;
    }
    for (std::size_t index = 0; index < lhs.size(); ++index) {
        if (lhs[index] != rhs[index]) {
            return true;
        }
    }
  return false;
}

bool HasNonZeroSamples(const std::vector<std::int16_t>& pcm) {
    for (const std::int16_t sample : pcm) {
        if (sample != 0) {
            return true;
        }
    }
    return false;
}

void TestVoiceFxMachineVoiceSmoke() {
    const std::vector<std::int16_t> input = BuildInputPcm();
    bag::VoiceFxConfig config{};
    config.sample_rate_hz = 44100;
    config.enable_diagnostics = true;
    config.preset = bag::VoiceFxPreset::kMachineVoice;
    config.subvoice_style = bag::VoiceFxSubvoiceStyle::kStandard;
    bag::VoiceFxResult output;
    test::AssertEq(
        bag::ApplyVoiceFx(config, input, &output), bag::ErrorCode::kOk,
        "Voice-fx module should accept valid mono PCM.");
    test::AssertEq(output.final_mix.size(), input.size(),
                   "Machine Voice should preserve PCM length.");
    test::AssertEq(output.main_voice.size(), input.size(),
                   "Machine Voice diagnostics track should match input length.");
    test::AssertTrue(output.subvoice.empty(),
                     "Machine Voice should not synthesize a subvoice track.");
    test::AssertTrue(Differs(output.final_mix, input),
                     "Machine Voice should color the source audio.");
    test::AssertTrue(PeakAbs(output.final_mix) <= 32767,
                     "Machine Voice output should stay inside int16 range.");
}

void TestVoiceFxProcessorSmoke() {
    const std::vector<std::int16_t> input = BuildInputPcm();
    bag::VoiceFxConfig config{};
    config.sample_rate_hz = 44100;
    config.enable_diagnostics = false;
    config.preset = bag::VoiceFxPreset::kMachineVoice;
    config.subvoice_style = bag::VoiceFxSubvoiceStyle::kStandard;

    bag::VoiceFxProcessor processor(config);
    std::vector<std::int16_t> streamed_output;
    constexpr std::size_t kBlockSamples = 480;
    for (std::size_t offset = 0; offset < input.size(); offset += kBlockSamples) {
        const std::size_t block_samples =
            std::min(kBlockSamples, input.size() - offset);
        std::vector<std::int16_t> block(
            input.begin() + static_cast<std::ptrdiff_t>(offset),
            input.begin() + static_cast<std::ptrdiff_t>(offset + block_samples));
        bag::VoiceFxResult output;
        test::AssertEq(
            processor.ProcessBlock(block, &output), bag::ErrorCode::kOk,
            "Voice-fx processor should accept valid PCM blocks.");
        test::AssertEq(output.final_mix.size(), block.size(),
                       "Voice-fx processor block output should preserve block length.");
        streamed_output.insert(
            streamed_output.end(), output.final_mix.begin(), output.final_mix.end());
    }

    bag::VoiceFxResult flushed;
    test::AssertEq(processor.Flush(&flushed), bag::ErrorCode::kOk,
                   "Voice-fx processor flush should succeed.");
    test::AssertTrue(flushed.final_mix.empty(),
                     "Current voice-fx processor flush should not append tail samples.");
    test::AssertEq(streamed_output.size(), input.size(),
                   "Voice-fx processor should preserve total sample count.");
    test::AssertTrue(Differs(streamed_output, input),
                     "Voice-fx processor output should differ from dry input.");
}

void TestVoiceFxBinaricCantSmoke() {
    const std::vector<std::int16_t> input = BuildInputPcm();
    bag::VoiceFxConfig config{};
    config.sample_rate_hz = 44100;
    config.enable_diagnostics = true;
    config.preset = bag::VoiceFxPreset::kBinaricCant;
    config.subvoice_style = bag::VoiceFxSubvoiceStyle::kStandard;
    bag::VoiceFxResult output;
    test::AssertEq(
        bag::ApplyVoiceFx(config, input, &output), bag::ErrorCode::kOk,
        "Binaric Cant should accept valid mono PCM.");
    test::AssertEq(output.final_mix.size(), input.size(),
                   "Binaric Cant final mix should preserve PCM length.");
    test::AssertEq(output.main_voice.size(), input.size(),
                   "Binaric Cant main voice should preserve PCM length.");
    test::AssertEq(output.subvoice.size(), input.size(),
                   "Binaric Cant subvoice should preserve PCM length.");
    test::AssertTrue(output.signal_overlay.empty(),
                     "Binaric Cant signal overlay should remain empty for now.");
    test::AssertTrue(Differs(output.subvoice, output.main_voice),
                     "Binaric Cant subvoice should be distinct from the main voice.");
    test::AssertTrue(Differs(output.final_mix, output.main_voice),
                     "Binaric Cant final mix should not collapse to the main track.");
    test::AssertTrue(PeakAbs(output.final_mix) <= 32767,
                     "Binaric Cant final mix should stay inside int16 range.");

    bag::VoiceFxResult repeat;
    test::AssertEq(
        bag::ApplyVoiceFx(config, input, &repeat), bag::ErrorCode::kOk,
        "Repeated Binaric Cant processing should succeed.");
    test::AssertEq(repeat.final_mix, output.final_mix,
                   "Binaric Cant final mix should stay deterministic.");
    test::AssertEq(repeat.subvoice, output.subvoice,
                   "Binaric Cant subvoice should stay deterministic.");
}

void TestVoiceFxBinaricCantSelectableSubvoiceStylesSmoke() {
    const std::vector<std::int16_t> input = BuildInputPcm();
    bag::VoiceFxConfig standard_config{};
    standard_config.sample_rate_hz = 44100;
    standard_config.enable_diagnostics = true;
    standard_config.preset = bag::VoiceFxPreset::kBinaricCant;
    standard_config.subvoice_style = bag::VoiceFxSubvoiceStyle::kStandard;

    bag::VoiceFxConfig litany_config = standard_config;
    litany_config.subvoice_style = bag::VoiceFxSubvoiceStyle::kLitany;
    bag::VoiceFxConfig hostility_config = standard_config;
    hostility_config.subvoice_style = bag::VoiceFxSubvoiceStyle::kHostility;
    bag::VoiceFxConfig collapse_config = standard_config;
    collapse_config.subvoice_style = bag::VoiceFxSubvoiceStyle::kCollapse;
    bag::VoiceFxConfig zeal_config = standard_config;
    zeal_config.subvoice_style = bag::VoiceFxSubvoiceStyle::kZeal;
    bag::VoiceFxConfig void_config = standard_config;
    void_config.subvoice_style = bag::VoiceFxSubvoiceStyle::kVoid;

    bag::VoiceFxResult standard;
    bag::VoiceFxResult litany;
    bag::VoiceFxResult hostility;
    bag::VoiceFxResult collapse;
    bag::VoiceFxResult zeal;
    bag::VoiceFxResult void_style;
    test::AssertEq(
        bag::ApplyVoiceFx(standard_config, input, &standard), bag::ErrorCode::kOk,
        "Binaric Cant standard style should succeed.");
    test::AssertEq(
        bag::ApplyVoiceFx(litany_config, input, &litany), bag::ErrorCode::kOk,
        "Binaric Cant litany style should succeed.");
    test::AssertEq(
        bag::ApplyVoiceFx(hostility_config, input, &hostility), bag::ErrorCode::kOk,
        "Binaric Cant hostility style should succeed.");
    test::AssertEq(
        bag::ApplyVoiceFx(collapse_config, input, &collapse), bag::ErrorCode::kOk,
        "Binaric Cant collapse style should succeed.");
    test::AssertEq(
        bag::ApplyVoiceFx(zeal_config, input, &zeal), bag::ErrorCode::kOk,
        "Binaric Cant zeal style should succeed.");
    test::AssertEq(
        bag::ApplyVoiceFx(void_config, input, &void_style), bag::ErrorCode::kOk,
        "Binaric Cant void style should succeed.");
    test::AssertTrue(Differs(standard.subvoice, litany.subvoice),
                     "Litany subvoice should differ from standard.");
    test::AssertTrue(Differs(standard.subvoice, hostility.subvoice),
                     "Hostility subvoice should differ from standard.");
    test::AssertTrue(Differs(standard.subvoice, collapse.subvoice),
                     "Collapse subvoice should differ from standard.");
    test::AssertTrue(Differs(standard.subvoice, zeal.subvoice),
                     "Zeal subvoice should differ from standard.");
    test::AssertTrue(Differs(standard.subvoice, void_style.subvoice),
                     "Void subvoice should differ from standard.");
}

void TestVoiceFxRawConstantSmoke() {
    const std::vector<std::int16_t> input = BuildInputPcm();
    bag::VoiceFxConfig config{};
    config.sample_rate_hz = 44100;
    config.enable_diagnostics = true;
    config.preset = bag::VoiceFxPreset::kRawConstant;
    config.subvoice_style = bag::VoiceFxSubvoiceStyle::kStandard;

    bag::VoiceFxResult output;
    test::AssertEq(
        bag::ApplyVoiceFx(config, input, &output), bag::ErrorCode::kOk,
        "Raw Constant should accept valid mono PCM.");
    test::AssertEq(output.final_mix.size(), input.size(),
                   "Raw Constant final mix should preserve PCM length.");
    test::AssertEq(output.main_voice, input,
                   "Raw Constant main voice should preserve the dry input.");
    test::AssertEq(output.subvoice.size(), input.size(),
                   "Raw Constant subvoice should preserve PCM length.");
    test::AssertTrue(HasNonZeroSamples(output.subvoice),
                     "Raw Constant should synthesize a parallel flash subvoice track.");
    test::AssertTrue(output.signal_overlay.empty(),
                     "Raw Constant signal overlay should remain empty.");
    test::AssertTrue(Differs(output.final_mix, output.main_voice),
                     "Raw Constant final mix should differ from the dry main voice.");
    test::AssertTrue(PeakAbs(output.final_mix) <= 32767,
                     "Raw Constant final mix should stay inside int16 range.");
}

void TestVoiceFxVoiceTriggerSmoke() {
    const std::vector<std::int16_t> input = BuildInputPcm();
    bag::VoiceFxConfig config{};
    config.sample_rate_hz = 44100;
    config.enable_diagnostics = true;
    config.preset = bag::VoiceFxPreset::kVoiceTrigger;
    config.subvoice_style = bag::VoiceFxSubvoiceStyle::kStandard;

    bag::VoiceFxResult output;
    test::AssertEq(
        bag::ApplyVoiceFx(config, input, &output), bag::ErrorCode::kOk,
        "Voice Trigger should accept valid mono PCM.");
    test::AssertEq(output.final_mix.size(), input.size(),
                   "Voice Trigger final mix should preserve PCM length.");
    test::AssertEq(output.main_voice, input,
                   "Voice Trigger main voice should preserve the dry input.");
    test::AssertEq(output.subvoice.size(), input.size(),
                   "Voice Trigger subvoice should preserve PCM length.");
    test::AssertTrue(HasNonZeroSamples(output.subvoice),
                     "Voice Trigger should synthesize a gated flash subvoice track.");
    test::AssertTrue(output.signal_overlay.empty(),
                     "Voice Trigger signal overlay should remain empty.");
    test::AssertTrue(Differs(output.final_mix, output.main_voice),
                     "Voice Trigger final mix should differ from the dry main voice.");
    test::AssertTrue(PeakAbs(output.final_mix) <= 32767,
                     "Voice Trigger final mix should stay inside int16 range.");
}

void TestVoiceFxSignalCantSmoke() {
    const std::vector<std::int16_t> input = BuildEventfulInputPcm();
    bag::VoiceFxConfig config{};
    config.sample_rate_hz = 44100;
    config.enable_diagnostics = true;
    config.preset = bag::VoiceFxPreset::kSignalCant;
    config.subvoice_style = bag::VoiceFxSubvoiceStyle::kStandard;

    bag::VoiceFxResult output;
    test::AssertEq(
        bag::ApplyVoiceFx(config, input, &output), bag::ErrorCode::kOk,
        "Signal Cant should accept valid mono PCM.");
    test::AssertEq(output.final_mix.size(), input.size(),
                   "Signal Cant final mix should preserve PCM length.");
    test::AssertEq(output.main_voice.size(), input.size(),
                   "Signal Cant main voice should preserve PCM length.");
    test::AssertEq(output.signal_overlay.size(), input.size(),
                   "Signal Cant overlay should preserve PCM length.");
    test::AssertTrue(output.subvoice.empty(),
                     "Signal Cant should not synthesize a Forge-style subvoice track.");
    test::AssertTrue(HasNonZeroSamples(output.signal_overlay),
                     "Signal Cant overlay should contain inserted machine-language bursts.");
    test::AssertTrue(Differs(output.signal_overlay, output.main_voice),
                     "Signal Cant overlay should not duplicate the main voice track.");
    test::AssertTrue(Differs(output.final_mix, output.main_voice),
                     "Signal Cant final mix should differ from the bare main voice.");
    test::AssertTrue(PeakAbs(output.final_mix) <= 32767,
                     "Signal Cant final mix should stay inside int16 range.");
}

void TestVoiceFxRobotVoxSmoke() {
    const std::vector<std::int16_t> input = BuildInputPcm();
    bag::VoiceFxConfig config{};
    config.sample_rate_hz = 44100;
    config.enable_diagnostics = true;
    config.preset = bag::VoiceFxPreset::kRobotVox;
    config.subvoice_style = bag::VoiceFxSubvoiceStyle::kStandard;

    bag::VoiceFxResult output;
    test::AssertEq(
        bag::ApplyVoiceFx(config, input, &output), bag::ErrorCode::kOk,
        "Robot Vox should accept valid mono PCM.");
    test::AssertEq(output.final_mix.size(), input.size(),
                   "Robot Vox should preserve PCM length.");
    test::AssertEq(output.main_voice.size(), input.size(),
                   "Robot Vox diagnostics track should match input length.");
    test::AssertTrue(output.subvoice.empty(),
                     "Robot Vox should not synthesize a subvoice track.");
    test::AssertTrue(output.signal_overlay.empty(),
                     "Robot Vox should not synthesize a signal overlay track.");
    test::AssertTrue(Differs(output.final_mix, input),
                     "Robot Vox should color the source audio.");
    test::AssertTrue(PeakAbs(output.final_mix) <= 32767,
                     "Robot Vox output should stay inside int16 range.");

    bag::VoiceFxConfig machine_config = config;
    machine_config.preset = bag::VoiceFxPreset::kMachineVoice;
    bag::VoiceFxResult machine;
    test::AssertEq(
        bag::ApplyVoiceFx(machine_config, input, &machine), bag::ErrorCode::kOk,
        "Machine Voice should succeed for Robot Vox comparison.");
    test::AssertTrue(Differs(output.final_mix, machine.final_mix),
                     "Robot Vox should sound distinct from Machine Voice.");
}

}  // namespace

namespace modules_leaf_smoke {

void RegisterLeafVoiceTests(test::Runner& runner) {
    runner.Add("ModulesLeaf.VoiceFxMachineVoiceSmoke",
               TestVoiceFxMachineVoiceSmoke);
    runner.Add("ModulesLeaf.VoiceFxProcessorSmoke",
               TestVoiceFxProcessorSmoke);
    runner.Add("ModulesLeaf.VoiceFxBinaricCantSmoke",
               TestVoiceFxBinaricCantSmoke);
    runner.Add("ModulesLeaf.VoiceFxBinaricCantSelectableSubvoiceStylesSmoke",
               TestVoiceFxBinaricCantSelectableSubvoiceStylesSmoke);
    runner.Add("ModulesLeaf.VoiceFxRawConstantSmoke",
               TestVoiceFxRawConstantSmoke);
    runner.Add("ModulesLeaf.VoiceFxVoiceTriggerSmoke",
               TestVoiceFxVoiceTriggerSmoke);
    runner.Add("ModulesLeaf.VoiceFxSignalCantSmoke",
               TestVoiceFxSignalCantSmoke);
    runner.Add("ModulesLeaf.VoiceFxRobotVoxSmoke",
               TestVoiceFxRobotVoxSmoke);
}

}  // namespace modules_leaf_smoke
