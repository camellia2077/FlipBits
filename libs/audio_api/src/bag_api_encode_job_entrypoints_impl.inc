#if FLIPBITS_ENABLE_ASYNC_ENCODE_JOB
bag_error_code bag_start_encode_text_job(const bag_encoder_config* config,
                                         const char* text,
                                         bag_encode_job** out_job) {
  if (config == nullptr || text == nullptr || out_job == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  *out_job = nullptr;
  if (bag_validate_encode_request(config, text) != BAG_VALIDATION_OK) {
    return BAG_INVALID_ARGUMENT;
  }

  bag_encode_job* job = nullptr;
  try {
    job = new (std::nothrow) bag_encode_job{};
    if (job == nullptr) {
      return BAG_INTERNAL;
    }

    const bag::CoreConfig core_config = ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved);
    job->operation = bag::CreateEncodeOperation(core_config, text);
    if (!job->operation) {
      delete job;
      return BAG_INTERNAL;
    }
    bool did_progress = false;
    const bag::ErrorCode pump_code =
        job->operation->Pump({}, &did_progress);
    if (pump_code != bag::ErrorCode::kOk) {
      delete job;
      return ToApiCode(pump_code);
    }
    *out_job = job;
    return BAG_OK;
  } catch (...) {
    delete job;
    *out_job = nullptr;
    return BAG_INTERNAL;
  }
}

bag_error_code bag_poll_encode_text_job(const bag_encode_job* job,
                                        bag_encode_job_progress* out_progress) {
  if (job == nullptr || !job->operation || out_progress == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  try {
    bool did_progress = false;
    const bag::ErrorCode pump_code = const_cast<bag::IEncodeOperation*>(
                                         job->operation.get())
                                         ->Pump({}, &did_progress);
    if (pump_code != bag::ErrorCode::kOk) {
      return ToApiCode(pump_code);
    }
    const bag::EncodeProgressSnapshot snapshot = job->operation->Snapshot();
    out_progress->state = ToApiOperationState(snapshot.state);
    out_progress->phase = ToApiJobPhase(snapshot.phase);
    out_progress->progress_0_to_1 = snapshot.overall_progress_0_to_1;
    out_progress->terminal_code = ToApiCode(snapshot.terminal_code);
    return BAG_OK;
  } catch (...) {
    return BAG_INTERNAL;
  }
}

bag_error_code bag_cancel_encode_text_job(bag_encode_job* job) {
  if (job == nullptr || !job->operation) {
    return BAG_INVALID_ARGUMENT;
  }
  try {
    return ToApiCode(job->operation->Cancel());
  } catch (...) {
    return BAG_INTERNAL;
  }
}

bag_error_code bag_take_encode_text_job_result(const bag_encode_job* job,
                                               bag_pcm16_result* out_result) {
  if (job == nullptr || !job->operation || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  try {
    bag::EncodedPcmFollowResult result{};
    const bag::ErrorCode code = job->operation->TakeResult(&result);
    if (code != bag::ErrorCode::kOk) {
      out_result->samples = nullptr;
      out_result->sample_count = 0;
      return ToApiCode(code);
    }
    return CopyPcmToApiResult(result.pcm, out_result);
  } catch (...) {
    out_result->samples = nullptr;
    out_result->sample_count = 0;
    return BAG_INTERNAL;
  }
}

bag_error_code bag_peek_encode_text_job_result_layout(
    const bag_encode_job* job, bag_encode_result_layout* out_layout) {
  if (job == nullptr || !job->operation || out_layout == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  try {
    bag::EncodedPcmFollowResult result{};
    const bag::ErrorCode code = job->operation->TakeResult(&result);
    if (code != bag::ErrorCode::kOk) {
      *out_layout = {};
      return ToApiCode(code);
    }
    FillEncodeResultLayout(result, out_layout);
    return BAG_OK;
  } catch (...) {
    *out_layout = {};
    return BAG_INTERNAL;
  }
}

bag_error_code bag_take_encode_text_job_result_with_follow(
    const bag_encode_job* job, bag_encode_result* out_result) {
  if (job == nullptr || !job->operation || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  try {
    bag::EncodedPcmFollowResult result{};
    const bag::ErrorCode code = job->operation->TakeResult(&result);
    if (code != bag::ErrorCode::kOk) {
      ClearApiEncodeResult(out_result);
      return ToApiCode(code);
    }
    return CopyStructuredEncodeResultToApi(result, out_result);
  } catch (...) {
    ClearApiEncodeResult(out_result);
    return BAG_INTERNAL;
  }
}

void bag_destroy_encode_text_job(bag_encode_job* job) {
  if (job == nullptr) {
    return;
  }
  if (job->operation) {
    job->operation->Cancel();
  }
  delete job;
}
#else
bag_error_code bag_start_encode_text_job(const bag_encoder_config* config,
                                         const char* text,
                                         bag_encode_job** out_job) {
  if (out_job != nullptr) {
    *out_job = nullptr;
  }
  return (config == nullptr || text == nullptr || out_job == nullptr)
             ? BAG_INVALID_ARGUMENT
             : BAG_NOT_IMPLEMENTED;
}

bag_error_code bag_poll_encode_text_job(const bag_encode_job* job,
                                        bag_encode_job_progress* out_progress) {
  if (job == nullptr || out_progress == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }
  out_progress->state = BAG_ENCODE_JOB_FAILED;
  out_progress->phase = BAG_ENCODE_JOB_PHASE_FINALIZING;
  out_progress->progress_0_to_1 = 0.0f;
  out_progress->terminal_code = BAG_NOT_IMPLEMENTED;
  return BAG_NOT_IMPLEMENTED;
}

bag_error_code bag_cancel_encode_text_job(bag_encode_job* job) {
  return job == nullptr ? BAG_INVALID_ARGUMENT : BAG_NOT_IMPLEMENTED;
}

bag_error_code bag_take_encode_text_job_result(const bag_encode_job* job,
                                               bag_pcm16_result* out_result) {
  if (job == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }
  out_result->samples = nullptr;
  out_result->sample_count = 0;
  return BAG_NOT_IMPLEMENTED;
}

bag_error_code bag_peek_encode_text_job_result_layout(
    const bag_encode_job* job, bag_encode_result_layout* out_layout) {
  if (job == nullptr || out_layout == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }
  *out_layout = {};
  return BAG_NOT_IMPLEMENTED;
}

bag_error_code bag_take_encode_text_job_result_with_follow(
    const bag_encode_job* job, bag_encode_result* out_result) {
  if (job == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }
  ClearApiEncodeResult(out_result);
  return BAG_NOT_IMPLEMENTED;
}

void bag_destroy_encode_text_job(bag_encode_job* job) {
  (void)job;
}
#endif

void bag_free_pcm16_result(bag_pcm16_result* result) {
  if (result == nullptr) {
    return;
  }

  delete[] result->samples;
  result->samples = nullptr;
  result->sample_count = 0;
}

void bag_free_encode_result(bag_encode_result* result) {
  if (result == nullptr) {
    return;
  }

  delete[] result->samples;
  result->samples = nullptr;
  result->sample_count = 0;
  result->raw_bytes_hex_size = 0;
  result->raw_bits_binary_size = 0;
  result->raw_payload_available = 0;
  result->raw_bytes_hex_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  result->raw_bits_binary_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  ClearApiFollowData(&result->follow_data);
  ClearApiTextFollowData(&result->text_follow_data);
}
