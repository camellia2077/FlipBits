namespace bag::mini_mode {
bool IsValidMorseToneConfig(const MorseToneConfig& config) {
  return config.sample_rate_hz > 0 && config.unit_samples > 0 &&
         config.amplitude > 0.0 && config.tone_freq_hz > 0.0;
}

std::size_t EncodedPayloadUnitCount(
    const std::vector<std::uint8_t>& payload) {
  std::size_t units = 0;
  for (std::size_t index = 0; index < payload.size(); ++index) {
    units += MorseUnitCountForPayloadByte(payload[index]);
    units += MorseTrailingGapUnits(payload, index);
  }
  return units;
}

MorseToneConfig MakeMorseToneConfig(const CoreConfig& config) {
  MorseToneConfig out{};
  out.sample_rate_hz = config.sample_rate_hz;
  out.unit_samples = config.frame_samples;
  return out;
}
}  // namespace bag::mini_mode
