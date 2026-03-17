namespace bag::flash {

using std::int16_t;
using std::size_t;

namespace {

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
                                   float progress_begin,
                                   float progress_end,
                                   std::size_t completed_samples,
                                   std::size_t total_samples) {
    const float ratio =
        total_samples == 0
            ? 1.0f
            : static_cast<float>(std::min(completed_samples, total_samples)) /
                static_cast<float>(total_samples);
    ReportEncodeProgress(
        progress_sink,
        phase,
        LerpProgress(progress_begin, progress_end, ratio));
}

void MaybeReportCompletedSampleProgress(const EncodeProgressSink* progress_sink,
                                        EncodeProgressPhase phase,
                                        float progress_begin,
                                        float progress_end,
                                        std::size_t completed_samples,
                                        std::size_t total_samples,
                                        std::size_t* last_reported_samples,
                                        bool force = false) {
    if (last_reported_samples == nullptr) {
        return;
    }

    const std::size_t clamped_completed_samples =
        total_samples == 0 ? 0 : std::min(completed_samples, total_samples);
    if (!force &&
        clamped_completed_samples < total_samples &&
        clamped_completed_samples - *last_reported_samples < kProgressReportStrideSamples) {
        return;
    }

    ReportCompletedSampleProgress(
        progress_sink,
        phase,
        progress_begin,
        progress_end,
        clamped_completed_samples,
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
    kRitualChant = 1,
};

enum class FixedShellTimbreProfile {
    kInvocationClosure = 0,
    kProtocolBoundary = 1,
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
    case FlashVoicingFlavor::kCodedBurst:
    case FlashVoicingFlavor::kRitualChant:
        return flavor;
    default:
        return FlashVoicingFlavor::kCodedBurst;
    }
}

std::size_t LeadingNonpayloadSamples(const FlashVoicingConfig& config) {
    return config.enable_preamble ? config.preamble_sample_count : static_cast<std::size_t>(0);
}

std::size_t TrailingNonpayloadSamples(const FlashVoicingConfig& config) {
    return config.enable_epilogue ? config.epilogue_sample_count : static_cast<std::size_t>(0);
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
        if (chunk.sample_offset + chunk.sample_count > payload_layout.payload_sample_count) {
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
    sanitized.attack_ratio = std::clamp(config.attack_ratio, 0.0, kMaxAttackReleaseRatio);
    sanitized.release_ratio = std::clamp(config.release_ratio, 0.0, kMaxAttackReleaseRatio);

    const double combined_attack_release = sanitized.attack_ratio + sanitized.release_ratio;
    if (combined_attack_release > kMaxAttackReleaseCombinedRatio && combined_attack_release > 0.0) {
        const double scale = kMaxAttackReleaseCombinedRatio / combined_attack_release;
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
}

size_t RatioToCount(double ratio, size_t sample_count) {
    if (ratio <= 0.0 || sample_count == 0) {
        return 0;
    }

    const size_t count =
        static_cast<size_t>(std::round(static_cast<double>(sample_count) * ratio));
    return std::clamp(count, static_cast<size_t>(1), sample_count);
}

std::pair<size_t, size_t> FitAttackReleaseCounts(size_t sample_count,
                                                 size_t attack_count,
                                                 size_t release_count) {
    if (sample_count == 0) {
        return {0, 0};
    }
    if (attack_count + release_count <= sample_count) {
        return {attack_count, release_count};
    }

    const double total = static_cast<double>(attack_count + release_count);
    if (total <= 0.0) {
        return {0, 0};
    }

    size_t fitted_attack =
        static_cast<size_t>(std::floor(static_cast<double>(sample_count) *
                                       static_cast<double>(attack_count) /
                                       total));
    fitted_attack = std::min(fitted_attack, sample_count);
    const size_t fitted_release = sample_count - fitted_attack;
    return {fitted_attack, fitted_release};
}

double ComputeEnvelope(size_t local_index,
                       size_t sample_count,
                       size_t attack_count,
                       size_t release_count) {
    if (sample_count == 0) {
        return 0.0;
    }

    double envelope = 1.0;
    if (attack_count > 0 && local_index < attack_count) {
        envelope = std::min(
            envelope,
            static_cast<double>(local_index + 1) / static_cast<double>(attack_count));
    }

    if (release_count > 0) {
        const size_t release_start =
            sample_count > release_count ? sample_count - release_count : 0;
        if (local_index >= release_start) {
            const size_t remaining = sample_count - local_index;
            envelope = std::min(
                envelope,
                static_cast<double>(remaining) / static_cast<double>(release_count));
        }
    }

    return std::clamp(envelope, 0.0, 1.0);
}

double ComputeHarmonicSample(double gain,
                             int harmonic_multiple,
                             double carrier_freq_hz,
                             size_t local_index,
                             int sample_rate_hz) {
    if (gain <= 0.0 || harmonic_multiple <= 1 || carrier_freq_hz <= 0.0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double harmonic_freq_hz = carrier_freq_hz * static_cast<double>(harmonic_multiple);
    const double nyquist_hz = static_cast<double>(sample_rate_hz) / 2.0;
    if (harmonic_freq_hz >= nyquist_hz) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    return gain * std::sin(2.0 * kPi * harmonic_freq_hz * time);
}

bool IsByteBoundaryChunk(size_t chunk_index) {
    return chunk_index > 0 && (chunk_index % static_cast<size_t>(8)) == 0;
}

bool IsNibbleBoundaryChunk(size_t chunk_index) {
    return chunk_index > 0 &&
           (chunk_index % static_cast<size_t>(4)) == 0 &&
           !IsByteBoundaryChunk(chunk_index);
}

size_t ComputeClickWindow(size_t chunk_sample_count) {
    if (chunk_sample_count == 0) {
        return 0;
    }
    return std::clamp(chunk_sample_count / static_cast<size_t>(96),
                      static_cast<size_t>(6),
                      static_cast<size_t>(24));
}

size_t ComputeSubsegmentIndex(size_t local_index,
                              size_t sample_count,
                              size_t part_count) {
    if (sample_count == 0 || part_count == 0) {
        return 0;
    }
    return std::min((local_index * part_count) / sample_count, part_count - static_cast<size_t>(1));
}

double ComputeSubsegmentProgress(size_t local_index,
                                 size_t sample_count,
                                 size_t part_index,
                                 size_t part_count) {
    if (sample_count == 0 || part_count == 0) {
        return 0.0;
    }

    const size_t begin = (sample_count * part_index) / part_count;
    const size_t end = std::max(begin + static_cast<size_t>(1),
                                (sample_count * (part_index + static_cast<size_t>(1))) / part_count);
    const size_t span = std::max(end - begin, static_cast<size_t>(1));
    const size_t clamped_index = std::min(local_index, end - static_cast<size_t>(1));
    return std::clamp(static_cast<double>(clamped_index - begin) / static_cast<double>(span), 0.0, 1.0);
}

double ComputeBurstWindow(double progress,
                          double begin,
                          double end) {
    if (progress <= begin || progress >= end || end <= begin) {
        return 0.0;
    }

    const double local = (progress - begin) / (end - begin);
    return std::sin(kPi * local);
}

double ComputeGroupedBurstPattern(size_t local_index,
                                  size_t sample_count) {
    if (sample_count == 0) {
        return 0.0;
    }

    constexpr double kGroupGains[4] = {1.00, 0.82, 0.94, 0.76};
    constexpr double kBeatGains[4] = {1.00, 0.12, 0.78, 0.10};

    const size_t beat_index =
        ComputeSubsegmentIndex(local_index, sample_count, kRhythmBeatCount);
    const size_t group_index =
        std::min(beat_index / kRhythmGroupSize, static_cast<size_t>(3));
    const size_t beat_in_group = beat_index % kRhythmGroupSize;
    const double beat_progress =
        ComputeSubsegmentProgress(local_index, sample_count, beat_index, kRhythmBeatCount);
    const double primary_burst = ComputeBurstWindow(beat_progress, 0.03, 0.31);
    const double secondary_burst = 0.56 * ComputeBurstWindow(beat_progress, 0.49, 0.71);
    const double beat_gain = kGroupGains[group_index] * kBeatGains[beat_in_group];
    return std::clamp(beat_gain * (primary_burst + secondary_burst), 0.0, 1.0);
}

VoicingStyleTraits MakeFlavorTraits(FlashVoicingFlavor flavor) {
    switch (NormalizeVoicingFlavor(flavor)) {
    case FlashVoicingFlavor::kRitualChant:
        return VoicingStyleTraits{
            .nibble_boundary_accent_factor = 0.12,
            .byte_boundary_click_scale = 0.68,
            .preamble_grouped_burst_mix = 0.26,
            .preamble_invocation_shell_mix = 1.12,
            .epilogue_grouped_burst_mix = 0.22,
            .epilogue_closure_shell_mix = 1.08,
            .preamble_rise_ratio = 0.09,
            .preamble_fall_ratio = 0.15,
            .epilogue_rise_ratio = 0.06,
            .epilogue_fall_ratio = 0.18,
            .epilogue_decay_exponent = 3.4,
            .payload_release_scale = 1.10,
            .payload_envelope_floor = 0.12,
            .metallic_layer_gain_scale = 0.18,
            .chant_resonance_gain = 0.008,
            .tremolo_depth_scale = 0.15,
            .tremolo_freq_hz = 5.4,
            .spectral_band_mix_scale = 0.24,
            .spectral_softclip_mix_scale = 0.20,
            .preamble_cadence_profile = FixedShellCadenceProfile::kRitualChant,
            .epilogue_cadence_profile = FixedShellCadenceProfile::kRitualChant,
            .preamble_phrase_mask_mix = 1.0,
            .epilogue_phrase_mask_mix = 1.0,
            .preamble_terminal_mute_begin = 0.88,
            .preamble_terminal_mute_end = 1.0,
            .epilogue_terminal_mute_begin = 0.84,
            .epilogue_terminal_mute_end = 1.0,
        };
    case FlashVoicingFlavor::kCodedBurst:
    default:
        return VoicingStyleTraits{
            .nibble_boundary_accent_factor = kNibbleBoundaryAccentFactor,
            .byte_boundary_click_scale = 1.0,
            .preamble_grouped_burst_mix = 0.54,
            .preamble_invocation_shell_mix = 1.10,
            .epilogue_grouped_burst_mix = 0.50,
            .epilogue_closure_shell_mix = 1.08,
            .preamble_rise_ratio = 0.05,
            .preamble_fall_ratio = 0.24,
            .epilogue_rise_ratio = 0.05,
            .epilogue_fall_ratio = 0.24,
            .epilogue_decay_exponent = 5.4,
            .payload_release_scale = 1.0,
            .payload_envelope_floor = 0.0,
            .metallic_layer_gain_scale = 1.0,
            .chant_resonance_gain = 0.0,
            .tremolo_depth_scale = 1.0,
            .tremolo_freq_hz = kTremoloFreqHz,
            .spectral_band_mix_scale = 1.0,
            .spectral_softclip_mix_scale = 1.0,
            .preamble_cadence_profile = FixedShellCadenceProfile::kProtocolHandshake,
            .epilogue_cadence_profile = FixedShellCadenceProfile::kProtocolHandshake,
            .preamble_timbre_profile = FixedShellTimbreProfile::kProtocolBoundary,
            .epilogue_timbre_profile = FixedShellTimbreProfile::kProtocolBoundary,
            .preamble_phrase_mask_mix = 1.0,
            .epilogue_phrase_mask_mix = 1.0,
            .preamble_terminal_mute_begin = 0.76,
            .preamble_terminal_mute_end = 1.0,
            .epilogue_terminal_mute_begin = 0.72,
            .epilogue_terminal_mute_end = 1.0,
        };
    }
}

bool ShouldApplySpectralTexture(const FlashVoicingConfig& config) {
    return config.attack_ratio > 0.0 ||
           config.release_ratio > 0.0 ||
           config.second_harmonic_gain > 0.0 ||
           config.third_harmonic_gain > 0.0;
}

bool ShouldApplyLayeredTimbre(const FlashVoicingConfig& config) {
    return config.second_harmonic_gain > 0.0 ||
           config.third_harmonic_gain > 0.0 ||
           config.attack_ratio > 0.0 ||
           config.release_ratio > 0.0;
}

double ComputeOnePoleAlpha(double cutoff_hz,
                           int sample_rate_hz) {
    if (cutoff_hz <= 0.0 || sample_rate_hz <= 0) {
        return 1.0;
    }

    const double exponent =
        -2.0 * kPi * cutoff_hz / static_cast<double>(sample_rate_hz);
    return std::clamp(1.0 - std::exp(exponent), 0.0, 1.0);
}

double SoftClipSample(double sample,
                      double drive) {
    if (drive <= 0.0) {
        return sample;
    }

    const double scaled = sample * drive;
    const double normalization = std::tanh(drive);
    if (normalization <= 0.0) {
        return sample;
    }
    return std::tanh(scaled) / normalization;
}

double ComputeSecondaryMetallicLayer(double carrier_freq_hz,
                                     size_t local_index,
                                     int sample_rate_hz) {
    if (carrier_freq_hz <= 0.0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double primary_freq_hz = carrier_freq_hz * 4.5;
    const double secondary_freq_hz = carrier_freq_hz * 6.25;
    const double nyquist_hz = static_cast<double>(sample_rate_hz) / 2.0;

    double layer = 0.0;
    if (primary_freq_hz < nyquist_hz) {
        layer += 0.68 * std::sin(2.0 * kPi * primary_freq_hz * time);
    }
    if (secondary_freq_hz < nyquist_hz) {
        layer += 0.32 * std::sin(2.0 * kPi * secondary_freq_hz * time + 0.35);
    }
    return kMetallicLayerGain * layer;
}

double ComputeChantResonanceLayer(double carrier_freq_hz,
                                  size_t payload_sample_index,
                                  int sample_rate_hz,
                                  double gain) {
    if (gain <= 0.0 || carrier_freq_hz <= 0.0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double time =
        static_cast<double>(payload_sample_index) / static_cast<double>(sample_rate_hz);
    const double lower_freq_hz = carrier_freq_hz * 0.75;
    const double upper_freq_hz = carrier_freq_hz * 1.50;
    const double nyquist_hz = static_cast<double>(sample_rate_hz) / 2.0;

    double layer = 0.0;
    if (lower_freq_hz > 0.0 && lower_freq_hz < nyquist_hz) {
        layer += 0.62 * std::sin(2.0 * kPi * lower_freq_hz * time + 0.18);
    }
    if (upper_freq_hz < nyquist_hz) {
        layer += 0.38 * std::sin(2.0 * kPi * upper_freq_hz * time + 0.52);
    }
    return gain * layer;
}

double ComputePayloadTremolo(std::size_t payload_sample_index,
                             int sample_rate_hz,
                             const VoicingStyleTraits& style_traits) {
    if (sample_rate_hz <= 0) {
        return 1.0;
    }

    const double time =
        static_cast<double>(payload_sample_index) / static_cast<double>(sample_rate_hz);
    return 1.0 +
           (kTremoloDepth * style_traits.tremolo_depth_scale) *
               std::sin(2.0 * kPi * style_traits.tremolo_freq_hz * time);
}

double ComputeInvocationSweep(size_t local_index,
                              size_t sample_count,
                              int sample_rate_hz) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double progress =
        static_cast<double>(local_index) / static_cast<double>(sample_count);
    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double begin_hz = 320.0;
    const double end_hz = 1180.0;
    const double sweep_freq_hz = begin_hz + (end_hz - begin_hz) * std::pow(progress, 0.72);
    const double envelope = ComputeBurstWindow(progress, 0.04, 0.58);
    return kInvocationSweepGain *
           std::sin(2.0 * kPi * sweep_freq_hz * time) *
           envelope;
}

double ComputeInvocationPulse(size_t local_index,
                              size_t sample_count,
                              int sample_rate_hz) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double progress =
        static_cast<double>(local_index) / static_cast<double>(sample_count);
    const double window =
        ComputeBurstWindow(progress, 0.62, 0.96);
    if (window <= 0.0) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double pulse =
        0.74 * std::sin(2.0 * kPi * 680.0 * time) +
        0.26 * std::sin(2.0 * kPi * 1360.0 * time + 0.22);
    return kInvocationPulseGain * pulse * window;
}

double ComputeClosurePulse(size_t local_index,
                           size_t sample_count,
                           int sample_rate_hz) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double progress =
        static_cast<double>(local_index) / static_cast<double>(sample_count);
    const double window =
        ComputeBurstWindow(progress, 0.12, 0.54);
    if (window <= 0.0) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double pulse =
        0.68 * std::sin(2.0 * kPi * 540.0 * time) +
        0.32 * std::sin(2.0 * kPi * 900.0 * time + 0.44);
    return kClosurePulseGain * pulse * window;
}

double ComputeClosureTail(size_t local_index,
                          size_t sample_count,
                          int sample_rate_hz) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double progress =
        static_cast<double>(local_index) / static_cast<double>(sample_count);
    const double window =
        ComputeBurstWindow(progress, 0.54, 0.98);
    if (window <= 0.0) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double decay = std::pow(std::max(0.0, 1.0 - progress), 1.6);
    const double tail =
        0.72 * std::sin(2.0 * kPi * 280.0 * time) +
        0.28 * std::sin(2.0 * kPi * 560.0 * time + 0.16);
    return kClosureTailGain * tail * window * decay;
}

double ComputeProtocolLeadSweep(size_t local_index,
                                size_t sample_count,
                                int sample_rate_hz) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double progress =
        static_cast<double>(local_index) / static_cast<double>(sample_count);
    const double window = ComputeBurstWindow(progress, 0.04, 0.74);
    if (window <= 0.0) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double chirp_progress = std::clamp((progress - 0.04) / 0.70, 0.0, 1.0);
    const double sweep_freq_hz = 760.0 + (1180.0 - 760.0) * std::pow(chirp_progress, 0.58);
    const double sweep =
        0.78 * std::sin(2.0 * kPi * sweep_freq_hz * time) +
        0.22 * std::sin(2.0 * kPi * sweep_freq_hz * 2.0 * time + 0.18);
    return 0.18 * sweep * window;
}

double ComputeProtocolLeadLockPulse(size_t local_index,
                                    size_t sample_count,
                                    int sample_rate_hz) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double progress =
        static_cast<double>(local_index) / static_cast<double>(sample_count);
    const double window = std::max(
        ComputeBurstWindow(progress, 0.06, 0.22),
        ComputeBurstWindow(progress, 0.56, 0.74));
    if (window <= 0.0) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double pulse =
        0.62 * std::sin(2.0 * kPi * 680.0 * time) +
        0.26 * std::sin(2.0 * kPi * 1360.0 * time + 0.20) +
        0.12 * std::sin(2.0 * kPi * 2040.0 * time + 0.08);
    return 0.16 * pulse * window;
}

double ComputeProtocolClosePulse(size_t local_index,
                                 size_t sample_count,
                                 int sample_rate_hz) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double progress =
        static_cast<double>(local_index) / static_cast<double>(sample_count);
    const double window = ComputeBurstWindow(progress, 0.10, 0.36);
    if (window <= 0.0) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double pulse =
        0.72 * std::sin(2.0 * kPi * 310.0 * time) +
        0.20 * std::sin(2.0 * kPi * 620.0 * time + 0.24) +
        0.08 * std::sin(2.0 * kPi * 930.0 * time + 0.10);
    return 0.22 * pulse * window;
}

double ComputeProtocolAckChirp(size_t local_index,
                               size_t sample_count,
                               int sample_rate_hz) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double progress =
        static_cast<double>(local_index) / static_cast<double>(sample_count);
    const double window = ComputeBurstWindow(progress, 0.52, 0.70);
    if (window <= 0.0) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double chirp_progress = std::clamp((progress - 0.52) / 0.18, 0.0, 1.0);
    const double chirp_freq_hz = 620.0 + (420.0 - 620.0) * std::pow(chirp_progress, 0.84);
    const double chirp =
        0.76 * std::sin(2.0 * kPi * chirp_freq_hz * time) +
        0.24 * std::sin(2.0 * kPi * chirp_freq_hz * 2.0 * time + 0.26);
    return 0.18 * chirp * window;
}

void ApplySpectralTexture(std::vector<int16_t>* pcm,
                          int sample_rate_hz,
                          const VoicingStyleTraits& style_traits,
                          const EncodeProgressSink* progress_sink = nullptr,
                          float progress_begin = 0.0f,
                          float progress_end = 1.0f,
                          std::size_t completed_samples_before = 0,
                          std::size_t total_work_samples = 0) {
    if (pcm == nullptr || pcm->empty() || sample_rate_hz <= 0) {
        return;
    }

    const double low_alpha = ComputeOnePoleAlpha(240.0, sample_rate_hz);
    const double band_alpha = ComputeOnePoleAlpha(2200.0, sample_rate_hz);
    double low_state = 0.0;
    double band_state = 0.0;
    std::size_t last_reported_samples = completed_samples_before;

    for (std::size_t index = 0; index < pcm->size(); ++index) {
        if (index > 0 && index % kProgressReportStrideSamples == 0) {
            ThrowIfEncodeCancelled(progress_sink);
        }
        const double dry = static_cast<double>((*pcm)[index]) / kPcmScale;
        low_state += low_alpha * (dry - low_state);
        const double band_input = dry - low_state;
        band_state += band_alpha * (band_input - band_state);
        const double bandpassed = band_state;
        const double saturated = SoftClipSample(dry, kSpectralSoftClipDrive);
        const double textured =
            dry * (1.0 - kSpectralBandMix * style_traits.spectral_band_mix_scale -
                   kSpectralSoftClipMix * style_traits.spectral_softclip_mix_scale) +
            bandpassed * (kSpectralBandMix * style_traits.spectral_band_mix_scale) +
            saturated * (kSpectralSoftClipMix * style_traits.spectral_softclip_mix_scale);
        (*pcm)[index] = static_cast<int16_t>(
            std::lround(std::clamp(textured, -1.0, 1.0) * kPcmScale));
        MaybeReportCompletedSampleProgress(
            progress_sink,
            EncodeProgressPhase::kPostprocessing,
            progress_begin,
            progress_end,
            completed_samples_before + index + 1,
            total_work_samples,
            &last_reported_samples);
    }

    MaybeReportCompletedSampleProgress(
        progress_sink,
        EncodeProgressPhase::kPostprocessing,
        progress_begin,
        progress_end,
        completed_samples_before + pcm->size(),
        total_work_samples,
        &last_reported_samples,
        true);
}

double ComputeBoundaryClick(double gain,
                            const VoicingStyleTraits& style_traits,
                            size_t chunk_index,
                            size_t local_index,
                            size_t click_window) {
    const double accent_scale = IsByteBoundaryChunk(chunk_index)
                                    ? style_traits.byte_boundary_click_scale
                                    : (IsNibbleBoundaryChunk(chunk_index)
                                           ? style_traits.nibble_boundary_accent_factor
                                           : 0.0);
    if (gain <= 0.0 || accent_scale <= 0.0 || click_window == 0 || local_index >= click_window) {
        return 0.0;
    }

    const double phase = static_cast<double>(local_index) / static_cast<double>(click_window);
    const double primary_burst = ComputeBurstWindow(phase, 0.00, 0.34);
    const double secondary_burst = 0.54 * ComputeBurstWindow(phase, 0.46, 0.80);
    const double decay = 1.0 - phase;
    const double carrier = std::sin(2.0 * kPi * (3.25 + accent_scale) * phase);
    return gain * accent_scale * carrier * (primary_burst + secondary_burst) * decay;
}

double ComputeSegmentEnvelope(size_t local_index,
                              size_t sample_count,
                              double rise_ratio,
                              double fall_ratio) {
    if (sample_count == 0) {
        return 0.0;
    }

    const size_t rise_count = RatioToCount(rise_ratio, sample_count);
    const size_t fall_count = RatioToCount(fall_ratio, sample_count);
    auto [fitted_rise_count, fitted_fall_count] =
        FitAttackReleaseCounts(sample_count, rise_count, fall_count);
    return ComputeEnvelope(local_index, sample_count, fitted_rise_count, fitted_fall_count);
}

double ComputePhraseMask(double progress,
                         double first_begin,
                         double first_end,
                         double second_begin,
                         double second_end,
                         double third_begin,
                         double third_end) {
    return std::max(
        ComputeBurstWindow(progress, first_begin, first_end),
        std::max(
            ComputeBurstWindow(progress, second_begin, second_end),
            ComputeBurstWindow(progress, third_begin, third_end)));
}

template <std::size_t N>
double ComputeCadenceMask(double progress,
                          const std::array<CadenceWindow, N>& windows) {
    double mask = 0.0;
    for (const CadenceWindow& window : windows) {
        mask = std::max(mask, ComputeBurstWindow(progress, window.begin, window.end));
    }
    return mask;
}

double ComputeTerminalMute(double progress,
                           double begin,
                           double end) {
    if (end <= begin || progress <= begin) {
        return 1.0;
    }
    if (progress >= end) {
        return 0.0;
    }

    const double local = std::clamp((progress - begin) / (end - begin), 0.0, 1.0);
    return std::cos(local * kPi * 0.5);
}

double ComputePreamblePhraseMask(size_t local_index,
                                 size_t sample_count,
                                 const VoicingStyleTraits& style_traits) {
    if (sample_count == 0 || style_traits.preamble_phrase_mask_mix <= 0.0) {
        return 1.0;
    }

    const double progress =
        static_cast<double>(local_index) / static_cast<double>(sample_count);
    double phrased_mask = 1.0;
    switch (style_traits.preamble_cadence_profile) {
    case FixedShellCadenceProfile::kRitualChant:
        phrased_mask = ComputePhraseMask(
            progress,
            0.03, 0.24,
            0.31, 0.59,
            0.66, 0.95);
        break;
    case FixedShellCadenceProfile::kProtocolHandshake:
    default:
        phrased_mask = ComputeCadenceMask(
            progress,
            std::array<CadenceWindow, 3>{
                CadenceWindow{0.05, 0.18},
                CadenceWindow{0.30, 0.43},
                CadenceWindow{0.58, 0.70},
            });
        break;
    }
    return (1.0 - style_traits.preamble_phrase_mask_mix) +
           style_traits.preamble_phrase_mask_mix * phrased_mask;
}

double ComputeEpiloguePhraseMask(size_t local_index,
                                 size_t sample_count,
                                 const VoicingStyleTraits& style_traits) {
    if (sample_count == 0 || style_traits.epilogue_phrase_mask_mix <= 0.0) {
        return 1.0;
    }

    const double progress =
        static_cast<double>(local_index) / static_cast<double>(sample_count);
    double phrased_mask = 1.0;
    switch (style_traits.epilogue_cadence_profile) {
    case FixedShellCadenceProfile::kRitualChant:
        phrased_mask = std::max(
            ComputeBurstWindow(progress, 0.08, 0.46),
            ComputeBurstWindow(progress, 0.56, 0.94));
        break;
    case FixedShellCadenceProfile::kProtocolHandshake:
    default:
        phrased_mask = ComputeCadenceMask(
            progress,
            std::array<CadenceWindow, 2>{
                CadenceWindow{0.12, 0.32},
                CadenceWindow{0.54, 0.68},
            });
        break;
    }
    return (1.0 - style_traits.epilogue_phrase_mask_mix) +
           style_traits.epilogue_phrase_mask_mix * phrased_mask;
}

double RenderFixedPreambleSample(size_t local_index,
                                 size_t sample_count,
                                 const VoicingStyleTraits& style_traits,
                                 int sample_rate_hz) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double progress = static_cast<double>(local_index) / static_cast<double>(sample_count);
    const double envelope = ComputeSegmentEnvelope(
        local_index,
        sample_count,
        style_traits.preamble_rise_ratio,
        style_traits.preamble_fall_ratio);
    const double grouped_burst = ComputeGroupedBurstPattern(local_index, sample_count);
    const double pulse_gain =
        0.02 + 0.98 * grouped_burst * style_traits.preamble_grouped_burst_mix;
    double carrier = 0.0;
    double shell = 0.0;
    switch (style_traits.preamble_timbre_profile) {
    case FixedShellTimbreProfile::kProtocolBoundary:
        carrier = 0.34 * std::sin(2.0 * kPi * 680.0 * time) +
                  0.18 * std::sin(2.0 * kPi * 1360.0 * time) +
                  0.08 * std::sin(2.0 * kPi * 2040.0 * time);
        shell = style_traits.preamble_invocation_shell_mix *
                (ComputeProtocolLeadSweep(local_index, sample_count, sample_rate_hz) +
                 ComputeProtocolLeadLockPulse(local_index, sample_count, sample_rate_hz));
        break;
    case FixedShellTimbreProfile::kInvocationClosure:
    default:
        carrier = 0.42 * std::sin(2.0 * kPi * 520.0 * time) +
                  0.16 * std::sin(2.0 * kPi * 1040.0 * time) +
                  0.06 * std::sin(2.0 * kPi * 1560.0 * time);
        shell = style_traits.preamble_invocation_shell_mix *
                (ComputeInvocationSweep(local_index, sample_count, sample_rate_hz) +
                 ComputeInvocationPulse(local_index, sample_count, sample_rate_hz));
        break;
    }
    const double phrase_mask =
        ComputePreamblePhraseMask(local_index, sample_count, style_traits);
    const double terminal_mute = ComputeTerminalMute(
        progress,
        style_traits.preamble_terminal_mute_begin,
        style_traits.preamble_terminal_mute_end);
    return std::clamp(
        (carrier * pulse_gain + shell) * envelope * phrase_mask * terminal_mute,
        -0.75,
        0.75);
}

double RenderFixedEpilogueSample(size_t local_index,
                                 size_t sample_count,
                                 const VoicingStyleTraits& style_traits,
                                 int sample_rate_hz) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return 0.0;
    }

    const double time = static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
    const double progress = static_cast<double>(local_index) / static_cast<double>(sample_count);
    const double decay = std::pow(
        std::max(0.0, 1.0 - progress),
        style_traits.epilogue_decay_exponent);
    const double envelope = ComputeSegmentEnvelope(
        local_index,
        sample_count,
        style_traits.epilogue_rise_ratio,
        style_traits.epilogue_fall_ratio);
    const double grouped_burst = ComputeGroupedBurstPattern(local_index, sample_count);
    const double pulse_gain =
        0.02 + 0.98 * grouped_burst * style_traits.epilogue_grouped_burst_mix;
    double carrier = 0.0;
    double shell = 0.0;
    switch (style_traits.epilogue_timbre_profile) {
    case FixedShellTimbreProfile::kProtocolBoundary:
        carrier = 0.42 * std::sin(2.0 * kPi * 310.0 * time) +
                  0.16 * std::sin(2.0 * kPi * 620.0 * time) +
                  0.07 * std::sin(2.0 * kPi * 930.0 * time);
        shell = style_traits.epilogue_closure_shell_mix *
                (ComputeProtocolClosePulse(local_index, sample_count, sample_rate_hz) +
                 ComputeProtocolAckChirp(local_index, sample_count, sample_rate_hz));
        break;
    case FixedShellTimbreProfile::kInvocationClosure:
    default:
        carrier = 0.36 * std::sin(2.0 * kPi * 360.0 * time) +
                  0.12 * std::sin(2.0 * kPi * 720.0 * time);
        shell = style_traits.epilogue_closure_shell_mix *
                (ComputeClosurePulse(local_index, sample_count, sample_rate_hz) +
                 ComputeClosureTail(local_index, sample_count, sample_rate_hz));
        break;
    }
    const double phrase_mask =
        ComputeEpiloguePhraseMask(local_index, sample_count, style_traits);
    const double terminal_mute = ComputeTerminalMute(
        progress,
        style_traits.epilogue_terminal_mute_begin,
        style_traits.epilogue_terminal_mute_end);
    return std::clamp(
        (carrier * pulse_gain + shell) * envelope * decay * phrase_mask * terminal_mute,
        -0.65,
        0.65);
}

}  // namespace

FlashVoicingConfig MakeFormalVoicingConfigForFlavor(const CoreConfig& config,
                                                    FlashVoicingFlavor flavor) {
    FlashVoicingConfig voicing{};
    voicing.sample_rate_hz = config.sample_rate_hz;
    switch (NormalizeVoicingFlavor(flavor)) {
    case FlashVoicingFlavor::kRitualChant:
        voicing.attack_ratio = 0.05;
        voicing.release_ratio = 0.05;
        voicing.second_harmonic_gain = 0.05;
        voicing.third_harmonic_gain = 0.01;
        voicing.boundary_click_gain = 0.015;
        voicing.enable_preamble = true;
        voicing.enable_epilogue = true;
        voicing.preamble_sample_count =
            config.frame_samples > 0 ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(16)
                                     : static_cast<std::size_t>(0);
        voicing.epilogue_sample_count =
            config.frame_samples > 0 ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(8)
                                     : static_cast<std::size_t>(0);
        break;
    case FlashVoicingFlavor::kCodedBurst:
    default:
        voicing.attack_ratio = 0.08;
        voicing.release_ratio = 0.08;
        voicing.second_harmonic_gain = 0.10;
        voicing.third_harmonic_gain = 0.03;
        voicing.boundary_click_gain = 0.02;
        voicing.enable_preamble = true;
        voicing.enable_epilogue = true;
        voicing.preamble_sample_count =
            config.frame_samples > 0
                ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(3)
                : static_cast<std::size_t>(0);
        voicing.epilogue_sample_count =
            config.frame_samples > 0
                ? static_cast<std::size_t>(config.frame_samples) * static_cast<std::size_t>(3)
                : static_cast<std::size_t>(0);
        break;
    }
    return voicing;
}

FlashVoicingDescriptor DescribeVoicingOutput(std::size_t total_sample_count,
                                             const FlashVoicingConfig& config) {
    const std::size_t leading_nonpayload_samples = LeadingNonpayloadSamples(config);
    const std::size_t trailing_nonpayload_samples = TrailingNonpayloadSamples(config);
    if (leading_nonpayload_samples + trailing_nonpayload_samples > total_sample_count) {
        throw std::invalid_argument("Voicing trim exceeds total sample count.");
    }

    FlashVoicingDescriptor descriptor{};
    descriptor.leading_nonpayload_samples = leading_nonpayload_samples;
    descriptor.trailing_nonpayload_samples = trailing_nonpayload_samples;
    descriptor.payload_sample_count =
        total_sample_count - leading_nonpayload_samples - trailing_nonpayload_samples;
    return descriptor;
}

std::vector<int16_t> TrimToPayloadPcm(const std::vector<int16_t>& voiced_pcm,
                                      const FlashVoicingDescriptor& descriptor) {
    const std::size_t described_total_sample_count = descriptor.leading_nonpayload_samples +
                                                     descriptor.payload_sample_count +
                                                     descriptor.trailing_nonpayload_samples;
    if (described_total_sample_count != voiced_pcm.size()) {
        throw std::invalid_argument("Voicing descriptor does not match PCM sample count.");
    }

    const std::size_t payload_begin = descriptor.leading_nonpayload_samples;
    const std::size_t payload_end = payload_begin + descriptor.payload_sample_count;
    return std::vector<int16_t>(voiced_pcm.begin() + static_cast<std::ptrdiff_t>(payload_begin),
                                voiced_pcm.begin() + static_cast<std::ptrdiff_t>(payload_end));
}

FlashVoicingResult ApplyVoicingToPayload(const std::vector<int16_t>& clean_payload_pcm,
                                         const FlashPayloadLayout& payload_layout,
                                         const FlashVoicingConfig& config,
                                         const EncodeProgressSink* progress_sink,
                                         float progress_begin,
                                         float progress_end) {
    return ApplyVoicingToPayloadWithFlavor(
        clean_payload_pcm,
        payload_layout,
        FlashVoicingFlavor::kCodedBurst,
        config,
        progress_sink,
        progress_begin,
        progress_end);
}

FlashVoicingResult ApplyVoicingToPayloadWithFlavor(const std::vector<int16_t>& clean_payload_pcm,
                                                   const FlashPayloadLayout& payload_layout,
                                                   FlashVoicingFlavor flavor,
                                                   const FlashVoicingConfig& config,
                                                   const EncodeProgressSink* progress_sink,
                                                   float progress_begin,
                                                   float progress_end) {
    ValidatePayloadLayout(clean_payload_pcm, payload_layout);
    const SanitizedVoicingConfig sanitized = SanitizeVoicingConfig(config);
    const VoicingStyleTraits style_traits = MakeFlavorTraits(flavor);
    const bool use_layered_timbre = ShouldApplyLayeredTimbre(config);
    const FlashVoicingDescriptor descriptor = DescribeVoicingOutput(
        clean_payload_pcm.size() + LeadingNonpayloadSamples(config) + TrailingNonpayloadSamples(config),
        config);

    FlashVoicingResult result{};
    result.pcm.resize(
        descriptor.leading_nonpayload_samples + descriptor.payload_sample_count +
            descriptor.trailing_nonpayload_samples,
        0);

    const std::size_t spectral_texture_work =
        ShouldApplySpectralTexture(config) ? result.pcm.size() : static_cast<std::size_t>(0);
    const std::size_t total_postprocess_work =
        descriptor.leading_nonpayload_samples +
        descriptor.payload_sample_count +
        descriptor.trailing_nonpayload_samples +
        spectral_texture_work;

    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPostprocessing, progress_begin);
    ThrowIfEncodeCancelled(progress_sink);
    if (total_postprocess_work == 0) {
        ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPostprocessing, progress_end);
        result.descriptor = descriptor;
        return result;
    }

    std::size_t completed_postprocess_work = 0;
    std::size_t last_reported_postprocess_work = 0;
    for (size_t local_index = 0; local_index < descriptor.leading_nonpayload_samples; ++local_index) {
        if (local_index > 0 && local_index % kProgressReportStrideSamples == 0) {
            ThrowIfEncodeCancelled(progress_sink);
        }
        const double sample = RenderFixedPreambleSample(
            local_index,
            descriptor.leading_nonpayload_samples,
            style_traits,
            sanitized.sample_rate_hz);
        result.pcm[local_index] = static_cast<int16_t>(std::lround(sample * kPcmScale));
        ++completed_postprocess_work;
        MaybeReportCompletedSampleProgress(
            progress_sink,
            EncodeProgressPhase::kPostprocessing,
            progress_begin,
            progress_end,
            completed_postprocess_work,
            total_postprocess_work,
            &last_reported_postprocess_work);
    }
    MaybeReportCompletedSampleProgress(
        progress_sink,
        EncodeProgressPhase::kPostprocessing,
        progress_begin,
        progress_end,
        completed_postprocess_work,
        total_postprocess_work,
        &last_reported_postprocess_work,
        true);

    for (size_t chunk_index = 0; chunk_index < payload_layout.chunks.size(); ++chunk_index) {
        ThrowIfEncodeCancelled(progress_sink);
        const FlashPayloadChunk& chunk = payload_layout.chunks[chunk_index];
        auto [attack_count, release_count] = FitAttackReleaseCounts(
            chunk.sample_count,
            RatioToCount(sanitized.attack_ratio, chunk.sample_count),
            RatioToCount(sanitized.release_ratio * style_traits.payload_release_scale,
                         chunk.sample_count));
        const size_t click_window = ComputeClickWindow(chunk.sample_count);

        for (size_t local_index = 0; local_index < chunk.sample_count; ++local_index) {
            const size_t payload_sample_index = chunk.sample_offset + local_index;
            const size_t sample_index =
                descriptor.leading_nonpayload_samples + payload_sample_index;
            const double clean_sample =
                static_cast<double>(clean_payload_pcm[payload_sample_index]) / kPcmScale;
            const double base_envelope =
                ComputeEnvelope(local_index, chunk.sample_count, attack_count, release_count);
            const double envelope =
                base_envelope + style_traits.payload_envelope_floor * (1.0 - base_envelope);
            const double harmonic_mix =
                envelope * ComputeHarmonicSample(
                               sanitized.second_harmonic_gain,
                               2,
                               chunk.carrier_freq_hz,
                               local_index,
                               sanitized.sample_rate_hz) +
                envelope * ComputeHarmonicSample(
                               sanitized.third_harmonic_gain,
                               3,
                               chunk.carrier_freq_hz,
                               local_index,
                               sanitized.sample_rate_hz);
            const double metallic_layer = use_layered_timbre
                                              ? envelope * ComputeSecondaryMetallicLayer(
                                                               chunk.carrier_freq_hz,
                                                               local_index,
                                                               sanitized.sample_rate_hz) *
                                                    style_traits.metallic_layer_gain_scale
                                              : 0.0;
            const double chant_resonance = use_layered_timbre
                                               ? envelope * ComputeChantResonanceLayer(
                                                                chunk.carrier_freq_hz,
                                                                payload_sample_index,
                                                                sanitized.sample_rate_hz,
                                                                style_traits.chant_resonance_gain)
                                               : 0.0;
            const double boundary_click = ComputeBoundaryClick(
                sanitized.boundary_click_gain,
                style_traits,
                chunk_index,
                local_index,
                click_window);
            const double tremolo = use_layered_timbre
                                       ? ComputePayloadTremolo(
                                             payload_sample_index,
                                             sanitized.sample_rate_hz,
                                             style_traits)
                                       : 1.0;

            const double voiced_sample = std::clamp(
                (clean_sample * envelope + harmonic_mix + metallic_layer + chant_resonance) *
                        tremolo +
                    boundary_click,
                -1.0,
                1.0);
            result.pcm[sample_index] =
                static_cast<int16_t>(std::lround(voiced_sample * kPcmScale));
            ++completed_postprocess_work;
            MaybeReportCompletedSampleProgress(
                progress_sink,
                EncodeProgressPhase::kPostprocessing,
                progress_begin,
                progress_end,
                completed_postprocess_work,
                total_postprocess_work,
                &last_reported_postprocess_work);
        }
        MaybeReportCompletedSampleProgress(
            progress_sink,
            EncodeProgressPhase::kPostprocessing,
            progress_begin,
            progress_end,
            completed_postprocess_work,
            total_postprocess_work,
            &last_reported_postprocess_work,
            true);
    }

    for (size_t local_index = 0; local_index < descriptor.trailing_nonpayload_samples; ++local_index) {
        if (local_index > 0 && local_index % kProgressReportStrideSamples == 0) {
            ThrowIfEncodeCancelled(progress_sink);
        }
        const double sample = RenderFixedEpilogueSample(
            local_index,
            descriptor.trailing_nonpayload_samples,
            style_traits,
            sanitized.sample_rate_hz);
        const size_t sample_index =
            descriptor.leading_nonpayload_samples + descriptor.payload_sample_count + local_index;
        result.pcm[sample_index] = static_cast<int16_t>(std::lround(sample * kPcmScale));
        ++completed_postprocess_work;
        MaybeReportCompletedSampleProgress(
            progress_sink,
            EncodeProgressPhase::kPostprocessing,
            progress_begin,
            progress_end,
            completed_postprocess_work,
            total_postprocess_work,
            &last_reported_postprocess_work);
    }
    MaybeReportCompletedSampleProgress(
        progress_sink,
        EncodeProgressPhase::kPostprocessing,
        progress_begin,
        progress_end,
        completed_postprocess_work,
        total_postprocess_work,
        &last_reported_postprocess_work,
        true);

    result.descriptor = descriptor;
    ThrowIfEncodeCancelled(progress_sink);
    if (ShouldApplySpectralTexture(config)) {
        ApplySpectralTexture(
            &result.pcm,
            sanitized.sample_rate_hz,
            style_traits,
            progress_sink,
            progress_begin,
            progress_end,
            completed_postprocess_work,
            total_postprocess_work);
        completed_postprocess_work += spectral_texture_work;
    }
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPostprocessing, progress_end);
    return result;
}

}  // namespace bag::flash
