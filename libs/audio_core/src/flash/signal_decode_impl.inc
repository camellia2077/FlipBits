namespace bag::flash {

using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {

int ChunkSize(const BfskConfig& config) {
  if (config.samples_per_bit == 0) {
    return 0;
  }
  if (config.samples_per_bit >
      static_cast<std::size_t>(std::numeric_limits<int>::max())) {
    throw std::invalid_argument(
        "BFSK samples_per_bit exceeds supported range.");
  }
  return static_cast<int>(config.samples_per_bit);
}

int SilenceSlotSize(const BfskConfig& config) {
  if (config.samples_per_silence_slot == 0) {
    return 0;
  }
  if (config.samples_per_silence_slot >
      static_cast<std::size_t>(std::numeric_limits<int>::max())) {
    throw std::invalid_argument(
        "BFSK samples_per_silence_slot exceeds supported range.");
  }
  return static_cast<int>(config.samples_per_silence_slot);
}

std::vector<uint8_t> BitsToBytes(const std::vector<int>& bits) {
  std::vector<uint8_t> bytes;
  bytes.reserve(bits.size() / 8);
  for (size_t index = 0; index + 7 < bits.size(); index += 8) {
    uint8_t value = 0;
    for (int bit = 0; bit < 8; ++bit) {
      value = static_cast<uint8_t>(
          (value << 1) | (bits[index + static_cast<size_t>(bit)] ? 1 : 0));
    }
    bytes.push_back(value);
  }
  return bytes;
}

double GoertzelPower(const int16_t* chunk, int chunk_size, int sample_rate_hz,
                     double target_freq_hz) {
  constexpr double kPi = 3.14159265358979323846;
  const double normalized_freq =
      target_freq_hz / static_cast<double>(sample_rate_hz);
  const double omega = 2.0 * kPi * normalized_freq;
  const double coeff = 2.0 * std::cos(omega);

  double q0 = 0.0;
  double q1 = 0.0;
  double q2 = 0.0;
  for (int index = 0; index < chunk_size; ++index) {
    const double sample =
        static_cast<double>(chunk[static_cast<size_t>(index)]) / 32767.0;
    q0 = coeff * q1 - q2 + sample;
    q2 = q1;
    q1 = q0;
  }
  return q1 * q1 + q2 * q2 - coeff * q1 * q2;
}

bool EndsWithUtf8Bytes(const std::vector<uint8_t>& bytes,
                       std::size_t byte_index, uint8_t first, uint8_t second,
                       uint8_t third);
bool IsLikelyUtf8CodePointBoundaryAfter(const std::vector<uint8_t>& bytes,
                                        std::size_t byte_index);

enum class ZealTimingClass {
  kBurst,
  kFast,
  kDrive,
  kHold,
};

ZealTimingClass ZealTimingClassForBitPosition(std::size_t bit_position) {
  static constexpr std::array<ZealTimingClass, 16> kPattern = {
      ZealTimingClass::kBurst, ZealTimingClass::kBurst, ZealTimingClass::kFast,
      ZealTimingClass::kBurst, ZealTimingClass::kDrive, ZealTimingClass::kBurst,
      ZealTimingClass::kFast,  ZealTimingClass::kHold,  ZealTimingClass::kBurst,
      ZealTimingClass::kBurst, ZealTimingClass::kFast,  ZealTimingClass::kBurst,
      ZealTimingClass::kHold,  ZealTimingClass::kFast,  ZealTimingClass::kBurst,
      ZealTimingClass::kDrive,
  };
  return kPattern[bit_position % kPattern.size()];
}

std::size_t ScaleZealFrameSamples(std::size_t frame_samples,
                                  std::size_t numerator,
                                  std::size_t denominator) {
  if (frame_samples == 0 || denominator == 0) {
    return 0;
  }
  return std::max(static_cast<std::size_t>(1),
                  (frame_samples * numerator) / denominator);
}

std::size_t ZealFrameSamples(const BfskConfig& config) {
  if (config.samples_per_silence_slot > 0) {
    return config.samples_per_silence_slot;
  }
  return config.samples_per_bit * static_cast<std::size_t>(2);
}

std::size_t ZealSamplesForTimingClass(ZealTimingClass timing_class,
                                      const BfskConfig& config) {
  const std::size_t frame_samples = ZealFrameSamples(config);
  switch (timing_class) {
    case ZealTimingClass::kBurst:
      return ScaleZealFrameSamples(frame_samples, 1, 2);
    case ZealTimingClass::kFast:
      return ScaleZealFrameSamples(frame_samples, 5, 8);
    case ZealTimingClass::kDrive:
      return ScaleZealFrameSamples(frame_samples, 3, 4);
    case ZealTimingClass::kHold:
    default:
      return ScaleZealFrameSamples(frame_samples, 1, 1);
  }
}

double ZealLowCarrierForTimingClass(ZealTimingClass timing_class) {
  switch (timing_class) {
    case ZealTimingClass::kBurst:
      return 900.0;
    case ZealTimingClass::kFast:
      return 760.0;
    case ZealTimingClass::kDrive:
      return 660.0;
    case ZealTimingClass::kHold:
    default:
      return 560.0;
  }
}

double ZealHighCarrierForTimingClass(ZealTimingClass timing_class) {
  return ZealLowCarrierForTimingClass(timing_class) * 2.0;
}

double CollapseLowCarrierForBit(std::size_t bit_position,
                                std::size_t byte_index, uint8_t source_byte,
                                uint8_t bit_index_in_byte) {
  (void)source_byte;
  static constexpr std::array<double, 8> kSag = {
      320.0, 305.0, 292.0, 280.0, 268.0, 254.0, 240.0, 226.0,
  };
  const std::size_t phrase_offset =
      (bit_position + byte_index +
       static_cast<std::size_t>(bit_index_in_byte / static_cast<uint8_t>(2))) %
      kSag.size();
  return kSag[phrase_offset];
}

double CollapseHighCarrierForBit(std::size_t bit_position,
                                 std::size_t byte_index, uint8_t source_byte,
                                 uint8_t bit_index_in_byte) {
  return CollapseLowCarrierForBit(bit_position, byte_index, source_byte,
                                  bit_index_in_byte) *
         2.0;
}

bool IsZealStrongPauseByte(const std::vector<uint8_t>& bytes,
                           std::size_t byte_index) {
  const uint8_t value = bytes[byte_index];
  switch (value) {
    case static_cast<uint8_t>('\n'):
    case static_cast<uint8_t>('\r'):
    case static_cast<uint8_t>('.'):
    case static_cast<uint8_t>('!'):
    case static_cast<uint8_t>('?'):
      return true;
    default:
      break;
  }
  return EndsWithUtf8Bytes(bytes, byte_index, 0xE3, 0x80, 0x82) ||
         EndsWithUtf8Bytes(bytes, byte_index, 0xEF, 0xBC, 0x81) ||
         EndsWithUtf8Bytes(bytes, byte_index, 0xEF, 0xBC, 0x9F);
}

std::size_t ZealPauseSlotCountAfterByte(const std::vector<uint8_t>& bytes,
                                        std::size_t byte_index) {
  if (!IsLikelyUtf8CodePointBoundaryAfter(bytes, byte_index)) {
    return 0;
  }
  const uint8_t value = bytes[byte_index];
  switch (value) {
    case static_cast<uint8_t>('\n'):
    case static_cast<uint8_t>('\r'):
      return 5;
    case static_cast<uint8_t>('.'):
    case static_cast<uint8_t>('!'):
    case static_cast<uint8_t>('?'):
      return 4;
    case static_cast<uint8_t>(','):
    case static_cast<uint8_t>(';'):
    case static_cast<uint8_t>(':'):
      return 1;
    default:
      break;
  }
  if (EndsWithUtf8Bytes(bytes, byte_index, 0xEF, 0xBC, 0x8C) ||
      EndsWithUtf8Bytes(bytes, byte_index, 0xEF, 0xBC, 0x9B) ||
      EndsWithUtf8Bytes(bytes, byte_index, 0xEF, 0xBC, 0x9A)) {
    return 1;
  }
  if (IsZealStrongPauseByte(bytes, byte_index)) {
    return 4;
  }
  return 0;
}

bool EndsWithUtf8Bytes(const std::vector<uint8_t>& bytes,
                       std::size_t byte_index, uint8_t first, uint8_t second,
                       uint8_t third) {
  if (byte_index < static_cast<std::size_t>(2)) {
    return false;
  }
  return bytes[byte_index - static_cast<std::size_t>(2)] == first &&
         bytes[byte_index - static_cast<std::size_t>(1)] == second &&
         bytes[byte_index] == third;
}

bool IsUtf8ContinuationByte(uint8_t value) {
  return (value & 0xC0U) == 0x80U;
}

bool IsLikelyUtf8CodePointBoundaryAfter(const std::vector<uint8_t>& bytes,
                                        std::size_t byte_index) {
  return byte_index + static_cast<std::size_t>(1) >= bytes.size() ||
         !IsUtf8ContinuationByte(
             bytes[byte_index + static_cast<std::size_t>(1)]);
}

}  // namespace

std::vector<uint8_t> DecodePcm16ToBytes(const std::vector<int16_t>& pcm,
                                        const BfskConfig& config) {
  const int chunk_size = ChunkSize(config);
  if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }
  if (pcm.empty()) {
    return {};
  }

  std::vector<int> bits;
  for (size_t offset = 0;
       offset + static_cast<size_t>(chunk_size) <= pcm.size();
       offset += static_cast<size_t>(chunk_size)) {
    const int16_t* chunk = pcm.data() + offset;
    const double low_power =
        GoertzelPower(chunk, chunk_size, config.sample_rate_hz,
                      config.low_freq_hz);
    const double high_power =
        GoertzelPower(chunk, chunk_size, config.sample_rate_hz,
                      config.high_freq_hz);
    bits.push_back(high_power > low_power ? 1 : 0);
  }
  return BitsToBytes(bits);
}

std::vector<uint8_t> DecodePcm16ToBytesWithPayloadLayout(
    const std::vector<int16_t>& pcm, const BfskConfig& config,
    FlashVoicingFlavor flavor) {
  const int bit_chunk_size = ChunkSize(config);
  if (bit_chunk_size <= 0 || config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }
  if (pcm.empty()) {
    return {};
  }

  const std::size_t estimated_bit_count =
      pcm.size() / static_cast<std::size_t>(bit_chunk_size);
  const std::size_t estimated_byte_count =
      (estimated_bit_count + static_cast<std::size_t>(7)) /
      static_cast<std::size_t>(8);
  const std::vector<uint8_t> placeholder_bytes(estimated_byte_count, 0U);
  const FlashPayloadLayout decode_layout =
      BuildPayloadLayoutForVoicing(placeholder_bytes, config, flavor);

  std::vector<int> bits;
  bits.reserve(estimated_bit_count);
  for (const FlashPayloadChunk& chunk : decode_layout.chunks) {
    if (chunk.kind != FlashPayloadSegmentKind::kBit ||
        chunk.sample_count == 0 || chunk.carrier_freq_hz <= 0.0 ||
        chunk.sample_offset + chunk.sample_count > pcm.size()) {
      continue;
    }
    const int16_t* chunk_pcm = pcm.data() + chunk.sample_offset;
    const double bit_carrier_power =
        GoertzelPower(chunk_pcm, static_cast<int>(chunk.sample_count),
                      config.sample_rate_hz, chunk.carrier_freq_hz);
    const double opposite_carrier_power = GoertzelPower(
        chunk_pcm, static_cast<int>(chunk.sample_count), config.sample_rate_hz,
        chunk.bit_value == 1 ? chunk.carrier_freq_hz / 2.0
                             : chunk.carrier_freq_hz * 2.0);
    bits.push_back(bit_carrier_power >= opposite_carrier_power
                       ? chunk.bit_value
                       : 1 - chunk.bit_value);
    if (bits.size() >= estimated_bit_count) {
      break;
    }
  }
  return BitsToBytes(bits);
}

std::vector<uint8_t> DecodePcm16ToBytesSkippingSilenceWithCarrierSchedule(
    const std::vector<int16_t>& pcm, const BfskConfig& config,
    FlashVoicingFlavor flavor) {
  const int bit_chunk_size = ChunkSize(config);
  const int silence_slot_size = SilenceSlotSize(config);
  if (bit_chunk_size <= 0 || silence_slot_size <= 0 ||
      config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }
  if (pcm.empty()) {
    return {};
  }

  (void)flavor;
  auto window_energy = [](const int16_t* chunk, int sample_count) {
    double energy = 0.0;
    for (int i = 0; i < sample_count; ++i) {
      const double sample = static_cast<double>(chunk[i]) / 32768.0;
      energy += sample * sample;
    }
    return energy / static_cast<double>(sample_count);
  };

  double max_window_energy = 0.0;
  for (std::size_t offset = 0;
       offset + static_cast<std::size_t>(bit_chunk_size) <= pcm.size();
       offset += static_cast<std::size_t>(silence_slot_size)) {
    max_window_energy = std::max(
        max_window_energy, window_energy(pcm.data() + offset, bit_chunk_size));
  }
  if (max_window_energy <= 0.0) {
    return {};
  }

  const double silence_threshold = max_window_energy * 0.02;
  std::vector<int> bits;
  for (std::size_t offset = 0;
       offset + static_cast<std::size_t>(bit_chunk_size) <= pcm.size();
       offset += static_cast<std::size_t>(silence_slot_size)) {
    const int16_t* chunk = pcm.data() + offset;
    if (window_energy(chunk, bit_chunk_size) < silence_threshold) {
      continue;
    }

    const std::size_t bit_position = bits.size();
    const std::size_t byte_index = bit_position / 8u;
    const uint8_t bit_index_in_byte =
        static_cast<uint8_t>(bit_position % 8u);
    const double low_freq =
        CollapseLowCarrierForBit(bit_position, byte_index, 0U,
                                 bit_index_in_byte);
    const double high_freq =
        CollapseHighCarrierForBit(bit_position, byte_index, 0U,
                                  bit_index_in_byte);
    const double low_power =
        GoertzelPower(chunk, bit_chunk_size, config.sample_rate_hz, low_freq);
    const double high_power =
        GoertzelPower(chunk, bit_chunk_size, config.sample_rate_hz, high_freq);
    bits.push_back(high_power > low_power ? 1 : 0);
  }
  return BitsToBytes(bits);
}

std::vector<uint8_t> DecodePcm16ToBytesSkippingSilence(
    const std::vector<int16_t>& pcm, const BfskConfig& config) {
  const int bit_chunk_size = ChunkSize(config);
  const int silence_slot_size = SilenceSlotSize(config);
  if (bit_chunk_size <= 0 || silence_slot_size <= 0 ||
      config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }
  if (pcm.empty()) {
    return {};
  }

  struct WindowPowers {
    double low = 0.0;
    double high = 0.0;
  };

  std::vector<WindowPowers> windows;
  double max_signal_power = 0.0;
  for (size_t offset = 0;
       offset + static_cast<size_t>(silence_slot_size) <= pcm.size();
       offset += static_cast<size_t>(silence_slot_size)) {
    const int16_t* chunk = pcm.data() + offset;
    const double low_power =
        GoertzelPower(chunk, silence_slot_size, config.sample_rate_hz,
                      config.low_freq_hz);
    const double high_power =
        GoertzelPower(chunk, silence_slot_size, config.sample_rate_hz,
                      config.high_freq_hz);
    windows.push_back(WindowPowers{.low = low_power, .high = high_power});
    max_signal_power =
        std::max(max_signal_power, std::max(low_power, high_power));
  }

  if (windows.empty() || max_signal_power <= 0.0) {
    return {};
  }

  const double silence_threshold = max_signal_power * 0.02;
  std::vector<int> bits;
  std::optional<int> current_tone_run_bit;
  for (const WindowPowers& window : windows) {
    const double signal_power = std::max(window.low, window.high);
    if (signal_power < silence_threshold) {
      current_tone_run_bit.reset();
      continue;
    }
    const int bit = window.high > window.low ? 1 : 0;
    if (config.collapse_tone_runs_when_skipping_silence) {
      if (!current_tone_run_bit || *current_tone_run_bit != bit) {
        bits.push_back(bit);
        current_tone_run_bit = bit;
      }
    } else {
      bits.push_back(bit);
    }
  }
  return BitsToBytes(bits);
}

std::vector<uint8_t> DecodeZealPcm16ToBytes(const std::vector<int16_t>& pcm,
                                            const BfskConfig& config) {
  const int bit_chunk_size = ChunkSize(config);
  const int silence_slot_size = SilenceSlotSize(config);
  if (bit_chunk_size <= 0 || silence_slot_size <= 0 ||
      config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }
  if (pcm.empty()) {
    return {};
  }

  const auto powers_for = [&](std::size_t offset,
                              ZealTimingClass timing_class) {
    const std::size_t sample_count =
        ZealSamplesForTimingClass(timing_class, config);
    const int window_size =
        static_cast<int>(std::min(sample_count, pcm.size() - offset));
    const int16_t* chunk = pcm.data() + offset;
    const double low_power =
        GoertzelPower(chunk, window_size, config.sample_rate_hz,
                      ZealLowCarrierForTimingClass(timing_class));
    const double high_power =
        GoertzelPower(chunk, window_size, config.sample_rate_hz,
                      ZealHighCarrierForTimingClass(timing_class));
    return std::pair<double, double>{low_power, high_power};
  };

  double max_signal_power = 0.0;
  for (std::size_t offset = 0, bit_position = 0; offset < pcm.size();
       ++bit_position) {
    const ZealTimingClass timing_class =
        ZealTimingClassForBitPosition(bit_position);
    const std::size_t sample_count =
        ZealSamplesForTimingClass(timing_class, config);
    if (sample_count == 0 || offset + sample_count > pcm.size()) {
      break;
    }
    const auto [low_power, high_power] = powers_for(offset, timing_class);
    max_signal_power =
        std::max(max_signal_power, std::max(low_power, high_power));
    offset += sample_count;
  }
  if (max_signal_power <= 0.0) {
    return {};
  }

  const double silence_threshold = max_signal_power * 0.03;
  std::vector<int> bits;
  bool force_burst_after_pause = false;
  for (std::size_t offset = 0, bit_position = 0; offset < pcm.size();) {
    const ZealTimingClass timing_class =
        force_burst_after_pause ? ZealTimingClass::kBurst
                                : ZealTimingClassForBitPosition(bit_position);
    const std::size_t sample_count =
        ZealSamplesForTimingClass(timing_class, config);
    if (sample_count == 0 || offset + sample_count > pcm.size()) {
      break;
    }
    const auto [low_power, high_power] = powers_for(offset, timing_class);
    const double signal_power = std::max(low_power, high_power);
    if (signal_power < silence_threshold) {
      offset += std::min(static_cast<std::size_t>(silence_slot_size),
                         pcm.size() - offset);
      force_burst_after_pause = true;
      continue;
    }

    bits.push_back(high_power > low_power ? 1 : 0);
    offset += sample_count;
    ++bit_position;
    force_burst_after_pause = false;
  }
  return BitsToBytes(bits);
}

}  // namespace bag::flash
