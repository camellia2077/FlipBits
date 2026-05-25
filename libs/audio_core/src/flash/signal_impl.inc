namespace bag::flash {

using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {

float LerpProgress(float begin, float end, float ratio) {
  return begin + (end - begin) * std::clamp(ratio, 0.0f, 1.0f);
}

void ThrowIfEncodeCancelled(const EncodeProgressSink* progress_sink) {
  if (ShouldCancelEncode(progress_sink)) {
    throw EncodeCancelled{};
  }
}

constexpr std::size_t kProgressReportStrideSamples = 256;

void ReportCompletedSampleProgress(const EncodeProgressSink* progress_sink,
                                   EncodeProgressPhase phase,
                                   float progress_begin, float progress_end,
                                   std::size_t completed_samples,
                                   std::size_t total_samples) {
  const float ratio =
      total_samples == 0
          ? 1.0f
          : static_cast<float>(std::min(completed_samples, total_samples)) /
                static_cast<float>(total_samples);
  ReportEncodeProgress(progress_sink, phase,
                       LerpProgress(progress_begin, progress_end, ratio));
}

void MaybeReportCompletedSampleProgress(
    const EncodeProgressSink* progress_sink, EncodeProgressPhase phase,
    float progress_begin, float progress_end, std::size_t completed_samples,
    std::size_t total_samples, std::size_t* last_reported_samples,
    bool force = false) {
  if (last_reported_samples == nullptr) {
    return;
  }

  const std::size_t clamped_completed_samples =
      total_samples == 0 ? 0 : std::min(completed_samples, total_samples);
  if (!force && clamped_completed_samples < total_samples &&
      clamped_completed_samples - *last_reported_samples <
          kProgressReportStrideSamples) {
    return;
  }

  ReportCompletedSampleProgress(progress_sink, phase, progress_begin,
                                progress_end, clamped_completed_samples,
                                total_samples);
  *last_reported_samples = clamped_completed_samples;
}

std::size_t SamplesPerBitForSignalProfile(FlashSignalProfile signal_profile,
                                          const CoreConfig& config) {
  if (config.frame_samples <= 0) {
    return 0;
  }

  switch (signal_profile) {
    case FlashSignalProfile::kLitany:
      return static_cast<std::size_t>(config.frame_samples) *
             static_cast<std::size_t>(6);
    case FlashSignalProfile::kZeal:
      return std::max(static_cast<std::size_t>(1),
                      static_cast<std::size_t>(config.frame_samples) /
                          static_cast<std::size_t>(2));
    case FlashSignalProfile::kHostility:
      return std::max(static_cast<std::size_t>(1),
                      (static_cast<std::size_t>(config.frame_samples) *
                       static_cast<std::size_t>(7)) /
                          static_cast<std::size_t>(8));
    case FlashSignalProfile::kVoid:
      return std::max(static_cast<std::size_t>(1),
                      (static_cast<std::size_t>(config.frame_samples) *
                       static_cast<std::size_t>(5)) /
                          static_cast<std::size_t>(2));
    case FlashSignalProfile::kStandard:
      return std::max(static_cast<std::size_t>(1),
                      (static_cast<std::size_t>(config.frame_samples) *
                       static_cast<std::size_t>(15)) /
                          static_cast<std::size_t>(16));
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

using BitTemplateBank = std::array<std::vector<int16_t>, 2>;

std::vector<int16_t> RenderToneChunk(std::size_t sample_count,
                                     double carrier_freq_hz,
                                     const BfskConfig& config) {
  if (sample_count == 0 || carrier_freq_hz <= 0.0 ||
      config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }

  std::vector<int16_t> pcm(sample_count);
  constexpr double kPi = 3.14159265358979323846;
  const double two_pi_f = 2.0 * kPi * carrier_freq_hz;
  for (std::size_t index = 0; index < sample_count; ++index) {
    const double time =
        static_cast<double>(index) / static_cast<double>(config.sample_rate_hz);
    const double sample = config.amplitude * std::sin(two_pi_f * time);
    const double clamped = std::max(-1.0, std::min(1.0, sample));
    pcm[index] = static_cast<int16_t>(clamped * 32767.0);
  }
  return pcm;
}

}  // namespace

class FlashChunkRenderer::Impl {
 public:
  Impl(const FlashPayloadChunk& chunk, const BfskConfig& config,
       std::vector<int16_t>* out_pcm)
      : chunk_(chunk), config_(config), out_pcm_(out_pcm) {
    if (out_pcm_ == nullptr) {
      throw std::invalid_argument("Flash chunk renderer requires PCM output.");
    }
    if (chunk_.sample_offset + chunk_.sample_count > out_pcm_->size()) {
      throw std::invalid_argument("Payload layout chunk exceeds PCM bounds.");
    }
    if (chunk_.kind == FlashPayloadSegmentKind::kBit &&
        (chunk_.carrier_freq_hz <= 0.0 || config_.sample_rate_hz <= 0)) {
      throw std::invalid_argument("Invalid BFSK config.");
    }
  }

  std::size_t TotalWork() const { return chunk_.sample_count; }
  bool Finished() const { return sample_index_ >= chunk_.sample_count; }

  FlashChunkRenderProgress Pump(std::size_t work_budget) {
    const std::size_t normalized_budget =
        std::max(work_budget, static_cast<std::size_t>(1));
    std::size_t advanced = 0;
    while (!Finished() && advanced < normalized_budget) {
      const std::size_t write_index = chunk_.sample_offset + sample_index_;
      if (chunk_.kind == FlashPayloadSegmentKind::kSilence) {
        (*out_pcm_)[write_index] = 0;
      } else {
        constexpr double kPi = 3.14159265358979323846;
        const double omega = 2.0 * kPi * chunk_.carrier_freq_hz;
        const double time = static_cast<double>(sample_index_) /
                            static_cast<double>(config_.sample_rate_hz);
        const double sample = config_.amplitude * std::sin(omega * time);
        const double clamped = std::clamp(sample, -1.0, 1.0);
        (*out_pcm_)[write_index] =
            static_cast<int16_t>(std::lround(clamped * 32767.0));
      }
      ++sample_index_;
      ++advanced;
    }
    return FlashChunkRenderProgress{
        .completed_work = sample_index_,
        .total_work = chunk_.sample_count,
        .finished = Finished(),
    };
  }

 private:
  FlashPayloadChunk chunk_{};
  BfskConfig config_{};
  std::vector<int16_t>* out_pcm_ = nullptr;
  std::size_t sample_index_ = 0;
};

FlashChunkRenderer::FlashChunkRenderer(const FlashPayloadChunk& chunk,
                                       const BfskConfig& config,
                                       std::vector<int16_t>* out_pcm)
    : impl_(std::make_unique<Impl>(chunk, config, out_pcm)) {}

FlashChunkRenderer::~FlashChunkRenderer() = default;

std::size_t FlashChunkRenderer::TotalWork() const {
  return impl_->TotalWork();
}

bool FlashChunkRenderer::Finished() const {
  return impl_->Finished();
}

FlashChunkRenderProgress FlashChunkRenderer::Pump(std::size_t work_budget) {
  return impl_->Pump(work_budget);
}

namespace {

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
  // Shorter decode windows use higher carriers so each bit still contains
  // enough waveform cycles for stable low/high discrimination.
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

double HostilityLowCarrierForBit(std::size_t bit_position,
                                 std::size_t byte_index, uint8_t source_byte,
                                 uint8_t bit_index_in_byte) {
  (void)source_byte;
  static constexpr std::array<double, 8> kJitter = {
      0.0, 58.0, 22.0, 96.0, 44.0, 118.0, 70.0, 34.0,
  };
  const std::size_t hash_index = (bit_position * static_cast<std::size_t>(5) +
                                  byte_index * static_cast<std::size_t>(3) +
                                  static_cast<std::size_t>(bit_index_in_byte)) %
                                 kJitter.size();
  return 500.0 + kJitter[hash_index];
}

double HostilityHighCarrierForBit(std::size_t bit_position,
                                  std::size_t byte_index, uint8_t source_byte,
                                  uint8_t bit_index_in_byte) {
  return HostilityLowCarrierForBit(bit_position, byte_index, source_byte,
                                   bit_index_in_byte) *
         2.0;
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

std::size_t VoidFrameSamples(const BfskConfig& config) {
  if (config.samples_per_silence_slot > 0) {
    return config.samples_per_silence_slot;
  }
  return config.samples_per_bit > 0
             ? std::max(static_cast<std::size_t>(1),
                        (config.samples_per_bit * static_cast<std::size_t>(2)) /
                            static_cast<std::size_t>(5))
             : static_cast<std::size_t>(0);
}

std::size_t ScaleVoidFrameSamples(std::size_t frame_samples,
                                  std::size_t numerator,
                                  std::size_t denominator) {
  if (frame_samples == 0 || denominator == 0) {
    return 0;
  }
  return std::max(static_cast<std::size_t>(1),
                  (frame_samples * numerator) / denominator);
}

std::size_t VoidSamplesForBit(std::size_t bit_position,
                              uint8_t bit_index_in_byte,
                              const BfskConfig& config) {
  const std::size_t frame_samples = VoidFrameSamples(config);
  if (bit_index_in_byte == static_cast<uint8_t>(7)) {
    return ScaleVoidFrameSamples(frame_samples, 11, 4);
  }
  if (((bit_position + static_cast<std::size_t>(3)) %
       static_cast<std::size_t>(19)) == 0) {
    return ScaleVoidFrameSamples(frame_samples, 3, 1);
  }
  return ScaleVoidFrameSamples(frame_samples, 5, 2);
}

double VoidLowCarrierForBit(std::size_t bit_position, std::size_t byte_index,
                            uint8_t bit_index_in_byte) {
  static constexpr std::array<double, 10> kSag = {
      230.0, 222.0, 214.0, 206.0, 198.0, 190.0, 184.0, 180.0, 194.0, 208.0,
  };
  const std::size_t phrase_offset =
      (bit_position + byte_index +
       static_cast<std::size_t>(bit_index_in_byte / static_cast<uint8_t>(4))) %
      kSag.size();
  return kSag[phrase_offset];
}

double VoidHighCarrierForBit(std::size_t bit_position, std::size_t byte_index,
                             uint8_t bit_index_in_byte) {
  return VoidLowCarrierForBit(bit_position, byte_index, bit_index_in_byte) *
         2.0;
}

std::size_t VoidPauseSlotCountAfterBit(std::size_t bit_position,
                                       std::size_t byte_index,
                                       uint8_t bit_index_in_byte) {
  if (bit_index_in_byte == static_cast<uint8_t>(7)) {
    if (((byte_index + static_cast<std::size_t>(1)) %
         static_cast<std::size_t>(9)) == 0) {
      return 6;
    }
    if (((byte_index + static_cast<std::size_t>(1)) %
         static_cast<std::size_t>(3)) == 0) {
      return 2;
    }
    return 1;
  }
  if (((bit_position + static_cast<std::size_t>(5)) %
       static_cast<std::size_t>(37)) == 0) {
    return 4;
  }
  return 0;
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

std::size_t LitanyPeriodicPauseSlotCountAfterByte(
    const std::vector<uint8_t>& bytes, std::size_t byte_index) {
  if (byte_index + static_cast<std::size_t>(1) >= bytes.size() ||
      !IsLikelyUtf8CodePointBoundaryAfter(bytes, byte_index)) {
    return 1;
  }

  const std::size_t cadence_position = byte_index + static_cast<std::size_t>(1);
  if ((cadence_position % static_cast<std::size_t>(12)) == 0) {
    return 5;
  }
  return 1;
}

std::size_t LitanyPauseSlotCountAfterByte(const std::vector<uint8_t>& bytes,
                                          std::size_t byte_index) {
  const uint8_t value = bytes[byte_index];
  switch (value) {
    case static_cast<uint8_t>(' '):
    case static_cast<uint8_t>('\t'):
      return 3;
    case static_cast<uint8_t>('\n'):
    case static_cast<uint8_t>('\r'):
      return 6;
    case static_cast<uint8_t>(','):
    case static_cast<uint8_t>(';'):
    case static_cast<uint8_t>(':'):
      return 4;
    case static_cast<uint8_t>('.'):
    case static_cast<uint8_t>('!'):
    case static_cast<uint8_t>('?'):
      return 8;
    default:
      break;
  }

  if (EndsWithUtf8Bytes(bytes, byte_index, 0xEF, 0xBC, 0x8C) ||
      EndsWithUtf8Bytes(bytes, byte_index, 0xEF, 0xBC, 0x9B) ||
      EndsWithUtf8Bytes(bytes, byte_index, 0xEF, 0xBC, 0x9A)) {
    return 4;
  }
  if (EndsWithUtf8Bytes(bytes, byte_index, 0xE3, 0x80, 0x82) ||
      EndsWithUtf8Bytes(bytes, byte_index, 0xEF, 0xBC, 0x81) ||
      EndsWithUtf8Bytes(bytes, byte_index, 0xEF, 0xBC, 0x9F)) {
    return 8;
  }
  return LitanyPeriodicPauseSlotCountAfterByte(bytes, byte_index);
}

std::uint32_t HashCollapsePause(std::size_t byte_index, uint8_t source_byte,
                                uint8_t bit_index_in_byte,
                                std::size_t bit_position) {
  std::uint32_t value = 2166136261U;
  value ^= static_cast<std::uint32_t>(bit_position + 0x9E3779B9U);
  value *= 16777619U;
  value ^= static_cast<std::uint32_t>(byte_index * 131U);
  value *= 16777619U;
  value ^= static_cast<std::uint32_t>(source_byte);
  value *= 16777619U;
  value ^= static_cast<std::uint32_t>(bit_index_in_byte);
  value *= 16777619U;
  return value;
}

double CollapseHashRatio(std::uint32_t hash, unsigned int shift) {
  return static_cast<double>((hash >> shift) & 0x3FFU) /
         static_cast<double>(0x3FFU);
}

bool CollapseStutterClusterStarts(std::uint32_t hash,
                                  uint8_t bit_index_in_byte) {
  const bool byte_tail = bit_index_in_byte == static_cast<uint8_t>(7);
  const double density = byte_tail ? 0.22 : 0.08;
  return CollapseHashRatio(hash, 0U) <= density;
}

std::size_t CollapseStutterClusterLength(std::uint32_t hash) {
  const double length_roll = CollapseHashRatio(hash, 10U);
  if (length_roll < 0.42) {
    return 2;
  }
  if (length_roll < 0.78) {
    return 3;
  }
  return 4;
}

std::size_t CollapsePanicBreakSlotCount(std::uint32_t hash) {
  return CollapseHashRatio(hash, 20U) <= 0.03 ? static_cast<std::size_t>(5)
                                              : static_cast<std::size_t>(0);
}

std::size_t CollapsePauseSlotCountAfterBit(const std::vector<uint8_t>& bytes,
                                           std::size_t byte_index,
                                           uint8_t source_byte,
                                           uint8_t bit_index_in_byte) {
  const std::size_t bit_position = byte_index * static_cast<std::size_t>(8) +
                                   static_cast<std::size_t>(bit_index_in_byte);
  const std::uint32_t hash = HashCollapsePause(byte_index, source_byte,
                                               bit_index_in_byte, bit_position);
  const std::size_t panic_break = CollapsePanicBreakSlotCount(hash);
  if (panic_break > 0) {
    return panic_break;
  }

  for (std::size_t distance = 0; distance < static_cast<std::size_t>(4);
       ++distance) {
    if (distance > bit_position) {
      break;
    }
    const std::size_t start_position = bit_position - distance;
    const std::size_t start_byte_index =
        start_position / static_cast<std::size_t>(8);
    if (start_byte_index >= bytes.size()) {
      continue;
    }
    const uint8_t start_bit_index =
        static_cast<uint8_t>(start_position % static_cast<std::size_t>(8));
    const uint8_t start_source_byte = bytes[start_byte_index];
    const std::uint32_t start_hash = HashCollapsePause(
        start_byte_index, start_source_byte, start_bit_index, start_position);
    if (!CollapseStutterClusterStarts(start_hash, start_bit_index)) {
      continue;
    }
    if (distance < CollapseStutterClusterLength(start_hash)) {
      return 2;
    }
  }
  return 0;
}

void AppendSilenceChunk(std::size_t slot_count, std::size_t chunk_size,
                        uint8_t source_byte, uint8_t bit_index_in_byte,
                        std::size_t byte_index, FlashPayloadLayout* layout) {
  if (layout == nullptr || slot_count == 0 || chunk_size == 0) {
    return;
  }
  FlashPayloadChunk silence{};
  silence.kind = FlashPayloadSegmentKind::kSilence;
  silence.source_byte = source_byte;
  silence.bit_index_in_byte = bit_index_in_byte;
  silence.byte_index = byte_index;
  silence.sample_offset = layout->payload_sample_count;
  silence.sample_count = chunk_size * slot_count;
  silence.carrier_freq_hz = 0.0;
  layout->chunks.push_back(silence);
  layout->payload_sample_count += silence.sample_count;
}

}  // namespace

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

FlashPayloadLayout BuildPayloadLayout(const std::vector<uint8_t>& bytes,
                                      const BfskConfig& config) {
  return BuildPayloadLayoutForVoicing(bytes, config,
                                      FlashVoicingFlavor::kStandard);
}

FlashPayloadLayout BuildPayloadLayoutForVoicing(
    const std::vector<uint8_t>& bytes, const BfskConfig& config,
    FlashVoicingFlavor flavor) {
  const int chunk_size = ChunkSize(config);
  if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }

  FlashPayloadLayout layout{};
  layout.chunks.reserve(bytes.size() * static_cast<std::size_t>(8));
  const FlashVoicingFlavor normalized_flavor =
      NormalizeSignalVoicingFlavor(flavor);
  bool force_zeal_burst_after_pause = false;

  // The layout is the payload timeline, not just the data-bit list. Silence
  // chunks are real zero-sample gaps used by cadence profiles, but they do not
  // carry bits and are skipped by the matching gap-aware decoder.
  for (std::size_t byte_index = 0; byte_index < bytes.size(); ++byte_index) {
    const uint8_t value = bytes[byte_index];
    for (int shift = 7; shift >= 0; --shift) {
      const uint8_t bit = static_cast<uint8_t>((value >> shift) & 0x01U);
      const uint8_t bit_index_in_byte = static_cast<std::uint8_t>(7 - shift);
      const std::size_t bit_position =
          byte_index * static_cast<std::size_t>(8) +
          static_cast<std::size_t>(bit_index_in_byte);
      FlashPayloadChunk chunk{};
      chunk.kind = FlashPayloadSegmentKind::kBit;
      chunk.bit_value = bit;
      chunk.source_byte = value;
      chunk.bit_index_in_byte = bit_index_in_byte;
      chunk.byte_index = byte_index;
      chunk.sample_offset = layout.payload_sample_count;
      if (normalized_flavor == FlashVoicingFlavor::kZeal) {
        const ZealTimingClass timing_class =
            force_zeal_burst_after_pause
                ? ZealTimingClass::kBurst
                : ZealTimingClassForBitPosition(bit_position);
        chunk.sample_count = ZealSamplesForTimingClass(timing_class, config);
        chunk.carrier_freq_hz =
            bit == 1 ? ZealHighCarrierForTimingClass(timing_class)
                     : ZealLowCarrierForTimingClass(timing_class);
        force_zeal_burst_after_pause = false;
      } else if (normalized_flavor == FlashVoicingFlavor::kHostility) {
        chunk.sample_count = static_cast<size_t>(chunk_size);
        chunk.carrier_freq_hz =
            bit == 1 ? HostilityHighCarrierForBit(bit_position, byte_index,
                                                  value, bit_index_in_byte)
                     : HostilityLowCarrierForBit(bit_position, byte_index,
                                                 value, bit_index_in_byte);
      } else if (normalized_flavor == FlashVoicingFlavor::kCollapse) {
        chunk.sample_count = static_cast<size_t>(chunk_size);
        chunk.carrier_freq_hz =
            bit == 1 ? CollapseHighCarrierForBit(bit_position, byte_index,
                                                 value, bit_index_in_byte)
                     : CollapseLowCarrierForBit(bit_position, byte_index, value,
                                                bit_index_in_byte);
      } else if (normalized_flavor == FlashVoicingFlavor::kVoid) {
        chunk.sample_count =
            VoidSamplesForBit(bit_position, bit_index_in_byte, config);
        chunk.carrier_freq_hz =
            bit == 1 ? VoidHighCarrierForBit(bit_position, byte_index,
                                             bit_index_in_byte)
                     : VoidLowCarrierForBit(bit_position, byte_index,
                                            bit_index_in_byte);
      } else {
        chunk.sample_count = static_cast<size_t>(chunk_size);
        chunk.carrier_freq_hz =
            bit == 1 ? config.high_freq_hz : config.low_freq_hz;
      }
      layout.chunks.push_back(chunk);
      layout.payload_sample_count += chunk.sample_count;

      std::size_t pause_slots = 0;
      if (normalized_flavor == FlashVoicingFlavor::kLitany) {
        pause_slots = bit_index_in_byte == static_cast<uint8_t>(7)
                          ? LitanyPauseSlotCountAfterByte(bytes, byte_index)
                          : static_cast<std::size_t>(2);
      } else if (normalized_flavor == FlashVoicingFlavor::kCollapse) {
        pause_slots = CollapsePauseSlotCountAfterBit(bytes, byte_index, value,
                                                     bit_index_in_byte);
      } else if (normalized_flavor == FlashVoicingFlavor::kZeal &&
                 bit_index_in_byte == static_cast<uint8_t>(7)) {
        pause_slots = ZealPauseSlotCountAfterByte(bytes, byte_index);
      } else if (normalized_flavor == FlashVoicingFlavor::kVoid) {
        pause_slots = VoidPauseSlotCountAfterBit(bit_position, byte_index,
                                                 bit_index_in_byte);
      }
      AppendSilenceChunk(pause_slots, config.samples_per_silence_slot, value,
                         bit_index_in_byte, byte_index, &layout);
      if (normalized_flavor == FlashVoicingFlavor::kZeal && pause_slots > 0) {
        force_zeal_burst_after_pause = true;
      }
    }
  }
  return layout;
}

std::vector<int16_t> EncodePayloadLayoutToPcm16(
    const FlashPayloadLayout& layout, const BfskConfig& config,
    const EncodeProgressSink* progress_sink, float progress_begin,
    float progress_end) {
  const int chunk_size = ChunkSize(config);
  if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }
  const std::size_t total_samples = layout.payload_sample_count;
  std::vector<int16_t> pcm;
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_begin);
  ThrowIfEncodeCancelled(progress_sink);
  if (total_samples == 0) {
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                         progress_end);
    return pcm;
  }

  pcm.resize(total_samples);
  std::size_t rendered_samples = 0;
  std::size_t last_reported_samples = 0;
  for (std::size_t chunk_index = 0; chunk_index < layout.chunks.size();
       ++chunk_index) {
    const FlashPayloadChunk& chunk = layout.chunks[chunk_index];
    ThrowIfEncodeCancelled(progress_sink);
    FlashChunkRenderer renderer(chunk, config, &pcm);
    while (!renderer.Finished()) {
      ThrowIfEncodeCancelled(progress_sink);
      const FlashChunkRenderProgress progress =
          renderer.Pump(kProgressReportStrideSamples);
      rendered_samples = chunk.sample_offset + progress.completed_work;
      if (rendered_samples > total_samples) {
        rendered_samples = total_samples;
      }
      MaybeReportCompletedSampleProgress(
          progress_sink, EncodeProgressPhase::kRenderingPcm, progress_begin,
          progress_end, rendered_samples, total_samples, &last_reported_samples,
          progress.finished);
    }
  }
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_end);

  return pcm;
}

std::vector<int16_t> EncodeBytesToPcm16(const std::vector<uint8_t>& bytes,
                                        const BfskConfig& config,
                                        const EncodeProgressSink* progress_sink,
                                        float progress_begin,
                                        float progress_end) {
  return EncodePayloadLayoutToPcm16(BuildPayloadLayout(bytes, config), config,
                                    progress_sink, progress_begin,
                                    progress_end);
}

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
    const double low_power = GoertzelPower(
        chunk, chunk_size, config.sample_rate_hz, config.low_freq_hz);
    const double high_power = GoertzelPower(
        chunk, chunk_size, config.sample_rate_hz, config.high_freq_hz);
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
    const std::size_t byte_index = bit_position / static_cast<std::size_t>(8);
    const uint8_t bit_index_in_byte =
        static_cast<uint8_t>(bit_position % static_cast<std::size_t>(8));
    const double low_freq = CollapseLowCarrierForBit(bit_position, byte_index,
                                                     0U, bit_index_in_byte);
    const double high_freq = CollapseHighCarrierForBit(bit_position, byte_index,
                                                       0U, bit_index_in_byte);
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
    const double low_power = GoertzelPower(
        chunk, silence_slot_size, config.sample_rate_hz, config.low_freq_hz);
    const double high_power = GoertzelPower(
        chunk, silence_slot_size, config.sample_rate_hz, config.high_freq_hz);
    windows.push_back(WindowPowers{.low = low_power, .high = high_power});
    max_signal_power =
        std::max(max_signal_power, std::max(low_power, high_power));
  }

  if (windows.empty() || max_signal_power <= 0.0) {
    return {};
  }

  // Litany/Collapse pauses are intentionally rendered as true silence inside
  // the payload. Use a relative energy gate so those gaps disappear before the
  // remaining voiced low/high windows are converted back into bits.
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
