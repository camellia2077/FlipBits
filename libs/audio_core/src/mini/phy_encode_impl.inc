namespace bag::mini_mode {
using std::int16_t;
using std::uint8_t;

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
}  // namespace bag::mini_mode
