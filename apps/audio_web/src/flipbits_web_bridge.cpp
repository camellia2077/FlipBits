#include "bag_api.h"

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#include <algorithm>
#endif

import bag.transport.facade;

#include <cstdint>
#include <vector>

#include <emscripten/emscripten.h>

namespace {

constexpr int kDefaultSampleRateHz = 44100;
constexpr int kDefaultFrameSamples = kDefaultSampleRateHz / 20;

std::vector<std::int16_t> g_last_samples;
std::string g_last_error;
int g_last_sample_rate_hz = kDefaultSampleRateHz;

EM_JS(void, flipbits_web_report_progress_js, (int phase, float progress), {
  if (typeof globalThis.flipbitsOnEncodeProgress === "function") {
    globalThis.flipbitsOnEncodeProgress(phase, progress);
  }
});

void ResetLastResult() {
  g_last_samples.clear();
  g_last_error.clear();
  g_last_sample_rate_hz = kDefaultSampleRateHz;
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
                                     int sample_rate_hz, int frame_samples) {
  bag_encoder_config config{};
  config.sample_rate_hz = NormalizeSampleRate(sample_rate_hz);
  config.frame_samples = NormalizeFrameSamples(sample_rate_hz, frame_samples);
  config.enable_diagnostics = 0;
  config.mode = static_cast<bag_transport_mode>(mode);
  config.flash_signal_profile = ToFlashSignalProfile(flash_style);
  config.flash_voicing_flavor = ToFlashVoicingFlavor(flash_style);
  config.reserved = 0;
  return config;
}

bag::TransportMode ToCoreMode(int mode) {
  switch (static_cast<bag_transport_mode>(mode)) {
    case BAG_TRANSPORT_MINI:
      return bag::TransportMode::kMini;
    case BAG_TRANSPORT_FLASH:
      return bag::TransportMode::kFlash;
    case BAG_TRANSPORT_PRO:
      return bag::TransportMode::kPro;
    case BAG_TRANSPORT_ULTRA:
      return bag::TransportMode::kUltra;
    default:
      return static_cast<bag::TransportMode>(-1);
  }
}

bag::FlashSignalProfile ToCoreFlashSignalProfile(int flash_style) {
  switch (ToFlashSignalProfile(flash_style)) {
    case BAG_FLASH_SIGNAL_PROFILE_STANDARD:
      return bag::FlashSignalProfile::kStandard;
    case BAG_FLASH_SIGNAL_PROFILE_LITANY:
      return bag::FlashSignalProfile::kLitany;
    case BAG_FLASH_SIGNAL_PROFILE_HOSTILITY:
      return bag::FlashSignalProfile::kHostility;
    case BAG_FLASH_SIGNAL_PROFILE_COLLAPSE:
      return bag::FlashSignalProfile::kCollapse;
    case BAG_FLASH_SIGNAL_PROFILE_ZEAL:
      return bag::FlashSignalProfile::kZeal;
    case BAG_FLASH_SIGNAL_PROFILE_VOID:
      return bag::FlashSignalProfile::kVoid;
    default:
      return static_cast<bag::FlashSignalProfile>(-1);
  }
}

bag::FlashVoicingFlavor ToCoreFlashVoicingFlavor(int flash_style) {
  switch (ToFlashVoicingFlavor(flash_style)) {
    case BAG_FLASH_VOICING_FLAVOR_STANDARD:
      return bag::FlashVoicingFlavor::kStandard;
    case BAG_FLASH_VOICING_FLAVOR_LITANY:
      return bag::FlashVoicingFlavor::kLitany;
    case BAG_FLASH_VOICING_FLAVOR_HOSTILITY:
      return bag::FlashVoicingFlavor::kHostility;
    case BAG_FLASH_VOICING_FLAVOR_COLLAPSE:
      return bag::FlashVoicingFlavor::kCollapse;
    case BAG_FLASH_VOICING_FLAVOR_ZEAL:
      return bag::FlashVoicingFlavor::kZeal;
    case BAG_FLASH_VOICING_FLAVOR_VOID:
      return bag::FlashVoicingFlavor::kVoid;
    default:
      return static_cast<bag::FlashVoicingFlavor>(-1);
  }
}

bag::CoreConfig MakeCoreConfig(int mode, int flash_style, int sample_rate_hz,
                               int frame_samples) {
  bag::CoreConfig config{};
  config.sample_rate_hz = NormalizeSampleRate(sample_rate_hz);
  config.frame_samples = NormalizeFrameSamples(sample_rate_hz, frame_samples);
  config.enable_diagnostics = false;
  config.mode = ToCoreMode(mode);
  config.flash_signal_profile = ToCoreFlashSignalProfile(flash_style);
  config.flash_voicing_flavor = ToCoreFlashVoicingFlavor(flash_style);
  config.reserved = 0;
  return config;
}

void OnEncodeProgress(void*, bag::EncodeProgressPhase phase,
                      float progress_0_to_1) {
  flipbits_web_report_progress_js(
      static_cast<int>(phase),
      std::clamp(progress_0_to_1, 0.0f, 1.0f));
}

void SetValidationError(bag_validation_issue issue) {
  const char* message = bag_validation_issue_message(issue);
  g_last_error = message != nullptr ? message : "Validation failed.";
}

void SetEncodeError(bag_error_code code) {
  const char* message = bag_error_code_message(code);
  g_last_error = message != nullptr ? message : "Encoding failed.";
}

}  // namespace

extern "C" {

EMSCRIPTEN_KEEPALIVE int flipbits_web_encode_text(const char* text, int mode,
                                                  int flash_style,
                                                  int sample_rate_hz,
                                                  int frame_samples) {
  ResetLastResult();

  if (text == nullptr) {
    g_last_error = "Text is required.";
    return 0;
  }

  const bag_encoder_config config =
      MakeEncoderConfig(mode, flash_style, sample_rate_hz, frame_samples);
  const bag_validation_issue validation =
      bag_validate_encode_request(&config, text);
  if (validation != BAG_VALIDATION_OK) {
    SetValidationError(validation);
    return 0;
  }

  const bag::CoreConfig core_config =
      MakeCoreConfig(mode, flash_style, sample_rate_hz, frame_samples);
  std::vector<std::int16_t> pcm_samples;
  const bag::EncodeProgressSink progress_sink{
      .user_data = nullptr,
      .on_progress = &OnEncodeProgress,
      .should_cancel = nullptr,
  };
  const bag::ErrorCode encode_code = bag::EncodeTextToPcm16(
      core_config, std::string(text), &pcm_samples, &progress_sink);
  if (encode_code != bag::ErrorCode::kOk) {
    SetEncodeError(static_cast<bag_error_code>(encode_code));
    return 0;
  }

  g_last_sample_rate_hz = core_config.sample_rate_hz;
  g_last_samples = std::move(pcm_samples);
  return 1;
}

EMSCRIPTEN_KEEPALIVE const char* flipbits_web_last_error_message() {
  return g_last_error.c_str();
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
