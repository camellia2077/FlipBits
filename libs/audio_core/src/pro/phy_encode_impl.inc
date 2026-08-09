namespace bag::pro {
using std::int16_t;
using std::size_t;
using std::uint8_t;

constexpr std::size_t kSymbolsPerPayloadByteForProgress = 2;

std::size_t NonZeroWork(std::size_t work) {
  return std::max(work, static_cast<std::size_t>(1));
}

struct ProEncodeWorkBudget {
  std::size_t prepare_payload_work = 1;
  std::size_t prepare_symbol_work = 1;
  std::size_t render_work = 1;
  std::size_t finalize_work = 1;
  std::size_t total_work = 4;
};

ProEncodeWorkBudget BuildEncodeWorkBudget(std::size_t payload_size,
                                          int symbol_samples) {
  const std::size_t symbol_count =
      payload_size * kSymbolsPerPayloadByteForProgress;

  ProEncodeWorkBudget budget{};
  budget.prepare_payload_work = NonZeroWork(payload_size);
  budget.prepare_symbol_work = NonZeroWork(symbol_count);
  budget.render_work = NonZeroWork(
      symbol_count * static_cast<std::size_t>(std::max(symbol_samples, 0)));
  budget.finalize_work = 1;
  budget.total_work = budget.prepare_payload_work + budget.prepare_symbol_work +
                      budget.render_work + budget.finalize_work;
  return budget;
}


ErrorCode EncodePayloadToPcm16(const std::vector<uint8_t>& payload,
                               const DualToneConfig& config,
                               std::vector<int16_t>* out_pcm) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<uint8_t> symbols;
  const ErrorCode symbols_code = EncodePayloadToSymbols(payload, &symbols);
  if (symbols_code != ErrorCode::kOk) {
    return symbols_code;
  }
  return EncodeSymbolsToPcm16(symbols, config, out_pcm);
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
  const ProEncodeWorkBudget work_budget =
      BuildEncodeWorkBudget(payload.size(), config.frame_samples);
  std::size_t completed_work = work_budget.prepare_payload_work;
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                       static_cast<float>(completed_work) /
                           static_cast<float>(work_budget.total_work));
  if (ShouldCancelEncode(progress_sink)) {
    return ErrorCode::kCancelled;
  }
  std::vector<uint8_t> symbols;
  const ErrorCode symbols_code = EncodePayloadToSymbols(payload, &symbols);
  if (symbols_code != ErrorCode::kOk) {
    return symbols_code;
  }
  completed_work += work_budget.prepare_symbol_work;
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                       static_cast<float>(completed_work) /
                           static_cast<float>(work_budget.total_work));
  const float render_progress_begin =
      static_cast<float>(completed_work) /
      static_cast<float>(work_budget.total_work);
  const float render_progress_end =
      static_cast<float>(completed_work + work_budget.render_work) /
      static_cast<float>(work_budget.total_work);
  const ErrorCode encode_code = EncodeSymbolsToPcm16(
      symbols, MakeDualToneConfig(config), out_pcm, progress_sink,
      render_progress_begin, render_progress_end);
  if (encode_code != ErrorCode::kOk) {
    return encode_code;
  }
  completed_work += work_budget.render_work;
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kFinalizing,
                       static_cast<float>(completed_work) /
                           static_cast<float>(work_budget.total_work));
  if (ShouldCancelEncode(progress_sink)) {
    return ErrorCode::kCancelled;
  }
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kFinalizing, 1.0f);
  return ErrorCode::kOk;
}
}  // namespace bag::pro
