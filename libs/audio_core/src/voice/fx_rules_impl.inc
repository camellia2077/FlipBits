std::vector<double> ToNormalizedPcm(const std::vector<std::int16_t>& input_pcm) {
    std::vector<double> output(input_pcm.size(), 0.0);
    for (std::size_t index = 0; index < input_pcm.size(); ++index) {
        output[index] = static_cast<double>(input_pcm[index]) * kInputScale;
    }
    return output;
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
