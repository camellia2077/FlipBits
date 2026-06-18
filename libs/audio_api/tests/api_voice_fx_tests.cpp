#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <vector>

#include "api_test_support.h"

namespace {

using namespace api_tests;

std::vector<std::int16_t> BuildVoiceInputPcm(int sample_rate_hz) {
  constexpr double kPi = 3.14159265358979323846;
  const std::size_t sample_count =
      static_cast<std::size_t>(sample_rate_hz / 2);
  std::vector<std::int16_t> pcm(sample_count, 0);
  for (std::size_t index = 0; index < sample_count; ++index) {
    const double time =
        static_cast<double>(index) / static_cast<double>(sample_rate_hz);
    const double sample =
        0.48 * std::sin(2.0 * kPi * 196.0 * time) +
        0.31 * std::sin(2.0 * kPi * 392.0 * time) +
        0.12 * std::sin(2.0 * kPi * 784.0 * time);
    pcm[index] = static_cast<std::int16_t>(
        std::lround(std::clamp(sample, -1.0, 1.0) * 32767.0));
  }
  return pcm;
}

std::vector<std::int16_t> BuildEventfulVoiceInputPcm(int sample_rate_hz) {
  constexpr double kPi = 3.14159265358979323846;
  const std::size_t sample_count =
      static_cast<std::size_t>(sample_rate_hz / 2);
  constexpr std::size_t kBurstCount = 7;
  const std::size_t burst_length =
      static_cast<std::size_t>(sample_rate_hz / 28);
  std::vector<std::int16_t> pcm(sample_count, 0);
  for (std::size_t burst = 0; burst < kBurstCount; ++burst) {
    const std::size_t start = burst * (sample_count / (kBurstCount + 1));
    for (std::size_t index = 0;
         index < burst_length && start + index < pcm.size(); ++index) {
      const double burst_phase =
          static_cast<double>(index) /
          static_cast<double>(std::max<std::size_t>(1, burst_length - 1));
      const double envelope = std::sin(burst_phase * kPi);
      const double time = static_cast<double>(start + index) /
                          static_cast<double>(sample_rate_hz);
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

int PeakAbs(const bag_pcm16_result& result) {
  int max_abs = 0;
  for (std::size_t index = 0; index < result.sample_count; ++index) {
    max_abs =
        std::max(max_abs, std::abs(static_cast<int>(result.samples[index])));
  }
  return max_abs;
}

bool DiffersFromInput(const bag_pcm16_result& result,
                      const std::vector<std::int16_t>& input) {
  if (result.sample_count != input.size() || result.samples == nullptr) {
    return true;
  }
  for (std::size_t index = 0; index < input.size(); ++index) {
    if (result.samples[index] != input[index]) {
      return true;
    }
  }
  return false;
}

bool DiffersBetweenTracks(const bag_pcm16_result& lhs,
                          const bag_pcm16_result& rhs) {
  if (lhs.sample_count != rhs.sample_count || lhs.samples == nullptr ||
      rhs.samples == nullptr) {
    return true;
  }
  for (std::size_t index = 0; index < lhs.sample_count; ++index) {
    if (lhs.samples[index] != rhs.samples[index]) {
      return true;
    }
  }
  return false;
}

bool HasNonZeroSamples(const bag_pcm16_result& result) {
  if (result.samples == nullptr) {
    return false;
  }
  for (std::size_t index = 0; index < result.sample_count; ++index) {
    if (result.samples[index] != 0) {
      return true;
    }
  }
  return false;
}

void AssertVoiceTrackLooksValid(const bag_pcm16_result& track,
                                std::size_t expected_count,
                                const std::string& message_prefix) {
  test::AssertEq(track.sample_count, expected_count,
                 message_prefix + " should preserve sample count.");
  test::AssertTrue(track.samples != nullptr,
                   message_prefix + " should allocate output PCM.");
  test::AssertTrue(PeakAbs(track) <= 32767,
                   message_prefix + " should stay inside int16 range.");
}

void TestApiVoiceFxRejectsInvalidArguments() {
  bag_voice_fx_config config{};
  config.sample_rate_hz = 44100;
  config.enable_diagnostics = 0;
  config.preset = BAG_VOICE_FX_MACHINE_VOICE;
  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
  config.reserved = 0;
  bag_voice_fx_result result{};
  const std::array<std::int16_t, 8> pcm = {0, 1, 2, 3, 4, 5, 6, 7};

  test::AssertEq(bag_apply_voice_fx(nullptr, pcm.data(), pcm.size(), &result),
                 BAG_INVALID_ARGUMENT,
                 "Null voice-fx config should be rejected.");
  test::AssertEq(bag_apply_voice_fx(&config, nullptr, pcm.size(), &result),
                 BAG_INVALID_ARGUMENT,
                 "Null voice-fx PCM pointer should be rejected.");
  test::AssertEq(bag_apply_voice_fx(&config, pcm.data(), pcm.size(), nullptr),
                 BAG_INVALID_ARGUMENT,
                 "Null voice-fx result should be rejected.");
  test::AssertEq(bag_apply_voice_fx(&config, pcm.data(), 0, &result),
                 BAG_INVALID_ARGUMENT,
                 "Zero-length voice-fx input should be rejected.");

  config.sample_rate_hz = 0;
  test::AssertEq(bag_apply_voice_fx(&config, pcm.data(), pcm.size(), &result),
                 BAG_INVALID_ARGUMENT,
                 "Voice-fx sample rate must be positive.");
  config.sample_rate_hz = 44100;
  config.preset = static_cast<bag_voice_fx_preset>(99);
  test::AssertEq(bag_apply_voice_fx(&config, pcm.data(), pcm.size(), &result),
                 BAG_INVALID_ARGUMENT,
                 "Unknown voice-fx preset should be rejected.");
  config.preset = BAG_VOICE_FX_MACHINE_VOICE;
  config.subvoice_style =
      static_cast<bag_voice_fx_subvoice_style>(99);
  test::AssertEq(bag_apply_voice_fx(&config, pcm.data(), pcm.size(), &result),
                 BAG_INVALID_ARGUMENT,
                 "Unsupported voice-fx subvoice style should be rejected.");

  bag_voice_fx_processor* processor = nullptr;
  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
  test::AssertEq(bag_create_voice_fx_processor(nullptr, &processor),
                 BAG_INVALID_ARGUMENT,
                 "Null voice-fx processor config should be rejected.");
  test::AssertEq(bag_create_voice_fx_processor(&config, nullptr),
                 BAG_INVALID_ARGUMENT,
                 "Null voice-fx processor out pointer should be rejected.");
  test::AssertEq(bag_create_voice_fx_processor(&config, &processor), BAG_OK,
                 "Valid voice-fx processor config should succeed.");
  test::AssertTrue(processor != nullptr,
                   "Valid voice-fx processor should allocate a handle.");
  test::AssertEq(
      bag_process_voice_fx_block(processor, nullptr, pcm.size(), &result),
      BAG_INVALID_ARGUMENT,
      "Null processor PCM pointer should be rejected.");
  test::AssertEq(
      bag_process_voice_fx_block(processor, pcm.data(), 0, &result),
      BAG_INVALID_ARGUMENT,
      "Zero-length processor PCM block should be rejected.");
  test::AssertEq(
      bag_process_voice_fx_block(processor, pcm.data(), pcm.size(), nullptr),
      BAG_INVALID_ARGUMENT,
      "Null processor result should be rejected.");
  test::AssertEq(bag_flush_voice_fx_processor(nullptr, &result),
                 BAG_INVALID_ARGUMENT,
                 "Null voice-fx processor should be rejected on flush.");
  bag_destroy_voice_fx_processor(processor);
}

void TestApiVoiceFxProcessorStreamsBlockOutput() {
  const int sample_rate_hz = 44100;
  const std::vector<std::int16_t> input = BuildVoiceInputPcm(sample_rate_hz);
  bag_voice_fx_config config{};
  config.sample_rate_hz = sample_rate_hz;
  config.enable_diagnostics = 0;
  config.preset = BAG_VOICE_FX_MACHINE_VOICE;
  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
  config.reserved = 0;

  bag_voice_fx_processor* processor = nullptr;
  test::AssertEq(bag_create_voice_fx_processor(&config, &processor), BAG_OK,
                 "Voice-fx processor should be creatable.");
  test::AssertTrue(processor != nullptr,
                   "Voice-fx processor handle should be returned.");

  std::vector<std::int16_t> streamed_output;
  std::size_t processed_samples = 0;
  constexpr std::size_t kBlockSamples = 480;
  while (processed_samples < input.size()) {
    const std::size_t remaining = input.size() - processed_samples;
    const std::size_t block_samples = std::min(kBlockSamples, remaining);
    bag_voice_fx_result block{};
    test::AssertEq(
        bag_process_voice_fx_block(processor, input.data() + processed_samples,
                                   block_samples, &block),
        BAG_OK, "Voice-fx processor block should succeed.");
    test::AssertEq(block.final_mix.sample_count, block_samples,
                   "Voice-fx processor should emit one wet sample per input sample.");
    streamed_output.insert(streamed_output.end(), block.final_mix.samples,
                           block.final_mix.samples + block.final_mix.sample_count);
    bag_free_voice_fx_result(&block);
    processed_samples += block_samples;
  }

  bag_voice_fx_result flushed{};
  test::AssertEq(bag_flush_voice_fx_processor(processor, &flushed), BAG_OK,
                 "Voice-fx processor flush should succeed.");
  test::AssertEq(flushed.final_mix.sample_count, std::size_t{0},
                 "Current voice-fx processor flush should not append tail samples.");
  bag_free_voice_fx_result(&flushed);
  bag_destroy_voice_fx_processor(processor);

  test::AssertEq(streamed_output.size(), input.size(),
                 "Voice-fx streamed output should preserve total sample count.");
  test::AssertTrue(DiffersFromInput(
                       bag_pcm16_result{
                           .samples = streamed_output.data(),
                           .sample_count = streamed_output.size(),
                       },
                       input),
                   "Voice-fx streamed output should differ from dry input.");
}

void TestApiVoiceFxMachineVoiceDiagnosticsContract() {
  const int sample_rate_hz = 44100;
  const std::vector<std::int16_t> input = BuildVoiceInputPcm(sample_rate_hz);
  bag_voice_fx_config config{};
  config.sample_rate_hz = sample_rate_hz;
  config.enable_diagnostics = 1;
  config.preset = BAG_VOICE_FX_MACHINE_VOICE;
  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
  config.reserved = 0;

  bag_voice_fx_result result{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &result), BAG_OK,
      "Machine Voice should process valid PCM.");
  AssertVoiceTrackLooksValid(result.final_mix, input.size(),
                             "Machine Voice final mix");
  AssertVoiceTrackLooksValid(result.main_voice, input.size(),
                             "Machine Voice diagnostics main track");
  test::AssertEq(result.subvoice.sample_count, std::size_t{0},
                 "Machine Voice should not return a subvoice track.");
  test::AssertEq(result.signal_overlay.sample_count, std::size_t{0},
                 "Machine Voice should not return a signal overlay track.");
  test::AssertTrue(DiffersFromInput(result.final_mix, input),
                   "Machine Voice final mix should differ from dry input.");

  bag_voice_fx_result repeat{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &repeat), BAG_OK,
      "Repeated Machine Voice application should succeed.");
  AssertPcmResultsEqual(result.final_mix, repeat.final_mix,
                        "Machine Voice final mix should stay deterministic.");
  AssertPcmResultsEqual(result.main_voice, repeat.main_voice,
                        "Machine Voice diagnostics track should stay deterministic.");
  bag_free_voice_fx_result(&result);
  bag_free_voice_fx_result(&repeat);
}

void TestApiVoiceFxBinaricCantReturnsDualLayerTracks() {
  const int sample_rate_hz = 44100;
  const std::vector<std::int16_t> input = BuildVoiceInputPcm(sample_rate_hz);
  bag_voice_fx_config config{};
  config.sample_rate_hz = sample_rate_hz;
  config.enable_diagnostics = 1;
  config.preset = BAG_VOICE_FX_BINARIC_CANT;
  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
  config.reserved = 0;

  bag_voice_fx_result binaric_cant{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &binaric_cant), BAG_OK,
      "Binaric Cant should process valid PCM.");
  AssertVoiceTrackLooksValid(binaric_cant.final_mix, input.size(),
                             "Binaric Cant final mix");
  AssertVoiceTrackLooksValid(binaric_cant.main_voice, input.size(),
                             "Binaric Cant main voice");
  AssertVoiceTrackLooksValid(binaric_cant.subvoice, input.size(),
                             "Binaric Cant subvoice");
  test::AssertEq(binaric_cant.signal_overlay.sample_count, std::size_t{0},
                 "Binaric Cant should leave signal overlay empty in VNext.");
  test::AssertTrue(DiffersBetweenTracks(binaric_cant.main_voice, binaric_cant.subvoice),
                   "Binaric Cant subvoice should not duplicate the main voice track.");

  bag_voice_fx_config machine_config = config;
  machine_config.preset = BAG_VOICE_FX_MACHINE_VOICE;
  bag_voice_fx_result machine{};
  test::AssertEq(
      bag_apply_voice_fx(&machine_config, input.data(), input.size(), &machine),
      BAG_OK,
      "Machine Voice should still succeed for comparison.");
  test::AssertTrue(DiffersBetweenTracks(binaric_cant.final_mix, machine.final_mix),
                   "Binaric Cant should not be only a louder Machine Voice.");

  bag_voice_fx_config no_diagnostics = config;
  no_diagnostics.enable_diagnostics = 0;
  bag_voice_fx_result no_debug{};
  test::AssertEq(bag_apply_voice_fx(&no_diagnostics, input.data(), input.size(),
                                    &no_debug),
                 BAG_OK,
                 "Binaric Cant without diagnostics should still succeed.");
  AssertVoiceTrackLooksValid(no_debug.final_mix, input.size(),
                             "Binaric Cant final mix without diagnostics");
  test::AssertEq(no_debug.main_voice.sample_count, std::size_t{0},
                 "Binaric Cant should omit diagnostics main track when disabled.");
  test::AssertEq(no_debug.subvoice.sample_count, std::size_t{0},
                 "Binaric Cant should omit diagnostics subvoice when disabled.");
  test::AssertEq(no_debug.signal_overlay.sample_count, std::size_t{0},
                 "Binaric Cant should omit signal overlay when diagnostics are disabled.");

  bag_free_voice_fx_result(&binaric_cant);
  bag_free_voice_fx_result(&machine);
  bag_free_voice_fx_result(&no_debug);
}

void TestApiVoiceFxBinaricCantSupportsSelectedSubvoiceStyles() {
  const int sample_rate_hz = 44100;
  const std::vector<std::int16_t> input = BuildVoiceInputPcm(sample_rate_hz);
  bag_voice_fx_config config{};
  config.sample_rate_hz = sample_rate_hz;
  config.enable_diagnostics = 1;
  config.preset = BAG_VOICE_FX_BINARIC_CANT;
  config.reserved = 0;

  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
  bag_voice_fx_result standard{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &standard), BAG_OK,
      "Binaric Cant standard subvoice should succeed.");

  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_LITANY;
  bag_voice_fx_result litany{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &litany), BAG_OK,
      "Binaric Cant litany subvoice should succeed.");

  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_HOSTILITY;
  bag_voice_fx_result hostility{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &hostility), BAG_OK,
      "Binaric Cant hostility subvoice should succeed.");

  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_COLLAPSE;
  bag_voice_fx_result collapse{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &collapse), BAG_OK,
      "Binaric Cant collapse subvoice should succeed.");

  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_ZEAL;
  bag_voice_fx_result zeal{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &zeal), BAG_OK,
      "Binaric Cant zeal subvoice should succeed.");

  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_VOID;
  bag_voice_fx_result void_style{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &void_style), BAG_OK,
      "Binaric Cant void subvoice should succeed.");

  test::AssertTrue(DiffersBetweenTracks(standard.subvoice, litany.subvoice),
                   "Litany subvoice should differ from standard.");
  test::AssertTrue(DiffersBetweenTracks(standard.subvoice, hostility.subvoice),
                   "Hostility subvoice should differ from standard.");
  test::AssertTrue(DiffersBetweenTracks(standard.subvoice, collapse.subvoice),
                   "Collapse subvoice should differ from standard.");
  test::AssertTrue(DiffersBetweenTracks(standard.subvoice, zeal.subvoice),
                   "Zeal subvoice should differ from standard.");
  test::AssertTrue(DiffersBetweenTracks(standard.subvoice, void_style.subvoice),
                   "Void subvoice should differ from standard.");
  test::AssertTrue(PeakAbs(zeal.subvoice) < PeakAbs(standard.subvoice) * 0.65,
                   "Zeal subvoice should be mixed lower than standard.");
  test::AssertTrue(
      PeakAbs(hostility.subvoice) < PeakAbs(standard.subvoice) * 0.55,
      "Hostility subvoice should be mixed lower than standard.");

  bag_free_voice_fx_result(&standard);
  bag_free_voice_fx_result(&litany);
  bag_free_voice_fx_result(&hostility);
  bag_free_voice_fx_result(&collapse);
  bag_free_voice_fx_result(&zeal);
  bag_free_voice_fx_result(&void_style);
}

void TestApiVoiceFxRawConstantReturnsDryPlusSubvoiceTracks() {
  const int sample_rate_hz = 44100;
  const std::vector<std::int16_t> input = BuildVoiceInputPcm(sample_rate_hz);
  bag_voice_fx_config config{};
  config.sample_rate_hz = sample_rate_hz;
  config.enable_diagnostics = 1;
  config.preset = BAG_VOICE_FX_RAW_CONSTANT;
  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
  config.reserved = 0;

  bag_voice_fx_result raw{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &raw), BAG_OK,
      "Raw Constant should process valid PCM.");
  AssertVoiceTrackLooksValid(raw.final_mix, input.size(), "Raw Constant final mix");
  AssertVoiceTrackLooksValid(raw.main_voice, input.size(), "Raw Constant main voice");
  AssertVoiceTrackLooksValid(raw.subvoice, input.size(), "Raw Constant subvoice");
  test::AssertEq(raw.signal_overlay.sample_count, std::size_t{0},
                 "Raw Constant should not return a signal overlay track.");
  AssertPcmResultsEqual(
      raw.main_voice,
      bag_pcm16_result{
          .samples = const_cast<std::int16_t*>(input.data()),
          .sample_count = input.size(),
      },
      "Raw Constant main voice should preserve the dry input.");
  test::AssertTrue(HasNonZeroSamples(raw.subvoice),
                   "Raw Constant subvoice should contain the parallel flash track.");
  test::AssertTrue(DiffersFromInput(raw.final_mix, input),
                   "Raw Constant final mix should differ from dry input after layering.");

  bag_voice_fx_config no_diagnostics = config;
  no_diagnostics.enable_diagnostics = 0;
  bag_voice_fx_result no_debug{};
  test::AssertEq(
      bag_apply_voice_fx(&no_diagnostics, input.data(), input.size(), &no_debug),
      BAG_OK, "Raw Constant without diagnostics should still succeed.");
  AssertVoiceTrackLooksValid(no_debug.final_mix, input.size(),
                             "Raw Constant final mix without diagnostics");
  test::AssertEq(no_debug.main_voice.sample_count, std::size_t{0},
                 "Raw Constant should omit diagnostics main track when disabled.");
  test::AssertEq(no_debug.subvoice.sample_count, std::size_t{0},
                 "Raw Constant should omit diagnostics subvoice when disabled.");
  test::AssertEq(no_debug.signal_overlay.sample_count, std::size_t{0},
                 "Raw Constant should omit signal overlay when diagnostics are disabled.");

  bag_free_voice_fx_result(&raw);
  bag_free_voice_fx_result(&no_debug);
}

void TestApiVoiceFxVoiceTriggerReturnsDryPlusGatedSubvoiceTracks() {
  const int sample_rate_hz = 44100;
  const std::vector<std::int16_t> input =
      BuildEventfulVoiceInputPcm(sample_rate_hz);

  bag_voice_fx_config trigger_config{};
  trigger_config.sample_rate_hz = sample_rate_hz;
  trigger_config.enable_diagnostics = 1;
  trigger_config.preset = BAG_VOICE_FX_VOICE_TRIGGER;
  trigger_config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
  trigger_config.reserved = 0;

  bag_voice_fx_result trigger{};
  test::AssertEq(
      bag_apply_voice_fx(&trigger_config, input.data(), input.size(), &trigger),
      BAG_OK,
      "Voice Trigger should process valid PCM.");
  AssertVoiceTrackLooksValid(trigger.final_mix, input.size(),
                             "Voice Trigger final mix");
  AssertVoiceTrackLooksValid(trigger.main_voice, input.size(),
                             "Voice Trigger main voice");
  AssertVoiceTrackLooksValid(trigger.subvoice, input.size(),
                             "Voice Trigger subvoice");
  AssertPcmResultsEqual(
      trigger.main_voice,
      bag_pcm16_result{
          .samples = const_cast<std::int16_t*>(input.data()),
          .sample_count = input.size(),
      },
      "Voice Trigger main voice should preserve the dry input.");
  test::AssertTrue(HasNonZeroSamples(trigger.subvoice),
                   "Voice Trigger subvoice should open on voiced input.");
  test::AssertTrue(DiffersFromInput(trigger.final_mix, input),
                   "Voice Trigger final mix should differ from dry input.");
  test::AssertEq(trigger.signal_overlay.sample_count, std::size_t{0},
                 "Voice Trigger should not return a signal overlay track.");

  bag_voice_fx_config constant_config = trigger_config;
  constant_config.preset = BAG_VOICE_FX_RAW_CONSTANT;
  bag_voice_fx_result constant{};
  test::AssertEq(
      bag_apply_voice_fx(
          &constant_config,
          input.data(),
          input.size(),
          &constant),
      BAG_OK,
      "Raw Constant should still succeed for Voice Trigger comparison.");
  test::AssertTrue(
      PeakAbs(trigger.subvoice) < PeakAbs(constant.subvoice),
      "Voice Trigger subvoice should be gated below Raw Constant subvoice peak.");

  bag_free_voice_fx_result(&trigger);
  bag_free_voice_fx_result(&constant);
}

void TestApiVoiceFxSignalCantReturnsOverlayTrack() {
  const int sample_rate_hz = 44100;
  const std::vector<std::int16_t> input =
      BuildEventfulVoiceInputPcm(sample_rate_hz);
  bag_voice_fx_config config{};
  config.sample_rate_hz = sample_rate_hz;
  config.enable_diagnostics = 1;
  config.preset = BAG_VOICE_FX_SIGNAL_CANT;
  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
  config.reserved = 0;

  bag_voice_fx_result signal_cant{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &signal_cant),
      BAG_OK, "Signal Cant should process valid PCM.");
  AssertVoiceTrackLooksValid(signal_cant.final_mix, input.size(),
                             "Signal Cant final mix");
  AssertVoiceTrackLooksValid(signal_cant.main_voice, input.size(),
                             "Signal Cant main voice");
  AssertVoiceTrackLooksValid(signal_cant.signal_overlay, input.size(),
                             "Signal Cant overlay");
  test::AssertEq(signal_cant.subvoice.sample_count, std::size_t{0},
                 "Signal Cant should not return a Binaric Cant-style subvoice track.");
  test::AssertTrue(HasNonZeroSamples(signal_cant.signal_overlay),
                   "Signal Cant overlay should contain inserted machine-language bursts.");
  test::AssertTrue(
      DiffersBetweenTracks(signal_cant.main_voice, signal_cant.signal_overlay),
      "Signal Cant overlay should differ from the main voice track.");

  bag_voice_fx_config machine_config = config;
  machine_config.preset = BAG_VOICE_FX_MACHINE_VOICE;
  bag_voice_fx_result machine{};
  test::AssertEq(
      bag_apply_voice_fx(&machine_config, input.data(), input.size(), &machine),
      BAG_OK, "Machine Voice should succeed for comparison.");
  test::AssertTrue(
      DiffersBetweenTracks(signal_cant.final_mix, machine.final_mix),
      "Signal Cant should not collapse to Machine Voice.");

  bag_voice_fx_config no_diagnostics = config;
  no_diagnostics.enable_diagnostics = 0;
  bag_voice_fx_result no_debug{};
  test::AssertEq(
      bag_apply_voice_fx(&no_diagnostics, input.data(), input.size(), &no_debug),
      BAG_OK, "Signal Cant without diagnostics should still succeed.");
  AssertVoiceTrackLooksValid(no_debug.final_mix, input.size(),
                             "Signal Cant final mix without diagnostics");
  test::AssertEq(no_debug.main_voice.sample_count, std::size_t{0},
                 "Signal Cant should omit diagnostics main track when disabled.");
  test::AssertEq(no_debug.subvoice.sample_count, std::size_t{0},
                 "Signal Cant should omit subvoice diagnostics when disabled.");
  test::AssertEq(no_debug.signal_overlay.sample_count, std::size_t{0},
                 "Signal Cant should omit overlay diagnostics when disabled.");

  bag_free_voice_fx_result(&signal_cant);
  bag_free_voice_fx_result(&machine);
  bag_free_voice_fx_result(&no_debug);
}

void TestApiVoiceFxRobotVoxDiagnosticsContract() {
  const int sample_rate_hz = 44100;
  const std::vector<std::int16_t> input = BuildVoiceInputPcm(sample_rate_hz);
  bag_voice_fx_config config{};
  config.sample_rate_hz = sample_rate_hz;
  config.enable_diagnostics = 1;
  config.preset = BAG_VOICE_FX_ROBOT_VOX;
  config.subvoice_style = BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
  config.reserved = 0;

  bag_voice_fx_result robot{};
  test::AssertEq(
      bag_apply_voice_fx(&config, input.data(), input.size(), &robot), BAG_OK,
      "Robot Vox should process valid PCM.");
  AssertVoiceTrackLooksValid(robot.final_mix, input.size(),
                             "Robot Vox final mix");
  AssertVoiceTrackLooksValid(robot.main_voice, input.size(),
                             "Robot Vox diagnostics main track");
  test::AssertEq(robot.subvoice.sample_count, std::size_t{0},
                 "Robot Vox should not return a subvoice track.");
  test::AssertEq(robot.signal_overlay.sample_count, std::size_t{0},
                 "Robot Vox should not return a signal overlay track.");
  test::AssertTrue(DiffersFromInput(robot.final_mix, input),
                   "Robot Vox final mix should differ from dry input.");

  bag_voice_fx_config machine_config = config;
  machine_config.preset = BAG_VOICE_FX_MACHINE_VOICE;
  bag_voice_fx_result machine{};
  test::AssertEq(
      bag_apply_voice_fx(&machine_config, input.data(), input.size(), &machine),
      BAG_OK, "Machine Voice should succeed for comparison.");
  test::AssertTrue(DiffersBetweenTracks(robot.final_mix, machine.final_mix),
                   "Robot Vox should not collapse to Machine Voice.");

  bag_free_voice_fx_result(&robot);
  bag_free_voice_fx_result(&machine);
}

}  // namespace

namespace api_tests {

void RegisterApiVoiceFxTests(test::Runner& runner) {
  runner.Add("Api.VoiceFxRejectsInvalidArguments",
             TestApiVoiceFxRejectsInvalidArguments);
  runner.Add("Api.VoiceFxProcessorStreamsBlockOutput",
             TestApiVoiceFxProcessorStreamsBlockOutput);
  runner.Add("Api.VoiceFxMachineVoiceDiagnosticsContract",
             TestApiVoiceFxMachineVoiceDiagnosticsContract);
  runner.Add("Api.VoiceFxBinaricCantReturnsDualLayerTracks",
             TestApiVoiceFxBinaricCantReturnsDualLayerTracks);
  runner.Add("Api.VoiceFxBinaricCantSupportsSelectedSubvoiceStyles",
             TestApiVoiceFxBinaricCantSupportsSelectedSubvoiceStyles);
  runner.Add("Api.VoiceFxRawConstantReturnsDryPlusSubvoiceTracks",
             TestApiVoiceFxRawConstantReturnsDryPlusSubvoiceTracks);
  runner.Add("Api.VoiceFxVoiceTriggerReturnsDryPlusGatedSubvoiceTracks",
             TestApiVoiceFxVoiceTriggerReturnsDryPlusGatedSubvoiceTracks);
  runner.Add("Api.VoiceFxSignalCantReturnsOverlayTrack",
             TestApiVoiceFxSignalCantReturnsOverlayTrack);
  runner.Add("Api.VoiceFxRobotVoxDiagnosticsContract",
             TestApiVoiceFxRobotVoxDiagnosticsContract);
}

}  // namespace api_tests
