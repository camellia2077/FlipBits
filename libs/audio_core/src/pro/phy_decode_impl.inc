namespace bag::pro {
using std::int16_t;
using std::size_t;
using std::uint8_t;

ErrorCode DecodePcm16ToPayloadWithResolvedUnit(
    const std::vector<int16_t>& pcm, const DualToneConfig& config,
    std::vector<uint8_t>* out_payload, int* out_unit_samples);
int ProDefaultSymbolSamples(int sample_rate_hz);

namespace {
constexpr double kPi = 3.14159265358979323846;

double GoertzelPower(const int16_t* chunk, int chunk_size, int sample_rate_hz,
                     double target_freq_hz) {
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

size_t FindStrongestBand(const int16_t* chunk, int chunk_size,
                         int sample_rate_hz,
                         const std::array<double, 4>& freqs_hz) {
  std::array<double, 4> powers{};
  for (size_t index = 0; index < freqs_hz.size(); ++index) {
    powers[index] =
        GoertzelPower(chunk, chunk_size, sample_rate_hz, freqs_hz[index]);
  }
  return static_cast<size_t>(std::distance(
      powers.begin(), std::max_element(powers.begin(), powers.end())));
}

class ProDecoder final : public ITransportDecoder {
 public:
  explicit ProDecoder(CoreConfig config) : config_(config) {}

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
      out_result->mode = TransportMode::kPro;
      out_result->text_status = DecodeContentStatus::kUnavailable;
      return ErrorCode::kNotReady;
    }

    int resolved_unit_samples = config_.frame_samples;
    const ErrorCode payload_code = DecodePcm16ToPayloadWithResolvedUnit(
        buffered_pcm_, MakeDualToneConfig(config_),
        &out_result->raw_payload_bytes, &resolved_unit_samples);
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
      out_result->mode = TransportMode::kPro;
      out_result->text_status = DecodeContentStatus::kInternalError;
      return ErrorCode::kInternal;
    }

    out_result->raw_payload_available = true;
    CoreConfig resolved_config = config_;
    resolved_config.frame_samples = resolved_unit_samples;
    out_result->follow_data =
        BuildPayloadFollowData(resolved_config, out_result->raw_payload_bytes);
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
    out_result->mode = TransportMode::kPro;
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
      out_result->mode = TransportMode::kPro;
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

ErrorCode DecodePcm16ToSymbols(const std::vector<int16_t>& pcm,
                               const DualToneConfig& config,
                               std::vector<uint8_t>* out_symbols) {
  if (out_symbols == nullptr || !IsValidDualToneConfig(config)) {
    return ErrorCode::kInvalidArgument;
  }
  if (pcm.empty()) {
    out_symbols->clear();
    return ErrorCode::kOk;
  }
  if (pcm.size() % static_cast<size_t>(config.symbol_samples) != 0) {
    return ErrorCode::kInvalidArgument;
  }

  out_symbols->clear();
  out_symbols->reserve(pcm.size() / static_cast<size_t>(config.symbol_samples));
  for (size_t offset = 0; offset < pcm.size();
       offset += static_cast<size_t>(config.symbol_samples)) {
    const int16_t* chunk = pcm.data() + offset;
    const size_t low_index =
        FindStrongestBand(chunk, config.symbol_samples, config.sample_rate_hz,
                          config.low_freqs_hz);
    const size_t high_index =
        FindStrongestBand(chunk, config.symbol_samples, config.sample_rate_hz,
                          config.high_freqs_hz);
    out_symbols->push_back(static_cast<uint8_t>(low_index * 4 + high_index));
  }

  return ErrorCode::kOk;
}

ErrorCode DecodePcm16ToPayloadForUnit(const std::vector<int16_t>& pcm,
                                      const DualToneConfig& config,
                                      std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<uint8_t> symbols;
  const ErrorCode symbols_code = DecodePcm16ToSymbols(pcm, config, &symbols);
  if (symbols_code != ErrorCode::kOk) {
    return symbols_code;
  }
  return DecodeSymbolsToPayload(symbols, out_payload);
}

int ProDefaultSymbolSamples(int sample_rate_hz) {
  if (sample_rate_hz <= 0) {
    return 0;
  }
  // Pro keeps its current fixed 50 ms symbol timing. This is the same
  // sample-rate-derived default used by the existing Android/native boundary.
  return sample_rate_hz / kCurrentBaseFramesPerSecond;
}

ErrorCode DecodePcm16ToPayloadWithResolvedUnit(
    const std::vector<int16_t>& pcm, const DualToneConfig& config,
    std::vector<uint8_t>* out_payload, int* out_unit_samples) {
  if (out_payload == nullptr || out_unit_samples == nullptr ||
      config.sample_rate_hz <= 0 || config.symbol_samples < 0) {
    return ErrorCode::kInvalidArgument;
  }

  DualToneConfig resolved_config = config;
  if (resolved_config.symbol_samples == 0) {
    resolved_config.symbol_samples =
        ProDefaultSymbolSamples(resolved_config.sample_rate_hz);
  }
  if (resolved_config.symbol_samples <= 0) {
    return ErrorCode::kInvalidArgument;
  }

  const ErrorCode code = DecodePcm16ToPayloadForUnit(
      pcm, resolved_config, out_payload);
  if (code == ErrorCode::kOk) {
    *out_unit_samples = resolved_config.symbol_samples;
  }
  return code;
}

ErrorCode DecodePcm16ToPayload(const std::vector<int16_t>& pcm,
                               const DualToneConfig& config,
                               std::vector<uint8_t>* out_payload) {
  int resolved_unit_samples = config.symbol_samples;
  return DecodePcm16ToPayloadWithResolvedUnit(
      pcm, config, out_payload, &resolved_unit_samples);
}

ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<int16_t>& pcm,
                            std::string* out_text) {
  if (out_text == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<uint8_t> payload;
  int resolved_unit_samples = config.frame_samples;
  const ErrorCode payload_code = DecodePcm16ToPayloadWithResolvedUnit(
      pcm, MakeDualToneConfig(config), &payload, &resolved_unit_samples);
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }
  return DecodePayloadToText(payload, out_text);
}

std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config) {
  return std::make_unique<ProDecoder>(config);
}
}  // namespace bag::pro
