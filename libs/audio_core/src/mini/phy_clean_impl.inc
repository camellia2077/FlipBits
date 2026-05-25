namespace bag::mini_mode {

using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {

constexpr double kPi = 3.14159265358979323846;
constexpr double kSilenceRmsThreshold = 0.04;
constexpr std::size_t kProgressReportStrideSamples = 256;

bool IsValidConfig(const MorseToneConfig& config) {
  return config.sample_rate_hz > 0 && config.unit_samples > 0 &&
         config.amplitude > 0.0 && config.tone_freq_hz > 0.0;
}

float LerpProgress(float begin, float end, float ratio) {
  return begin + (end - begin) * std::clamp(ratio, 0.0f, 1.0f);
}

void ReportSampleProgress(const EncodeProgressSink* progress_sink,
                          float progress_begin, float progress_end,
                          std::size_t completed_samples,
                          std::size_t total_samples) {
  const float ratio =
      total_samples == 0
          ? 1.0f
          : static_cast<float>(std::min(completed_samples, total_samples)) /
                static_cast<float>(total_samples);
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       LerpProgress(progress_begin, progress_end, ratio));
}

std::size_t EncodedPayloadUnitCount(const std::vector<uint8_t>& payload) {
  std::size_t units = 0;
  for (std::size_t index = 0; index < payload.size(); ++index) {
    units += MorseUnitCountForPayloadByte(payload[index]);
    units += MorseTrailingGapUnits(payload, index);
  }
  return units;
}

void AppendSilenceUnits(std::size_t unit_count, const MorseToneConfig& config,
                        std::vector<int16_t>* out_pcm) {
  if (out_pcm == nullptr || unit_count == 0) {
    return;
  }
  out_pcm->insert(out_pcm->end(),
                  unit_count * static_cast<std::size_t>(config.unit_samples),
                  0);
}

void AppendToneUnits(std::size_t unit_count, const MorseToneConfig& config,
                     std::vector<int16_t>* out_pcm) {
  if (out_pcm == nullptr || unit_count == 0) {
    return;
  }
  const std::size_t sample_count =
      unit_count * static_cast<std::size_t>(config.unit_samples);
  const std::size_t start_offset = out_pcm->size();
  out_pcm->resize(start_offset + sample_count);
  const double omega = 2.0 * kPi * config.tone_freq_hz;
  for (std::size_t index = 0; index < sample_count; ++index) {
    const double time =
        static_cast<double>(index) / static_cast<double>(config.sample_rate_hz);
    const double sample = config.amplitude * std::sin(omega * time);
    const double clamped = std::clamp(sample, -1.0, 1.0);
    (*out_pcm)[start_offset + index] =
        static_cast<int16_t>(std::lround(clamped * 32767.0));
  }
}

double ChunkRms(const std::vector<int16_t>& pcm, std::size_t start,
                std::size_t end) {
  if (end <= start || start >= pcm.size()) {
    return 0.0;
  }
  end = std::min(end, pcm.size());
  double sum = 0.0;
  for (std::size_t index = start; index < end; ++index) {
    const double sample = static_cast<double>(pcm[index]) / 32767.0;
    sum += sample * sample;
  }
  return std::sqrt(sum / static_cast<double>(end - start));
}

void FlushDecodedPattern(std::string* pattern,
                         std::vector<uint8_t>* out_payload) {
  if (pattern == nullptr || out_payload == nullptr || pattern->empty()) {
    return;
  }
  const char decoded = CharForMorsePattern(*pattern);
  if (decoded != '\0') {
    out_payload->push_back(static_cast<uint8_t>(decoded));
  }
  pattern->clear();
}

class MiniDecoder final : public ITransportDecoder {
 public:
  explicit MiniDecoder(CoreConfig config) : config_(config) {}

  ErrorCode PushPcm(const PcmBlock& block) override {
    if (block.samples == nullptr || block.sample_count == 0) {
      return ErrorCode::kInvalidArgument;
    }
    buffered_pcm_.insert(buffered_pcm_.end(), block.samples,
                         block.samples + block.sample_count);
    has_pending_result_ = true;
    return ErrorCode::kOk;
  }

  ErrorCode PollDecodeResult(DecodeResult* out_result) override {
    if (out_result == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    if (!has_pending_result_ || buffered_pcm_.empty()) {
      out_result->text.clear();
      out_result->raw_payload_bytes.clear();
      out_result->raw_payload_available = false;
      out_result->follow_data = PayloadFollowData{};
      out_result->follow_available = false;
      out_result->text_follow_data = TextFollowData{};
      out_result->text_follow_available = false;
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kMini;
      out_result->text_status = DecodeContentStatus::kUnavailable;
      return ErrorCode::kNotReady;
    }

    const ErrorCode payload_code =
        DecodePcm16ToPayload(buffered_pcm_, MakeMorseToneConfig(config_),
                             &out_result->raw_payload_bytes);
    if (payload_code != ErrorCode::kOk) {
      out_result->text.clear();
      out_result->raw_payload_bytes.clear();
      out_result->raw_payload_available = false;
      out_result->follow_data = PayloadFollowData{};
      out_result->follow_available = false;
      out_result->text_follow_data = TextFollowData{};
      out_result->text_follow_available = false;
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kMini;
      out_result->text_status = DecodeContentStatus::kInternalError;
      return ErrorCode::kInternal;
    }

    out_result->raw_payload_available = true;
    out_result->follow_data =
        BuildPayloadFollowData(config_, out_result->raw_payload_bytes);
    out_result->follow_available = out_result->follow_data.available;
    out_result->text.clear();
    const ErrorCode text_code =
        DecodePayloadToText(out_result->raw_payload_bytes, &out_result->text);
    out_result->text_status = text_code == ErrorCode::kOk
                                  ? DecodeContentStatus::kOk
                                  : DecodeContentStatus::kInvalidTextPayload;
    if (text_code != ErrorCode::kOk) {
      out_result->text.clear();
    }
    out_result->text_follow_data =
        text_code == ErrorCode::kOk
            ? BuildTextFollowData(out_result->follow_data, out_result->text)
            : TextFollowData{};
    out_result->text_follow_available = out_result->text_follow_data.available;
    out_result->complete = true;
    out_result->confidence = 1.0f;
    out_result->mode = TransportMode::kMini;
    has_pending_result_ = false;
    return ErrorCode::kOk;
  }

  ErrorCode PollTextResult(TextResult* out_result) override {
    if (out_result == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    DecodeResult decode_result{};
    const ErrorCode code = PollDecodeResult(&decode_result);
    if (code != ErrorCode::kOk) {
      out_result->text.clear();
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kMini;
      return code;
    }
    out_result->text = decode_result.text;
    out_result->complete = decode_result.complete;
    out_result->confidence = decode_result.confidence;
    out_result->mode = decode_result.mode;
    return ErrorCode::kOk;
  }

  void Reset() override {
    buffered_pcm_.clear();
    has_pending_result_ = false;
  }

 private:
  CoreConfig config_;
  std::vector<int16_t> buffered_pcm_;
  bool has_pending_result_ = false;
};

}  // namespace

MorseToneConfig MakeMorseToneConfig(const CoreConfig& config) {
  MorseToneConfig out{};
  out.sample_rate_hz = config.sample_rate_hz;
  out.unit_samples = config.frame_samples;
  return out;
}

class ToneUnitRenderer::Impl {
 public:
  Impl(const std::vector<uint8_t>& payload, std::size_t payload_index,
       const MorseToneConfig& config, std::vector<int16_t>* out_pcm)
      : payload_(payload),
        payload_index_(payload_index),
        config_(config),
        out_pcm_(out_pcm) {
    if (out_pcm_ == nullptr || !IsValidConfig(config_) ||
        payload_index_ >= payload_.size()) {
      throw std::invalid_argument("Invalid mini tone renderer state.");
    }
    BuildSegments();
  }

  std::size_t TotalWork() const { return total_work_; }
  bool Finished() const { return completed_work_ >= total_work_; }

  ToneUnitRenderProgress Pump(std::size_t work_budget) {
    const std::size_t normalized_budget =
        std::max(work_budget, static_cast<std::size_t>(1));
    std::size_t advanced = 0;
    while (!Finished() && advanced < normalized_budget) {
      if (segment_index_ >= segments_.size()) {
        break;
      }
      const Segment& segment = segments_[segment_index_];
      const std::size_t write_index = start_offset_ + completed_work_;
      if (!segment.is_tone) {
        (*out_pcm_)[write_index] = 0;
      } else {
        constexpr double kPi = 3.14159265358979323846;
        const double omega = 2.0 * kPi * config_.tone_freq_hz;
        const double time = static_cast<double>(segment_sample_index_) /
                            static_cast<double>(config_.sample_rate_hz);
        const double sample = config_.amplitude * std::sin(omega * time);
        (*out_pcm_)[write_index] = static_cast<int16_t>(
            std::lround(std::clamp(sample, -1.0, 1.0) * 32767.0));
      }
      ++segment_sample_index_;
      ++completed_work_;
      ++advanced;
      if (segment_sample_index_ >= segment.sample_count) {
        ++segment_index_;
        segment_sample_index_ = 0;
      }
    }
    return ToneUnitRenderProgress{
        .completed_work = completed_work_,
        .total_work = total_work_,
        .finished = Finished(),
    };
  }

 private:
  struct Segment {
    bool is_tone = false;
    std::size_t sample_count = 0;
  };

  void AppendSegment(bool is_tone, std::size_t unit_count) {
    if (unit_count == 0) {
      return;
    }
    const std::size_t sample_count =
        unit_count * static_cast<std::size_t>(config_.unit_samples);
    segments_.push_back(
        Segment{.is_tone = is_tone, .sample_count = sample_count});
    total_work_ += sample_count;
  }

  void BuildSegments() {
    start_offset_ = out_pcm_->size();
    const char ch = static_cast<char>(payload_[payload_index_]);
    if (ch == ' ') {
      AppendSegment(false, kMorseWordGapUnits);
    } else {
      const std::string_view pattern = MorsePatternForChar(ch);
      if (pattern.empty()) {
        throw std::invalid_argument("Invalid mini Morse symbol.");
      }
      for (std::size_t index = 0; index < pattern.size(); ++index) {
        AppendSegment(true,
                      pattern[index] == '-' ? kMorseDashUnits : kMorseDotUnits);
        if (index + 1 < pattern.size()) {
          AppendSegment(false, kMorseElementGapUnits);
        }
      }
      AppendSegment(false, MorseTrailingGapUnits(payload_, payload_index_));
    }
    out_pcm_->resize(start_offset_ + total_work_);
  }

  const std::vector<uint8_t>& payload_;
  std::size_t payload_index_ = 0;
  MorseToneConfig config_{};
  std::vector<int16_t>* out_pcm_ = nullptr;
  std::vector<Segment> segments_;
  std::size_t start_offset_ = 0;
  std::size_t total_work_ = 0;
  std::size_t completed_work_ = 0;
  std::size_t segment_index_ = 0;
  std::size_t segment_sample_index_ = 0;
};

ToneUnitRenderer::ToneUnitRenderer(const std::vector<uint8_t>& payload,
                                   std::size_t payload_index,
                                   const MorseToneConfig& config,
                                   std::vector<int16_t>* out_pcm)
    : impl_(std::make_unique<Impl>(payload, payload_index, config, out_pcm)) {}

ToneUnitRenderer::~ToneUnitRenderer() = default;

std::size_t ToneUnitRenderer::TotalWork() const {
  return impl_->TotalWork();
}

bool ToneUnitRenderer::Finished() const {
  return impl_->Finished();
}

ToneUnitRenderProgress ToneUnitRenderer::Pump(std::size_t work_budget) {
  return impl_->Pump(work_budget);
}

ErrorCode EncodePayloadToPcm16(const std::vector<uint8_t>& payload,
                               const MorseToneConfig& config,
                               std::vector<int16_t>* out_pcm,
                               const EncodeProgressSink* progress_sink,
                               float progress_begin, float progress_end) {
  if (out_pcm == nullptr || !IsValidConfig(config)) {
    return ErrorCode::kInvalidArgument;
  }

  out_pcm->clear();
  const std::size_t total_samples =
      EncodedPayloadUnitCount(payload) *
      static_cast<std::size_t>(config.unit_samples);
  out_pcm->reserve(total_samples);
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_begin);

  std::size_t last_reported_samples = 0;
  for (std::size_t byte_index = 0; byte_index < payload.size(); ++byte_index) {
    if (ShouldCancelEncode(progress_sink)) {
      out_pcm->clear();
      return ErrorCode::kCancelled;
    }
    try {
      ToneUnitRenderer renderer(payload, byte_index, config, out_pcm);
      const std::size_t chunk_start_offset =
          out_pcm->size() - renderer.TotalWork();
      while (!renderer.Finished()) {
        if (ShouldCancelEncode(progress_sink)) {
          out_pcm->clear();
          return ErrorCode::kCancelled;
        }
        const ToneUnitRenderProgress progress =
            renderer.Pump(kProgressReportStrideSamples);
        const std::size_t rendered_samples =
            chunk_start_offset + progress.completed_work;
        if (rendered_samples - last_reported_samples >=
                kProgressReportStrideSamples ||
            progress.finished) {
          ReportSampleProgress(progress_sink, progress_begin, progress_end,
                               rendered_samples, total_samples);
          last_reported_samples = rendered_samples;
        }
      }
    } catch (const std::invalid_argument&) {
      out_pcm->clear();
      return ErrorCode::kInvalidArgument;
    }
  }

  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_end);
  return ErrorCode::kOk;
}

ErrorCode DecodePcm16ToPayload(const std::vector<int16_t>& pcm,
                               const MorseToneConfig& config,
                               std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr || !IsValidConfig(config)) {
    return ErrorCode::kInvalidArgument;
  }
  out_payload->clear();
  if (pcm.empty()) {
    return ErrorCode::kOk;
  }
  if (pcm.size() % static_cast<std::size_t>(config.unit_samples) != 0) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<bool> tone_units;
  tone_units.reserve(pcm.size() /
                     static_cast<std::size_t>(config.unit_samples));
  for (std::size_t offset = 0; offset < pcm.size();
       offset += static_cast<std::size_t>(config.unit_samples)) {
    tone_units.push_back(
        ChunkRms(pcm, offset,
                 offset + static_cast<std::size_t>(config.unit_samples)) >=
        kSilenceRmsThreshold);
  }

  std::string pattern;
  std::size_t index = 0;
  while (index < tone_units.size()) {
    const bool is_tone = tone_units[index];
    std::size_t run_end = index + 1;
    while (run_end < tone_units.size() && tone_units[run_end] == is_tone) {
      ++run_end;
    }
    const std::size_t run_units = run_end - index;
    if (is_tone) {
      pattern.push_back(run_units >= 2 ? '-' : '.');
    } else if (run_units >= kMorseWordGapUnits) {
      FlushDecodedPattern(&pattern, out_payload);
      // Separator-only mini payloads are valid in this project because
      // whitespace itself is encodable text. Preserve one separator when the
      // decoded PCM is all silence instead of collapsing back to empty.
      if (out_payload->empty() ||
          out_payload->back() != static_cast<uint8_t>(' ')) {
        out_payload->push_back(static_cast<uint8_t>(' '));
      }
    } else if (run_units >= kMorseLetterGapUnits) {
      FlushDecodedPattern(&pattern, out_payload);
    }
    index = run_end;
  }
  FlushDecodedPattern(&pattern, out_payload);
  if (out_payload->size() > 1 &&
      out_payload->back() == static_cast<uint8_t>(' ')) {
    out_payload->pop_back();
  }
  return ErrorCode::kOk;
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm) {
  return EncodeTextToPcm16(config, text, out_pcm, nullptr);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                       0.0f);
  if (ShouldCancelEncode(progress_sink)) {
    return ErrorCode::kCancelled;
  }
  std::vector<uint8_t> payload;
  const ErrorCode payload_code = EncodeTextToPayload(text, &payload);
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                       0.20f);
  const ErrorCode encode_code =
      EncodePayloadToPcm16(payload, MakeMorseToneConfig(config), out_pcm,
                           progress_sink, 0.20f, 0.95f);
  if (encode_code != ErrorCode::kOk) {
    return encode_code;
  }
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kFinalizing, 1.0f);
  return ErrorCode::kOk;
}

ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<int16_t>& pcm,
                            std::string* out_text) {
  if (out_text == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  std::vector<uint8_t> payload;
  const ErrorCode payload_code =
      DecodePcm16ToPayload(pcm, MakeMorseToneConfig(config), &payload);
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }
  return DecodePayloadToText(payload, out_text);
}

std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config) {
  return std::make_unique<MiniDecoder>(config);
}

}  // namespace bag::mini_mode
