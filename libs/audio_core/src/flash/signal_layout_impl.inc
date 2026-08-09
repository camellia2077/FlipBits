namespace bag::flash {

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

int ChunkSize(const BfskConfig& config) {
  if (config.samples_per_bit == 0) {
    return 0;
  }
  if (config.samples_per_bit >
      static_cast<std::size_t>(std::numeric_limits<int>::max())) {
    throw std::invalid_argument("BFSK samples_per_bit exceeds supported range.");
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

}  // namespace

class FlashChunkRenderer::Impl {
 public:
  Impl(const FlashPayloadChunk& chunk, const BfskConfig& config,
       std::vector<std::int16_t>* out_pcm)
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
            static_cast<std::int16_t>(std::lround(clamped * 32767.0));
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
  std::vector<std::int16_t>* out_pcm_ = nullptr;
  std::size_t sample_index_ = 0;
};

FlashChunkRenderer::FlashChunkRenderer(const FlashPayloadChunk& chunk,
                                       const BfskConfig& config,
                                       std::vector<std::int16_t>* out_pcm)
    : impl_(std::make_unique<Impl>(chunk, config, out_pcm)) {}

FlashChunkRenderer::~FlashChunkRenderer() = default;

std::size_t FlashChunkRenderer::TotalWork() const {
  return impl_->TotalWork();
}

bool FlashChunkRenderer::Finished() const { return impl_->Finished(); }

FlashChunkRenderProgress FlashChunkRenderer::Pump(std::size_t work_budget) {
  return impl_->Pump(work_budget);
}

namespace {

FlashVoicingFlavor NormalizeLayoutFlavor(FlashVoicingFlavor flavor) {
  return NormalizeSignalVoicingFlavor(flavor);
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

double HostilityLowCarrierForBit(std::size_t bit_position,
                                 std::size_t byte_index, uint8_t source_byte,
                                 uint8_t bit_index_in_byte) {
  (void)source_byte;
  static constexpr std::array<double, 8> kJitter = {
      0.0, 58.0, 22.0, 96.0, 44.0, 118.0, 70.0, 34.0,
  };
  const std::size_t hash_index =
      (bit_position * 5u + byte_index * 3u + bit_index_in_byte) %
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
      (bit_position + byte_index + bit_index_in_byte / 2u) % kSag.size();
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
                        (config.samples_per_bit * 2u) / 5u)
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
  if (bit_index_in_byte == 7) {
    return ScaleVoidFrameSamples(frame_samples, 11, 4);
  }
  if ((bit_position + 3u) % 19u == 0) {
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
      (bit_position + byte_index + bit_index_in_byte / 4u) % kSag.size();
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
  if (bit_index_in_byte == 7) {
    if ((byte_index + 1u) % 9u == 0) {
      return 6;
    }
    if ((byte_index + 1u) % 3u == 0) {
      return 2;
    }
    return 1;
  }
  if ((bit_position + 5u) % 37u == 0) {
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

bool IsUtf8ContinuationByte(uint8_t value) {
  return (value & 0xC0U) == 0x80U;
}

bool IsLikelyUtf8CodePointBoundaryAfter(const std::vector<uint8_t>& bytes,
                                        std::size_t byte_index) {
  return byte_index + 1u >= bytes.size() ||
         !IsUtf8ContinuationByte(bytes[byte_index + 1u]);
}

bool EndsWithUtf8Bytes(const std::vector<uint8_t>& bytes,
                       std::size_t byte_index, uint8_t first, uint8_t second,
                       uint8_t third) {
  if (byte_index < 2u) {
    return false;
  }
  return bytes[byte_index - 2u] == first && bytes[byte_index - 1u] == second &&
         bytes[byte_index] == third;
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

std::size_t LitanyPeriodicPauseSlotCountAfterByte(
    const std::vector<uint8_t>& bytes, std::size_t byte_index) {
  if (byte_index + 1u >= bytes.size() ||
      !IsLikelyUtf8CodePointBoundaryAfter(bytes, byte_index)) {
    return 1;
  }
  const std::size_t cadence_position = byte_index + 1u;
  return (cadence_position % 12u) == 0 ? 5u : 1u;
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
  const bool byte_tail = bit_index_in_byte == 7;
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
  return CollapseHashRatio(hash, 20U) <= 0.03 ? 5u : 0u;
}

std::size_t CollapsePauseSlotCountAfterBit(const std::vector<uint8_t>& bytes,
                                           std::size_t byte_index,
                                           uint8_t source_byte,
                                           uint8_t bit_index_in_byte) {
  const std::size_t bit_position = byte_index * 8u + bit_index_in_byte;
  const std::uint32_t hash = HashCollapsePause(
      byte_index, source_byte, bit_index_in_byte, bit_position);
  const std::size_t panic_break = CollapsePanicBreakSlotCount(hash);
  if (panic_break > 0) {
    return panic_break;
  }
  for (std::size_t distance = 0; distance < 4u; ++distance) {
    if (distance > bit_position) {
      break;
    }
    const std::size_t start_position = bit_position - distance;
    const std::size_t start_byte_index = start_position / 8u;
    if (start_byte_index >= bytes.size()) {
      continue;
    }
    const uint8_t start_bit_index =
        static_cast<uint8_t>(start_position % 8u);
    const std::uint8_t start_source_byte = bytes[start_byte_index];
    const std::uint32_t start_hash = HashCollapsePause(
        start_byte_index, start_source_byte, start_bit_index, start_position);
    if (CollapseStutterClusterStarts(start_hash, start_bit_index) &&
        distance < CollapseStutterClusterLength(start_hash)) {
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

FlashPayloadLayout BuildPayloadLayout(const std::vector<std::uint8_t>& bytes,
                                      const BfskConfig& config) {
  return BuildPayloadLayoutForVoicing(bytes, config,
                                      FlashVoicingFlavor::kStandard);
}

FlashPayloadLayout BuildPayloadLayoutForVoicing(
    const std::vector<std::uint8_t>& bytes, const BfskConfig& config,
    FlashVoicingFlavor flavor) {
  const int chunk_size = ChunkSize(config);
  if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }

  FlashPayloadLayout layout{};
  layout.chunks.reserve(bytes.size() * 8u);
  const FlashVoicingFlavor normalized_flavor = NormalizeLayoutFlavor(flavor);
  bool force_zeal_burst_after_pause = false;
  for (std::size_t byte_index = 0; byte_index < bytes.size(); ++byte_index) {
    const std::uint8_t value = bytes[byte_index];
    for (int shift = 7; shift >= 0; --shift) {
      const std::uint8_t bit =
          static_cast<std::uint8_t>((value >> shift) & 0x01U);
      const std::uint8_t bit_index_in_byte =
          static_cast<std::uint8_t>(7 - shift);
      const std::size_t bit_position = byte_index * 8u + bit_index_in_byte;
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
        chunk.sample_count = static_cast<std::size_t>(chunk_size);
        chunk.carrier_freq_hz =
            bit == 1 ? HostilityHighCarrierForBit(
                           bit_position, byte_index, value, bit_index_in_byte)
                     : HostilityLowCarrierForBit(
                           bit_position, byte_index, value, bit_index_in_byte);
      } else if (normalized_flavor == FlashVoicingFlavor::kCollapse) {
        chunk.sample_count = static_cast<std::size_t>(chunk_size);
        chunk.carrier_freq_hz =
            bit == 1 ? CollapseHighCarrierForBit(
                           bit_position, byte_index, value, bit_index_in_byte)
                     : CollapseLowCarrierForBit(
                           bit_position, byte_index, value, bit_index_in_byte);
      } else if (normalized_flavor == FlashVoicingFlavor::kVoid) {
        chunk.sample_count =
            VoidSamplesForBit(bit_position, bit_index_in_byte, config);
        chunk.carrier_freq_hz =
            bit == 1 ? VoidHighCarrierForBit(bit_position, byte_index,
                                             bit_index_in_byte)
                     : VoidLowCarrierForBit(bit_position, byte_index,
                                            bit_index_in_byte);
      } else {
        chunk.sample_count = static_cast<std::size_t>(chunk_size);
        chunk.carrier_freq_hz = bit == 1 ? config.high_freq_hz : config.low_freq_hz;
      }
      layout.chunks.push_back(chunk);
      layout.payload_sample_count += chunk.sample_count;

      std::size_t pause_slots = 0;
      if (normalized_flavor == FlashVoicingFlavor::kLitany) {
        pause_slots = bit_index_in_byte == 7
                          ? LitanyPauseSlotCountAfterByte(bytes, byte_index)
                          : 2u;
      } else if (normalized_flavor == FlashVoicingFlavor::kCollapse) {
        pause_slots = CollapsePauseSlotCountAfterBit(
            bytes, byte_index, value, bit_index_in_byte);
      } else if (normalized_flavor == FlashVoicingFlavor::kZeal &&
                 bit_index_in_byte == 7) {
        pause_slots = ZealPauseSlotCountAfterByte(bytes, byte_index);
      } else if (normalized_flavor == FlashVoicingFlavor::kVoid) {
        pause_slots = VoidPauseSlotCountAfterBit(
            bit_position, byte_index, bit_index_in_byte);
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

std::vector<std::int16_t> EncodePayloadLayoutToPcm16(
    const FlashPayloadLayout& layout, const BfskConfig& config,
    const EncodeProgressSink* progress_sink, float progress_begin,
    float progress_end) {
  const int chunk_size = ChunkSize(config);
  if (chunk_size <= 0 || config.sample_rate_hz <= 0) {
    throw std::invalid_argument("Invalid BFSK config.");
  }
  const std::size_t total_samples = layout.payload_sample_count;
  std::vector<std::int16_t> pcm;
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

}  // namespace bag::flash
