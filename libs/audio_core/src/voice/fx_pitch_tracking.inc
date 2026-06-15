std::vector<double> BuildTrackedPitchContour(
    const std::vector<std::int16_t>& input_pcm,
    const int sample_rate_hz,
    const MainVoiceTuning& tuning) {
    std::vector<double> contour(input_pcm.size(), 0.0);
    if (input_pcm.empty() || sample_rate_hz <= 0) {
        return contour;
    }

    const std::size_t frame_samples =
        DelaySamplesForRate(sample_rate_hz, tuning.pitch_tracking_frame_ms);
    const std::size_t hop_samples =
        DelaySamplesForRate(sample_rate_hz, tuning.pitch_tracking_hop_ms);
    const std::size_t min_lag = std::max<std::size_t>(
        1U,
        static_cast<std::size_t>(
            std::lround(static_cast<double>(sample_rate_hz) /
                        tuning.pitch_tracking_max_hz)));
    const std::size_t max_lag = std::max(
        min_lag + 1U,
        static_cast<std::size_t>(
            std::lround(static_cast<double>(sample_rate_hz) /
                        tuning.pitch_tracking_min_hz)));
    double previous_frequency_hz = tuning.pitch_bias_frequency_hz;

    for (std::size_t frame_start = 0; frame_start < input_pcm.size();
         frame_start += hop_samples) {
        const std::size_t frame_end =
            std::min(input_pcm.size(), frame_start + frame_samples);
        if (frame_end <= frame_start + max_lag + 2U) {
            break;
        }

        double energy = 0.0;
        for (std::size_t index = frame_start; index < frame_end; ++index) {
            const double sample =
                static_cast<double>(input_pcm[index]) * kInputScale;
            energy += sample * sample;
        }
        const std::size_t frame_length = frame_end - frame_start;
        const double rms = std::sqrt(energy / static_cast<double>(frame_length));

        double detected_frequency_hz = 0.0;
        if (rms >= 0.018) {
            double best_corr = 0.0;
            std::size_t best_lag = min_lag;
            for (std::size_t lag = min_lag; lag <= max_lag; ++lag) {
                double numerator = 0.0;
                double energy_a = 0.0;
                double energy_b = 0.0;
                for (std::size_t index = frame_start; index + lag < frame_end;
                     ++index) {
                    const double a =
                        static_cast<double>(input_pcm[index]) * kInputScale;
                    const double b =
                        static_cast<double>(input_pcm[index + lag]) * kInputScale;
                    numerator += a * b;
                    energy_a += a * a;
                    energy_b += b * b;
                }
                if (energy_a <= 1e-9 || energy_b <= 1e-9) {
                    continue;
                }
                const double corr =
                    numerator / std::sqrt(energy_a * energy_b);
                if (corr > best_corr) {
                    best_corr = corr;
                    best_lag = lag;
                }
            }
            if (best_corr >= tuning.pitch_tracking_voiced_threshold) {
                detected_frequency_hz =
                    static_cast<double>(sample_rate_hz) /
                    static_cast<double>(best_lag);
            }
        }

        double target_frequency_hz = tuning.pitch_bias_frequency_hz;
        if (detected_frequency_hz > 0.0) {
            const double flattened =
                detected_frequency_hz * (1.0 - tuning.pitch_flatten_strength) +
                tuning.pitch_bias_frequency_hz * tuning.pitch_flatten_strength;
            const double shifted =
                flattened / std::pow(2.0, tuning.pitch_shift_down_cents / 1200.0);
            target_frequency_hz =
                previous_frequency_hz * 0.72 + shifted * 0.28;
            previous_frequency_hz = target_frequency_hz;
        } else {
            target_frequency_hz =
                previous_frequency_hz * 0.92 +
                tuning.pitch_bias_frequency_hz * 0.08;
            previous_frequency_hz = target_frequency_hz;
        }

        const std::size_t fill_end =
            std::min(input_pcm.size(), frame_start + hop_samples);
        for (std::size_t index = frame_start; index < fill_end; ++index) {
            contour[index] = target_frequency_hz;
        }
    }

    if (!contour.empty() && contour.front() == 0.0) {
        contour.front() = tuning.pitch_bias_frequency_hz;
    }
    for (std::size_t index = 1; index < contour.size(); ++index) {
        if (contour[index] == 0.0) {
            contour[index] = contour[index - 1];
        }
    }
    return contour;
}
