#include "bag_api.h"

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

#include <cstdint>
#include <vector>

#include <emscripten/emscripten.h>

namespace {

constexpr int kDefaultSampleRateHz = 44100;
constexpr int kDefaultFrameSamples = kDefaultSampleRateHz / 20;

std::vector<std::int16_t> g_last_samples;
std::string g_last_error;
int g_last_sample_rate_hz = kDefaultSampleRateHz;
bag_encode_operation* g_current_operation = nullptr;
bag_encode_operation_progress g_current_progress{};
bag_encode_operation_work_plan g_current_work_plan{};
bag_encode_operation_diagnostics g_current_diagnostics{};
bag_encode_operation_diagnostics g_last_diagnostics{};
bag_voice_fx_processor* g_current_voice_fx_processor = nullptr;

void ResetLastResult() {
  g_last_samples.clear();
  g_last_error.clear();
  g_last_sample_rate_hz = kDefaultSampleRateHz;
  g_last_diagnostics = {};
}

const bag_encode_operation_diagnostics& ActiveDiagnostics() {
  return g_current_operation != nullptr ? g_current_diagnostics
                                        : g_last_diagnostics;
}

void DestroyCurrentOperation() {
  if (g_current_operation != nullptr) {
    bag_destroy_encode_operation(g_current_operation);
    g_current_operation = nullptr;
  }
  g_current_progress = {};
  g_current_work_plan = {};
  g_current_diagnostics = {};
}

void DestroyCurrentVoiceFxProcessor() {
  if (g_current_voice_fx_processor != nullptr) {
    bag_destroy_voice_fx_processor(g_current_voice_fx_processor);
    g_current_voice_fx_processor = nullptr;
  }
}

int NormalizeSampleRate(int sample_rate_hz) {
  return sample_rate_hz > 0 ? sample_rate_hz : kDefaultSampleRateHz;
}

int NormalizeFrameSamples(int sample_rate_hz, int frame_samples) {
  if (frame_samples > 0) {
    return frame_samples;
  }

  const int normalized_sample_rate = NormalizeSampleRate(sample_rate_hz);
  return normalized_sample_rate > 0 ? normalized_sample_rate / 20
                                    : kDefaultFrameSamples;
}

bag_flash_signal_profile ToFlashSignalProfile(int flash_style) {
  switch (flash_style) {
    case BAG_FLASH_SIGNAL_PROFILE_STANDARD:
      return BAG_FLASH_SIGNAL_PROFILE_STANDARD;
    case BAG_FLASH_SIGNAL_PROFILE_LITANY:
      return BAG_FLASH_SIGNAL_PROFILE_LITANY;
    case BAG_FLASH_SIGNAL_PROFILE_HOSTILITY:
      return BAG_FLASH_SIGNAL_PROFILE_HOSTILITY;
    case BAG_FLASH_SIGNAL_PROFILE_COLLAPSE:
      return BAG_FLASH_SIGNAL_PROFILE_COLLAPSE;
    case BAG_FLASH_SIGNAL_PROFILE_ZEAL:
      return BAG_FLASH_SIGNAL_PROFILE_ZEAL;
    case BAG_FLASH_SIGNAL_PROFILE_VOID:
      return BAG_FLASH_SIGNAL_PROFILE_VOID;
    default:
      return static_cast<bag_flash_signal_profile>(-1);
  }
}

bag_flash_voicing_flavor ToFlashVoicingFlavor(int flash_style) {
  switch (flash_style) {
    case BAG_FLASH_VOICING_FLAVOR_STANDARD:
      return BAG_FLASH_VOICING_FLAVOR_STANDARD;
    case BAG_FLASH_VOICING_FLAVOR_LITANY:
      return BAG_FLASH_VOICING_FLAVOR_LITANY;
    case BAG_FLASH_VOICING_FLAVOR_HOSTILITY:
      return BAG_FLASH_VOICING_FLAVOR_HOSTILITY;
    case BAG_FLASH_VOICING_FLAVOR_COLLAPSE:
      return BAG_FLASH_VOICING_FLAVOR_COLLAPSE;
    case BAG_FLASH_VOICING_FLAVOR_ZEAL:
      return BAG_FLASH_VOICING_FLAVOR_ZEAL;
    case BAG_FLASH_VOICING_FLAVOR_VOID:
      return BAG_FLASH_VOICING_FLAVOR_VOID;
    default:
      return static_cast<bag_flash_voicing_flavor>(-1);
  }
}

bag_encoder_config MakeEncoderConfig(int mode, int flash_style,
                                     int sample_rate_hz, int frame_samples,
                                     int enable_diagnostics) {
  bag_encoder_config config{};
  config.sample_rate_hz = NormalizeSampleRate(sample_rate_hz);
  config.frame_samples = NormalizeFrameSamples(sample_rate_hz, frame_samples);
  config.enable_diagnostics = enable_diagnostics != 0 ? 1 : 0;
  config.mode = static_cast<bag_transport_mode>(mode);
  config.flash_signal_profile = ToFlashSignalProfile(flash_style);
  config.flash_voicing_flavor = ToFlashVoicingFlavor(flash_style);
  config.reserved = 0;
  return config;
}

void SetValidationError(bag_validation_issue issue) {
  const char* message = bag_validation_issue_message(issue);
  g_last_error = message != nullptr ? message : "Validation failed.";
}

void SetEncodeError(bag_error_code code) {
  const char* message = bag_error_code_message(code);
  g_last_error = message != nullptr ? message : "Encoding failed.";
}

bag_voice_fx_preset ToVoiceFxPreset(int preset) {
  switch (preset) {
    case BAG_VOICE_FX_MACHINE_VOICE:
      return BAG_VOICE_FX_MACHINE_VOICE;
    case BAG_VOICE_FX_BINARIC_CANT:
      return BAG_VOICE_FX_BINARIC_CANT;
    case BAG_VOICE_FX_SIGNAL_CANT:
      return BAG_VOICE_FX_SIGNAL_CANT;
    case BAG_VOICE_FX_ROBOT_VOX:
      return BAG_VOICE_FX_ROBOT_VOX;
    case BAG_VOICE_FX_RAW_CONSTANT:
      return BAG_VOICE_FX_RAW_CONSTANT;
    case BAG_VOICE_FX_VOICE_TRIGGER:
      return BAG_VOICE_FX_VOICE_TRIGGER;
    default:
      return static_cast<bag_voice_fx_preset>(-1);
  }
}

bag_voice_fx_subvoice_style ToVoiceFxSubvoiceStyle(int subvoice_style) {
  switch (subvoice_style) {
    case BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD:
      return BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD;
    case BAG_VOICE_FX_SUBVOICE_STYLE_LITANY:
      return BAG_VOICE_FX_SUBVOICE_STYLE_LITANY;
    case BAG_VOICE_FX_SUBVOICE_STYLE_HOSTILITY:
      return BAG_VOICE_FX_SUBVOICE_STYLE_HOSTILITY;
    case BAG_VOICE_FX_SUBVOICE_STYLE_COLLAPSE:
      return BAG_VOICE_FX_SUBVOICE_STYLE_COLLAPSE;
    case BAG_VOICE_FX_SUBVOICE_STYLE_ZEAL:
      return BAG_VOICE_FX_SUBVOICE_STYLE_ZEAL;
    case BAG_VOICE_FX_SUBVOICE_STYLE_VOID:
      return BAG_VOICE_FX_SUBVOICE_STYLE_VOID;
    default:
      return static_cast<bag_voice_fx_subvoice_style>(-1);
  }
}

bag_voice_fx_config MakeVoiceFxConfig(int preset, int subvoice_style,
                                      int sample_rate_hz) {
  bag_voice_fx_config config{};
  config.sample_rate_hz = NormalizeSampleRate(sample_rate_hz);
  config.enable_diagnostics = 0;
  config.preset = ToVoiceFxPreset(preset);
  config.subvoice_style = ToVoiceFxSubvoiceStyle(subvoice_style);
  config.reserved = 0;
  return config;
}

std::vector<std::int16_t> PcmBytesToSamples(const unsigned char* pcm_bytes,
                                            int byte_count) {
  std::vector<std::int16_t> samples;
  if (pcm_bytes == nullptr || byte_count <= 0) {
    return samples;
  }

  samples.reserve(static_cast<std::size_t>(byte_count / 2));
  for (int index = 0; index + 1 < byte_count; index += 2) {
    const auto low = static_cast<std::uint16_t>(pcm_bytes[index]);
    const auto high = static_cast<std::uint16_t>(pcm_bytes[index + 1]) << 8;
    samples.push_back(static_cast<std::int16_t>(low | high));
  }
  return samples;
}

void AppendVoiceFxResult(const bag_voice_fx_result& result) {
  if (result.final_mix.samples == nullptr || result.final_mix.sample_count == 0) {
    return;
  }
  g_last_samples.insert(
      g_last_samples.end(), result.final_mix.samples,
      result.final_mix.samples + result.final_mix.sample_count);
}

}  // namespace

extern "C" {

EMSCRIPTEN_KEEPALIVE int flipbits_web_begin_encode_operation(
    const char* text, int mode, int flash_style, int sample_rate_hz,
    int frame_samples, int enable_diagnostics) {
  ResetLastResult();
  DestroyCurrentOperation();
  DestroyCurrentVoiceFxProcessor();

  if (text == nullptr) {
    g_last_error = "Text is required.";
    return 0;
  }

  const bag_encoder_config config =
      MakeEncoderConfig(mode, flash_style, sample_rate_hz, frame_samples,
                        enable_diagnostics);
  const bag_validation_issue validation =
      bag_validate_encode_request(&config, text);
  if (validation != BAG_VALIDATION_OK) {
    SetValidationError(validation);
    return 0;
  }

  g_last_sample_rate_hz = config.sample_rate_hz;
  if (bag_create_encode_operation(&config, text, &g_current_operation) !=
      BAG_OK) {
    g_current_operation = nullptr;
    g_last_error = "Failed to create encode operation.";
    return 0;
  }

  if (bag_get_encode_operation_work_plan(g_current_operation,
                                         &g_current_work_plan) != BAG_OK) {
    g_last_error = "Failed to query encode work plan.";
    DestroyCurrentOperation();
    return 0;
  }

  if (bag_get_encode_operation_diagnostics(g_current_operation,
                                           &g_current_diagnostics) != BAG_OK) {
    g_last_error = "Failed to query encode diagnostics.";
    DestroyCurrentOperation();
    return 0;
  }

  if (bag_poll_encode_operation(g_current_operation, &g_current_progress) !=
      BAG_OK) {
    g_last_error = "Failed to query encode progress snapshot.";
    DestroyCurrentOperation();
    return 0;
  }

  return 1;
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_pump_encode_operation(
    int max_work_units, int max_wall_time_ms) {
  if (g_current_operation == nullptr) {
    return static_cast<int>(BAG_NOT_READY);
  }

  int did_progress = 0;
  const bag_encode_operation_pump_budget budget{
      .max_work_units =
          max_work_units > 0 ? static_cast<std::uint64_t>(max_work_units) : 0ULL,
      .max_wall_time_ms =
          max_wall_time_ms > 0 ? static_cast<std::uint32_t>(max_wall_time_ms)
                               : 0U,
  };
  const bag_error_code pump_code =
      bag_pump_encode_operation(g_current_operation, budget, &did_progress);
  if (pump_code != BAG_OK) {
    SetEncodeError(pump_code);
    return static_cast<int>(pump_code);
  }

  const bag_error_code poll_code =
      bag_poll_encode_operation(g_current_operation, &g_current_progress);
  if (poll_code != BAG_OK) {
    SetEncodeError(poll_code);
    return static_cast<int>(poll_code);
  }

  const bag_error_code plan_code =
      bag_get_encode_operation_work_plan(g_current_operation,
                                         &g_current_work_plan);
  if (plan_code != BAG_OK) {
    SetEncodeError(plan_code);
    return static_cast<int>(plan_code);
  }

  const bag_error_code diagnostics_code =
      bag_get_encode_operation_diagnostics(g_current_operation,
                                            &g_current_diagnostics);
  if (diagnostics_code != BAG_OK) {
    SetEncodeError(diagnostics_code);
    return static_cast<int>(diagnostics_code);
  }

  return static_cast<int>(BAG_OK);
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_take_encode_operation_result() {
  if (g_current_operation == nullptr) {
    return 0;
  }

  bag_encode_result result{};
  const bag_error_code take_code =
      bag_take_encode_operation_result(g_current_operation, &result);
  if (take_code != BAG_OK) {
    SetEncodeError(take_code);
    bag_free_encode_result(&result);
    DestroyCurrentOperation();
    return 0;
  }

  g_last_diagnostics = g_current_diagnostics;
  g_last_samples.assign(result.samples, result.samples + result.sample_count);
  bag_free_encode_result(&result);
  DestroyCurrentOperation();
  return 1;
}

EMSCRIPTEN_KEEPALIVE void flipbits_web_abort_encode_operation() {
  if (g_current_operation == nullptr) {
    return;
  }
  const bag_error_code cancel_code =
      bag_cancel_encode_operation(g_current_operation);
  if (cancel_code != BAG_OK && cancel_code != BAG_CANCELLED) {
    SetEncodeError(cancel_code);
  }
  DestroyCurrentOperation();
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_begin_voice_fx(
    int preset, int subvoice_style, int sample_rate_hz) {
  ResetLastResult();
  DestroyCurrentOperation();
  DestroyCurrentVoiceFxProcessor();

  const bag_voice_fx_config config =
      MakeVoiceFxConfig(preset, subvoice_style, sample_rate_hz);
  g_last_sample_rate_hz = config.sample_rate_hz;

  const bag_error_code create_code =
      bag_create_voice_fx_processor(&config, &g_current_voice_fx_processor);
  if (create_code != BAG_OK) {
    g_current_voice_fx_processor = nullptr;
    SetEncodeError(create_code);
    return 0;
  }

  return 1;
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_apply_voice_fx_pcm_bytes(
    int preset, int subvoice_style, int sample_rate_hz,
    const unsigned char* pcm_bytes, int byte_count) {
  ResetLastResult();
  DestroyCurrentOperation();
  DestroyCurrentVoiceFxProcessor();

  if (pcm_bytes == nullptr || byte_count <= 0 || (byte_count % 2) != 0) {
    g_last_error = "PCM16 audio input is required.";
    return 0;
  }

  const std::vector<std::int16_t> input =
      PcmBytesToSamples(pcm_bytes, byte_count);
  if (input.empty()) {
    g_last_error = "PCM16 audio input is required.";
    return 0;
  }

  const bag_voice_fx_config config =
      MakeVoiceFxConfig(preset, subvoice_style, sample_rate_hz);
  g_last_sample_rate_hz = config.sample_rate_hz;

  bag_voice_fx_result result{};
  const bag_error_code process_code =
      bag_apply_voice_fx(&config, input.data(), input.size(), &result);
  if (process_code != BAG_OK) {
    SetEncodeError(process_code);
    bag_free_voice_fx_result(&result);
    return 0;
  }

  AppendVoiceFxResult(result);
  bag_free_voice_fx_result(&result);
  return 1;
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_process_voice_fx_pcm_bytes(
    const unsigned char* pcm_bytes, int byte_count) {
  if (g_current_voice_fx_processor == nullptr) {
    g_last_error = "Voice FX processor is not ready.";
    return 0;
  }
  if (pcm_bytes == nullptr || byte_count <= 0 || (byte_count % 2) != 0) {
    g_last_error = "PCM16 audio input is required.";
    return 0;
  }

  const std::vector<std::int16_t> input =
      PcmBytesToSamples(pcm_bytes, byte_count);
  if (input.empty()) {
    g_last_error = "PCM16 audio input is required.";
    return 0;
  }

  bag_voice_fx_result result{};
  const bag_error_code process_code = bag_process_voice_fx_block(
      g_current_voice_fx_processor, input.data(), input.size(), &result);
  if (process_code != BAG_OK) {
    SetEncodeError(process_code);
    bag_free_voice_fx_result(&result);
    DestroyCurrentVoiceFxProcessor();
    return 0;
  }

  AppendVoiceFxResult(result);
  bag_free_voice_fx_result(&result);
  return 1;
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_finish_voice_fx() {
  if (g_current_voice_fx_processor == nullptr) {
    g_last_error = "Voice FX processor is not ready.";
    return 0;
  }

  bag_voice_fx_result result{};
  const bag_error_code flush_code =
      bag_flush_voice_fx_processor(g_current_voice_fx_processor, &result);
  if (flush_code != BAG_OK) {
    SetEncodeError(flush_code);
    bag_free_voice_fx_result(&result);
    DestroyCurrentVoiceFxProcessor();
    return 0;
  }

  AppendVoiceFxResult(result);
  bag_free_voice_fx_result(&result);
  DestroyCurrentVoiceFxProcessor();
  return 1;
}

EMSCRIPTEN_KEEPALIVE void flipbits_web_abort_voice_fx() {
  DestroyCurrentVoiceFxProcessor();
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_current_operation_state() {
  return static_cast<int>(g_current_progress.state);
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_current_operation_phase() {
  return static_cast<int>(g_current_progress.phase);
}

EMSCRIPTEN_KEEPALIVE double flipbits_web_current_operation_overall_progress() {
  return static_cast<double>(g_current_progress.overall_progress_0_to_1);
}

EMSCRIPTEN_KEEPALIVE double flipbits_web_current_operation_phase_progress() {
  return static_cast<double>(g_current_progress.phase_progress_0_to_1);
}

EMSCRIPTEN_KEEPALIVE double flipbits_web_current_operation_completed_work_units() {
  return static_cast<double>(g_current_progress.completed_work_units);
}

EMSCRIPTEN_KEEPALIVE double flipbits_web_current_operation_total_work_units() {
  return static_cast<double>(g_current_progress.total_work_units);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_phase_completed_work_units() {
  return static_cast<double>(g_current_progress.phase_completed_work_units);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_phase_total_work_units() {
  return static_cast<double>(g_current_progress.phase_total_work_units);
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_current_operation_terminal_code() {
  return static_cast<int>(g_current_progress.terminal_code);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_estimated_pcm_sample_count() {
  return static_cast<double>(g_current_progress.estimated_pcm_sample_count);
}

EMSCRIPTEN_KEEPALIVE double flipbits_web_current_operation_payload_byte_count() {
  return static_cast<double>(g_current_progress.payload_byte_count);
}

EMSCRIPTEN_KEEPALIVE double flipbits_web_current_operation_segment_count() {
  return static_cast<double>(g_current_progress.segment_count);
}

EMSCRIPTEN_KEEPALIVE double flipbits_web_current_operation_current_segment_index() {
  return static_cast<double>(g_current_progress.current_segment_index);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_plan_preparing_work_units() {
  return static_cast<double>(g_current_work_plan.preparing_input_work_units);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_plan_rendering_work_units() {
  return static_cast<double>(g_current_work_plan.rendering_pcm_work_units);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_plan_postprocessing_work_units() {
  return static_cast<double>(g_current_work_plan.postprocessing_work_units);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_plan_finalizing_work_units() {
  return static_cast<double>(g_current_work_plan.finalizing_work_units);
}

EMSCRIPTEN_KEEPALIVE double flipbits_web_current_operation_plan_total_work_units() {
  return static_cast<double>(g_current_work_plan.total_work_units);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_plan_estimated_pcm_sample_count() {
  return static_cast<double>(g_current_work_plan.estimated_pcm_sample_count);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_plan_payload_byte_count() {
  return static_cast<double>(g_current_work_plan.payload_byte_count);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_plan_segment_count() {
  return static_cast<double>(g_current_work_plan.segment_count);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_prepare_ms() {
  return ActiveDiagnostics().flash_payload_prepare_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_sample_setup_ms() {
  return ActiveDiagnostics().flash_payload_sample_setup_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_envelope_ms() {
  return ActiveDiagnostics().flash_payload_envelope_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_articulation_ms() {
  return ActiveDiagnostics().flash_payload_articulation_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_harmonic_ms() {
  return ActiveDiagnostics().flash_payload_harmonic_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_metallic_ms() {
  return ActiveDiagnostics().flash_payload_metallic_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_chant_resonance_ms() {
  return ActiveDiagnostics().flash_payload_chant_resonance_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_chant_drone_ms() {
  return ActiveDiagnostics().flash_payload_chant_drone_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_mechanical_throat_ms() {
  return ActiveDiagnostics().flash_payload_mechanical_throat_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_standard_low_voice_ms() {
  return ActiveDiagnostics().flash_payload_standard_low_voice_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_hostility_edge_ms() {
  return ActiveDiagnostics().flash_payload_hostility_edge_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_boundary_click_ms() {
  return ActiveDiagnostics().flash_payload_boundary_click_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_modulation_ms() {
  return ActiveDiagnostics().flash_payload_modulation_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_mix_shape_store_ms() {
  return ActiveDiagnostics().flash_payload_mix_shape_store_ms;
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_voiced_samples() {
  return static_cast<double>(ActiveDiagnostics().flash_payload_voiced_samples);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_silence_samples() {
  return static_cast<double>(ActiveDiagnostics().flash_payload_silence_samples);
}

EMSCRIPTEN_KEEPALIVE double
flipbits_web_current_operation_diagnostics_flash_payload_profiled_samples() {
  return static_cast<double>(ActiveDiagnostics().flash_payload_profiled_samples);
}

EMSCRIPTEN_KEEPALIVE const char* flipbits_web_last_error_message() {
  return g_last_error.c_str();
}

EMSCRIPTEN_KEEPALIVE const char* flipbits_web_error_message_from_code(int code) {
  const char* message =
      bag_error_code_message(static_cast<bag_error_code>(code));
  return message != nullptr ? message : "Encoding failed.";
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_last_sample_rate_hz() {
  return g_last_sample_rate_hz;
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_last_sample_count() {
  return static_cast<int>(g_last_samples.size());
}

EMSCRIPTEN_KEEPALIVE const std::int16_t* flipbits_web_last_samples_ptr() {
  return g_last_samples.empty() ? nullptr : g_last_samples.data();
}

EMSCRIPTEN_KEEPALIVE int flipbits_web_last_sample_at(int index) {
  if (index < 0 ||
      static_cast<std::size_t>(index) >= g_last_samples.size()) {
    return 0;
  }
  return static_cast<int>(g_last_samples[static_cast<std::size_t>(index)]);
}

}  // extern "C"
