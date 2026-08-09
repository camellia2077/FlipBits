namespace bag::flash {

namespace {

std::size_t SamplesPerBitForSignalProfile(FlashSignalProfile signal_profile,
                                          const CoreConfig& config) {
  if (config.frame_samples <= 0) {
    return 0;
  }

  switch (signal_profile) {
    case FlashSignalProfile::kLitany:
      return static_cast<std::size_t>(config.frame_samples) * 6u;
    case FlashSignalProfile::kZeal:
      return std::max(static_cast<std::size_t>(1),
                      static_cast<std::size_t>(config.frame_samples) / 2u);
    case FlashSignalProfile::kHostility:
      return std::max(static_cast<std::size_t>(1),
                      (static_cast<std::size_t>(config.frame_samples) * 7u) /
                          8u);
    case FlashSignalProfile::kVoid:
      return std::max(static_cast<std::size_t>(1),
                      (static_cast<std::size_t>(config.frame_samples) * 5u) /
                          2u);
    case FlashSignalProfile::kStandard:
      return std::max(static_cast<std::size_t>(1),
                      (static_cast<std::size_t>(config.frame_samples) * 15u) /
                          16u);
    case FlashSignalProfile::kCollapse:
    default:
      return static_cast<std::size_t>(config.frame_samples);
  }
}

std::size_t SamplesPerSilenceSlotForSignalProfile(
    FlashSignalProfile signal_profile, const CoreConfig& config) {
  if (config.frame_samples <= 0) {
    return 0;
  }
  switch (signal_profile) {
    case FlashSignalProfile::kLitany:
    case FlashSignalProfile::kZeal:
    case FlashSignalProfile::kVoid:
      return static_cast<std::size_t>(config.frame_samples);
    case FlashSignalProfile::kStandard:
    case FlashSignalProfile::kHostility:
    case FlashSignalProfile::kCollapse:
    default:
      return SamplesPerBitForSignalProfile(signal_profile, config);
  }
}

bool ShouldCollapseToneRunsWhenSkippingSilence(
    FlashSignalProfile signal_profile) {
  // Litany renders one long voiced bit as multiple silence-slot windows. During
  // gap-aware decode those adjacent voiced windows must collapse back to one
  // bit.
  return signal_profile == FlashSignalProfile::kLitany;
}

}  // namespace

FlashVoicingFlavor NormalizeSignalVoicingFlavor(FlashVoicingFlavor flavor) {
  switch (flavor) {
    case FlashVoicingFlavor::kLitany:
    case FlashVoicingFlavor::kHostility:
    case FlashVoicingFlavor::kCollapse:
    case FlashVoicingFlavor::kZeal:
    case FlashVoicingFlavor::kVoid:
    case FlashVoicingFlavor::kStandard:
      return flavor;
    default:
      return FlashVoicingFlavor::kStandard;
  }
}

BfskConfig MakeBfskConfigForSignalProfile(const CoreConfig& config,
                                          FlashSignalProfile signal_profile) {
  if (!IsValidFlashSignalProfile(signal_profile)) {
    throw std::invalid_argument("Invalid flash signal profile.");
  }

  BfskConfig bfsk{};
  bfsk.sample_rate_hz = config.sample_rate_hz;
  switch (signal_profile) {
    case FlashSignalProfile::kLitany:
      bfsk.low_freq_hz = 220.0;
      bfsk.high_freq_hz = 440.0;
      break;
    case FlashSignalProfile::kHostility:
      bfsk.low_freq_hz = 559.0;
      bfsk.high_freq_hz = 1118.0;
      break;
    case FlashSignalProfile::kCollapse:
      bfsk.low_freq_hz = 280.0;
      bfsk.high_freq_hz = 560.0;
      break;
    case FlashSignalProfile::kZeal:
      bfsk.low_freq_hz = 900.0;
      bfsk.high_freq_hz = 1800.0;
      break;
    case FlashSignalProfile::kVoid:
      bfsk.low_freq_hz = 205.0;
      bfsk.high_freq_hz = 410.0;
      break;
    case FlashSignalProfile::kStandard:
    default:
      bfsk.low_freq_hz = 300.0;
      bfsk.high_freq_hz = 600.0;
      break;
  }
  bfsk.samples_per_bit = SamplesPerBitForSignalProfile(signal_profile, config);
  bfsk.samples_per_silence_slot =
      SamplesPerSilenceSlotForSignalProfile(signal_profile, config);
  bfsk.bit_duration_sec = config.sample_rate_hz > 0
                              ? static_cast<double>(bfsk.samples_per_bit) /
                                    static_cast<double>(config.sample_rate_hz)
                              : 0.0;
  bfsk.collapse_tone_runs_when_skipping_silence =
      ShouldCollapseToneRunsWhenSkippingSilence(signal_profile);
  return bfsk;
}

BfskConfig MakeBfskConfig(const CoreConfig& config) {
  return MakeBfskConfigForSignalProfile(config, config.flash_signal_profile);
}

}  // namespace bag::flash
