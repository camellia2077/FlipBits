struct bag_decoder {
  std::unique_ptr<bag::ITransportDecoder> decoder;
};

struct bag_encode_job {
  bag::CoreConfig core_config;
  std::string text;
  mutable std::mutex result_mutex;
  bag::EncodedPcmFollowResult result;
  std::atomic<bag_encode_job_state> state = BAG_ENCODE_JOB_QUEUED;
  std::atomic<bag_encode_job_phase> phase =
      BAG_ENCODE_JOB_PHASE_PREPARING_INPUT;
  std::atomic<float> progress_0_to_1 = 0.0f;
  std::atomic<bag_error_code> terminal_code = BAG_NOT_READY;
  std::atomic<bool> cancel_requested = false;
  // The repo is moving on a C++23 toolchain, but this job stays on
  // std::thread + cancel_requested because the actual Android NDK/libc++
  // environment is not consistently reliable for std::jthread/stop_token.
  std::thread worker;
};

namespace {
inline constexpr const char kModeFlash[] = "flash";
inline constexpr const char kModePro[] = "pro";
inline constexpr const char kModeUltra[] = "ultra";

std::string FormatRawBytesHex(const std::vector<std::uint8_t>& bytes);
std::string FormatRawBitsBinary(const std::vector<std::uint8_t>& bytes);
bag_decode_content_status CopyStringToApiBuffer(std::string_view input,
                                                char* buffer,
                                                std::size_t buffer_size);
std::string JoinTextTokens(const std::vector<std::string>& tokens);

bag::CoreConfig ToCoreConfig(int sample_rate_hz, int frame_samples,
                             int enable_diagnostics, bag_transport_mode mode,
                             bag_flash_signal_profile flash_signal_profile,
                             bag_flash_voicing_flavor flash_voicing_flavor,
                             int reserved) {
  bag::CoreConfig core_config{};
  core_config.sample_rate_hz = sample_rate_hz;
  core_config.frame_samples = frame_samples;
  core_config.enable_diagnostics = enable_diagnostics != 0;
  switch (mode) {
    case BAG_TRANSPORT_FLASH:
      core_config.mode = bag::TransportMode::kFlash;
      break;
    case BAG_TRANSPORT_PRO:
      core_config.mode = bag::TransportMode::kPro;
      break;
    case BAG_TRANSPORT_ULTRA:
      core_config.mode = bag::TransportMode::kUltra;
      break;
    default:
      core_config.mode = static_cast<bag::TransportMode>(-1);
      break;
  }
  switch (flash_signal_profile) {
    case BAG_FLASH_SIGNAL_PROFILE_CODED_BURST:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kCodedBurst;
      break;
    case BAG_FLASH_SIGNAL_PROFILE_RITUAL_CHANT:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kRitualChant;
      break;
    case BAG_FLASH_SIGNAL_PROFILE_DEEP_RITUAL:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kDeepRitual;
      break;
    default:
      core_config.flash_signal_profile =
          static_cast<bag::FlashSignalProfile>(-1);
      break;
  }
  switch (flash_voicing_flavor) {
    case BAG_FLASH_VOICING_FLAVOR_CODED_BURST:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kCodedBurst;
      break;
    case BAG_FLASH_VOICING_FLAVOR_RITUAL_CHANT:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kRitualChant;
      break;
    case BAG_FLASH_VOICING_FLAVOR_DEEP_RITUAL:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kDeepRitual;
      break;
    default:
      core_config.flash_voicing_flavor =
          static_cast<bag::FlashVoicingFlavor>(-1);
      break;
  }
  core_config.reserved = reserved;
  return core_config;
}

bag_transport_mode ToApiMode(bag::TransportMode mode) {
  switch (mode) {
    case bag::TransportMode::kFlash:
      return BAG_TRANSPORT_FLASH;
    case bag::TransportMode::kPro:
      return BAG_TRANSPORT_PRO;
    case bag::TransportMode::kUltra:
      return BAG_TRANSPORT_ULTRA;
    default:
      return BAG_TRANSPORT_FLASH;
  }
}

bag_error_code ToApiCode(bag::ErrorCode code) {
  switch (code) {
    case bag::ErrorCode::kOk:
      return BAG_OK;
    case bag::ErrorCode::kInvalidArgument:
      return BAG_INVALID_ARGUMENT;
    case bag::ErrorCode::kNotReady:
      return BAG_NOT_READY;
    case bag::ErrorCode::kNotImplemented:
      return BAG_NOT_IMPLEMENTED;
    case bag::ErrorCode::kCancelled:
      return BAG_CANCELLED;
    case bag::ErrorCode::kInternal:
    default:
      return BAG_INTERNAL;
  }
}

bag_decode_content_status ToApiDecodeContentStatus(
    bag::DecodeContentStatus status) {
  switch (status) {
    case bag::DecodeContentStatus::kOk:
      return BAG_DECODE_CONTENT_STATUS_OK;
    case bag::DecodeContentStatus::kUnavailable:
      return BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    case bag::DecodeContentStatus::kInvalidTextPayload:
      return BAG_DECODE_CONTENT_STATUS_INVALID_TEXT_PAYLOAD;
    case bag::DecodeContentStatus::kBufferTooSmall:
      return BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
    case bag::DecodeContentStatus::kInternalError:
    default:
      return BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR;
  }
}

bag_encode_job_phase ToApiJobPhase(bag::EncodeProgressPhase phase) {
  switch (phase) {
    case bag::EncodeProgressPhase::kPreparingInput:
      return BAG_ENCODE_JOB_PHASE_PREPARING_INPUT;
    case bag::EncodeProgressPhase::kRenderingPcm:
      return BAG_ENCODE_JOB_PHASE_RENDERING_PCM;
    case bag::EncodeProgressPhase::kPostprocessing:
      return BAG_ENCODE_JOB_PHASE_POSTPROCESSING;
    case bag::EncodeProgressPhase::kFinalizing:
    default:
      return BAG_ENCODE_JOB_PHASE_FINALIZING;
  }
}

void UpdateMonotonicProgress(std::atomic<float>* progress_0_to_1,
                             float candidate) {
  if (progress_0_to_1 == nullptr) {
    return;
  }

  candidate = std::clamp(candidate, 0.0f, 1.0f);
  float current = progress_0_to_1->load();
  while (candidate > current &&
         !progress_0_to_1->compare_exchange_weak(current, candidate)) {
  }
}

void OnEncodeJobProgress(void* user_data, bag::EncodeProgressPhase phase,
                         float progress_0_to_1) {
  auto* job = static_cast<bag_encode_job*>(user_data);
  if (job == nullptr) {
    return;
  }

  job->phase.store(ToApiJobPhase(phase));
  UpdateMonotonicProgress(&job->progress_0_to_1, progress_0_to_1);
}

bool ShouldCancelEncodeJob(void* user_data) {
  auto* job = static_cast<bag_encode_job*>(user_data);
  return job != nullptr && job->cancel_requested.load();
}

bag_error_code CopyPcmToApiResult(const std::vector<std::int16_t>& pcm,
                                  bag_pcm16_result* out_result) {
  if (out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  out_result->samples = nullptr;
  out_result->sample_count = 0;
  if (pcm.empty()) {
    return BAG_OK;
  }

  auto* samples = new (std::nothrow) std::int16_t[pcm.size()];
  if (samples == nullptr) {
    return BAG_INTERNAL;
  }

  std::copy_n(pcm.data(), pcm.size(), samples);
  out_result->samples = samples;
  out_result->sample_count = pcm.size();
  return BAG_OK;
}

void ClearApiFollowData(bag_payload_follow_data* out_follow) {
  if (out_follow == nullptr) {
    return;
  }

  out_follow->byte_timeline_count = 0;
  out_follow->byte_timeline_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_follow->binary_group_timeline_count = 0;
  out_follow->binary_group_timeline_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_follow->payload_begin_sample = 0;
  out_follow->payload_sample_count = 0;
  out_follow->total_pcm_sample_count = 0;
  out_follow->available = 0;
}

void ClearApiTextFollowData(bag_text_follow_data* out_text_follow) {
  if (out_text_follow == nullptr) {
    return;
  }

  if (out_text_follow->text_tokens_buffer != nullptr &&
      out_text_follow->text_tokens_buffer_size > 0) {
    out_text_follow->text_tokens_buffer[0] = '\0';
  }
  out_text_follow->text_tokens_size = 0;
  out_text_follow->text_tokens_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->text_token_timeline_count = 0;
  out_text_follow->text_token_timeline_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->token_raw_segments_count = 0;
  out_text_follow->token_raw_segments_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->token_raw_display_units_count = 0;
  out_text_follow->token_raw_display_units_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  if (out_text_follow->lyric_lines_buffer != nullptr &&
      out_text_follow->lyric_lines_buffer_size > 0) {
    out_text_follow->lyric_lines_buffer[0] = '\0';
  }
  out_text_follow->lyric_lines_size = 0;
  out_text_follow->lyric_lines_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->lyric_line_timeline_count = 0;
  out_text_follow->lyric_line_timeline_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->line_token_ranges_count = 0;
  out_text_follow->line_token_ranges_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->line_raw_segments_count = 0;
  out_text_follow->line_raw_segments_status =
      BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_text_follow->available = 0;
}

void ClearApiEncodeResult(bag_encode_result* out_result) {
  if (out_result == nullptr) {
    return;
  }

  out_result->samples = nullptr;
  out_result->sample_count = 0;
  if (out_result->raw_bytes_hex_buffer != nullptr &&
      out_result->raw_bytes_hex_buffer_size > 0) {
    out_result->raw_bytes_hex_buffer[0] = '\0';
  }
  if (out_result->raw_bits_binary_buffer != nullptr &&
      out_result->raw_bits_binary_buffer_size > 0) {
    out_result->raw_bits_binary_buffer[0] = '\0';
  }
  out_result->raw_bytes_hex_size = 0;
  out_result->raw_bits_binary_size = 0;
  out_result->raw_bytes_hex_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_bits_binary_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_payload_available = 0;
  ClearApiFollowData(&out_result->follow_data);
  ClearApiTextFollowData(&out_result->text_follow_data);
}

void ClearApiDecodeResult(bag_decode_result* out_result) {
  if (out_result == nullptr) {
    return;
  }

  if (out_result->text_buffer != nullptr && out_result->text_buffer_size > 0) {
    out_result->text_buffer[0] = '\0';
  }
  if (out_result->raw_bytes_hex_buffer != nullptr &&
      out_result->raw_bytes_hex_buffer_size > 0) {
    out_result->raw_bytes_hex_buffer[0] = '\0';
  }
  if (out_result->raw_bits_binary_buffer != nullptr &&
      out_result->raw_bits_binary_buffer_size > 0) {
    out_result->raw_bits_binary_buffer[0] = '\0';
  }
  out_result->text_size = 0;
  out_result->raw_bytes_hex_size = 0;
  out_result->raw_bits_binary_size = 0;
  out_result->complete = 0;
  out_result->confidence = 0.0f;
  out_result->mode = BAG_TRANSPORT_FLASH;
  out_result->text_decode_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_bytes_hex_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_bits_binary_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_payload_available = 0;
  ClearApiFollowData(&out_result->follow_data);
  ClearApiTextFollowData(&out_result->text_follow_data);
}

template <typename SourceEntry, typename ApiEntry, typename CopyFn>
bag_decode_content_status CopyTimelineEntries(const std::vector<SourceEntry>& input,
                                              ApiEntry* buffer,
                                              std::size_t buffer_count,
                                              CopyFn&& copy_entry) {
  if (buffer == nullptr) {
    return input.empty() ? BAG_DECODE_CONTENT_STATUS_OK
                         : BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  }

  const std::size_t copy_count = std::min(buffer_count, input.size());
  for (std::size_t index = 0; index < copy_count; ++index) {
    copy_entry(input[index], &buffer[index]);
  }
  return copy_count == input.size() ? BAG_DECODE_CONTENT_STATUS_OK
                                    : BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
}

void CopyFollowDataToApi(const bag::PayloadFollowData& follow_data,
                         bag_payload_follow_data* out_follow) {
  if (out_follow == nullptr) {
    return;
  }

  out_follow->payload_begin_sample = follow_data.payload_begin_sample;
  out_follow->payload_sample_count = follow_data.payload_sample_count;
  out_follow->total_pcm_sample_count = follow_data.total_pcm_sample_count;
  out_follow->available = follow_data.available ? 1 : 0;
  out_follow->byte_timeline_count = follow_data.byte_timeline.size();
  out_follow->binary_group_timeline_count =
      follow_data.binary_group_timeline.size();
  if (!follow_data.available) {
    out_follow->byte_timeline_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_follow->binary_group_timeline_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    return;
  }

  out_follow->byte_timeline_status = CopyTimelineEntries(
      follow_data.byte_timeline, out_follow->byte_timeline_buffer,
      out_follow->byte_timeline_buffer_count,
      [](const bag::PayloadFollowByteEntry& source,
         bag_payload_follow_byte_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->byte_index = source.byte_index;
      });
  out_follow->binary_group_timeline_status = CopyTimelineEntries(
      follow_data.binary_group_timeline,
      out_follow->binary_group_timeline_buffer,
      out_follow->binary_group_timeline_buffer_count,
      [](const bag::PayloadFollowBinaryGroupEntry& source,
         bag_payload_follow_binary_group_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->group_index = source.group_index;
        destination->bit_offset = source.bit_offset;
        destination->bit_count = source.bit_count;
      });
}

void CopyTextFollowDataToApi(const bag::TextFollowData& text_follow_data,
                             bag_text_follow_data* out_text_follow) {
  if (out_text_follow == nullptr) {
    return;
  }

  out_text_follow->available = text_follow_data.available ? 1 : 0;
  out_text_follow->text_token_timeline_count =
      text_follow_data.text_token_timeline.size();
  out_text_follow->token_raw_segments_count =
      text_follow_data.token_raw_segments.size();
  out_text_follow->token_raw_display_units_count =
      text_follow_data.token_raw_display_units.size();
  out_text_follow->lyric_line_timeline_count =
      text_follow_data.lyric_line_timeline.size();
  out_text_follow->line_token_ranges_count =
      text_follow_data.line_token_ranges.size();
  out_text_follow->line_raw_segments_count =
      text_follow_data.line_raw_segments.size();
  out_text_follow->text_tokens_size = 0;
  out_text_follow->lyric_lines_size = 0;
  if (!text_follow_data.available) {
    out_text_follow->text_tokens_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->text_token_timeline_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->token_raw_segments_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->token_raw_display_units_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->lyric_lines_status = BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->lyric_line_timeline_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->line_token_ranges_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    out_text_follow->line_raw_segments_status =
        BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
    return;
  }

  std::vector<std::size_t> token_offsets(text_follow_data.text_tokens.size(), 0);
  std::vector<std::size_t> token_sizes(text_follow_data.text_tokens.size(), 0);
  std::size_t joined_size = 0;
  for (std::size_t index = 0; index < text_follow_data.text_tokens.size(); ++index) {
    token_offsets[index] = joined_size;
    token_sizes[index] = text_follow_data.text_tokens[index].size();
    joined_size += token_sizes[index];
    if (index + 1 < text_follow_data.text_tokens.size()) {
      ++joined_size;
    }
  }
  out_text_follow->text_tokens_size = joined_size;
  out_text_follow->text_tokens_status = CopyStringToApiBuffer(
      JoinTextTokens(text_follow_data.text_tokens),
      out_text_follow->text_tokens_buffer, out_text_follow->text_tokens_buffer_size);
  out_text_follow->lyric_lines_size = JoinTextTokens(text_follow_data.lyric_lines).size();
  out_text_follow->lyric_lines_status = CopyStringToApiBuffer(
      JoinTextTokens(text_follow_data.lyric_lines),
      out_text_follow->lyric_lines_buffer, out_text_follow->lyric_lines_buffer_size);
  out_text_follow->text_token_timeline_status = CopyTimelineEntries(
      text_follow_data.text_token_timeline,
      out_text_follow->text_token_timeline_buffer,
      out_text_follow->text_token_timeline_buffer_count,
      [&](const bag::TextFollowTokenTimelineEntry& source,
          bag_text_follow_token_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->token_index = source.token_index;
        destination->text_offset =
            source.token_index < token_offsets.size()
                ? token_offsets[source.token_index]
                : static_cast<std::size_t>(0);
        destination->text_size =
            source.token_index < token_sizes.size()
                 ? token_sizes[source.token_index]
                 : static_cast<std::size_t>(0);
      });
  out_text_follow->token_raw_segments_status = CopyTimelineEntries(
      text_follow_data.token_raw_segments,
      out_text_follow->token_raw_segments_buffer,
      out_text_follow->token_raw_segments_buffer_count,
      [](const bag::TextFollowRawSegmentEntry& source,
         bag_text_follow_raw_segment_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->token_index = source.token_index;
        destination->byte_offset = source.byte_offset;
        destination->byte_count = source.byte_count;
      });
  out_text_follow->token_raw_display_units_status = CopyTimelineEntries(
      text_follow_data.token_raw_display_units,
      out_text_follow->token_raw_display_units_buffer,
      out_text_follow->token_raw_display_units_buffer_count,
      [](const bag::TextFollowRawDisplayUnitEntry& source,
         bag_text_follow_raw_display_unit_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->token_index = source.token_index;
        destination->byte_index_within_token = source.byte_index_within_token;
        destination->byte_offset = source.byte_offset;
        destination->byte_count = source.byte_count;
      });
  out_text_follow->lyric_line_timeline_status = CopyTimelineEntries(
      text_follow_data.lyric_line_timeline,
      out_text_follow->lyric_line_timeline_buffer,
      out_text_follow->lyric_line_timeline_buffer_count,
      [](const bag::TextFollowLyricLineTimelineEntry& source,
         bag_text_follow_lyric_line_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->line_index = source.line_index;
      });
  out_text_follow->line_token_ranges_status = CopyTimelineEntries(
      text_follow_data.line_token_ranges,
      out_text_follow->line_token_ranges_buffer,
      out_text_follow->line_token_ranges_buffer_count,
      [](const bag::TextFollowLineTokenRangeEntry& source,
         bag_text_follow_line_token_range_entry* destination) {
        destination->line_index = source.line_index;
        destination->token_begin_index = source.token_begin_index;
        destination->token_count = source.token_count;
      });
  out_text_follow->line_raw_segments_status = CopyTimelineEntries(
      text_follow_data.line_raw_segments,
      out_text_follow->line_raw_segments_buffer,
      out_text_follow->line_raw_segments_buffer_count,
      [](const bag::TextFollowLineRawSegmentEntry& source,
         bag_text_follow_line_raw_segment_entry* destination) {
        destination->start_sample = source.start_sample;
        destination->sample_count = source.sample_count;
        destination->line_index = source.line_index;
        destination->byte_offset = source.byte_offset;
        destination->byte_count = source.byte_count;
      });
}

bag_error_code CopyStructuredEncodeResultToApi(
    const bag::EncodedPcmFollowResult& result, bag_encode_result* out_result) {
  if (out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  ClearApiEncodeResult(out_result);
  bag_pcm16_result pcm_result{};
  const bag_error_code pcm_code = CopyPcmToApiResult(result.pcm, &pcm_result);
  if (pcm_code != BAG_OK) {
    return pcm_code;
  }
  out_result->samples = pcm_result.samples;
  out_result->sample_count = pcm_result.sample_count;

  const std::string raw_bytes_hex =
      FormatRawBytesHex(result.follow_data.raw_payload_bytes);
  const std::string raw_bits_binary =
      FormatRawBitsBinary(result.follow_data.raw_payload_bytes);
  out_result->raw_bytes_hex_size = raw_bytes_hex.size();
  out_result->raw_bits_binary_size = raw_bits_binary.size();
  out_result->raw_payload_available = result.follow_data.available ? 1 : 0;
  out_result->raw_bytes_hex_status =
      result.follow_data.available
          ? CopyStringToApiBuffer(raw_bytes_hex, out_result->raw_bytes_hex_buffer,
                                  out_result->raw_bytes_hex_buffer_size)
          : BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_bits_binary_status =
      result.follow_data.available
          ? CopyStringToApiBuffer(raw_bits_binary,
                                  out_result->raw_bits_binary_buffer,
                                  out_result->raw_bits_binary_buffer_size)
          : BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  CopyFollowDataToApi(result.follow_data, &out_result->follow_data);
  CopyTextFollowDataToApi(result.text_follow_data, &out_result->text_follow_data);
  return BAG_OK;
}

void ClearEncodeJobResult(bag_encode_job* job) {
  if (job == nullptr) {
    return;
  }

  const std::lock_guard lock(job->result_mutex);
  job->result = {};
}

void CompleteEncodeJobSuccess(bag_encode_job* job,
                              bag::EncodedPcmFollowResult&& result) {
  {
    const std::lock_guard lock(job->result_mutex);
    job->result = std::move(result);
  }
  job->phase.store(BAG_ENCODE_JOB_PHASE_FINALIZING);
  UpdateMonotonicProgress(&job->progress_0_to_1, 1.0f);
  job->terminal_code.store(BAG_OK);
  job->state.store(BAG_ENCODE_JOB_SUCCEEDED);
}

void CompleteEncodeJobFailure(bag_encode_job* job, bag_error_code terminal_code,
                              bag_encode_job_state state) {
  ClearEncodeJobResult(job);
  job->terminal_code.store(terminal_code);
  job->state.store(state);
}

void JoinEncodeJobWorker(bag_encode_job* job) {
  if (job != nullptr && job->worker.joinable()) {
    job->worker.join();
  }
}

std::string FormatRawBytesHex(const std::vector<std::uint8_t>& bytes) {
  if (bytes.empty()) {
    return {};
  }

  static constexpr char kHexDigits[] = "0123456789ABCDEF";
  std::string out;
  out.reserve(bytes.size() * 3 - 1);
  for (std::size_t index = 0; index < bytes.size(); ++index) {
    const std::uint8_t value = bytes[index];
    if (index > 0) {
      out.push_back(' ');
    }
    out.push_back(kHexDigits[(value >> 4) & 0x0F]);
    out.push_back(kHexDigits[value & 0x0F]);
  }
  return out;
}

std::string FormatRawBitsBinary(const std::vector<std::uint8_t>& bytes) {
  if (bytes.empty()) {
    return {};
  }

  std::string out;
  out.reserve(bytes.size() * 9 - 1);
  for (std::size_t index = 0; index < bytes.size(); ++index) {
    const std::uint8_t value = bytes[index];
    if (index > 0) {
      out.push_back(' ');
    }
    for (int shift = 7; shift >= 0; --shift) {
      out.push_back(((value >> shift) & 0x01U) != 0U ? '1' : '0');
    }
  }
  return out;
}

std::string JoinTextTokens(const std::vector<std::string>& tokens) {
  if (tokens.empty()) {
    return {};
  }

  std::size_t total_size = 0;
  for (const auto& token : tokens) {
    total_size += token.size();
  }
  total_size += tokens.size() - 1;

  std::string out;
  out.reserve(total_size);
  for (std::size_t index = 0; index < tokens.size(); ++index) {
    if (index > 0) {
      out.push_back('\n');
    }
    out.append(tokens[index]);
  }
  return out;
}

bag_decode_content_status CopyStringToApiBuffer(std::string_view input,
                                                char* buffer,
                                                std::size_t buffer_size) {
  if (buffer == nullptr) {
    return BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  }
  if (buffer_size == 0) {
    return input.empty() ? BAG_DECODE_CONTENT_STATUS_OK
                         : BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
  }

  const std::size_t copy_size =
      std::min(input.size(), buffer_size - static_cast<std::size_t>(1));
  if (copy_size > 0) {
    std::copy_n(input.data(), copy_size, buffer);
  }
  buffer[copy_size] = '\0';
  return copy_size == input.size() ? BAG_DECODE_CONTENT_STATUS_OK
                                   : BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL;
}

void RunEncodeTextJobWorker(bag_encode_job* job) {
  if (job == nullptr) {
    return;
  }

  job->state.store(BAG_ENCODE_JOB_RUNNING);
  job->phase.store(BAG_ENCODE_JOB_PHASE_PREPARING_INPUT);
  job->progress_0_to_1.store(0.0f);
  job->terminal_code.store(BAG_NOT_READY);

  const bag::EncodeProgressSink progress_sink{
      .user_data = job,
      .on_progress = &OnEncodeJobProgress,
      .should_cancel = &ShouldCancelEncodeJob,
  };

  try {
    bag::EncodedPcmFollowResult result{};
    const bag::ErrorCode encode_code = bag::EncodeTextToPcm16WithFollowData(
        job->core_config, job->text, &result, &progress_sink);
    switch (encode_code) {
      case bag::ErrorCode::kOk:
        CompleteEncodeJobSuccess(job, std::move(result));
        break;
      case bag::ErrorCode::kCancelled:
        CompleteEncodeJobFailure(job, BAG_CANCELLED, BAG_ENCODE_JOB_CANCELLED);
        break;
      default:
        CompleteEncodeJobFailure(job, ToApiCode(encode_code),
                                 BAG_ENCODE_JOB_FAILED);
        break;
    }
  } catch (const std::invalid_argument&) {
    CompleteEncodeJobFailure(job, BAG_INVALID_ARGUMENT, BAG_ENCODE_JOB_FAILED);
  } catch (...) {
    CompleteEncodeJobFailure(job, BAG_INTERNAL, BAG_ENCODE_JOB_FAILED);
  }
}

bag_validation_issue ToApiValidationIssue(bag::TransportValidationIssue issue) {
  switch (issue) {
    case bag::TransportValidationIssue::kOk:
      return BAG_VALIDATION_OK;
    case bag::TransportValidationIssue::kInvalidSampleRate:
      return BAG_VALIDATION_INVALID_SAMPLE_RATE;
    case bag::TransportValidationIssue::kInvalidFrameSamples:
      return BAG_VALIDATION_INVALID_FRAME_SAMPLES;
    case bag::TransportValidationIssue::kInvalidMode:
      return BAG_VALIDATION_INVALID_MODE;
    case bag::TransportValidationIssue::kProAsciiOnly:
      return BAG_VALIDATION_PRO_ASCII_ONLY;
    case bag::TransportValidationIssue::kPayloadTooLarge:
      return BAG_VALIDATION_PAYLOAD_TOO_LARGE;
    case bag::TransportValidationIssue::kInvalidFlashSignalProfile:
      return BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE;
    case bag::TransportValidationIssue::kInvalidFlashVoicingFlavor:
      return BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR;
    default:
      return BAG_VALIDATION_INVALID_MODE;
  }
}
}  // namespace

const char* bag_transport_mode_name(bag_transport_mode mode) {
  switch (mode) {
    case BAG_TRANSPORT_FLASH:
      return kModeFlash;
    case BAG_TRANSPORT_PRO:
      return kModePro;
    case BAG_TRANSPORT_ULTRA:
      return kModeUltra;
    default:
      return "unknown";
  }
}

int bag_try_parse_transport_mode(const char* raw_mode,
                                 bag_transport_mode* out_mode) {
  if (raw_mode == nullptr || out_mode == nullptr) {
    return 0;
  }
  const std::string mode_text(raw_mode);
  if (mode_text == kModeFlash) {
    *out_mode = BAG_TRANSPORT_FLASH;
    return 1;
  }
  if (mode_text == kModePro) {
    *out_mode = BAG_TRANSPORT_PRO;
    return 1;
  }
  if (mode_text == kModeUltra) {
    *out_mode = BAG_TRANSPORT_ULTRA;
    return 1;
  }
  return 0;
}

bag_validation_issue bag_validate_encode_request(
    const bag_encoder_config* config, const char* text) {
  if (config == nullptr) {
    return BAG_VALIDATION_NULL_CONFIG;
  }
  if (text == nullptr) {
    return BAG_VALIDATION_NULL_TEXT;
  }

  return ToApiValidationIssue(bag::ValidateEncodeRequest(
      ToCoreConfig(config->sample_rate_hz, config->frame_samples,
                   config->enable_diagnostics, config->mode,
                   config->flash_signal_profile, config->flash_voicing_flavor,
                   config->reserved),
      text));
}

bag_validation_issue bag_validate_decoder_config(
    const bag_decoder_config* config) {
  if (config == nullptr) {
    return BAG_VALIDATION_NULL_CONFIG;
  }
  return ToApiValidationIssue(bag::ValidateDecoderConfig(ToCoreConfig(
      config->sample_rate_hz, config->frame_samples, config->enable_diagnostics,
      config->mode, config->flash_signal_profile, config->flash_voicing_flavor,
      config->reserved)));
}

const char* bag_validation_issue_message(bag_validation_issue issue) {
  switch (issue) {
    case BAG_VALIDATION_OK:
      return "";
    case BAG_VALIDATION_NULL_CONFIG:
      return "Missing configuration.";
    case BAG_VALIDATION_NULL_TEXT:
      return "Missing input text.";
    case BAG_VALIDATION_NULL_DECODER_OUTPUT:
      return "Missing decoder output target.";
    case BAG_VALIDATION_INVALID_SAMPLE_RATE:
      return "Sample rate must be greater than 0.";
    case BAG_VALIDATION_INVALID_FRAME_SAMPLES:
      return "Frame sample count must be greater than 0.";
    case BAG_VALIDATION_INVALID_MODE:
      return "Unsupported transport mode. Use flash, pro, or ultra. Charset "
             "rules: flash accepts unrestricted text bytes, pro requires "
             "ASCII, ultra is for UTF-8 text.";
    case BAG_VALIDATION_PRO_ASCII_ONLY:
      return "`pro` mode only supports ASCII input. Use `flash` for "
             "unrestricted text bytes or `ultra` for UTF-8 text.";
    case BAG_VALIDATION_PAYLOAD_TOO_LARGE:
      return "Payload exceeds the 512-byte single-frame limit.";
    case BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE:
      return "Unsupported flash signal profile. Use coded_burst, "
             "ritual_chant, or deep_ritual.";
    case BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR:
      return "Unsupported flash voicing flavor. Use coded_burst, "
             "ritual_chant, or deep_ritual.";
    default:
      return "Unknown validation issue.";
  }
}

const char* bag_error_code_message(bag_error_code code) {
  switch (code) {
    case BAG_OK:
      return "OK.";
    case BAG_INVALID_ARGUMENT:
      return "Invalid argument.";
    case BAG_NOT_READY:
      return "Result not ready.";
    case BAG_NOT_IMPLEMENTED:
      return "Not implemented.";
    case BAG_CANCELLED:
      return "Cancelled.";
    case BAG_INTERNAL:
    default:
      return "Internal error.";
  }
}

bag_error_code bag_encode_text(const bag_encoder_config* config,
                               const char* text, bag_pcm16_result* out_result) {
  if (config == nullptr || text == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  if (bag_validate_encode_request(config, text) != BAG_VALIDATION_OK) {
    out_result->samples = nullptr;
    out_result->sample_count = 0;
    return BAG_INVALID_ARGUMENT;
  }

  try {
    const bag::CoreConfig core_config = ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved);
    std::vector<std::int16_t> pcm;
    const bag::ErrorCode encode_code =
        bag::EncodeTextToPcm16(core_config, text, &pcm);
    if (encode_code != bag::ErrorCode::kOk) {
      out_result->samples = nullptr;
      out_result->sample_count = 0;
      return ToApiCode(encode_code);
    }
    return CopyPcmToApiResult(pcm, out_result);
  } catch (const std::invalid_argument&) {
    out_result->samples = nullptr;
    out_result->sample_count = 0;
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    out_result->samples = nullptr;
    out_result->sample_count = 0;
    return BAG_INTERNAL;
  }
}

bag_error_code bag_encode_text_with_follow(const bag_encoder_config* config,
                                           const char* text,
                                           bag_encode_result* out_result) {
  if (config == nullptr || text == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  if (bag_validate_encode_request(config, text) != BAG_VALIDATION_OK) {
    ClearApiEncodeResult(out_result);
    return BAG_INVALID_ARGUMENT;
  }

  try {
    const bag::CoreConfig core_config = ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved);
    bag::EncodedPcmFollowResult result{};
    const bag::ErrorCode encode_code =
        bag::EncodeTextToPcm16WithFollowData(core_config, text, &result);
    if (encode_code != bag::ErrorCode::kOk) {
      ClearApiEncodeResult(out_result);
      return ToApiCode(encode_code);
    }
    return CopyStructuredEncodeResultToApi(result, out_result);
  } catch (const std::invalid_argument&) {
    ClearApiEncodeResult(out_result);
    return BAG_INVALID_ARGUMENT;
  } catch (...) {
    ClearApiEncodeResult(out_result);
    return BAG_INTERNAL;
  }
}

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

    job->core_config = ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved);
    job->text = text;
    job->worker = std::thread([job]() { RunEncodeTextJobWorker(job); });
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
  if (job == nullptr || out_progress == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  out_progress->state = job->state.load();
  out_progress->phase = job->phase.load();
  out_progress->progress_0_to_1 =
      std::clamp(job->progress_0_to_1.load(), 0.0f, 1.0f);
  out_progress->terminal_code = job->terminal_code.load();
  return BAG_OK;
}

bag_error_code bag_cancel_encode_text_job(bag_encode_job* job) {
  if (job == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  job->cancel_requested.store(true);
  return BAG_OK;
}

bag_error_code bag_take_encode_text_job_result(const bag_encode_job* job,
                                               bag_pcm16_result* out_result) {
  if (job == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  const bag_encode_job_state state = job->state.load();
  if (state == BAG_ENCODE_JOB_SUCCEEDED) {
    const std::lock_guard lock(job->result_mutex);
    return CopyPcmToApiResult(job->result.pcm, out_result);
  }

  out_result->samples = nullptr;
  out_result->sample_count = 0;
  if (state == BAG_ENCODE_JOB_CANCELLED) {
    return BAG_CANCELLED;
  }
  if (state == BAG_ENCODE_JOB_FAILED) {
    const bag_error_code terminal_code = job->terminal_code.load();
    return terminal_code == BAG_NOT_READY ? BAG_INTERNAL : terminal_code;
  }
  return BAG_NOT_READY;
}

bag_error_code bag_take_encode_text_job_result_with_follow(
    const bag_encode_job* job, bag_encode_result* out_result) {
  if (job == nullptr || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  const bag_encode_job_state state = job->state.load();
  if (state == BAG_ENCODE_JOB_SUCCEEDED) {
    const std::lock_guard lock(job->result_mutex);
    return CopyStructuredEncodeResultToApi(job->result, out_result);
  }

  ClearApiEncodeResult(out_result);
  if (state == BAG_ENCODE_JOB_CANCELLED) {
    return BAG_CANCELLED;
  }
  if (state == BAG_ENCODE_JOB_FAILED) {
    const bag_error_code terminal_code = job->terminal_code.load();
    return terminal_code == BAG_NOT_READY ? BAG_INTERNAL : terminal_code;
  }
  return BAG_NOT_READY;
}

void bag_destroy_encode_text_job(bag_encode_job* job) {
  if (job == nullptr) {
    return;
  }

  job->cancel_requested.store(true);
  JoinEncodeJobWorker(job);
  delete job;
}

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

  const std::string raw_bytes_hex = FormatRawBytesHex(result.raw_payload_bytes);
  const std::string raw_bits_binary =
      FormatRawBitsBinary(result.raw_payload_bytes);
  out_result->text_size = result.text.size();
  out_result->raw_bytes_hex_size = raw_bytes_hex.size();
  out_result->raw_bits_binary_size = raw_bits_binary.size();
  out_result->complete = result.complete ? 1 : 0;
  out_result->confidence = result.confidence;
  out_result->mode = ToApiMode(result.mode);
  out_result->text_decode_status = ToApiDecodeContentStatus(result.text_status);
  out_result->raw_payload_available = result.raw_payload_available ? 1 : 0;
  out_result->raw_bytes_hex_status =
      result.raw_payload_available
          ? CopyStringToApiBuffer(raw_bytes_hex, out_result->raw_bytes_hex_buffer,
                                  out_result->raw_bytes_hex_buffer_size)
          : BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  out_result->raw_bits_binary_status =
      result.raw_payload_available
          ? CopyStringToApiBuffer(raw_bits_binary,
                                  out_result->raw_bits_binary_buffer,
                                  out_result->raw_bits_binary_buffer_size)
          : BAG_DECODE_CONTENT_STATUS_UNAVAILABLE;
  CopyFollowDataToApi(result.follow_data, &out_result->follow_data);
  CopyTextFollowDataToApi(result.text_follow_data, &out_result->text_follow_data);
  const bag_decode_content_status copied_text_status =
      CopyStringToApiBuffer(result.text, out_result->text_buffer,
                            out_result->text_buffer_size);
  if (out_result->text_decode_status == BAG_DECODE_CONTENT_STATUS_OK &&
      copied_text_status == BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL) {
    out_result->text_decode_status = copied_text_status;
  }
  return BAG_OK;
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

const char* bag_core_version(void) {
  return bag::CoreVersion();
}
