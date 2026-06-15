double SubvoiceStyleMixGain(const bag::VoiceFxSubvoiceStyle subvoice_style) {
    switch (subvoice_style) {
        case bag::VoiceFxSubvoiceStyle::kZeal:
            return 0.50;
        case bag::VoiceFxSubvoiceStyle::kHostility:
            return 0.42;
        case bag::VoiceFxSubvoiceStyle::kLitany:
            return 1.18;
        case bag::VoiceFxSubvoiceStyle::kCollapse:
        case bag::VoiceFxSubvoiceStyle::kVoid:
        case bag::VoiceFxSubvoiceStyle::kStandard:
        default:
            return 1.0;
    }
}

std::vector<std::int16_t> BuildFlashStandardSubvoiceBasePcm(
    const std::size_t sample_count,
    const int sample_rate_hz,
    const bag::VoiceFxSubvoiceStyle subvoice_style) {
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return {};
    }

    bag::CoreConfig config{};
    config.sample_rate_hz = sample_rate_hz;
    config.frame_samples = std::max(
        1,
        static_cast<int>(std::lround(
            static_cast<double>(sample_rate_hz) * 0.05)));
    switch (subvoice_style) {
        case bag::VoiceFxSubvoiceStyle::kLitany:
            config.flash_signal_profile = bag::FlashSignalProfile::kLitany;
            config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
            break;
        case bag::VoiceFxSubvoiceStyle::kHostility:
            config.flash_signal_profile = bag::FlashSignalProfile::kHostility;
            config.flash_voicing_flavor = bag::FlashVoicingFlavor::kHostility;
            break;
        case bag::VoiceFxSubvoiceStyle::kCollapse:
            config.flash_signal_profile = bag::FlashSignalProfile::kCollapse;
            config.flash_voicing_flavor = bag::FlashVoicingFlavor::kCollapse;
            break;
        case bag::VoiceFxSubvoiceStyle::kZeal:
            config.flash_signal_profile = bag::FlashSignalProfile::kZeal;
            config.flash_voicing_flavor = bag::FlashVoicingFlavor::kZeal;
            break;
        case bag::VoiceFxSubvoiceStyle::kVoid:
            config.flash_signal_profile = bag::FlashSignalProfile::kVoid;
            config.flash_voicing_flavor = bag::FlashVoicingFlavor::kVoid;
            break;
        case bag::VoiceFxSubvoiceStyle::kStandard:
        default:
            config.flash_signal_profile = bag::FlashSignalProfile::kStandard;
            config.flash_voicing_flavor = bag::FlashVoicingFlavor::kStandard;
            break;
    }

    const bag::flash::BfskConfig signal_config =
        bag::flash::MakeBfskConfigForSignalProfile(
            config,
            config.flash_signal_profile);
    const std::size_t estimated_bits =
        std::max<std::size_t>(
            8U,
            (sample_count + signal_config.samples_per_bit - 1U) /
                signal_config.samples_per_bit);
    const std::size_t byte_count =
        std::max<std::size_t>(1U, (estimated_bits + 7U) / 8U);

    std::vector<std::uint8_t> placeholder_bytes(byte_count, 0U);
    for (std::size_t index = 0; index < placeholder_bytes.size(); ++index) {
        placeholder_bytes[index] = (index % 2U) == 0U ? 0xA5U : 0x5AU;
    }

    const bag::flash::FlashPayloadLayout payload_layout =
        bag::flash::BuildPayloadLayoutForVoicing(
            placeholder_bytes,
            signal_config,
            config.flash_voicing_flavor);
    const std::vector<std::int16_t> clean_payload =
        bag::flash::EncodePayloadLayoutToPcm16(payload_layout, signal_config);
    const bag::flash::FlashVoicingConfig voicing_config =
        bag::flash::MakeFormalVoicingConfigForFlavor(
            config,
            config.flash_voicing_flavor);
    const bag::flash::FlashVoicingResult voiced =
        bag::flash::ApplyVoicingToPayloadWithFlavor(
            clean_payload,
            payload_layout,
            config.flash_voicing_flavor,
            voicing_config);
    std::vector<std::int16_t> payload_only =
        bag::flash::TrimToPayloadPcm(voiced.pcm, voiced.descriptor);
    if (payload_only.empty()) {
        return std::vector<std::int16_t>(sample_count, 0);
    }
    if (payload_only.size() < sample_count) {
        std::vector<std::int16_t> repeated(sample_count, 0);
        for (std::size_t index = 0; index < sample_count; ++index) {
            repeated[index] = payload_only[index % payload_only.size()];
        }
        return repeated;
    }
    payload_only.resize(sample_count);
    return payload_only;
}

double BurstWindow(const std::size_t index, const std::size_t sample_count) {
    if (sample_count <= 1) {
        return 1.0;
    }
    const double phase =
        static_cast<double>(index) / static_cast<double>(sample_count - 1U);
    return std::sin(kPi * phase);
}

std::vector<double> BuildDtmfLikeBurst(
    const std::size_t sample_count,
    const int sample_rate_hz) {
    std::vector<double> burst(sample_count, 0.0);
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return burst;
    }
    for (std::size_t index = 0; index < sample_count; ++index) {
        const double time =
            static_cast<double>(index) / static_cast<double>(sample_rate_hz);
        const double sample =
            0.58 * std::sin(2.0 * kPi * 770.0 * time) +
            0.42 * std::sin(2.0 * kPi * 1336.0 * time);
        burst[index] = std::tanh(1.65 * sample) * BurstWindow(index, sample_count);
    }
    return burst;
}

std::vector<double> BuildChirpBurst(
    const std::size_t sample_count,
    const int sample_rate_hz) {
    std::vector<double> burst(sample_count, 0.0);
    if (sample_count == 0 || sample_rate_hz <= 0) {
        return burst;
    }
    double phase = 0.0;
    for (std::size_t index = 0; index < sample_count; ++index) {
        const double ratio =
            static_cast<double>(index) / static_cast<double>(sample_count);
        const double frequency_hz = 950.0 + ratio * 900.0;
        phase += 2.0 * kPi * frequency_hz / static_cast<double>(sample_rate_hz);
        const double click =
            index < 4U || index + 5U >= sample_count
                ? ((index % 2U) == 0U ? 0.22 : -0.22)
                : 0.0;
        const double sample =
            std::sin(phase) * 0.82 + std::sin(phase * 0.5) * 0.18 + click;
        burst[index] = std::tanh(1.75 * sample) * BurstWindow(index, sample_count);
    }
    return burst;
}

void MixBurstIntoOverlay(
    const std::vector<double>& burst,
    const double gain,
    const std::size_t start_sample,
    std::vector<double>* overlay) {
    if (overlay == nullptr || burst.empty() || start_sample >= overlay->size()) {
        return;
    }
    const std::size_t remaining = overlay->size() - start_sample;
    const std::size_t count = std::min(remaining, burst.size());
    for (std::size_t index = 0; index < count; ++index) {
        (*overlay)[start_sample + index] += burst[index] * gain;
    }
}

std::vector<double> BuildSignalCantWindowTexture(
    const int sample_rate_hz,
    const SignalCantTuning& tuning,
    const std::size_t event_index) {
    const std::size_t window_samples =
        DelaySamplesForRate(sample_rate_hz, tuning.signal_window_ms);
    std::vector<double> window(window_samples, 0.0);
    if (window.empty()) {
        return window;
    }

    const std::size_t segment_length =
        std::max<std::size_t>(1U, window_samples / 3U);
    const std::vector<std::int16_t> flash_source_pcm =
        BuildFlashStandardSubvoiceBasePcm(
            DelaySamplesForRate(sample_rate_hz, tuning.flash_burst_ms),
            sample_rate_hz,
            bag::VoiceFxSubvoiceStyle::kStandard);
    std::vector<double> flash_burst(flash_source_pcm.size(), 0.0);
    for (std::size_t index = 0; index < flash_source_pcm.size(); ++index) {
        flash_burst[index] =
            static_cast<double>(flash_source_pcm[index]) * kInputScale *
            BurstWindow(index, flash_source_pcm.size());
    }
    const std::vector<double> dtmf_burst =
        BuildDtmfLikeBurst(
            DelaySamplesForRate(sample_rate_hz, tuning.dtmf_burst_ms),
            sample_rate_hz);
    const std::vector<double> chirp_burst =
        BuildChirpBurst(
            DelaySamplesForRate(sample_rate_hz, tuning.chirp_burst_ms),
            sample_rate_hz);

    const std::size_t pattern = event_index % 4U;
    if (pattern == 0U) {
        for (std::size_t index = 0; index < window.size(); ++index) {
            const double held = flash_burst[index % flash_burst.size()];
            const double cadence =
                0.78 + 0.22 *
                           (0.5 + 0.5 *
                                      std::sin(
                                          2.0 * kPi * 12.0 *
                                          static_cast<double>(index) /
                                          static_cast<double>(sample_rate_hz)));
            window[index] += held * tuning.flash_gain * cadence;
        }
        MixBurstIntoOverlay(chirp_burst, tuning.chirp_gain * 1.1, 0U, &window);
        MixBurstIntoOverlay(
            chirp_burst,
            tuning.chirp_gain * 0.95,
            std::min(window_samples - 1U, segment_length * 2U),
            &window);
    } else if (pattern == 1U) {
        MixBurstIntoOverlay(dtmf_burst, tuning.dtmf_gain * 1.1, 0U, &window);
        MixBurstIntoOverlay(
            flash_burst,
            tuning.flash_gain * 0.96,
            segment_length / 2U,
            &window);
        MixBurstIntoOverlay(
            dtmf_burst,
            tuning.dtmf_gain * 0.92,
            segment_length + segment_length / 4U,
            &window);
        MixBurstIntoOverlay(
            flash_burst,
            tuning.flash_gain * 0.78,
            std::min(window_samples - 1U, segment_length * 2U),
            &window);
    } else if (pattern == 2U) {
        MixBurstIntoOverlay(chirp_burst, tuning.chirp_gain * 1.18, 0U, &window);
        MixBurstIntoOverlay(
            chirp_burst,
            tuning.chirp_gain,
            segment_length / 2U,
            &window);
        MixBurstIntoOverlay(
            flash_burst,
            tuning.flash_gain * 0.86,
            segment_length + segment_length / 4U,
            &window);
        MixBurstIntoOverlay(
            chirp_burst,
            tuning.chirp_gain * 0.88,
            std::min(window_samples - 1U, segment_length * 2U),
            &window);
    } else {
        MixBurstIntoOverlay(flash_burst, tuning.flash_gain, 0U, &window);
        MixBurstIntoOverlay(
            dtmf_burst,
            tuning.dtmf_gain * 0.96,
            segment_length / 3U,
            &window);
        MixBurstIntoOverlay(
            chirp_burst,
            tuning.chirp_gain * 0.9,
            segment_length,
            &window);
        MixBurstIntoOverlay(
            flash_burst,
            tuning.flash_gain * 0.82,
            segment_length + segment_length / 2U,
            &window);
    }

    const std::size_t tremolo_period =
        std::max<std::size_t>(
            8U,
            static_cast<std::size_t>(sample_rate_hz / 42));
    for (std::size_t index = 0; index < window.size(); ++index) {
        const double open =
            0.56 + 0.44 *
                       (0.5 + 0.5 *
                                  std::sin(
                                      2.0 * kPi * 16.0 *
                                      static_cast<double>(index) /
                                      static_cast<double>(sample_rate_hz)));
        const double cadence =
            ((index / tremolo_period) % 2U) == 0U ? 1.0 : 0.82;
        window[index] *= open * cadence * tuning.window_gain;
    }
    return window;
}

std::vector<double> RenderSignalCantOverlay(
    const std::vector<double>& envelope,
    const int sample_rate_hz) {
    const SignalCantTuning tuning = SignalCantOverlayTuning();
    std::vector<double> overlay(envelope.size(), 0.0);
    if (envelope.empty() || sample_rate_hz <= 0) {
        return overlay;
    }

    const std::size_t minimum_gap_samples =
        DelaySamplesForRate(sample_rate_hz, tuning.minimum_gap_ms);

    std::size_t next_available_sample = 0;
    double previous_env = 0.0;
    std::size_t event_index = 0;
    for (std::size_t index = 0; index < envelope.size(); ++index) {
        const double env = envelope[index];
        const double delta = env - previous_env;
        const bool level_crossed =
            previous_env < tuning.onset_level_threshold &&
            env >= tuning.onset_level_threshold;
        previous_env = env;
        if (index < next_available_sample ||
            env < tuning.onset_level_threshold ||
            (delta < tuning.onset_delta_threshold && !level_crossed)) {
            continue;
        }

        const std::vector<double> signal_window =
            BuildSignalCantWindowTexture(sample_rate_hz, tuning, event_index);
        MixBurstIntoOverlay(signal_window, 1.0, index, &overlay);
        next_available_sample = index + minimum_gap_samples;
        ++event_index;
    }

    return overlay;
}

std::vector<double> RenderGatedFlashSubvoiceBranch(
    const std::vector<double>& envelope,
    const int sample_rate_hz,
    const bag::VoiceFxSubvoiceStyle subvoice_style,
    const GatedFlashSubvoiceTuning& tuning) {
    const double style_gain = SubvoiceStyleMixGain(subvoice_style);
    const std::size_t sample_count = envelope.size();
    const std::size_t delay_samples =
        DelaySamplesForRate(sample_rate_hz, tuning.delay_ms);
    const double attack_coeff = TimeToCoeff(sample_rate_hz, tuning.attack_ms);
    const double release_coeff = TimeToCoeff(sample_rate_hz, tuning.release_ms);
    const std::vector<std::int16_t> flash_base_pcm =
        BuildFlashStandardSubvoiceBasePcm(sample_count, sample_rate_hz, subvoice_style);

    std::vector<double> shaped(sample_count, 0.0);
    std::vector<double> output(sample_count, 0.0);
    double gate = 0.0;
    double resonator_a_state_1 = 0.0;
    double resonator_a_state_2 = 0.0;
    double resonator_b_state_1 = 0.0;
    double resonator_b_state_2 = 0.0;

    for (std::size_t index = 0; index < sample_count; ++index) {
        const double env = envelope[index];
        const double normalized_gate = std::clamp(
            (env - tuning.gate_open_threshold) /
                (tuning.gate_full_threshold - tuning.gate_open_threshold),
            0.0,
            1.0);
        const double target_gate =
            normalized_gate > 0.0
                ? tuning.gate_floor + (1.0 - tuning.gate_floor) * normalized_gate
                : 0.0;
        const double coeff = target_gate > gate ? attack_coeff : release_coeff;
        gate = coeff * gate + (1.0 - coeff) * target_gate;
        const double flash_sample =
            static_cast<double>(flash_base_pcm[index]) * kInputScale;
        const double resonator_a = ResonatorStep(
            flash_sample,
            tuning.resonator_a_hz,
            tuning.resonator_a_q,
            sample_rate_hz,
            &resonator_a_state_1,
            &resonator_a_state_2);
        const double resonator_b = ResonatorStep(
            flash_sample,
            tuning.resonator_b_hz,
            tuning.resonator_b_q,
            sample_rate_hz,
            &resonator_b_state_1,
            &resonator_b_state_2);
        const double resonated =
            flash_sample * (1.0 - tuning.resonator_mix) +
            (resonator_a * 0.58 + resonator_b * 0.42) * tuning.resonator_mix;
        const double envelope_drive =
            std::pow(
                std::min(1.0, env * tuning.envelope_drive),
                tuning.envelope_curve);
        shaped[index] =
            resonated * gate *
            (tuning.envelope_floor + (1.0 - tuning.envelope_floor) * envelope_drive) *
            tuning.output_gain * style_gain;
    }

    for (std::size_t index = 0; index < sample_count; ++index) {
        const double delayed = index >= delay_samples ? shaped[index - delay_samples] : 0.0;
        output[index] =
            delayed * (1.0 - tuning.dry_flash_mix) +
            shaped[index] * tuning.dry_flash_mix;
    }
    return output;
}
