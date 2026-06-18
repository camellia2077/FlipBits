namespace bag::flash {

using std::int16_t;
using std::size_t;

namespace {
#include "voicing_internal_support.inc"
#include "voicing_internal_texture.inc"
#include "voicing_internal_shell.inc"

}  // namespace

FlashVoicingConfig MakeFormalVoicingConfigForFlavor(const CoreConfig& config,
                                                    FlashVoicingFlavor flavor) {
  FlashVoicingConfig voicing{};
  voicing.sample_rate_hz = config.sample_rate_hz;
  switch (NormalizeVoicingFlavor(flavor)) {
    case FlashVoicingFlavor::kLitany:
      voicing.attack_ratio = 0.05;
      voicing.release_ratio = 0.05;
      voicing.second_harmonic_gain = 0.008;
      voicing.third_harmonic_gain = 0.001;
      voicing.boundary_click_gain = 0.002;
      voicing.enable_preamble = true;
      voicing.enable_epilogue = true;
      voicing.preamble_sample_count =
          SecondsToSampleCount(config.sample_rate_hz, kLitanyPreambleSeconds);
      voicing.epilogue_sample_count =
          SecondsToSampleCount(config.sample_rate_hz, kLitanyEpilogueSeconds);
      break;
    case FlashVoicingFlavor::kHostility:
      voicing.attack_ratio = 0.020;
      voicing.release_ratio = 0.026;
      voicing.second_harmonic_gain = 0.112;
      voicing.third_harmonic_gain = 0.058;
      voicing.boundary_click_gain = 0.032;
      voicing.enable_preamble = true;
      voicing.enable_epilogue = true;
      voicing.preamble_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      voicing.epilogue_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      break;
    case FlashVoicingFlavor::kCollapse:
      voicing.attack_ratio = 0.10;
      voicing.release_ratio = 0.12;
      voicing.second_harmonic_gain = 0.035;
      voicing.third_harmonic_gain = 0.006;
      voicing.boundary_click_gain = 0.010;
      voicing.enable_preamble = true;
      voicing.enable_epilogue = true;
      voicing.preamble_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      voicing.epilogue_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      break;
    case FlashVoicingFlavor::kZeal:
      voicing.attack_ratio = 0.026;
      voicing.release_ratio = 0.032;
      voicing.second_harmonic_gain = 0.070;
      voicing.third_harmonic_gain = 0.026;
      voicing.boundary_click_gain = 0.016;
      voicing.enable_preamble = true;
      voicing.enable_epilogue = true;
      voicing.preamble_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      voicing.epilogue_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      break;
    case FlashVoicingFlavor::kVoid:
      voicing.attack_ratio = 0.115;
      voicing.release_ratio = 0.215;
      voicing.second_harmonic_gain = 0.010;
      voicing.third_harmonic_gain = 0.001;
      voicing.boundary_click_gain = 0.001;
      voicing.enable_preamble = true;
      voicing.enable_epilogue = true;
      voicing.preamble_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      voicing.epilogue_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      break;
    case FlashVoicingFlavor::kStandard:
    default:
      voicing.attack_ratio = 0.045;
      voicing.release_ratio = 0.045;
      voicing.second_harmonic_gain = 0.020;
      voicing.third_harmonic_gain = 0.002;
      voicing.boundary_click_gain = 0.006;
      voicing.enable_preamble = true;
      voicing.enable_epilogue = true;
      voicing.preamble_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      voicing.epilogue_sample_count =
          config.frame_samples > 0
              ? static_cast<std::size_t>(config.frame_samples) *
                    static_cast<std::size_t>(3)
              : static_cast<std::size_t>(0);
      break;
  }
  return voicing;
}

FlashVoicingDescriptor DescribeVoicingOutput(std::size_t total_sample_count,
                                             const FlashVoicingConfig& config) {
  const std::size_t leading_nonpayload_samples =
      LeadingNonpayloadSamples(config);
  const std::size_t trailing_nonpayload_samples =
      TrailingNonpayloadSamples(config);
  if (leading_nonpayload_samples + trailing_nonpayload_samples >
      total_sample_count) {
    throw std::invalid_argument("Voicing trim exceeds total sample count.");
  }

  FlashVoicingDescriptor descriptor{};
  descriptor.leading_nonpayload_samples = leading_nonpayload_samples;
  descriptor.trailing_nonpayload_samples = trailing_nonpayload_samples;
  descriptor.payload_sample_count = total_sample_count -
                                    leading_nonpayload_samples -
                                    trailing_nonpayload_samples;
  return descriptor;
}

std::vector<int16_t> TrimToPayloadPcm(
    const std::vector<int16_t>& voiced_pcm,
    const FlashVoicingDescriptor& descriptor) {
  const std::size_t described_total_sample_count =
      descriptor.leading_nonpayload_samples + descriptor.payload_sample_count +
      descriptor.trailing_nonpayload_samples;
  if (described_total_sample_count != voiced_pcm.size()) {
    throw std::invalid_argument(
        "Voicing descriptor does not match PCM sample count.");
  }

  const std::size_t payload_begin = descriptor.leading_nonpayload_samples;
  const std::size_t payload_end =
      payload_begin + descriptor.payload_sample_count;
  return std::vector<int16_t>(
      voiced_pcm.begin() + static_cast<std::ptrdiff_t>(payload_begin),
      voiced_pcm.begin() + static_cast<std::ptrdiff_t>(payload_end));
}

class FlashVoicingStepper::Impl {
 public:
  Impl(const std::vector<int16_t>& clean_payload_pcm,
       const FlashPayloadLayout& payload_layout, FlashVoicingFlavor flavor,
       const FlashVoicingConfig& config, bool enable_diagnostics)
      : clean_payload_pcm_(clean_payload_pcm),
        payload_layout_(payload_layout),
        flavor_(flavor),
        config_(config),
        enable_diagnostics_(enable_diagnostics),
        sanitized_(SanitizeVoicingConfig(config)),
        style_traits_(MakeFlavorTraits(flavor)),
        use_layered_timbre_(ShouldApplyLayeredTimbre(config)),
        use_metallic_layer_(use_layered_timbre_ &&
                            style_traits_.metallic_layer_gain_scale != 0.0),
        use_chant_drone_(style_traits_.chant_drone_gain > 0.0),
        use_mechanical_throat_(style_traits_.mechanical_throat_gain > 0.0),
        use_standard_low_voice_(use_layered_timbre_ &&
                                style_traits_.standard_low_voice_gain > 0.0),
        use_hostility_edge_(style_traits_.hostility_edge_gain > 0.0),
        use_collapse_tremor_(style_traits_.collapse_tremor_depth > 0.0),
        use_chant_swell_(style_traits_.chant_swell_depth > 0.0 &&
                         style_traits_.chant_swell_freq_hz > 0.0),
        use_payload_softclip_(style_traits_.payload_softclip_mix > 0.0 &&
                              style_traits_.payload_softclip_drive > 1.0),
        descriptor_(DescribeVoicingOutput(clean_payload_pcm.size() +
                                              LeadingNonpayloadSamples(config) +
                                              TrailingNonpayloadSamples(config),
                                          config)),
        spectral_texture_enabled_(ShouldApplySpectralTexture(config)),
        spectral_texture_work_(spectral_texture_enabled_
                                   ? descriptor_.leading_nonpayload_samples +
                                         descriptor_.payload_sample_count +
                                         descriptor_.trailing_nonpayload_samples
                                   : static_cast<std::size_t>(0)),
        total_postprocess_work_(descriptor_.leading_nonpayload_samples +
                                descriptor_.payload_sample_count +
                                descriptor_.trailing_nonpayload_samples +
                                spectral_texture_work_) {
    ValidatePayloadLayout(clean_payload_pcm_, payload_layout_);
    result_.pcm.resize(descriptor_.leading_nonpayload_samples +
                           descriptor_.payload_sample_count +
                           descriptor_.trailing_nonpayload_samples,
                       0);
    result_.descriptor = descriptor_;
    spectral_low_alpha_ =
        spectral_texture_enabled_
            ? ComputeOnePoleAlpha(240.0, sanitized_.sample_rate_hz)
            : 0.0;
    spectral_band_alpha_ =
        spectral_texture_enabled_
            ? ComputeOnePoleAlpha(2200.0, sanitized_.sample_rate_hz)
            : 0.0;
  }

  FlashVoicingStepProgress Pump(std::size_t work_budget) {
    const std::size_t normalized_budget =
        std::max(work_budget, static_cast<std::size_t>(1));
    std::size_t advanced = 0;
    while (!finished_ && advanced < normalized_budget) {
      if (stage_ == Stage::kPreamble) {
        PumpPreamble();
      } else if (stage_ == Stage::kPayload) {
        PumpPayload();
      } else if (stage_ == Stage::kEpilogue) {
        PumpEpilogue();
      } else if (stage_ == Stage::kSpectralTexture) {
        PumpSpectralTexture();
      } else {
        finished_ = true;
      }
      ++advanced;
    }
    return FlashVoicingStepProgress{
        .completed_work = completed_postprocess_work_,
        .total_work = total_postprocess_work_,
        .finished = finished_,
    };
  }

  const FlashVoicingResult& Result() const { return result_; }
  FlashVoicingDiagnostics Diagnostics() const { return diagnostics_; }
  std::size_t TotalWork() const { return total_postprocess_work_; }
  bool Finished() const { return finished_; }

 private:
  class ScopedDiagnosticTimer {
   public:
    ScopedDiagnosticTimer(bool enabled, double* bucket_ms)
        : enabled_(enabled),
          bucket_ms_(bucket_ms),
          started_at_(enabled ? std::chrono::steady_clock::now()
                              : std::chrono::steady_clock::time_point{}) {}

    ~ScopedDiagnosticTimer() {
      if (!enabled_ || bucket_ms_ == nullptr) {
        return;
      }
      const auto elapsed = std::chrono::steady_clock::now() - started_at_;
      *bucket_ms_ += std::chrono::duration<double, std::milli>(elapsed).count();
    }

   private:
    bool enabled_ = false;
    double* bucket_ms_ = nullptr;
    std::chrono::steady_clock::time_point started_at_{};
  };

  static constexpr std::size_t kPayloadDiagnosticSampleInterval = 4096;

  enum class Stage {
    kPreamble = 0,
    kPayload = 1,
    kEpilogue = 2,
    kSpectralTexture = 3,
    kDone = 4,
  };

  void AdvanceStageIfNeeded() {
    if (stage_ == Stage::kPreamble &&
        preamble_index_ >= descriptor_.leading_nonpayload_samples) {
      stage_ = Stage::kPayload;
      chunk_index_ = 0;
      chunk_local_index_ = 0;
    }
    if (stage_ == Stage::kPayload &&
        chunk_index_ >= payload_layout_.chunks.size()) {
      stage_ = Stage::kEpilogue;
      epilogue_index_ = 0;
    }
    if (stage_ == Stage::kEpilogue &&
        epilogue_index_ >= descriptor_.trailing_nonpayload_samples) {
      stage_ =
          spectral_texture_enabled_ ? Stage::kSpectralTexture : Stage::kDone;
      spectral_index_ = 0;
    }
    if (stage_ == Stage::kSpectralTexture &&
        spectral_index_ >= result_.pcm.size()) {
      stage_ = Stage::kDone;
      finished_ = true;
    }
    if (stage_ == Stage::kDone) {
      finished_ = true;
    }
  }

  void PumpPreamble() {
    if (preamble_index_ >= descriptor_.leading_nonpayload_samples) {
      AdvanceStageIfNeeded();
      return;
    }
    const double sample = RenderFixedPreambleSample(
        preamble_index_, descriptor_.leading_nonpayload_samples, style_traits_,
        sanitized_.sample_rate_hz);
    result_.pcm[preamble_index_] =
        static_cast<int16_t>(std::lround(sample * kPcmScale));
    ++preamble_index_;
    ++completed_postprocess_work_;
    AdvanceStageIfNeeded();
  }

  void PrepareChunkState(const FlashPayloadChunk& chunk) {
    ScopedDiagnosticTimer timer(enable_diagnostics_,
                                &diagnostics_.payload_prepare_ms);
    std::tie(chunk_attack_count_, chunk_release_count_) =
        FitAttackReleaseCounts(
            chunk.sample_count,
            RatioToCount(sanitized_.attack_ratio, chunk.sample_count),
            RatioToCount(
                sanitized_.release_ratio * style_traits_.payload_release_scale,
                chunk.sample_count));
    chunk_click_window_ = ComputeClickWindow(chunk.sample_count);
    chunk_litany_text_pause_weight_ =
        ComputeLitanyTextPauseWeight(payload_layout_, chunk_index_);
    chunk_collapse_hesitation_weight_ =
        ComputeCollapseHesitationWeight(chunk, chunk_index_, style_traits_);
  }

  void PumpPayload() {
    if (chunk_index_ >= payload_layout_.chunks.size()) {
      AdvanceStageIfNeeded();
      return;
    }

    const FlashPayloadChunk& chunk = payload_layout_.chunks[chunk_index_];
    if (chunk_local_index_ == 0) {
      PrepareChunkState(chunk);
    }

    const bool profile_payload_sample =
        enable_diagnostics_ &&
        ((completed_postprocess_work_ % kPayloadDiagnosticSampleInterval) == 0);
    if (profile_payload_sample) {
      ++diagnostics_.payload_profiled_samples;
    }
    std::size_t payload_sample_index = 0;
    std::size_t sample_index = 0;
    {
      ScopedDiagnosticTimer timer(profile_payload_sample,
                                  &diagnostics_.payload_sample_setup_ms);
      payload_sample_index = chunk.sample_offset + chunk_local_index_;
      sample_index = descriptor_.leading_nonpayload_samples +
                     payload_sample_index;
    }

    if (chunk.kind == FlashPayloadSegmentKind::kSilence) {
      result_.pcm[sample_index] = 0;
      if (enable_diagnostics_) {
        ++diagnostics_.payload_silence_samples;
      }
    } else {
      if (enable_diagnostics_) {
        ++diagnostics_.payload_voiced_samples;
      }
      double clean_sample = 0.0;
      double envelope = 0.0;
      {
        ScopedDiagnosticTimer timer(profile_payload_sample,
                                    &diagnostics_.payload_envelope_ms);
        clean_sample = static_cast<double>(
                           clean_payload_pcm_[payload_sample_index]) /
                       kPcmScale;
        const double base_envelope =
            ComputeEnvelope(chunk_local_index_, chunk.sample_count,
                            chunk_attack_count_, chunk_release_count_);
        envelope = base_envelope +
                   style_traits_.payload_envelope_floor *
                       (1.0 - base_envelope);
      }
      double payload_articulation = 1.0;
      {
        ScopedDiagnosticTimer timer(profile_payload_sample,
                                    &diagnostics_.payload_articulation_ms);
        const double phrase_articulation = ComputeLitanyPhraseArticulation(
            chunk_index_, chunk_local_index_, chunk.sample_count,
            style_traits_);
        const double litany_text_articulation =
            ComputeLitanyTextPauseArticulation(
                chunk_litany_text_pause_weight_, chunk_local_index_,
                chunk.sample_count, style_traits_);
        const double collapse_hesitation_articulation =
            ComputeCollapseHesitationArticulation(
                chunk_collapse_hesitation_weight_, chunk_local_index_,
                chunk.sample_count, style_traits_);
        payload_articulation = phrase_articulation *
                               litany_text_articulation *
                               collapse_hesitation_articulation;
      }
      const double layer_gain = envelope * payload_articulation;
      double harmonic_mix = 0.0;
      {
        ScopedDiagnosticTimer timer(profile_payload_sample,
                                    &diagnostics_.payload_harmonic_ms);
        harmonic_mix =
            layer_gain *
                ComputeHarmonicSample(sanitized_.second_harmonic_gain, 2,
                                      chunk.carrier_freq_hz, chunk_local_index_,
                                      sanitized_.sample_rate_hz) +
            layer_gain *
                ComputeHarmonicSample(sanitized_.third_harmonic_gain, 3,
                                      chunk.carrier_freq_hz, chunk_local_index_,
                                      sanitized_.sample_rate_hz);
      }
      double metallic_layer = 0.0;
      {
        ScopedDiagnosticTimer timer(profile_payload_sample,
                                    &diagnostics_.payload_metallic_ms);
        metallic_layer =
            use_metallic_layer_
                ? layer_gain *
                      ComputeSecondaryMetallicLayer(chunk.carrier_freq_hz,
                                                    chunk_local_index_,
                                                    sanitized_.sample_rate_hz) *
                      style_traits_.metallic_layer_gain_scale
                : 0.0;
      }
      double chant_resonance = 0.0;
      {
        ScopedDiagnosticTimer timer(profile_payload_sample,
                                    &diagnostics_.payload_chant_resonance_ms);
        chant_resonance =
            use_layered_timbre_
                ? layer_gain * ComputeChantResonanceLayer(
                                   chunk.carrier_freq_hz, payload_sample_index,
                                   sanitized_.sample_rate_hz,
                                   style_traits_.chant_resonance_gain)
                : 0.0;
      }
      double chant_drone = 0.0;
      {
        ScopedDiagnosticTimer timer(profile_payload_sample,
                                    &diagnostics_.payload_chant_drone_ms);
        chant_drone = use_chant_drone_
                          ? layer_gain * ComputeChantDroneLayer(
                                             payload_sample_index,
                                             sanitized_.sample_rate_hz,
                                             style_traits_.chant_drone_gain)
                          : 0.0;
      }
      double mechanical_throat = 0.0;
      {
        ScopedDiagnosticTimer timer(
            profile_payload_sample,
            &diagnostics_.payload_mechanical_throat_ms);
        mechanical_throat =
            use_mechanical_throat_
                ? layer_gain * ComputeMechanicalThroatLayer(
                                   payload_sample_index,
                                   sanitized_.sample_rate_hz,
                                   style_traits_.mechanical_throat_gain)
                : 0.0;
      }
      double standard_low_voice = 0.0;
      {
        ScopedDiagnosticTimer timer(
            profile_payload_sample,
            &diagnostics_.payload_standard_low_voice_ms);
        standard_low_voice =
            use_standard_low_voice_
                ? layer_gain * ComputeStandardLowVoiceLayer(
                                   payload_sample_index,
                                   sanitized_.sample_rate_hz, style_traits_)
                : 0.0;
      }
      double hostility_edge = 0.0;
      {
        ScopedDiagnosticTimer timer(profile_payload_sample,
                                    &diagnostics_.payload_hostility_edge_ms);
        hostility_edge =
            use_hostility_edge_
                ? layer_gain * ComputeHostilityEdgeLayer(
                                   chunk.carrier_freq_hz, chunk_local_index_,
                                   sanitized_.sample_rate_hz,
                                   style_traits_.hostility_edge_gain)
                : 0.0;
      }
      double boundary_click = 0.0;
      {
        ScopedDiagnosticTimer timer(profile_payload_sample,
                                    &diagnostics_.payload_boundary_click_ms);
        boundary_click =
            chunk_local_index_ < chunk_click_window_
                ? ComputeBoundaryClick(sanitized_.boundary_click_gain,
                                       style_traits_, chunk_index_,
                                       chunk_local_index_, chunk_click_window_)
                : 0.0;
      }
      double tremolo = 1.0;
      double collapse_tremor = 1.0;
      double chant_swell = 1.0;
      {
        ScopedDiagnosticTimer timer(profile_payload_sample,
                                    &diagnostics_.payload_modulation_ms);
        tremolo = use_layered_timbre_
                      ? ComputePayloadTremolo(payload_sample_index,
                                              sanitized_.sample_rate_hz,
                                              style_traits_)
                      : 1.0;
        collapse_tremor =
            use_collapse_tremor_
                ? ComputeCollapseTremor(payload_sample_index,
                                        sanitized_.sample_rate_hz,
                                        style_traits_)
                : 1.0;
        chant_swell =
            use_chant_swell_
                ? ComputeChantSwellGain(payload_sample_index,
                                        sanitized_.sample_rate_hz,
                                        style_traits_)
                : 1.0;
      }
      {
        ScopedDiagnosticTimer timer(profile_payload_sample,
                                    &diagnostics_.payload_mix_shape_store_ms);
        const double mixed_sample =
            (clean_sample * layer_gain + harmonic_mix + metallic_layer +
             chant_resonance + chant_drone + mechanical_throat +
             standard_low_voice + hostility_edge) *
                tremolo * collapse_tremor * chant_swell +
            boundary_click;
        const double shaped_sample =
            use_payload_softclip_
                ? ApplyPayloadSoftClip(mixed_sample, style_traits_)
                : mixed_sample;
        const double voiced_sample = std::clamp(shaped_sample, -1.0, 1.0);
        result_.pcm[sample_index] =
            static_cast<int16_t>(std::lround(voiced_sample * kPcmScale));
      }
    }

    ++chunk_local_index_;
    ++completed_postprocess_work_;
    if (chunk_local_index_ >= chunk.sample_count) {
      ++chunk_index_;
      chunk_local_index_ = 0;
    }
    AdvanceStageIfNeeded();
  }

  void PumpEpilogue() {
    if (epilogue_index_ >= descriptor_.trailing_nonpayload_samples) {
      AdvanceStageIfNeeded();
      return;
    }
    const double sample = RenderFixedEpilogueSample(
        epilogue_index_, descriptor_.trailing_nonpayload_samples, style_traits_,
        sanitized_.sample_rate_hz);
    const std::size_t sample_index = descriptor_.leading_nonpayload_samples +
                                     descriptor_.payload_sample_count +
                                     epilogue_index_;
    result_.pcm[sample_index] =
        static_cast<int16_t>(std::lround(sample * kPcmScale));
    ++epilogue_index_;
    ++completed_postprocess_work_;
    AdvanceStageIfNeeded();
  }

  void PumpSpectralTexture() {
    if (spectral_index_ >= result_.pcm.size()) {
      AdvanceStageIfNeeded();
      return;
    }
    const double dry =
        static_cast<double>(result_.pcm[spectral_index_]) / kPcmScale;
    spectral_low_state_ += spectral_low_alpha_ * (dry - spectral_low_state_);
    const double band_input = dry - spectral_low_state_;
    spectral_band_state_ +=
        spectral_band_alpha_ * (band_input - spectral_band_state_);
    const double bandpassed = spectral_band_state_;
    const double saturated = SoftClipSample(dry, kSpectralSoftClipDrive);
    const double textured =
        dry *
            (1.0 - kSpectralBandMix * style_traits_.spectral_band_mix_scale -
             kSpectralSoftClipMix * style_traits_.spectral_softclip_mix_scale) +
        bandpassed *
            (kSpectralBandMix * style_traits_.spectral_band_mix_scale) +
        saturated *
            (kSpectralSoftClipMix * style_traits_.spectral_softclip_mix_scale);
    result_.pcm[spectral_index_] = static_cast<int16_t>(
        std::lround(std::clamp(textured, -1.0, 1.0) * kPcmScale));
    ++spectral_index_;
    ++completed_postprocess_work_;
    AdvanceStageIfNeeded();
  }

  const std::vector<int16_t>& clean_payload_pcm_;
  const FlashPayloadLayout& payload_layout_;
  FlashVoicingFlavor flavor_;
  FlashVoicingConfig config_;
  bool enable_diagnostics_ = false;
  FlashVoicingDiagnostics diagnostics_{};
  SanitizedVoicingConfig sanitized_{};
  VoicingStyleTraits style_traits_{};
  bool use_layered_timbre_ = false;
  bool use_metallic_layer_ = false;
  bool use_chant_drone_ = false;
  bool use_mechanical_throat_ = false;
  bool use_standard_low_voice_ = false;
  bool use_hostility_edge_ = false;
  bool use_collapse_tremor_ = false;
  bool use_chant_swell_ = false;
  bool use_payload_softclip_ = false;
  FlashVoicingDescriptor descriptor_{};
  bool spectral_texture_enabled_ = false;
  std::size_t spectral_texture_work_ = 0;
  std::size_t total_postprocess_work_ = 0;
  FlashVoicingResult result_{};
  Stage stage_ = Stage::kPreamble;
  bool finished_ = false;
  std::size_t completed_postprocess_work_ = 0;
  std::size_t preamble_index_ = 0;
  std::size_t chunk_index_ = 0;
  std::size_t chunk_local_index_ = 0;
  std::size_t epilogue_index_ = 0;
  std::size_t spectral_index_ = 0;
  std::size_t chunk_attack_count_ = 0;
  std::size_t chunk_release_count_ = 0;
  std::size_t chunk_click_window_ = 0;
  double chunk_litany_text_pause_weight_ = 0.0;
  double chunk_collapse_hesitation_weight_ = 0.0;
  double spectral_low_alpha_ = 0.0;
  double spectral_band_alpha_ = 0.0;
  double spectral_low_state_ = 0.0;
  double spectral_band_state_ = 0.0;
};

FlashVoicingStepper::FlashVoicingStepper(
    const std::vector<int16_t>& clean_payload_pcm,
    const FlashPayloadLayout& payload_layout, FlashVoicingFlavor flavor,
    const FlashVoicingConfig& config, bool enable_diagnostics)
    : impl_(std::make_unique<Impl>(clean_payload_pcm, payload_layout, flavor,
                                   config, enable_diagnostics)) {}

FlashVoicingStepper::~FlashVoicingStepper() = default;

const FlashVoicingResult& FlashVoicingStepper::Result() const {
  return impl_->Result();
}

FlashVoicingDiagnostics FlashVoicingStepper::Diagnostics() const {
  return impl_->Diagnostics();
}

std::size_t FlashVoicingStepper::TotalWork() const {
  return impl_->TotalWork();
}

bool FlashVoicingStepper::Finished() const {
  return impl_->Finished();
}

FlashVoicingStepProgress FlashVoicingStepper::Pump(std::size_t work_budget) {
  return impl_->Pump(work_budget);
}

FlashVoicingResult ApplyVoicingToPayload(
    const std::vector<int16_t>& clean_payload_pcm,
    const FlashPayloadLayout& payload_layout, const FlashVoicingConfig& config,
    const EncodeProgressSink* progress_sink, float progress_begin,
    float progress_end) {
  return ApplyVoicingToPayloadWithFlavor(
      clean_payload_pcm, payload_layout, FlashVoicingFlavor::kStandard, config,
      progress_sink, progress_begin, progress_end);
}

FlashVoicingResult ApplyVoicingToPayloadWithFlavor(
    const std::vector<int16_t>& clean_payload_pcm,
    const FlashPayloadLayout& payload_layout, FlashVoicingFlavor flavor,
    const FlashVoicingConfig& config, const EncodeProgressSink* progress_sink,
    float progress_begin, float progress_end) {
  FlashVoicingStepper stepper(clean_payload_pcm, payload_layout, flavor,
                              config);
  const std::size_t total_postprocess_work = stepper.TotalWork();
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPostprocessing,
                       progress_begin);
  ThrowIfEncodeCancelled(progress_sink);
  if (total_postprocess_work == 0) {
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPostprocessing,
                         progress_end);
    return stepper.Result();
  }

  std::size_t last_reported_postprocess_work = 0;
  while (!stepper.Finished()) {
    ThrowIfEncodeCancelled(progress_sink);
    const FlashVoicingStepProgress progress =
        stepper.Pump(kProgressReportStrideSamples);
    MaybeReportCompletedSampleProgress(
        progress_sink, EncodeProgressPhase::kPostprocessing, progress_begin,
        progress_end, progress.completed_work, total_postprocess_work,
        &last_reported_postprocess_work, progress.finished);
  }
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPostprocessing,
                       progress_end);
  return stepper.Result();
}

}  // namespace bag::flash
