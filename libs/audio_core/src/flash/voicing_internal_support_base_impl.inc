constexpr double kPi = 3.14159265358979323846;
constexpr double kPcmScale = 32767.0;
constexpr double kMaxAttackReleaseRatio = 0.18;
constexpr double kMaxAttackReleaseCombinedRatio = 0.30;
constexpr double kMaxSecondHarmonicGain = 0.12;
constexpr double kMaxThirdHarmonicGain = 0.08;
constexpr double kMaxBoundaryClickGain = 0.05;
constexpr double kMaxAdditiveGainBudget = 0.18;
constexpr std::size_t kRhythmBeatCount = 16;
constexpr std::size_t kRhythmGroupSize = 4;
constexpr double kNibbleBoundaryAccentFactor = 0.24;
constexpr double kSpectralBandMix = 0.14;
constexpr double kSpectralSoftClipMix = 0.10;
constexpr double kSpectralSoftClipDrive = 1.18;
constexpr double kMetallicLayerGain = 0.022;
constexpr double kTremoloDepth = 0.045;
constexpr double kTremoloFreqHz = 7.5;
constexpr double kInvocationSweepGain = 0.14;
constexpr double kInvocationPulseGain = 0.18;
constexpr double kClosurePulseGain = 0.20;
constexpr double kClosureTailGain = 0.11;
constexpr double kLitanyPreambleSeconds = 1.35;
constexpr double kLitanyEpilogueSeconds = 1.15;

float LerpProgress(float begin, float end, float ratio) {
  return begin + (end - begin) * std::clamp(ratio, 0.0f, 1.0f);
}

void ThrowIfEncodeCancelled(const EncodeProgressSink* progress_sink) {
  if (ShouldCancelEncode(progress_sink)) {
    throw EncodeCancelled{};
  }
}

constexpr std::size_t kProgressReportStrideSamples = 256;

void ReportCompletedSampleProgress(const EncodeProgressSink* progress_sink,
                                   EncodeProgressPhase phase,
                                   float progress_begin, float progress_end,
                                   std::size_t completed_samples,
                                   std::size_t total_samples) {
  const float ratio =
      total_samples == 0
          ? 1.0f
          : static_cast<float>(std::min(completed_samples, total_samples)) /
                static_cast<float>(total_samples);
  ReportEncodeProgress(progress_sink, phase,
                       LerpProgress(progress_begin, progress_end, ratio));
}

void MaybeReportCompletedSampleProgress(
    const EncodeProgressSink* progress_sink, EncodeProgressPhase phase,
    float progress_begin, float progress_end, std::size_t completed_samples,
    std::size_t total_samples, std::size_t* last_reported_samples,
    bool force = false) {
  if (last_reported_samples == nullptr) {
    return;
  }

  const std::size_t clamped_completed_samples =
      total_samples == 0 ? 0 : std::min(completed_samples, total_samples);
  if (!force && clamped_completed_samples < total_samples &&
      clamped_completed_samples - *last_reported_samples <
          kProgressReportStrideSamples) {
    return;
  }

  ReportCompletedSampleProgress(progress_sink, phase, progress_begin,
                                progress_end, clamped_completed_samples,
                                total_samples);
  *last_reported_samples = clamped_completed_samples;
}

struct SanitizedVoicingConfig {
  int sample_rate_hz = 0;
  double attack_ratio = 0.0;
  double release_ratio = 0.0;
  double second_harmonic_gain = 0.0;
  double third_harmonic_gain = 0.0;
  double boundary_click_gain = 0.0;
};

enum class FixedShellCadenceProfile {
  kProtocolHandshake = 0,
  kStandardProtocol = 3,
  kHostilityChallenge = 4,
  kCollapseFailure = 5,
  kLitanyInvocation = 6,
  kZealRising = 7,
  kVoidDescent = 8,
};

enum class FixedShellTimbreProfile {
  kInvocationClosure = 0,
  kProtocolBoundary = 1,
  kStandardBoundary = 2,
  kHostilityEdge = 3,
  kCollapseFailure = 4,
  kLitanyInvocation = 5,
  kZealRising = 6,
  kVoidDescent = 7,
};

struct CadenceWindow {
  double begin = 0.0;
  double end = 0.0;
};

struct VoicingStyleTraits {
  double nibble_boundary_accent_factor = 0.0;
  double byte_boundary_click_scale = 1.0;
  double preamble_grouped_burst_mix = 1.0;
  double preamble_invocation_shell_mix = 1.0;
  double epilogue_grouped_burst_mix = 1.0;
  double epilogue_closure_shell_mix = 1.0;
  double preamble_rise_ratio = 0.12;
  double preamble_fall_ratio = 0.18;
  double epilogue_rise_ratio = 0.08;
  double epilogue_fall_ratio = 0.26;
  double epilogue_decay_exponent = 2.0;
  double payload_release_scale = 1.0;
  double payload_envelope_floor = 0.0;
  double metallic_layer_gain_scale = 1.0;
  double chant_resonance_gain = 0.0;
  double chant_drone_gain = 0.0;
  double chant_swell_depth = 0.0;
  double chant_swell_freq_hz = 0.65;
  double mechanical_throat_gain = 0.0;
  double litany_phrase_dip_depth = 0.0;
  double litany_phrase_tail_begin = 1.0;
  double litany_text_pause_tail_begin = 1.0;
  double standard_low_voice_gain = 0.0;
  double standard_low_voice_freq_hz = 120.0;
  double hostility_edge_gain = 0.0;
  double collapse_tremor_depth = 0.0;
  double collapse_hesitation_depth = 0.0;
  double collapse_hesitation_density = 0.0;
  double collapse_hesitation_tail_begin = 1.0;
  double payload_softclip_drive = 1.0;
  double payload_softclip_mix = 0.0;
  double tremolo_depth_scale = 1.0;
  double tremolo_freq_hz = kTremoloFreqHz;
  double spectral_band_mix_scale = 1.0;
  double spectral_softclip_mix_scale = 1.0;
  FixedShellCadenceProfile preamble_cadence_profile =
      FixedShellCadenceProfile::kProtocolHandshake;
  FixedShellCadenceProfile epilogue_cadence_profile =
      FixedShellCadenceProfile::kProtocolHandshake;
  FixedShellTimbreProfile preamble_timbre_profile =
      FixedShellTimbreProfile::kInvocationClosure;
  FixedShellTimbreProfile epilogue_timbre_profile =
      FixedShellTimbreProfile::kInvocationClosure;
  double preamble_phrase_mask_mix = 0.0;
  double epilogue_phrase_mask_mix = 0.0;
  double preamble_terminal_mute_begin = 1.0;
  double preamble_terminal_mute_end = 1.0;
  double epilogue_terminal_mute_begin = 1.0;
  double epilogue_terminal_mute_end = 1.0;
};

FlashVoicingFlavor NormalizeVoicingFlavor(FlashVoicingFlavor flavor) {
  switch (flavor) {
    case FlashVoicingFlavor::kStandard:
    case FlashVoicingFlavor::kLitany:
    case FlashVoicingFlavor::kHostility:
    case FlashVoicingFlavor::kCollapse:
    case FlashVoicingFlavor::kZeal:
    case FlashVoicingFlavor::kVoid:
      return flavor;
    default:
      return FlashVoicingFlavor::kStandard;
  }
}

std::size_t LeadingNonpayloadSamples(const FlashVoicingConfig& config) {
  return config.enable_preamble ? config.preamble_sample_count
                                : static_cast<std::size_t>(0);
}

std::size_t TrailingNonpayloadSamples(const FlashVoicingConfig& config) {
  return config.enable_epilogue ? config.epilogue_sample_count
                                : static_cast<std::size_t>(0);
}

std::size_t SecondsToSampleCount(int sample_rate_hz, double seconds) {
  return sample_rate_hz > 0 && seconds > 0.0
             ? static_cast<std::size_t>(
                   std::lround(static_cast<double>(sample_rate_hz) * seconds))
             : static_cast<std::size_t>(0);
}

void ValidatePayloadLayout(const std::vector<int16_t>& clean_payload_pcm,
                           const FlashPayloadLayout& payload_layout) {
  if (payload_layout.payload_sample_count != clean_payload_pcm.size()) {
    throw std::invalid_argument("Payload layout sample count mismatch.");
  }

  size_t expected_offset = 0;
  for (const FlashPayloadChunk& chunk : payload_layout.chunks) {
    if (chunk.sample_offset != expected_offset) {
      throw std::invalid_argument("Payload layout offsets must be contiguous.");
    }
    if (chunk.sample_offset + chunk.sample_count >
        payload_layout.payload_sample_count) {
      throw std::invalid_argument("Payload chunk exceeds payload bounds.");
    }
    expected_offset += chunk.sample_count;
  }

  if (expected_offset != payload_layout.payload_sample_count) {
    throw std::invalid_argument("Payload layout must cover the full payload.");
  }
}

SanitizedVoicingConfig SanitizeVoicingConfig(const FlashVoicingConfig& config) {
  SanitizedVoicingConfig sanitized{};
  sanitized.sample_rate_hz = std::max(config.sample_rate_hz, 0);
  sanitized.attack_ratio =
      std::clamp(config.attack_ratio, 0.0, kMaxAttackReleaseRatio);
  sanitized.release_ratio =
      std::clamp(config.release_ratio, 0.0, kMaxAttackReleaseRatio);

  const double combined_attack_release =
      sanitized.attack_ratio + sanitized.release_ratio;
  if (combined_attack_release > kMaxAttackReleaseCombinedRatio &&
      combined_attack_release > 0.0) {
    const double scale =
        kMaxAttackReleaseCombinedRatio / combined_attack_release;
    sanitized.attack_ratio *= scale;
    sanitized.release_ratio *= scale;
  }

  sanitized.second_harmonic_gain =
      std::clamp(config.second_harmonic_gain, 0.0, kMaxSecondHarmonicGain);
  sanitized.third_harmonic_gain =
      std::clamp(config.third_harmonic_gain, 0.0, kMaxThirdHarmonicGain);
  sanitized.boundary_click_gain =
      std::clamp(config.boundary_click_gain, 0.0, kMaxBoundaryClickGain);

  const double additive_gain_sum = sanitized.second_harmonic_gain +
                                   sanitized.third_harmonic_gain +
                                   sanitized.boundary_click_gain;
  if (additive_gain_sum > kMaxAdditiveGainBudget && additive_gain_sum > 0.0) {
    const double scale = kMaxAdditiveGainBudget / additive_gain_sum;
    sanitized.second_harmonic_gain *= scale;
    sanitized.third_harmonic_gain *= scale;
    sanitized.boundary_click_gain *= scale;
  }

  return sanitized;
