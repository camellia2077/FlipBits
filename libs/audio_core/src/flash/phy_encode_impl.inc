namespace bag::flash {
ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm) {
  return EncodeTextToPcm16(config, text, out_pcm, nullptr);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<std::int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink) {
  return EncodeTextToPcm16WithSignalProfileAndFlavor(
      config, text, config.flash_signal_profile, config.flash_voicing_flavor,
      out_pcm, progress_sink);
}

ErrorCode EncodeTextToPcm16WithSignalProfileAndFlavor(
    const CoreConfig& config, const std::string& text,
    FlashSignalProfile signal_profile, FlashVoicingFlavor flavor,
    std::vector<std::int16_t>* out_pcm) {
  return EncodeTextToPcm16WithSignalProfileAndFlavor(
      config, text, signal_profile, flavor, out_pcm, nullptr);
}

ErrorCode EncodeTextToPcm16WithSignalProfileAndFlavor(
    const CoreConfig& config, const std::string& text,
    FlashSignalProfile signal_profile, FlashVoicingFlavor flavor,
    std::vector<std::int16_t>* out_pcm,
    const EncodeProgressSink* progress_sink) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (!IsValidFlashSignalProfile(signal_profile) ||
      !IsValidFlashVoicingFlavor(flavor)) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<std::uint8_t> bytes;
  const ErrorCode bytes_code = EncodeTextToBytes(text, &bytes);
  if (bytes_code != ErrorCode::kOk) {
    return bytes_code;
  }

  try {
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                         0.0f);
    if (ShouldCancelEncode(progress_sink)) {
      return ErrorCode::kCancelled;
    }
    const FlashSignalProfile normalized_signal_profile =
        NormalizeFormalSignalProfile(signal_profile);
    const FlashVoicingFlavor normalized_flavor =
        NormalizeFormalVoicingFlavor(flavor);
    const BfskConfig signal_config =
        MakeBfskConfigForSignalProfile(config, normalized_signal_profile);
    const FlashVoicingConfig formal_voicing =
        MakeFormalVoicingConfigForFlavor(config, normalized_flavor);
    const FlashPayloadLayout payload_layout =
        BuildPayloadLayoutForVoicing(bytes, signal_config, normalized_flavor);
    const FlashEncodeWorkBudget work_budget =
        BuildFlashEncodeWorkBudget(bytes, payload_layout, formal_voicing);
    std::size_t completed_work = work_budget.prepare_text_work;
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                         static_cast<float>(completed_work) /
                             static_cast<float>(work_budget.total_work));
    if (ShouldCancelEncode(progress_sink)) {
      return ErrorCode::kCancelled;
    }
    completed_work += work_budget.prepare_layout_work;
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kPreparingInput,
                         static_cast<float>(completed_work) /
                             static_cast<float>(work_budget.total_work));
    if (ShouldCancelEncode(progress_sink)) {
      return ErrorCode::kCancelled;
    }
    const float render_progress_begin =
        static_cast<float>(completed_work) /
        static_cast<float>(work_budget.total_work);
    const float render_progress_end =
        static_cast<float>(completed_work + work_budget.render_work) /
        static_cast<float>(work_budget.total_work);
    const std::vector<std::int16_t> clean_payload_pcm =
        EncodePayloadLayoutToPcm16(payload_layout, signal_config, progress_sink,
                                   render_progress_begin, render_progress_end);
    completed_work += work_budget.render_work;
    const float postprocess_progress_begin =
        static_cast<float>(completed_work) /
        static_cast<float>(work_budget.total_work);
    const float postprocess_progress_end =
        static_cast<float>(completed_work + work_budget.postprocess_work) /
        static_cast<float>(work_budget.total_work);
    const FlashVoicingResult voiced_payload = ApplyVoicingToPayloadWithFlavor(
        clean_payload_pcm, payload_layout, normalized_flavor, formal_voicing,
        progress_sink, postprocess_progress_begin, postprocess_progress_end);
    completed_work += work_budget.postprocess_work;

    if (!IsFormalVoicingCompatible(voiced_payload, clean_payload_pcm.size(),
                                   formal_voicing)) {
      return ErrorCode::kInternal;
    }

    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kFinalizing,
                         static_cast<float>(completed_work) /
                             static_cast<float>(work_budget.total_work));
    if (ShouldCancelEncode(progress_sink)) {
      return ErrorCode::kCancelled;
    }
    *out_pcm = voiced_payload.pcm;
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kFinalizing, 1.0f);
    return ErrorCode::kOk;
  } catch (const EncodeCancelled&) {
    return ErrorCode::kCancelled;
  } catch (...) {
    return ErrorCode::kInvalidArgument;
  }
}

}  // namespace bag::flash
