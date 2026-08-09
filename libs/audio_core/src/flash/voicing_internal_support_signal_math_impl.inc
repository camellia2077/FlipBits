bool ShouldApplySpectralTexture(const FlashVoicingConfig& config) {
  return config.attack_ratio > 0.0 || config.release_ratio > 0.0 ||
         config.second_harmonic_gain > 0.0 || config.third_harmonic_gain > 0.0;
}

bool ShouldApplyLayeredTimbre(const FlashVoicingConfig& config) {
  return config.second_harmonic_gain > 0.0 ||
         config.third_harmonic_gain > 0.0 || config.attack_ratio > 0.0 ||
         config.release_ratio > 0.0;
}

double ComputeOnePoleAlpha(double cutoff_hz, int sample_rate_hz) {
  if (cutoff_hz <= 0.0 || sample_rate_hz <= 0) {
    return 1.0;
  }

  const double exponent =
      -2.0 * kPi * cutoff_hz / static_cast<double>(sample_rate_hz);
  return std::clamp(1.0 - std::exp(exponent), 0.0, 1.0);
}

double SoftClipSample(double sample, double drive) {
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

double ComputeSecondaryMetallicLayer(double carrier_freq_hz, size_t local_index,
                                     int sample_rate_hz) {
  if (carrier_freq_hz <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
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
                                  int sample_rate_hz, double gain) {
  if (gain <= 0.0 || carrier_freq_hz <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time = static_cast<double>(payload_sample_index) /
                      static_cast<double>(sample_rate_hz);
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

double ComputeChantDroneLayer(std::size_t sample_index, int sample_rate_hz,
                              double gain) {
  if (gain <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(sample_index) / static_cast<double>(sample_rate_hz);
  const double breath = 0.68 + 0.32 * std::sin(2.0 * kPi * 0.65 * time + 0.35);
  const double drone = 0.56 * std::sin(2.0 * kPi * 60.0 * time + 0.12) +
                       0.29 * std::sin(2.0 * kPi * 120.0 * time + 0.41) +
                       0.15 * std::sin(2.0 * kPi * 180.0 * time + 0.73);
  return gain * breath * drone;
}

double ComputeChantSwellGain(std::size_t sample_index, int sample_rate_hz,
                             const VoicingStyleTraits& style_traits) {
  if (style_traits.chant_swell_depth <= 0.0 ||
      style_traits.chant_swell_freq_hz <= 0.0 || sample_rate_hz <= 0) {
    return 1.0;
  }

  const double time =
      static_cast<double>(sample_index) / static_cast<double>(sample_rate_hz);
  const double depth = std::clamp(style_traits.chant_swell_depth, 0.0, 0.24);
  const double swell =
      std::sin(2.0 * kPi * style_traits.chant_swell_freq_hz * time + 0.35);
  return std::clamp(1.0 + depth * swell, 1.0 - depth, 1.0 + depth);
}

double ComputeMechanicalThroatLayer(std::size_t sample_index,
                                    int sample_rate_hz, double gain) {
  if (gain <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(sample_index) / static_cast<double>(sample_rate_hz);
  const double throat = 0.48 * std::sin(2.0 * kPi * 160.0 * time + 0.27) +
                        0.34 * std::sin(2.0 * kPi * 420.0 * time + 0.61) +
                        0.18 * std::sin(2.0 * kPi * 560.0 * time + 1.04);
  const double motor_buzz =
      0.62 + 0.38 * std::sin(2.0 * kPi * 18.0 * time + 0.44);
  return gain * motor_buzz * throat;
}

double ComputeStandardLowVoiceLayer(std::size_t sample_index,
                                    int sample_rate_hz,
                                    const VoicingStyleTraits& style_traits) {
  if (style_traits.standard_low_voice_gain <= 0.0 ||
      style_traits.standard_low_voice_freq_hz <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(sample_index) / static_cast<double>(sample_rate_hz);
  const double phase =
      2.0 * kPi * style_traits.standard_low_voice_freq_hz * time;
  const double square_like = (std::sin(phase) + std::sin(3.0 * phase) / 3.0 +
                              std::sin(5.0 * phase) / 5.0) /
                             (1.0 + 1.0 / 3.0 + 1.0 / 5.0);
  return style_traits.standard_low_voice_gain * square_like;
}

double ComputeHostilityEdgeLayer(double carrier_freq_hz,
                                 std::size_t local_index, int sample_rate_hz,
                                 double gain) {
  if (gain <= 0.0 || carrier_freq_hz <= 0.0 || sample_rate_hz <= 0) {
    return 0.0;
  }

  const double time =
      static_cast<double>(local_index) / static_cast<double>(sample_rate_hz);
  const double nyquist_hz = static_cast<double>(sample_rate_hz) / 2.0;
  double layer = 0.0;
  const double second_freq_hz = carrier_freq_hz * 2.0;
  const double fifth_freq_hz = carrier_freq_hz * 5.0;
  if (second_freq_hz < nyquist_hz) {
    layer += 0.46 * std::sin(2.0 * kPi * second_freq_hz * time + 0.10);
  }
  if (fifth_freq_hz < nyquist_hz) {
    layer += 0.24 * std::sin(2.0 * kPi * fifth_freq_hz * time + 0.38);
  }
  layer += 0.30 * std::copysign(
                      1.0, std::sin(2.0 * kPi * carrier_freq_hz * time + 0.18));
  return gain * layer;
}

double ApplyPayloadSoftClip(double sample,
                            const VoicingStyleTraits& style_traits) {
  if (style_traits.payload_softclip_mix <= 0.0 ||
      style_traits.payload_softclip_drive <= 1.0) {
    return sample;
  }

  const double clipped =
      SoftClipSample(sample, style_traits.payload_softclip_drive);
  const double mix = std::clamp(style_traits.payload_softclip_mix, 0.0, 1.0);
  return sample * (1.0 - mix) + clipped * mix;
}

double ComputeCollapseTremor(std::size_t payload_sample_index,
                             int sample_rate_hz,
                             const VoicingStyleTraits& style_traits) {
  if (style_traits.collapse_tremor_depth <= 0.0 || sample_rate_hz <= 0) {
    return 1.0;
  }

  const double time = static_cast<double>(payload_sample_index) /
                      static_cast<double>(sample_rate_hz);
  const double primary = std::sin(2.0 * kPi * 8.6 * time + 0.33);
  const double secondary = std::sin(2.0 * kPi * 13.7 * time + 1.10);
  const double tremor =
      style_traits.collapse_tremor_depth * (0.72 * primary + 0.28 * secondary);
  return std::clamp(1.0 + tremor, 0.72, 1.18);
}
