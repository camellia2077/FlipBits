namespace {
#include "fx_tuning_impl.inc"
#include "fx_rules_impl.inc"

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
