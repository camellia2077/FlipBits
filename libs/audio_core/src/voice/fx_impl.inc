namespace {

constexpr double kPi = 3.14159265358979323846;
constexpr double kInputScale = 1.0 / 32768.0;
constexpr double kOutputScale = 32767.0;

struct MainVoiceTuning {
    double high_pass_hz;
    double low_pass_hz;
    double am_depth;
    double am_frequency_hz;
    double am_secondary_depth;
    double am_secondary_frequency_hz;
    double pre_gain;
    double compressor_threshold;
    double compressor_ratio;
    double compressor_makeup;
    double limiter_drive;
    double output_trim;
    double comb_delay_ms;
    double comb_feedback;
    double comb_mix;
    double slap_delay_ms;
    double slap_feedback;
    double slap_mix;
    double resonant_mix;
    double resonant_frequency_hz;
    double resonant_q;
    double throat_formant_mix;
    double throat_formant_a_hz;
    double throat_formant_a_q;
    double throat_formant_b_hz;
    double throat_formant_b_q;
    double cabinet_resonant_mix;
    double cabinet_resonant_frequency_hz;
    double cabinet_resonant_q;
    double pitch_bias_mix;
    double pitch_bias_frequency_hz;
    double pitch_bias_second_harmonic_mix;
    double pitch_bias_third_harmonic_mix;
    double pitch_tracking_min_hz;
    double pitch_tracking_max_hz;
    double pitch_tracking_frame_ms;
    double pitch_tracking_hop_ms;
    double pitch_tracking_voiced_threshold;
    double pitch_flatten_strength;
    double pitch_bias_gate_threshold;
    double pitch_bias_attack_ms;
    double pitch_bias_release_ms;
    double pitch_shift_down_cents;
    double pitch_shift_mix;
    double parallel_detune_mix;
    double parallel_detune_cents;
    double chopper_depth;
    double chopper_frequency_hz;
    double static_mix;
    double static_gate_threshold;
    double static_gate_attack_ms;
    double static_gate_release_ms;
    double transient_soften_ms;
    double transient_soften_mix;
};

struct MachineParallelLayerTuning {
    double mix;
    double resonator_a_hz;
    double resonator_a_q;
    double resonator_b_hz;
    double resonator_b_q;
    double resonator_mix;
    double comb_delay_ms;
    double comb_feedback;
    double comb_mix;
    double pitch_bias_mix;
    double pitch_bias_frequency_hz;
    double pitch_bias_second_harmonic_mix;
    double pitch_bias_third_harmonic_mix;
    double static_mix;
    double gate_threshold;
};

struct GatedFlashSubvoiceTuning {
    double attack_ms;
    double release_ms;
    double gate_floor;
    double gate_open_threshold;
    double gate_full_threshold;
    double delay_ms;
    double dry_flash_mix;
    double output_gain;
    double envelope_floor;
    double envelope_drive;
    double envelope_curve;
    double resonator_mix;
    double resonator_a_hz;
    double resonator_a_q;
    double resonator_b_hz;
    double resonator_b_q;
};

struct SignalCantTuning {
    double onset_delta_threshold;
    double onset_level_threshold;
    double minimum_gap_ms;
    double cluster_spacing_ms;
    double signal_window_ms;
    double flash_burst_ms;
    double dtmf_burst_ms;
    double chirp_burst_ms;
    double flash_gain;
    double dtmf_gain;
    double chirp_gain;
    double overlay_mix;
    double output_trim;
    double main_duck;
    double window_gain;
};

double ClampUnit(const double value) {
    return std::clamp(value, -1.0, 1.0);
}

double CompressSample(const double input,
                      const double threshold,
                      const double ratio,
                      const double makeup) {
    const double sign = input < 0.0 ? -1.0 : 1.0;
    const double amplitude = std::abs(input);
    double compressed = amplitude;
    if (amplitude > threshold) {
        compressed = threshold + (amplitude - threshold) / ratio;
    }
    return sign * compressed * makeup;
}

double DeterministicNoise(const std::size_t index) {
    std::uint32_t state =
        static_cast<std::uint32_t>(index * 1103515245ULL + 12345ULL);
    state ^= state >> 13U;
    state *= 1274126177U;
    const double normalized =
        static_cast<double>(state & 0xFFFFU) / 32767.5 - 1.0;
    return normalized;
}

double HighPassStep(
    const double input,
    const double alpha,
    double* previous_input,
    double* previous_output) {
    const double output = alpha * (*previous_output + input - *previous_input);
    *previous_input = input;
    *previous_output = output;
    return output;
}

double LowPassStep(
    const double input,
    const double alpha,
    double* previous_output) {
    const double output = *previous_output + alpha * (input - *previous_output);
    *previous_output = output;
    return output;
}

std::size_t DelaySamplesForRate(
    const int sample_rate_hz,
    const double delay_ms) {
    return static_cast<std::size_t>(std::max(
        1L,
        std::lround(static_cast<double>(sample_rate_hz) * (delay_ms / 1000.0))));
}

double HighPassAlpha(
    const int sample_rate_hz,
    const double cutoff_hz) {
    const double dt = 1.0 / static_cast<double>(sample_rate_hz);
    const double rc = 1.0 / (2.0 * kPi * cutoff_hz);
    return rc / (rc + dt);
}

double LowPassAlpha(
    const int sample_rate_hz,
    const double cutoff_hz) {
    const double dt = 1.0 / static_cast<double>(sample_rate_hz);
    const double rc = 1.0 / (2.0 * kPi * cutoff_hz);
    return dt / (rc + dt);
}

double ResonatorStep(
    const double input,
    const double frequency_hz,
    const double q,
    const int sample_rate_hz,
    double* state_1,
    double* state_2) {
    const double omega =
        2.0 * kPi * frequency_hz / static_cast<double>(sample_rate_hz);
    const double alpha = std::sin(omega) / (2.0 * q);
    const double a0 = 1.0 + alpha;
    const double b0 = alpha / a0;
    const double b1 = 0.0;
    const double b2 = -alpha / a0;
    const double a1 = (-2.0 * std::cos(omega)) / a0;
    const double a2 = (1.0 - alpha) / a0;
    const double output = b0 * input + *state_1;
    *state_1 = b1 * input - a1 * output + *state_2;
    *state_2 = b2 * input - a2 * output;
    return output;
}

double PitchModStep(
    const std::vector<double>& source,
    const std::size_t index,
    const double cents) {
    const double ratio = std::pow(2.0, cents / 1200.0);
    const double source_index = static_cast<double>(index) / ratio;
    const auto base_index = static_cast<std::size_t>(source_index);
    if (base_index + 1 >= source.size()) {
        return source.back();
    }
    const double frac = source_index - static_cast<double>(base_index);
    return source[base_index] * (1.0 - frac) + source[base_index + 1] * frac;
}

double TimeToCoeff(const int sample_rate_hz, const double time_ms) {
    const double samples =
        std::max(1.0, static_cast<double>(sample_rate_hz) * time_ms / 1000.0);
    return std::exp(-1.0 / samples);
}

#include "fx_pitch_tracking.inc"

MainVoiceTuning MainTuningForPreset(const bag::VoiceFxPreset preset) {
    switch (preset) {
        case bag::VoiceFxPreset::kMachineVoice:
            return MainVoiceTuning{
                .high_pass_hz = 280.0,
                .low_pass_hz = 2620.0,
                .am_depth = 0.56,
                .am_frequency_hz = 78.0,
                .am_secondary_depth = 0.18,
                .am_secondary_frequency_hz = 144.0,
                .pre_gain = 3.45,
                .compressor_threshold = 0.19,
                .compressor_ratio = 2.9,
                .compressor_makeup = 1.26,
                .limiter_drive = 1.38,
                .output_trim = 0.93,
                .comb_delay_ms = 17.5,
                .comb_feedback = 0.26,
                .comb_mix = 0.2,
                .slap_delay_ms = 8.0,
                .slap_feedback = 0.08,
                .slap_mix = 0.13,
                .resonant_mix = 0.22,
                .resonant_frequency_hz = 1460.0,
                .resonant_q = 2.7,
                .throat_formant_mix = 0.24,
                .throat_formant_a_hz = 560.0,
                .throat_formant_a_q = 1.3,
                .throat_formant_b_hz = 1180.0,
                .throat_formant_b_q = 1.55,
                .cabinet_resonant_mix = 0.15,
                .cabinet_resonant_frequency_hz = 910.0,
                .cabinet_resonant_q = 1.85,
                .pitch_bias_mix = 0.21,
                .pitch_bias_frequency_hz = 102.0,
                .pitch_bias_second_harmonic_mix = 0.27,
                .pitch_bias_third_harmonic_mix = 0.0,
                .pitch_tracking_min_hz = 82.0,
                .pitch_tracking_max_hz = 255.0,
                .pitch_tracking_frame_ms = 28.0,
                .pitch_tracking_hop_ms = 10.0,
                .pitch_tracking_voiced_threshold = 0.58,
                .pitch_flatten_strength = 0.74,
                .pitch_bias_gate_threshold = 0.028,
                .pitch_bias_attack_ms = 7.0,
                .pitch_bias_release_ms = 42.0,
                .pitch_shift_down_cents = 56.0,
                .pitch_shift_mix = 0.18,
                .parallel_detune_mix = 0.11,
                .parallel_detune_cents = -16.0,
                .chopper_depth = 0.05,
                .chopper_frequency_hz = 8.5,
                .static_mix = 0.016,
                .static_gate_threshold = 0.03,
                .static_gate_attack_ms = 6.0,
                .static_gate_release_ms = 38.0,
                .transient_soften_ms = 2.8,
                .transient_soften_mix = 0.24,
            };
        case bag::VoiceFxPreset::kBinaricCant:
            return MainVoiceTuning{
                .high_pass_hz = 320.0,
                .low_pass_hz = 2550.0,
                .am_depth = 0.42,
                .am_frequency_hz = 78.0,
                .am_secondary_depth = 0.12,
                .am_secondary_frequency_hz = 146.0,
                .pre_gain = 3.05,
                .compressor_threshold = 0.22,
                .compressor_ratio = 2.0,
                .compressor_makeup = 1.12,
                .limiter_drive = 1.32,
                .output_trim = 0.84,
                .comb_delay_ms = 16.5,
                .comb_feedback = 0.3,
                .comb_mix = 0.27,
                .slap_delay_ms = 7.0,
                .slap_feedback = 0.08,
                .slap_mix = 0.16,
                .resonant_mix = 0.37,
                .resonant_frequency_hz = 1450.0,
                .resonant_q = 3.1,
                .throat_formant_mix = 0.34,
                .throat_formant_a_hz = 560.0,
                .throat_formant_a_q = 1.55,
                .throat_formant_b_hz = 1240.0,
                .throat_formant_b_q = 1.9,
                .cabinet_resonant_mix = 0.23,
                .cabinet_resonant_frequency_hz = 910.0,
                .cabinet_resonant_q = 2.25,
                .pitch_bias_mix = 0.26,
                .pitch_bias_frequency_hz = 104.0,
                .pitch_bias_second_harmonic_mix = 0.36,
                .pitch_bias_third_harmonic_mix = 0.12,
                .pitch_tracking_min_hz = 82.0,
                .pitch_tracking_max_hz = 255.0,
                .pitch_tracking_frame_ms = 28.0,
                .pitch_tracking_hop_ms = 10.0,
                .pitch_tracking_voiced_threshold = 0.58,
                .pitch_flatten_strength = 0.82,
                .pitch_bias_gate_threshold = 0.028,
                .pitch_bias_attack_ms = 6.0,
                .pitch_bias_release_ms = 44.0,
                .pitch_shift_down_cents = 60.0,
                .pitch_shift_mix = 0.15,
                .parallel_detune_mix = 0.08,
                .parallel_detune_cents = -17.0,
                .chopper_depth = 0.055,
                .chopper_frequency_hz = 8.8,
                .static_mix = 0.0035,
                .static_gate_threshold = 0.038,
                .static_gate_attack_ms = 5.0,
                .static_gate_release_ms = 28.0,
                .transient_soften_ms = 2.0,
                .transient_soften_mix = 0.14,
            };
        case bag::VoiceFxPreset::kSignalCant:
            return MainVoiceTuning{
                .high_pass_hz = 340.0,
                .low_pass_hz = 2450.0,
                .am_depth = 0.38,
                .am_frequency_hz = 76.0,
                .am_secondary_depth = 0.08,
                .am_secondary_frequency_hz = 138.0,
                .pre_gain = 2.95,
                .compressor_threshold = 0.2,
                .compressor_ratio = 2.4,
                .compressor_makeup = 1.15,
                .limiter_drive = 1.22,
                .output_trim = 0.9,
                .comb_delay_ms = 11.0,
                .comb_feedback = 0.14,
                .comb_mix = 0.09,
                .slap_delay_ms = 5.5,
                .slap_feedback = 0.04,
                .slap_mix = 0.06,
                .resonant_mix = 0.16,
                .resonant_frequency_hz = 1640.0,
                .resonant_q = 3.2,
                .throat_formant_mix = 0.24,
                .throat_formant_a_hz = 620.0,
                .throat_formant_a_q = 1.65,
                .throat_formant_b_hz = 1320.0,
                .throat_formant_b_q = 2.15,
                .cabinet_resonant_mix = 0.04,
                .cabinet_resonant_frequency_hz = 1180.0,
                .cabinet_resonant_q = 1.35,
                .pitch_bias_mix = 0.28,
                .pitch_bias_frequency_hz = 92.0,
                .pitch_bias_second_harmonic_mix = 0.34,
                .pitch_bias_third_harmonic_mix = 0.08,
                .pitch_tracking_min_hz = 78.0,
                .pitch_tracking_max_hz = 240.0,
                .pitch_tracking_frame_ms = 30.0,
                .pitch_tracking_hop_ms = 10.0,
                .pitch_tracking_voiced_threshold = 0.56,
                .pitch_flatten_strength = 0.82,
                .pitch_bias_gate_threshold = 0.026,
                .pitch_bias_attack_ms = 7.0,
                .pitch_bias_release_ms = 44.0,
                .pitch_shift_down_cents = 78.0,
                .pitch_shift_mix = 0.22,
                .parallel_detune_mix = 0.035,
                .parallel_detune_cents = -10.0,
                .chopper_depth = 0.025,
                .chopper_frequency_hz = 9.0,
                .static_mix = 0.0025,
                .static_gate_threshold = 0.04,
                .static_gate_attack_ms = 5.0,
                .static_gate_release_ms = 24.0,
                .transient_soften_ms = 2.0,
                .transient_soften_mix = 0.12,
            };
        case bag::VoiceFxPreset::kRobotVox:
            return MainVoiceTuning{
                .high_pass_hz = 260.0,
                .low_pass_hz = 2200.0,
                .am_depth = 0.42,
                .am_frequency_hz = 52.0,
                .am_secondary_depth = 0.02,
                .am_secondary_frequency_hz = 104.0,
                .pre_gain = 2.85,
                .compressor_threshold = 0.2,
                .compressor_ratio = 2.7,
                .compressor_makeup = 1.18,
                .limiter_drive = 1.3,
                .output_trim = 0.92,
                .comb_delay_ms = 12.5,
                .comb_feedback = 0.16,
                .comb_mix = 0.1,
                .slap_delay_ms = 96.0,
                .slap_feedback = 0.16,
                .slap_mix = 0.27,
                .resonant_mix = 0.08,
                .resonant_frequency_hz = 1120.0,
                .resonant_q = 1.45,
                .throat_formant_mix = 0.1,
                .throat_formant_a_hz = 710.0,
                .throat_formant_a_q = 0.9,
                .throat_formant_b_hz = 1560.0,
                .throat_formant_b_q = 1.0,
                .cabinet_resonant_mix = 0.05,
                .cabinet_resonant_frequency_hz = 1180.0,
                .cabinet_resonant_q = 1.15,
                .pitch_bias_mix = 0.4,
                .pitch_bias_frequency_hz = 84.0,
                .pitch_bias_second_harmonic_mix = 0.48,
                .pitch_bias_third_harmonic_mix = 0.2,
                .pitch_tracking_min_hz = 76.0,
                .pitch_tracking_max_hz = 225.0,
                .pitch_tracking_frame_ms = 28.0,
                .pitch_tracking_hop_ms = 10.0,
                .pitch_tracking_voiced_threshold = 0.55,
                .pitch_flatten_strength = 0.96,
                .pitch_bias_gate_threshold = 0.026,
                .pitch_bias_attack_ms = 3.5,
                .pitch_bias_release_ms = 68.0,
                .pitch_shift_down_cents = 110.0,
                .pitch_shift_mix = 0.32,
                .parallel_detune_mix = 0.025,
                .parallel_detune_cents = -9.0,
                .chopper_depth = 0.08,
                .chopper_frequency_hz = 5.6,
                .static_mix = 0.002,
                .static_gate_threshold = 0.034,
                .static_gate_attack_ms = 5.0,
                .static_gate_release_ms = 28.0,
                .transient_soften_ms = 1.8,
                .transient_soften_mix = 0.12,
            };
        case bag::VoiceFxPreset::kRawConstant:
        case bag::VoiceFxPreset::kVoiceTrigger:
            break;
    }
    return MainTuningForPreset(bag::VoiceFxPreset::kMachineVoice);
}

MachineParallelLayerTuning MachineVoiceParallelLayerTuning() {
    return MachineParallelLayerTuning{
        .mix = 0.18,
        .resonator_a_hz = 760.0,
        .resonator_a_q = 5.2,
        .resonator_b_hz = 1540.0,
        .resonator_b_q = 4.6,
        .resonator_mix = 0.58,
        .comb_delay_ms = 13.5,
        .comb_feedback = 0.28,
        .comb_mix = 0.34,
        .pitch_bias_mix = 0.18,
        .pitch_bias_frequency_hz = 96.0,
        .pitch_bias_second_harmonic_mix = 0.32,
        .pitch_bias_third_harmonic_mix = 0.1,
        .static_mix = 0.007,
        .gate_threshold = 0.024,
    };
}

GatedFlashSubvoiceTuning GatedFlashSubvoiceStandardTuning() {
    return GatedFlashSubvoiceTuning{
        .attack_ms = 5.0,
        .release_ms = 86.0,
        .gate_floor = 0.12,
        .gate_open_threshold = 0.022,
        .gate_full_threshold = 0.11,
        .delay_ms = 11.0,
        .dry_flash_mix = 0.18,
        .output_gain = 0.62,
        .envelope_floor = 0.38,
        .envelope_drive = 9.0,
        .envelope_curve = 1.0,
        .resonator_mix = 0.0,
        .resonator_a_hz = 720.0,
        .resonator_a_q = 4.0,
        .resonator_b_hz = 1480.0,
        .resonator_b_q = 4.0,
    };
}

GatedFlashSubvoiceTuning GatedFlashSubvoiceBinaricCantTuning() {
    return GatedFlashSubvoiceTuning{
        .attack_ms = 3.0,
        .release_ms = 54.0,
        .gate_floor = 0.045,
        .gate_open_threshold = 0.018,
        .gate_full_threshold = 0.105,
        .delay_ms = 12.5,
        .dry_flash_mix = 0.14,
        .output_gain = 0.66,
        .envelope_floor = 0.18,
        .envelope_drive = 13.0,
        .envelope_curve = 0.82,
        .resonator_mix = 0.42,
        .resonator_a_hz = 680.0,
        .resonator_a_q = 6.2,
        .resonator_b_hz = 1520.0,
        .resonator_b_q = 7.0,
    };
}

SignalCantTuning SignalCantOverlayTuning() {
    return SignalCantTuning{
        .onset_delta_threshold = 0.0075,
        .onset_level_threshold = 0.028,
        .minimum_gap_ms = 160.0,
        .cluster_spacing_ms = 30.0,
        .signal_window_ms = 150.0,
        .flash_burst_ms = 52.0,
        .dtmf_burst_ms = 58.0,
        .chirp_burst_ms = 44.0,
        .flash_gain = 0.46,
        .dtmf_gain = 0.42,
        .chirp_gain = 0.38,
        .overlay_mix = 0.42,
        .output_trim = 0.86,
        .main_duck = 0.14,
        .window_gain = 1.28,
    };
}

#include "fx_main_voice_render.inc"

std::vector<double> ExtractVoiceEnvelope(
    const std::vector<std::int16_t>& input_pcm,
    const int sample_rate_hz) {
    std::vector<double> envelope(input_pcm.size(), 0.0);
    const double attack_coeff = TimeToCoeff(sample_rate_hz, 4.0);
    const double release_coeff = TimeToCoeff(sample_rate_hz, 72.0);
    double current = 0.0;
    for (std::size_t index = 0; index < input_pcm.size(); ++index) {
        const double target = std::abs(static_cast<double>(input_pcm[index]) * kInputScale);
        const double coeff = target > current ? attack_coeff : release_coeff;
        current = coeff * current + (1.0 - coeff) * target;
        envelope[index] = current;
    }
    return envelope;
}

#include "fx_signal_layers.inc"
std::vector<std::int16_t> ToPcm16(const std::vector<double>& input) {
    std::vector<std::int16_t> output(input.size(), 0);
    for (std::size_t index = 0; index < input.size(); ++index) {
        output[index] = static_cast<std::int16_t>(
            std::lround(ClampUnit(input[index]) * kOutputScale));
    }
    return output;
}

std::vector<double> ToNormalizedPcm(const std::vector<std::int16_t>& input_pcm) {
    std::vector<double> output(input_pcm.size(), 0.0);
    for (std::size_t index = 0; index < input_pcm.size(); ++index) {
        output[index] = static_cast<double>(input_pcm[index]) * kInputScale;
    }
    return output;
}

std::vector<double> MixBinaricCantVoices(
    const std::vector<double>& main_voice,
    const std::vector<double>& subvoice) {
    std::vector<double> mixed(main_voice.size(), 0.0);
    for (std::size_t index = 0; index < mixed.size(); ++index) {
        const double wet = main_voice[index] * 0.84 + subvoice[index] * 0.52;
        mixed[index] = std::tanh(1.38 * wet) * 0.98;
    }
    return mixed;
}

std::vector<double> MixBinaricCantLitanyVoices(
    const std::vector<double>& main_voice,
    const std::vector<double>& subvoice,
    const int sample_rate_hz) {
    std::vector<double> mixed(main_voice.size(), 0.0);
    const std::size_t tail_delay_samples =
        DelaySamplesForRate(sample_rate_hz, 58.0);
    std::vector<double> tail_delay(tail_delay_samples, 0.0);
    for (std::size_t index = 0; index < mixed.size(); ++index) {
        const std::size_t tail_index = index % tail_delay_samples;
        const double tail_sample = tail_delay[tail_index];
        const double flattened_main = std::tanh(main_voice[index] * 1.12) * 0.92;
        tail_delay[tail_index] = flattened_main + tail_sample * 0.14;
        const double chant_main =
            flattened_main * 0.9 + tail_sample * 0.1;
        const double wet = chant_main * 0.78 + subvoice[index] * 0.58;
        mixed[index] = std::tanh(1.32 * wet) * 0.96;
    }
    return mixed;
}

std::vector<double> MixRawVoices(
    const std::vector<double>& main_voice,
    const std::vector<double>& subvoice) {
    std::vector<double> mixed(main_voice.size(), 0.0);
    for (std::size_t index = 0; index < mixed.size(); ++index) {
        mixed[index] = ClampUnit(main_voice[index] * 0.74 + subvoice[index] * 0.24);
    }
    return mixed;
}

std::vector<double> MixVoiceTriggerVoices(
    const std::vector<double>& main_voice,
    const std::vector<double>& subvoice,
    const bag::VoiceFxSubvoiceStyle subvoice_style) {
    const double subvoice_mix =
        subvoice_style == bag::VoiceFxSubvoiceStyle::kLitany ? 0.34 : 0.24;
    const double main_mix =
        subvoice_style == bag::VoiceFxSubvoiceStyle::kLitany ? 0.7 : 0.74;
    std::vector<double> mixed(main_voice.size(), 0.0);
    for (std::size_t index = 0; index < mixed.size(); ++index) {
        mixed[index] = ClampUnit(main_voice[index] * main_mix + subvoice[index] * subvoice_mix);
    }
    return mixed;
}

std::vector<double> MixSignalCantVoices(
    const std::vector<double>& main_voice,
    const std::vector<double>& signal_overlay) {
    std::vector<double> mixed(main_voice.size(), 0.0);
    const SignalCantTuning tuning = SignalCantOverlayTuning();
    for (std::size_t index = 0; index < mixed.size(); ++index) {
        const double overlay_presence =
            std::min(1.0, std::abs(signal_overlay[index]) * 2.4);
        const double duck =
            1.0 - overlay_presence * tuning.main_duck;
        const double wet =
            main_voice[index] * duck +
            signal_overlay[index] * tuning.overlay_mix;
        mixed[index] = std::tanh(1.35 * wet) * tuning.output_trim;
    }
    return mixed;
}

std::vector<double> MixDualTrackVoices(
    const bag::VoiceFxConfig& config,
    const std::vector<double>& main_voice,
    const std::vector<double>& subvoice) {
    if (config.preset == bag::VoiceFxPreset::kVoiceTrigger) {
        return MixVoiceTriggerVoices(main_voice, subvoice, config.subvoice_style);
    }
    if (config.preset == bag::VoiceFxPreset::kRawConstant) {
        return MixRawVoices(main_voice, subvoice);
    }
    if (config.subvoice_style == bag::VoiceFxSubvoiceStyle::kLitany) {
        return MixBinaricCantLitanyVoices(main_voice, subvoice, config.sample_rate_hz);
    }
    return MixBinaricCantVoices(main_voice, subvoice);
}

void ClearVoiceFxResult(bag::VoiceFxResult* out_result) {
    if (out_result == nullptr) {
        return;
    }
    out_result->final_mix.clear();
    out_result->main_voice.clear();
    out_result->subvoice.clear();
    out_result->signal_overlay.clear();
}

bag::ErrorCode ValidateVoiceFxConfig(const bag::VoiceFxConfig& config) {
    if (config.sample_rate_hz <= 0) {
        return bag::ErrorCode::kInvalidArgument;
    }
    if (config.preset != bag::VoiceFxPreset::kMachineVoice &&
        config.preset != bag::VoiceFxPreset::kBinaricCant &&
        config.preset != bag::VoiceFxPreset::kSignalCant &&
        config.preset != bag::VoiceFxPreset::kRobotVox &&
        config.preset != bag::VoiceFxPreset::kRawConstant &&
        config.preset != bag::VoiceFxPreset::kVoiceTrigger) {
        return bag::ErrorCode::kInvalidArgument;
    }
    if (config.subvoice_style != bag::VoiceFxSubvoiceStyle::kStandard &&
        config.subvoice_style != bag::VoiceFxSubvoiceStyle::kLitany &&
        config.subvoice_style != bag::VoiceFxSubvoiceStyle::kHostility &&
        config.subvoice_style != bag::VoiceFxSubvoiceStyle::kCollapse &&
        config.subvoice_style != bag::VoiceFxSubvoiceStyle::kZeal &&
        config.subvoice_style != bag::VoiceFxSubvoiceStyle::kVoid) {
        return bag::ErrorCode::kInvalidArgument;
    }
    return bag::ErrorCode::kOk;
}

std::size_t VoiceFxProcessorLookbackSamples(const int sample_rate_hz) {
    return std::max<std::size_t>(
        4096U,
        DelaySamplesForRate(sample_rate_hz, 260.0));
}

std::vector<std::int16_t> TailSamples(
    const std::vector<std::int16_t>& input,
    const std::size_t tail_sample_count) {
    if (tail_sample_count == 0 || input.empty()) {
        return {};
    }
    const std::size_t take_count = std::min(tail_sample_count, input.size());
    return std::vector<std::int16_t>(
        input.end() - static_cast<std::ptrdiff_t>(take_count),
        input.end());
}

void CopyVoiceFxTail(
    const bag::VoiceFxResult& window_result,
    const std::size_t tail_sample_count,
    bag::VoiceFxResult* out_result) {
    if (out_result == nullptr) {
        return;
    }
    ClearVoiceFxResult(out_result);
    out_result->final_mix = TailSamples(window_result.final_mix, tail_sample_count);
    out_result->main_voice = TailSamples(window_result.main_voice, tail_sample_count);
    out_result->subvoice = TailSamples(window_result.subvoice, tail_sample_count);
    out_result->signal_overlay =
        TailSamples(window_result.signal_overlay, tail_sample_count);
}

bag::ErrorCode ProcessVoiceFxWindow(
    const bag::VoiceFxConfig& config,
    const std::vector<std::int16_t>& input_pcm,
    bag::VoiceFxResult* out_result) {
    const bag::ErrorCode validation = ValidateVoiceFxConfig(config);
    if (validation != bag::ErrorCode::kOk || input_pcm.empty() || out_result == nullptr) {
        return bag::ErrorCode::kInvalidArgument;
    }

    ClearVoiceFxResult(out_result);
    const bool is_raw_constant_preset =
        config.preset == bag::VoiceFxPreset::kRawConstant;
    const bool is_voice_trigger_preset =
        config.preset == bag::VoiceFxPreset::kVoiceTrigger;
    const bool uses_dry_main_voice =
        is_raw_constant_preset || is_voice_trigger_preset;
    const std::vector<double> main_voice =
        uses_dry_main_voice
            ? ToNormalizedPcm(input_pcm)
            : RenderMainVoiceBranch(input_pcm, config.sample_rate_hz, config.preset);
    if (uses_dry_main_voice) {
        out_result->main_voice = input_pcm;
    } else {
        out_result->main_voice = ToPcm16(main_voice);
    }

    if (config.preset == bag::VoiceFxPreset::kMachineVoice ||
        config.preset == bag::VoiceFxPreset::kRobotVox) {
        out_result->final_mix = out_result->main_voice;
        if (!config.enable_diagnostics) {
            out_result->main_voice.clear();
        }
        return bag::ErrorCode::kOk;
    }

    if (config.preset == bag::VoiceFxPreset::kSignalCant) {
        const std::vector<double> envelope =
            ExtractVoiceEnvelope(input_pcm, config.sample_rate_hz);
        const std::vector<double> signal_overlay =
            RenderSignalCantOverlay(envelope, config.sample_rate_hz);
        const std::vector<double> final_mix =
            MixSignalCantVoices(main_voice, signal_overlay);
        out_result->final_mix = ToPcm16(final_mix);
        if (config.enable_diagnostics) {
            out_result->signal_overlay = ToPcm16(signal_overlay);
        } else {
            out_result->main_voice.clear();
        }
        return bag::ErrorCode::kOk;
    }

    const std::vector<std::int16_t> raw_subvoice_pcm =
        is_raw_constant_preset
            ? BuildFlashStandardSubvoiceBasePcm(
                input_pcm.size(),
                config.sample_rate_hz,
                config.subvoice_style)
            : std::vector<std::int16_t>{};
    const std::vector<double> subvoice =
        is_raw_constant_preset
            ? ToNormalizedPcm(raw_subvoice_pcm)
            : RenderGatedFlashSubvoiceBranch(
                ExtractVoiceEnvelope(input_pcm, config.sample_rate_hz),
                config.sample_rate_hz,
                config.subvoice_style,
                config.preset == bag::VoiceFxPreset::kBinaricCant
                    ? GatedFlashSubvoiceBinaricCantTuning()
                    : GatedFlashSubvoiceStandardTuning());
    const std::vector<double> final_mix =
        MixDualTrackVoices(config, main_voice, subvoice);

    out_result->final_mix = ToPcm16(final_mix);
    if (config.enable_diagnostics) {
        if (is_raw_constant_preset) {
            out_result->subvoice = raw_subvoice_pcm;
        } else {
            out_result->subvoice = ToPcm16(subvoice);
        }
    } else {
        out_result->main_voice.clear();
        out_result->subvoice.clear();
    }
    return bag::ErrorCode::kOk;
}

}  // namespace

namespace bag {

struct VoiceFxProcessor::Impl {
    explicit Impl(const VoiceFxConfig& config_in)
        : config(config_in),
          lookback_samples(VoiceFxProcessorLookbackSamples(config_in.sample_rate_hz)) {}

    VoiceFxConfig config{};
    std::size_t lookback_samples = 0;
    std::vector<std::int16_t> history;
};

VoiceFxProcessor::VoiceFxProcessor(const VoiceFxConfig& config)
    : impl_(std::make_unique<Impl>(config)) {
    if (ValidateVoiceFxConfig(config) != ErrorCode::kOk) {
        throw std::invalid_argument("invalid voice fx config");
    }
}

VoiceFxProcessor::~VoiceFxProcessor() = default;

VoiceFxProcessor::VoiceFxProcessor(VoiceFxProcessor&&) noexcept = default;

VoiceFxProcessor& VoiceFxProcessor::operator=(VoiceFxProcessor&&) noexcept = default;

ErrorCode VoiceFxProcessor::ProcessBlock(
    const std::vector<std::int16_t>& input_pcm,
    VoiceFxResult* out_result) {
    if (impl_ == nullptr || input_pcm.empty() || out_result == nullptr) {
        return ErrorCode::kInvalidArgument;
    }

    std::vector<std::int16_t> window_input;
    window_input.reserve(impl_->history.size() + input_pcm.size());
    window_input.insert(
        window_input.end(),
        impl_->history.begin(),
        impl_->history.end());
    window_input.insert(
        window_input.end(),
        input_pcm.begin(),
        input_pcm.end());

    VoiceFxResult window_result;
    const ErrorCode code =
        ProcessVoiceFxWindow(impl_->config, window_input, &window_result);
    if (code != ErrorCode::kOk) {
        ClearVoiceFxResult(out_result);
        return code;
    }

    CopyVoiceFxTail(window_result, input_pcm.size(), out_result);

    if (window_input.size() <= impl_->lookback_samples) {
        impl_->history = std::move(window_input);
    } else {
        impl_->history.assign(
            window_input.end() -
                static_cast<std::ptrdiff_t>(impl_->lookback_samples),
            window_input.end());
    }
    return ErrorCode::kOk;
}

ErrorCode VoiceFxProcessor::Flush(VoiceFxResult* out_result) {
    if (impl_ == nullptr || out_result == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    ClearVoiceFxResult(out_result);
    impl_->history.clear();
    return ErrorCode::kOk;
}

std::size_t VoiceFxProcessor::LookbackSamples() const {
    if (impl_ == nullptr) {
        return 0;
    }
    return impl_->lookback_samples;
}

ErrorCode ApplyVoiceFx(const VoiceFxConfig& config,
                       const std::vector<std::int16_t>& input_pcm,
                       VoiceFxResult* out_result) {
    if (input_pcm.empty() || out_result == nullptr) {
        return ErrorCode::kInvalidArgument;
    }
    try {
        VoiceFxProcessor processor(config);
        const ErrorCode process_code = processor.ProcessBlock(input_pcm, out_result);
        if (process_code != ErrorCode::kOk) {
            return process_code;
        }
        VoiceFxResult flush_result;
        return processor.Flush(&flush_result);
    } catch (const std::invalid_argument&) {
        ClearVoiceFxResult(out_result);
        return ErrorCode::kInvalidArgument;
    } catch (...) {
        ClearVoiceFxResult(out_result);
        return ErrorCode::kInternal;
    }
}

}  // namespace bag
