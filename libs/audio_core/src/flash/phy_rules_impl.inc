namespace bag::flash {
namespace {

bool UsesSpectralTexture(const FlashVoicingConfig& config) {
  return config.attack_ratio > 0.0 || config.release_ratio > 0.0 ||
         config.second_harmonic_gain > 0.0 || config.third_harmonic_gain > 0.0;
}

std::size_t NonZeroWork(std::size_t work) {
  return std::max(work, static_cast<std::size_t>(1));
}

}  // namespace

FlashSignalProfile NormalizeFormalSignalProfile(
    FlashSignalProfile signal_profile) {
  switch (signal_profile) {
    case FlashSignalProfile::kStandard:
    case FlashSignalProfile::kLitany:
    case FlashSignalProfile::kHostility:
    case FlashSignalProfile::kCollapse:
    case FlashSignalProfile::kZeal:
    case FlashSignalProfile::kVoid:
      return signal_profile;
    default:
      return FlashSignalProfile::kStandard;
  }
}

FlashVoicingFlavor NormalizeFormalVoicingFlavor(FlashVoicingFlavor flavor) {
  switch (flavor) {
    case FlashVoicingFlavor::kStandard:
    case FlashVoicingFlavor::kLitany:
    case FlashVoicingFlavor::kHostility:
    case FlashVoicingFlavor::kCollapse:
    case FlashVoicingFlavor::kZeal:
    case FlashVoicingFlavor::kVoid:
      return flavor;
    default:
      return FlashVoicingFlavor::kStandard;
  }
}

bool IsFormalVoicingCompatible(const FlashVoicingResult& voiced_payload,
                               std::size_t clean_payload_sample_count,
                               const FlashVoicingConfig& config) {
  return voiced_payload.descriptor.leading_nonpayload_samples ==
             (config.enable_preamble ? config.preamble_sample_count
                                     : static_cast<std::size_t>(0)) &&
         voiced_payload.descriptor.trailing_nonpayload_samples ==
             (config.enable_epilogue ? config.epilogue_sample_count
                                     : static_cast<std::size_t>(0)) &&
         voiced_payload.descriptor.payload_sample_count ==
             clean_payload_sample_count &&
         voiced_payload.pcm.size() ==
             clean_payload_sample_count +
                 voiced_payload.descriptor.leading_nonpayload_samples +
                 voiced_payload.descriptor.trailing_nonpayload_samples;
}

std::vector<std::int16_t> TrimFormalPayloadPcmForFlavor(
    const CoreConfig& config, FlashVoicingFlavor flavor,
    const std::vector<std::int16_t>& pcm) {
  const FlashVoicingDescriptor descriptor = DescribeVoicingOutput(
      pcm.size(), MakeFormalVoicingConfigForFlavor(config, flavor));
  return TrimToPayloadPcm(pcm, descriptor);
}

FlashEncodeWorkBudget BuildFlashEncodeWorkBudget(
    const std::vector<std::uint8_t>& bytes,
    const FlashPayloadLayout& payload_layout,
    const FlashVoicingConfig& voicing_config) {
  const std::size_t payload_sample_count = payload_layout.payload_sample_count;
  const std::size_t leading_nonpayload_samples =
      voicing_config.enable_preamble ? voicing_config.preamble_sample_count
                                     : static_cast<std::size_t>(0);
  const std::size_t trailing_nonpayload_samples =
      voicing_config.enable_epilogue ? voicing_config.epilogue_sample_count
                                     : static_cast<std::size_t>(0);
  const std::size_t voiced_total_samples = leading_nonpayload_samples +
                                           payload_sample_count +
                                           trailing_nonpayload_samples;
  const std::size_t spectral_texture_work = UsesSpectralTexture(voicing_config)
                                                ? voiced_total_samples
                                                : static_cast<std::size_t>(0);

  FlashEncodeWorkBudget budget{};
  budget.prepare_text_work = NonZeroWork(bytes.size());
  budget.prepare_layout_work = NonZeroWork(payload_layout.chunks.size());
  budget.render_work = NonZeroWork(payload_sample_count);
  budget.postprocess_work =
      NonZeroWork(voiced_total_samples + spectral_texture_work);
  budget.finalize_work = 1;
  budget.total_work = budget.prepare_text_work + budget.prepare_layout_work +
                      budget.render_work + budget.postprocess_work +
                      budget.finalize_work;
  return budget;
}

std::vector<std::uint8_t> DecodeFormalPayloadBytes(
    const CoreConfig& config, FlashSignalProfile signal_profile,
    FlashVoicingFlavor flavor,
    const std::vector<std::int16_t>& trimmed_payload_pcm) {
  const BfskConfig signal_config =
      MakeBfskConfigForSignalProfile(config, signal_profile);
  switch (NormalizeFormalVoicingFlavor(flavor)) {
    case FlashVoicingFlavor::kLitany:
      return DecodePcm16ToBytesSkippingSilence(trimmed_payload_pcm,
                                               signal_config);
    case FlashVoicingFlavor::kCollapse:
      return DecodePcm16ToBytesSkippingSilenceWithCarrierSchedule(
          trimmed_payload_pcm, signal_config, flavor);
    case FlashVoicingFlavor::kZeal:
      return DecodeZealPcm16ToBytes(trimmed_payload_pcm, signal_config);
    case FlashVoicingFlavor::kHostility:
    case FlashVoicingFlavor::kVoid:
      return DecodePcm16ToBytesWithPayloadLayout(trimmed_payload_pcm,
                                                 signal_config, flavor);
    case FlashVoicingFlavor::kStandard:
    default:
      return DecodePcm16ToBytes(trimmed_payload_pcm, signal_config);
  }
}

}  // namespace bag::flash
