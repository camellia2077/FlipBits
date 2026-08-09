namespace bag::pro {
bool IsValidDualToneConfig(const DualToneConfig& config) {
  return config.sample_rate_hz > 0 && config.symbol_samples > 0 &&
         config.amplitude > 0.0;
}

DualToneConfig MakeDualToneConfig(const CoreConfig& config) {
  DualToneConfig out{};
  out.sample_rate_hz = config.sample_rate_hz;
  out.symbol_samples = config.frame_samples;
  return out;
}
}  // namespace bag::pro
