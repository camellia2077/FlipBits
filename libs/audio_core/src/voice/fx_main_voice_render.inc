std::vector<double> RenderMainVoiceBranch(
    const std::vector<std::int16_t>& input_pcm,
    const int sample_rate_hz,
    const bag::VoiceFxPreset preset) {
    const MainVoiceTuning tuning = MainTuningForPreset(preset);
    const double hp_alpha = HighPassAlpha(sample_rate_hz, tuning.high_pass_hz);
    const double lp_alpha = LowPassAlpha(sample_rate_hz, tuning.low_pass_hz);
    const double transient_alpha = LowPassAlpha(
        sample_rate_hz,
        std::max(180.0, 1000.0 / std::max(0.8, tuning.transient_soften_ms)));
    const std::size_t comb_delay_samples =
        DelaySamplesForRate(sample_rate_hz, tuning.comb_delay_ms);
    const std::size_t slap_delay_samples =
        DelaySamplesForRate(sample_rate_hz, tuning.slap_delay_ms);
    const bool use_chamber_reflections =
        preset == bag::VoiceFxPreset::kBinharic;
    const std::array<double, 4> chamber_delay_ms = {5.5, 9.5, 14.0, 23.0};
    const std::array<double, 4> chamber_feedback = {0.09, 0.075, 0.06, 0.045};
    const std::array<double, 4> chamber_gain = {0.17, -0.13, 0.11, -0.085};
    const bool use_mechanical_flutter =
        preset == bag::VoiceFxPreset::kBinharic;
    const double mechanical_flutter_mix =
        use_mechanical_flutter ? 1.0 : 0.0;
    const std::vector<double> tracked_pitch_hz =
        BuildTrackedPitchContour(input_pcm, sample_rate_hz, tuning);

    std::vector<double> comb_delayed(comb_delay_samples, 0.0);
    std::vector<double> slap_delayed(slap_delay_samples, 0.0);
    std::vector<std::vector<double>> chamber_delayed;
    if (use_chamber_reflections) {
        chamber_delayed.reserve(chamber_delay_ms.size());
        for (const double delay_ms : chamber_delay_ms) {
            chamber_delayed.emplace_back(
                DelaySamplesForRate(sample_rate_hz, delay_ms),
                0.0);
        }
    }
    std::vector<double> shaped(input_pcm.size(), 0.0);
    std::vector<double> output(input_pcm.size(), 0.0);
    double hp_previous_input_1 = 0.0;
    double hp_previous_output_1 = 0.0;
    double hp_previous_input_2 = 0.0;
    double hp_previous_output_2 = 0.0;
    double lp_previous_output_1 = 0.0;
    double lp_previous_output_2 = 0.0;
    double resonator_state_1 = 0.0;
    double resonator_state_2 = 0.0;
    double throat_state_a_1 = 0.0;
    double throat_state_a_2 = 0.0;
    double throat_state_b_1 = 0.0;
    double throat_state_b_2 = 0.0;
    double cabinet_state_1 = 0.0;
    double cabinet_state_2 = 0.0;
    double transient_previous = 0.0;
    const double static_attack_coeff =
        TimeToCoeff(sample_rate_hz, tuning.static_gate_attack_ms);
    const double static_release_coeff =
        TimeToCoeff(sample_rate_hz, tuning.static_gate_release_ms);
    double static_gate = 0.0;
    const double pitch_bias_attack_coeff =
        TimeToCoeff(sample_rate_hz, tuning.pitch_bias_attack_ms);
    const double pitch_bias_release_coeff =
        TimeToCoeff(sample_rate_hz, tuning.pitch_bias_release_ms);
    double pitch_bias_gate = 0.0;
    double pitch_bias_phase = 0.0;

    for (std::size_t index = 0; index < input_pcm.size(); ++index) {
        double sample = static_cast<double>(input_pcm[index]) * kInputScale;
        sample =
            HighPassStep(sample, hp_alpha, &hp_previous_input_1, &hp_previous_output_1);
        sample =
            HighPassStep(sample, hp_alpha, &hp_previous_input_2, &hp_previous_output_2);
        sample = LowPassStep(sample, lp_alpha, &lp_previous_output_1);
        sample = LowPassStep(sample, lp_alpha, &lp_previous_output_2);
        sample = CompressSample(
            sample,
            tuning.compressor_threshold,
            tuning.compressor_ratio,
            tuning.compressor_makeup);
        const double softened =
            LowPassStep(sample, transient_alpha, &transient_previous);
        sample =
            sample * (1.0 - tuning.transient_soften_mix) +
            softened * tuning.transient_soften_mix;
        sample = std::tanh(tuning.pre_gain * sample);
        const double resonant = ResonatorStep(
            sample,
            tuning.resonant_frequency_hz,
            tuning.resonant_q,
            sample_rate_hz,
            &resonator_state_1,
            &resonator_state_2);
        const double throat_formant_a = ResonatorStep(
            sample,
            tuning.throat_formant_a_hz,
            tuning.throat_formant_a_q,
            sample_rate_hz,
            &throat_state_a_1,
            &throat_state_a_2);
        const double throat_formant_b = ResonatorStep(
            sample,
            tuning.throat_formant_b_hz,
            tuning.throat_formant_b_q,
            sample_rate_hz,
            &throat_state_b_1,
            &throat_state_b_2);
        const double cabinet = ResonatorStep(
            sample,
            tuning.cabinet_resonant_frequency_hz,
            tuning.cabinet_resonant_q,
            sample_rate_hz,
            &cabinet_state_1,
            &cabinet_state_2);
        const double throat_shaped =
            throat_formant_a * 0.62 + throat_formant_b * 0.38;
        sample =
            sample * (1.0 - tuning.resonant_mix) + resonant * tuning.resonant_mix;
        sample =
            sample * (1.0 - tuning.throat_formant_mix) +
            throat_shaped * tuning.throat_formant_mix;
        sample =
            sample * (1.0 - tuning.cabinet_resonant_mix) +
            cabinet * tuning.cabinet_resonant_mix;

        const double static_gate_target =
            std::abs(sample) >= tuning.static_gate_threshold ? 1.0 : 0.0;
        const double static_gate_coeff =
            static_gate_target > static_gate ? static_attack_coeff : static_release_coeff;
        static_gate =
            static_gate_coeff * static_gate +
            (1.0 - static_gate_coeff) * static_gate_target;
        const double pitch_bias_target =
            std::abs(sample) >= tuning.pitch_bias_gate_threshold ? 1.0 : 0.0;
        const double pitch_bias_coeff =
            pitch_bias_target > pitch_bias_gate
                ? pitch_bias_attack_coeff
                : pitch_bias_release_coeff;
        pitch_bias_gate =
            pitch_bias_coeff * pitch_bias_gate +
            (1.0 - pitch_bias_coeff) * pitch_bias_target;

        const double primary_phase =
            2.0 * kPi * tuning.am_frequency_hz * static_cast<double>(index) /
            static_cast<double>(sample_rate_hz);
        const double secondary_phase =
            2.0 * kPi * tuning.am_secondary_frequency_hz *
            static_cast<double>(index) / static_cast<double>(sample_rate_hz);
        const double flutter_phase_a =
            2.0 * kPi * 4.3 * static_cast<double>(index) /
            static_cast<double>(sample_rate_hz);
        const double flutter_phase_b =
            2.0 * kPi * 11.7 * static_cast<double>(index) /
            static_cast<double>(sample_rate_hz);
        const double flutter =
            mechanical_flutter_mix * pitch_bias_gate *
            (0.041 * std::sin(flutter_phase_a) +
             0.021 * std::sin(flutter_phase_b + 0.7));
        const double flutter_depth_bias =
            mechanical_flutter_mix * pitch_bias_gate *
            (0.052 * std::sin(flutter_phase_a + 1.1) +
             0.029 * std::sin(flutter_phase_b + 2.4));
        const double fluttered_am_depth =
            std::clamp(tuning.am_depth + flutter_depth_bias, 0.0, 0.92);
        const double fluttered_secondary_am_depth =
            std::clamp(
                tuning.am_secondary_depth + flutter_depth_bias * 0.55,
                0.0,
                0.72);
        const double primary_modulator =
            (1.0 - fluttered_am_depth) +
            fluttered_am_depth * (0.5 * (std::sin(primary_phase) + 1.0));
        const double secondary_modulator =
            (1.0 - fluttered_secondary_am_depth) +
            fluttered_secondary_am_depth *
                (0.5 * (std::sin(secondary_phase) + 1.0));
        const double chopper =
            1.0 - tuning.chopper_depth +
            tuning.chopper_depth *
                (0.5 + 0.5 * std::sin(
                                   2.0 * kPi * tuning.chopper_frequency_hz *
                                   static_cast<double>(index) /
                                   static_cast<double>(sample_rate_hz)));
        const double tracked_frequency_hz =
            index < tracked_pitch_hz.size() && tracked_pitch_hz[index] > 0.0
                ? tracked_pitch_hz[index]
                : tuning.pitch_bias_frequency_hz;
        const double fluttered_tracked_frequency_hz =
            tracked_frequency_hz * (1.0 + flutter * 0.7);
        pitch_bias_phase +=
            2.0 * kPi * fluttered_tracked_frequency_hz /
            static_cast<double>(sample_rate_hz);
        const double pitch_bias_base_mix =
            std::max(0.0, 1.0 - tuning.pitch_bias_second_harmonic_mix -
                               tuning.pitch_bias_third_harmonic_mix);
        const double pitch_bias_carrier =
            std::sin(pitch_bias_phase) * pitch_bias_base_mix +
            std::sin(pitch_bias_phase * 2.0) * tuning.pitch_bias_second_harmonic_mix +
            std::sin(pitch_bias_phase * 3.0) * tuning.pitch_bias_third_harmonic_mix;
        const double pitch_bias_layer =
            pitch_bias_carrier * pitch_bias_gate *
            std::min(1.0, std::abs(sample) * 4.2) * tuning.pitch_bias_mix;
        const double fluttered_sample = sample * (1.0 + flutter);
        const double fluttered_pitch_bias_layer =
            pitch_bias_layer * (1.0 + flutter * 1.8);
        const double static_bed =
            DeterministicNoise(index) * tuning.static_mix *
            static_gate * (0.3 + 0.7 * std::min(1.0, std::abs(sample) * 5.0));
        shaped[index] =
            fluttered_sample * primary_modulator * secondary_modulator * chopper +
            static_bed + fluttered_pitch_bias_layer;
    }

    const bool use_parallel_machine_layer =
        preset == bag::VoiceFxPreset::kMachineVoice;
    const MachineParallelLayerTuning parallel_tuning =
        MachineVoiceParallelLayerTuning();
    const std::size_t parallel_comb_delay_samples =
        DelaySamplesForRate(sample_rate_hz, parallel_tuning.comb_delay_ms);
    std::vector<double> parallel_comb_delayed(
        use_parallel_machine_layer ? parallel_comb_delay_samples : 1U,
        0.0);
    double parallel_resonator_a_state_1 = 0.0;
    double parallel_resonator_a_state_2 = 0.0;
    double parallel_resonator_b_state_1 = 0.0;
    double parallel_resonator_b_state_2 = 0.0;
    double parallel_pitch_phase = 0.0;

    for (std::size_t index = 0; index < shaped.size(); ++index) {
        double sample = shaped[index];
        if (tuning.pitch_shift_mix > 0.0) {
            const double lowered =
                PitchModStep(shaped, index, tuning.pitch_shift_down_cents);
            sample =
                sample * (1.0 - tuning.pitch_shift_mix) +
                lowered * tuning.pitch_shift_mix;
        }
        if (tuning.parallel_detune_mix > 0.0) {
            const double detuned =
                PitchModStep(shaped, index, tuning.parallel_detune_cents);
            sample =
                sample * (1.0 - tuning.parallel_detune_mix) +
                detuned * tuning.parallel_detune_mix;
        }

        const std::size_t comb_index = index % comb_delay_samples;
        const double comb_sample = comb_delayed[comb_index];
        comb_delayed[comb_index] =
            sample + comb_sample * tuning.comb_feedback;

        const std::size_t slap_index = index % slap_delay_samples;
        const double slap_sample = slap_delayed[slap_index];
        slap_delayed[slap_index] =
            sample + slap_sample * tuning.slap_feedback;

        double chamber_sample = 0.0;
        for (std::size_t tap = 0; tap < chamber_delayed.size(); ++tap) {
            std::vector<double>& chamber_line = chamber_delayed[tap];
            const std::size_t chamber_index = index % chamber_line.size();
            const double reflected = chamber_line[chamber_index];
            chamber_line[chamber_index] =
                sample + reflected * chamber_feedback[tap];
            chamber_sample += reflected * chamber_gain[tap];
        }

        const double wet =
            sample +
            comb_sample * tuning.comb_mix +
            slap_sample * tuning.slap_mix +
            chamber_sample;
        double final_wet = wet;
        if (use_parallel_machine_layer) {
            const double parallel_resonator_a = ResonatorStep(
                sample,
                parallel_tuning.resonator_a_hz,
                parallel_tuning.resonator_a_q,
                sample_rate_hz,
                &parallel_resonator_a_state_1,
                &parallel_resonator_a_state_2);
            const double parallel_resonator_b = ResonatorStep(
                sample,
                parallel_tuning.resonator_b_hz,
                parallel_tuning.resonator_b_q,
                sample_rate_hz,
                &parallel_resonator_b_state_1,
                &parallel_resonator_b_state_2);
            const double parallel_resonated =
                sample * (1.0 - parallel_tuning.resonator_mix) +
                (parallel_resonator_a * 0.56 + parallel_resonator_b * 0.44) *
                    parallel_tuning.resonator_mix;
            const std::size_t parallel_comb_index =
                index % parallel_comb_delay_samples;
            const double parallel_comb_sample =
                parallel_comb_delayed[parallel_comb_index];
            parallel_comb_delayed[parallel_comb_index] =
                parallel_resonated +
                parallel_comb_sample * parallel_tuning.comb_feedback;

            parallel_pitch_phase +=
                2.0 * kPi * parallel_tuning.pitch_bias_frequency_hz /
                static_cast<double>(sample_rate_hz);
            const double parallel_pitch_base_mix =
                std::max(
                    0.0,
                    1.0 - parallel_tuning.pitch_bias_second_harmonic_mix -
                        parallel_tuning.pitch_bias_third_harmonic_mix);
            const double parallel_pitch_carrier =
                std::sin(parallel_pitch_phase) * parallel_pitch_base_mix +
                std::sin(parallel_pitch_phase * 2.0) *
                    parallel_tuning.pitch_bias_second_harmonic_mix +
                std::sin(parallel_pitch_phase * 3.0) *
                    parallel_tuning.pitch_bias_third_harmonic_mix;
            const double gate =
                std::min(1.0, std::abs(sample) / parallel_tuning.gate_threshold);
            const double parallel_static =
                DeterministicNoise(index + 131U) *
                parallel_tuning.static_mix * gate;
            const double parallel_layer =
                parallel_resonated +
                parallel_comb_sample * parallel_tuning.comb_mix +
                parallel_pitch_carrier * parallel_tuning.pitch_bias_mix * gate +
                parallel_static;
            final_wet =
                wet * (1.0 - parallel_tuning.mix) +
                parallel_layer * parallel_tuning.mix;
        }
        output[index] = std::tanh(tuning.limiter_drive * final_wet) * tuning.output_trim;
    }

    return output;
}
