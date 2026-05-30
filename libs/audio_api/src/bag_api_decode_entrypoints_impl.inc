bag_error_code bag_create_decoder(const bag_decoder_config* config,
                                  bag_decoder** out_decoder) {
  if (config == nullptr || out_decoder == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  if (bag_validate_decoder_config(config) != BAG_VALIDATION_OK) {
    return BAG_INVALID_ARGUMENT;
  }

  auto* decoder = new bag_decoder{};
  decoder->decoder = bag::CreateTransportDecoder(ToCoreConfig(
      config->sample_rate_hz, config->frame_samples, config->enable_diagnostics,
      config->mode, config->flash_signal_profile, config->flash_voicing_flavor,
      config->reserved));
  if (!decoder->decoder) {
    delete decoder;
    return BAG_INTERNAL;
  }

  *out_decoder = decoder;
  return BAG_OK;
}

void bag_destroy_decoder(bag_decoder* decoder) {
  delete decoder;
}

bag_error_code bag_push_pcm(bag_decoder* decoder, const int16_t* samples,
                            size_t sample_count, int64_t timestamp_ms) {
  if (decoder == nullptr || decoder->decoder == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  bag::PcmBlock block{};
  block.samples = samples;
  block.sample_count = sample_count;
  block.timestamp_ms = timestamp_ms;
  return ToApiCode(decoder->decoder->PushPcm(block));
}

bag_error_code bag_poll_decode_result(bag_decoder* decoder,
                                      bag_decode_result* out_result) {
  if (decoder == nullptr || decoder->decoder == nullptr ||
      out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  bag::DecodeResult result{};
  const bag_error_code code =
      ToApiCode(decoder->decoder->PollDecodeResult(&result));
  if (code != BAG_OK) {
    ClearApiDecodeResult(out_result);
    return code;
  }
  return CopyStructuredDecodeResultToApi(result, out_result);
}

bag_error_code bag_poll_result(bag_decoder* decoder,
                               bag_text_result* out_result) {
  if (decoder == nullptr || decoder->decoder == nullptr ||
      out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  bag_decode_result decode_result{};
  decode_result.text_buffer = out_result->buffer;
  decode_result.text_buffer_size = out_result->buffer_size;
  const bag_error_code code = bag_poll_decode_result(decoder, &decode_result);
  if (code != BAG_OK) {
    if (out_result->buffer != nullptr && out_result->buffer_size > 0) {
      out_result->buffer[0] = '\0';
    }
    out_result->text_size = 0;
    out_result->complete = 0;
    out_result->confidence = 0.0f;
    out_result->mode = BAG_TRANSPORT_FLASH;
    return code;
  }

  if (decode_result.text_decode_status != BAG_DECODE_CONTENT_STATUS_OK &&
      decode_result.text_decode_status !=
          BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL) {
    if (out_result->buffer != nullptr && out_result->buffer_size > 0) {
      out_result->buffer[0] = '\0';
    }
    out_result->text_size = 0;
    out_result->complete = 0;
    out_result->confidence = 0.0f;
    out_result->mode = decode_result.mode;
    return BAG_INTERNAL;
  }

  out_result->text_size = decode_result.text_size;
  out_result->complete = decode_result.complete;
  out_result->confidence = decode_result.confidence;
  out_result->mode = decode_result.mode;
  return BAG_OK;
}

void bag_reset(bag_decoder* decoder) {
  if (decoder == nullptr || decoder->decoder == nullptr) {
    return;
  }
  decoder->decoder->Reset();
}

namespace {

constexpr std::uint64_t kDecodePrepareWorkUnits = 1;
constexpr std::uint64_t kDecodeFinalizeWorkUnits = 1;

bool IsTerminalDecodeOperationState(bag_decode_operation_state state) {
  return state == BAG_DECODE_OPERATION_SUCCEEDED ||
         state == BAG_DECODE_OPERATION_FAILED ||
         state == BAG_DECODE_OPERATION_CANCELLED;
}

std::uint64_t NonZeroDecodeWork(std::uint64_t work) {
  return std::max<std::uint64_t>(work, 1);
}

std::uint64_t DecodeOperationReadingWork(
    const bag_decode_operation* operation) {
  return operation == nullptr ? 0 : NonZeroDecodeWork(operation->pcm.size());
}

std::uint64_t DecodeOperationPayloadWork(
    const bag_decode_operation* operation) {
  return operation == nullptr
             ? 0
             : NonZeroDecodeWork(static_cast<std::uint64_t>(
                   std::max<std::size_t>(1, operation->pcm.size() / 16)));
}

std::uint64_t DecodeOperationTotalWork(
    const bag_decode_operation* operation) {
  return kDecodePrepareWorkUnits + DecodeOperationReadingWork(operation) +
         DecodeOperationPayloadWork(operation) + kDecodeFinalizeWorkUnits;
}

std::uint64_t DecodeOperationPhaseTotalWork(
    const bag_decode_operation* operation, bag_decode_operation_phase phase) {
  switch (phase) {
    case BAG_DECODE_OPERATION_PHASE_PREPARING_INPUT:
      return kDecodePrepareWorkUnits;
    case BAG_DECODE_OPERATION_PHASE_READING_PCM:
      return DecodeOperationReadingWork(operation);
    case BAG_DECODE_OPERATION_PHASE_DECODING_PAYLOAD:
      return DecodeOperationPayloadWork(operation);
    case BAG_DECODE_OPERATION_PHASE_FINALIZING:
    default:
      return kDecodeFinalizeWorkUnits;
  }
}

std::uint64_t DecodeOperationPhaseBaseWork(
    const bag_decode_operation* operation, bag_decode_operation_phase phase) {
  switch (phase) {
    case BAG_DECODE_OPERATION_PHASE_PREPARING_INPUT:
      return 0;
    case BAG_DECODE_OPERATION_PHASE_READING_PCM:
      return kDecodePrepareWorkUnits;
    case BAG_DECODE_OPERATION_PHASE_DECODING_PAYLOAD:
      return kDecodePrepareWorkUnits + DecodeOperationReadingWork(operation);
    case BAG_DECODE_OPERATION_PHASE_FINALIZING:
    default:
      return kDecodePrepareWorkUnits + DecodeOperationReadingWork(operation) +
             DecodeOperationPayloadWork(operation);
  }
}

void UpdateDecodeOperationPhaseProgress(bag_decode_operation* operation,
                                        bag_decode_operation_phase phase,
                                        std::uint64_t phase_completed_units) {
  if (operation == nullptr) {
    return;
  }

  const std::uint64_t phase_total =
      DecodeOperationPhaseTotalWork(operation, phase);
  const std::uint64_t clamped_phase_units =
      std::min<std::uint64_t>(phase_total, phase_completed_units);
  const std::uint64_t total_work = DecodeOperationTotalWork(operation);
  const std::uint64_t total_completed = std::min<std::uint64_t>(
      total_work, DecodeOperationPhaseBaseWork(operation, phase) +
                      clamped_phase_units);

  operation->state = BAG_DECODE_OPERATION_RUNNING;
  operation->phase = phase;
  operation->phase_completed_work_units = clamped_phase_units;
  operation->completed_work_units = total_completed;
  operation->phase_progress_0_to_1 =
      phase_total == 0
          ? 1.0f
          : static_cast<float>(clamped_phase_units) /
                static_cast<float>(phase_total);
  operation->overall_progress_0_to_1 =
      total_work == 0 ? 1.0f
                      : static_cast<float>(total_completed) /
                            static_cast<float>(total_work);
}

void MarkDecodeOperationTerminal(bag_decode_operation* operation,
                                 bag_error_code code,
                                 bag_decode_operation_state state) {
  if (operation == nullptr) {
    return;
  }

  operation->terminal_code = code;
  operation->state = state;
  if (state == BAG_DECODE_OPERATION_SUCCEEDED) {
    operation->phase = BAG_DECODE_OPERATION_PHASE_FINALIZING;
    operation->overall_progress_0_to_1 = 1.0f;
    operation->phase_progress_0_to_1 = 1.0f;
    operation->completed_work_units = DecodeOperationTotalWork(operation);
    operation->phase_completed_work_units = kDecodeFinalizeWorkUnits;
  }
}

bool PumpDecodeOperationStep(bag_decode_operation* operation,
                             std::uint64_t work_budget) {
  if (operation == nullptr ||
      IsTerminalDecodeOperationState(operation->state)) {
    return false;
  }

  if (operation->cancel_requested) {
    MarkDecodeOperationTerminal(operation, BAG_CANCELLED,
                                BAG_DECODE_OPERATION_CANCELLED);
    return true;
  }

  const std::uint64_t normalized_work_budget =
      std::max<std::uint64_t>(work_budget, 1);
  if (operation->state == BAG_DECODE_OPERATION_QUEUED) {
    operation->terminal_code = BAG_NOT_READY;
    UpdateDecodeOperationPhaseProgress(
        operation, BAG_DECODE_OPERATION_PHASE_PREPARING_INPUT,
        kDecodePrepareWorkUnits);
    operation->phase = BAG_DECODE_OPERATION_PHASE_READING_PCM;
    operation->phase_progress_0_to_1 = 0.0f;
    operation->phase_completed_work_units = 0;
    return true;
  }

  if (operation->phase == BAG_DECODE_OPERATION_PHASE_READING_PCM) {
    const std::size_t remaining_samples =
        operation->pcm.size() - operation->pushed_pcm_sample_count;
    if (remaining_samples > 0) {
      const std::size_t chunk_size =
          normalized_work_budget >= static_cast<std::uint64_t>(remaining_samples)
              ? remaining_samples
              : static_cast<std::size_t>(normalized_work_budget);
      bag::PcmBlock block{};
      block.samples =
          operation->pcm.data() + operation->pushed_pcm_sample_count;
      block.sample_count = chunk_size;
      block.timestamp_ms =
          static_cast<std::int64_t>(operation->pushed_pcm_sample_count);
      const bag::ErrorCode push_code = operation->decoder->PushPcm(block);
      if (push_code != bag::ErrorCode::kOk) {
        MarkDecodeOperationTerminal(operation, ToApiCode(push_code),
                                    BAG_DECODE_OPERATION_FAILED);
        return true;
      }
      operation->pushed_pcm_sample_count += chunk_size;
      UpdateDecodeOperationPhaseProgress(
          operation, BAG_DECODE_OPERATION_PHASE_READING_PCM,
          static_cast<std::uint64_t>(operation->pushed_pcm_sample_count));
      return true;
    }
    operation->phase = BAG_DECODE_OPERATION_PHASE_DECODING_PAYLOAD;
    operation->phase_progress_0_to_1 = 0.0f;
    operation->phase_completed_work_units = 0;
    return true;
  }

  if (operation->phase == BAG_DECODE_OPERATION_PHASE_DECODING_PAYLOAD) {
    const bag::ErrorCode core_code =
        operation->decoder->PollDecodeResult(&operation->result);
    const bag_error_code code = ToApiCode(core_code);
    if (code != BAG_OK) {
      operation->result = {};
      MarkDecodeOperationTerminal(operation, code,
                                  BAG_DECODE_OPERATION_FAILED);
      return true;
    }
    UpdateDecodeOperationPhaseProgress(
        operation, BAG_DECODE_OPERATION_PHASE_DECODING_PAYLOAD,
        DecodeOperationPayloadWork(operation));
    operation->phase = BAG_DECODE_OPERATION_PHASE_FINALIZING;
    operation->phase_progress_0_to_1 = 0.0f;
    operation->phase_completed_work_units = 0;
    return true;
  }

  UpdateDecodeOperationPhaseProgress(operation,
                                     BAG_DECODE_OPERATION_PHASE_FINALIZING,
                                     kDecodeFinalizeWorkUnits);
  MarkDecodeOperationTerminal(operation, BAG_OK,
                              BAG_DECODE_OPERATION_SUCCEEDED);
  return true;
}

}  // namespace

bag_error_code bag_create_decode_operation(
    const bag_decoder_config* config, const int16_t* samples,
    size_t sample_count, bag_decode_operation** out_operation) {
  if (config == nullptr || samples == nullptr || sample_count == 0 ||
      out_operation == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  *out_operation = nullptr;
  if (bag_validate_decoder_config(config) != BAG_VALIDATION_OK) {
    return BAG_INVALID_ARGUMENT;
  }

  try {
    auto* operation = new (std::nothrow) bag_decode_operation{};
    if (operation == nullptr) {
      return BAG_INTERNAL;
    }
    operation->config = *config;
    operation->pcm.assign(samples, samples + sample_count);
    operation->decoder = bag::CreateTransportDecoder(ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved));
    if (!operation->decoder) {
      delete operation;
      return BAG_INTERNAL;
    }
    *out_operation = operation;
    return BAG_OK;
  } catch (...) {
    *out_operation = nullptr;
    return BAG_INTERNAL;
  }
}

bag_error_code bag_run_decode_operation(bag_decode_operation* operation) {
  if (operation == nullptr || !operation->decoder) {
    return BAG_INVALID_ARGUMENT;
  }
  try {
    int did_progress = 0;
    while (true) {
      const bag_error_code code =
          bag_pump_decode_operation(operation, {}, &did_progress);
      if (code != BAG_OK) {
        return code;
      }
      if (operation->state == BAG_DECODE_OPERATION_SUCCEEDED) {
        return BAG_OK;
      }
      if (operation->state == BAG_DECODE_OPERATION_CANCELLED) {
        return BAG_CANCELLED;
      }
      if (operation->state == BAG_DECODE_OPERATION_FAILED) {
        return operation->terminal_code;
      }
      if (did_progress == 0) {
        return BAG_NOT_READY;
      }
    }
  } catch (...) {
    return BAG_INTERNAL;
  }
}

bag_error_code bag_cancel_decode_operation(bag_decode_operation* operation) {
  if (operation == nullptr || !operation->decoder) {
    return BAG_INVALID_ARGUMENT;
  }
  operation->cancel_requested = true;
  if (operation->state == BAG_DECODE_OPERATION_QUEUED ||
      !IsTerminalDecodeOperationState(operation->state)) {
    MarkDecodeOperationTerminal(operation, BAG_CANCELLED,
                                BAG_DECODE_OPERATION_CANCELLED);
  }
  return BAG_OK;
}

bag_error_code bag_get_decode_operation_work_plan(
    const bag_decode_operation* operation,
    bag_decode_operation_work_plan* out_work_plan) {
  if (operation == nullptr || !operation->decoder || out_work_plan == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  out_work_plan->preparing_input_work_units = kDecodePrepareWorkUnits;
  out_work_plan->reading_pcm_work_units = DecodeOperationReadingWork(operation);
  out_work_plan->decoding_payload_work_units =
      DecodeOperationPayloadWork(operation);
  out_work_plan->finalizing_work_units = kDecodeFinalizeWorkUnits;
  out_work_plan->total_work_units = DecodeOperationTotalWork(operation);
  out_work_plan->pcm_sample_count = operation->pcm.size();
  return BAG_OK;
}

bag_error_code bag_pump_decode_operation(
    bag_decode_operation* operation, bag_decode_operation_pump_budget budget,
    int* out_did_progress) {
  if (operation == nullptr || !operation->decoder ||
      out_did_progress == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  try {
    *out_did_progress = 0;
    if (IsTerminalDecodeOperationState(operation->state)) {
      return BAG_OK;
    }

    const std::uint64_t before_units = operation->completed_work_units;
    const std::uint64_t total_work = DecodeOperationTotalWork(operation);
    const std::uint64_t target_units =
        budget.max_work_units > 0
            ? std::min<std::uint64_t>(total_work,
                                      before_units + budget.max_work_units)
            : total_work;

    while (operation->completed_work_units < target_units &&
           !IsTerminalDecodeOperationState(operation->state)) {
      const std::uint64_t remaining_budget =
          target_units - operation->completed_work_units;
      if (!PumpDecodeOperationStep(operation, remaining_budget)) {
        break;
      }
    }

    *out_did_progress = operation->completed_work_units > before_units ? 1 : 0;
    return BAG_OK;
  } catch (...) {
    *out_did_progress = 0;
    MarkDecodeOperationTerminal(operation, BAG_INTERNAL,
                                BAG_DECODE_OPERATION_FAILED);
    return BAG_INTERNAL;
  }
}

bag_error_code bag_poll_decode_operation(
    const bag_decode_operation* operation,
    bag_decode_operation_progress* out_progress) {
  if (operation == nullptr || !operation->decoder || out_progress == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  out_progress->state = operation->state;
  out_progress->phase = operation->phase;
  out_progress->overall_progress_0_to_1 =
      std::clamp(operation->overall_progress_0_to_1, 0.0f, 1.0f);
  out_progress->phase_progress_0_to_1 =
      std::clamp(operation->phase_progress_0_to_1, 0.0f, 1.0f);
  out_progress->completed_work_units = operation->completed_work_units;
  out_progress->total_work_units = DecodeOperationTotalWork(operation);
  out_progress->phase_completed_work_units =
      operation->phase_completed_work_units;
  out_progress->phase_total_work_units =
      DecodeOperationPhaseTotalWork(operation, operation->phase);
  out_progress->terminal_code = operation->terminal_code;
  out_progress->pcm_sample_count = operation->pcm.size();
  out_progress->pushed_pcm_sample_count = operation->pushed_pcm_sample_count;
  return BAG_OK;
}

bag_error_code bag_take_decode_operation_result(
    const bag_decode_operation* operation, bag_decode_result* out_result) {
  if (operation == nullptr || !operation->decoder || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  if (operation->state == BAG_DECODE_OPERATION_SUCCEEDED) {
    return CopyStructuredDecodeResultToApi(operation->result, out_result);
  }

  ClearApiDecodeResult(out_result);
  if (operation->state == BAG_DECODE_OPERATION_CANCELLED) {
    return BAG_CANCELLED;
  }
  if (operation->state == BAG_DECODE_OPERATION_FAILED) {
    return operation->terminal_code;
  }
  return BAG_NOT_READY;
}

void bag_destroy_decode_operation(bag_decode_operation* operation) {
  delete operation;
}
