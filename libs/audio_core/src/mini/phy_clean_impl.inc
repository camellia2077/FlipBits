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
    out_result->text_status =
        text_code == ErrorCode::kOk
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
    const char ch = static_cast<char>(payload[byte_index]);
    if (ch == ' ') {
      AppendSilenceUnits(kMorseWordGapUnits, config, out_pcm);
    } else {
      const std::string_view pattern = MorsePatternForChar(ch);
      if (pattern.empty()) {
        return ErrorCode::kInvalidArgument;
      }
      for (std::size_t index = 0; index < pattern.size(); ++index) {
        AppendToneUnits(pattern[index] == '-' ? kMorseDashUnits : kMorseDotUnits,
                        config, out_pcm);
        if (index + 1 < pattern.size()) {
          AppendSilenceUnits(kMorseElementGapUnits, config, out_pcm);
        }
      }
      AppendSilenceUnits(MorseTrailingGapUnits(payload, byte_index), config,
                         out_pcm);
    }
    if (out_pcm->size() - last_reported_samples >=
            kProgressReportStrideSamples ||
        byte_index + 1 == payload.size()) {
      ReportSampleProgress(progress_sink, progress_begin, progress_end,
                           out_pcm->size(), total_samples);
      last_reported_samples = out_pcm->size();
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
  tone_units.reserve(pcm.size() / static_cast<std::size_t>(config.unit_samples));
  for (std::size_t offset = 0; offset < pcm.size();
       offset += static_cast<std::size_t>(config.unit_samples)) {
    tone_units.push_back(
        ChunkRms(pcm, offset, offset + static_cast<std::size_t>(config.unit_samples)) >=
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
      if (!out_payload->empty() &&
          out_payload->back() != static_cast<uint8_t>(' ')) {
        out_payload->push_back(static_cast<uint8_t>(' '));
      }
    } else if (run_units >= kMorseLetterGapUnits) {
      FlushDecodedPattern(&pattern, out_payload);
    }
    index = run_end;
  }
  FlushDecodedPattern(&pattern, out_payload);
  if (!out_payload->empty() && out_payload->back() == static_cast<uint8_t>(' ')) {
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
  const ErrorCode encode_code = EncodePayloadToPcm16(
      payload, MakeMorseToneConfig(config), out_pcm, progress_sink, 0.20f,
      0.95f);
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
