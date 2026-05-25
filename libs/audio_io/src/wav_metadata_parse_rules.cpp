#include "wav_metadata_parse_rules.h"

#include <string>

namespace audio_io::detail::bytes_impl {
namespace {

constexpr std::size_t kRiffHeaderSize = 12;
constexpr std::size_t kChunkHeaderSize = 8;
constexpr std::size_t kFlipBitsMetadataIsoUtcByteCount = 20;
constexpr std::size_t kFlipBitsMetadataVersionTextByteCount = 32;
constexpr std::size_t kFlipBitsMetadataVersionOffset = 0;
constexpr std::size_t kFlipBitsMetadataModeOffset = 1;
constexpr std::size_t kFlipBitsMetadataHasFlashStyleOffset = 2;
constexpr std::size_t kFlipBitsMetadataFlashStyleOffset = 3;
constexpr std::size_t kFlipBitsMetadataInputSourceKindOffset = 4;
constexpr std::size_t kFlipBitsMetadataHasMiniSpeedStyleOffset = 5;
constexpr std::size_t kFlipBitsMetadataMiniSpeedStyleOffset = 6;
constexpr std::size_t kFlipBitsMetadataDurationOffset = 8;
constexpr std::size_t kFlipBitsMetadataCreatedAtOffset = 12;
constexpr std::size_t kFlipBitsMetadataSampleRateHzOffset =
    kFlipBitsMetadataCreatedAtOffset + kFlipBitsMetadataIsoUtcByteCount;
constexpr std::size_t kFlipBitsMetadataFrameSamplesOffset =
    kFlipBitsMetadataSampleRateHzOffset + 4;
constexpr std::size_t kFlipBitsMetadataPcmSampleCountOffset =
    kFlipBitsMetadataFrameSamplesOffset + 4;
constexpr std::size_t kFlipBitsMetadataPayloadByteCountOffset =
    kFlipBitsMetadataPcmSampleCountOffset + 4;
constexpr std::size_t kFlipBitsMetadataAppVersionOffset =
    kFlipBitsMetadataPayloadByteCountOffset + 4;
constexpr std::size_t kFlipBitsMetadataCoreVersionOffset =
    kFlipBitsMetadataAppVersionOffset + kFlipBitsMetadataVersionTextByteCount;
constexpr std::size_t kFlipBitsMetadataSegmentCountOffset =
    kFlipBitsMetadataCoreVersionOffset + kFlipBitsMetadataVersionTextByteCount;
constexpr std::size_t kFlipBitsMetadataSegmentSampleCountCountOffset =
    kFlipBitsMetadataSegmentCountOffset + 4;
constexpr std::size_t kFlipBitsMetadataHeaderSize =
    kFlipBitsMetadataSegmentSampleCountCountOffset + 4;
constexpr std::size_t kFlipBitsMetadataFixedPayloadSize =
    kFlipBitsMetadataHeaderSize + (2 * kFlipBitsMetadataVersionTextByteCount);
constexpr std::uint8_t kFlipBitsMetadataVersionV7 = 7;

bool IsSupportedMode(FlipBitsAudioMetadataMode mode) {
  return mode == FlipBitsAudioMetadataMode::kMini ||
         mode == FlipBitsAudioMetadataMode::kFlash ||
         mode == FlipBitsAudioMetadataMode::kPro ||
         mode == FlipBitsAudioMetadataMode::kUltra;
}

bool IsSupportedFlashVoicingStyle(
    FlipBitsAudioMetadataFlashVoicingStyle style) {
  return style == FlipBitsAudioMetadataFlashVoicingStyle::kStandard ||
         style == FlipBitsAudioMetadataFlashVoicingStyle::kLitany ||
         style == FlipBitsAudioMetadataFlashVoicingStyle::kHostility ||
         style == FlipBitsAudioMetadataFlashVoicingStyle::kCollapse ||
         style == FlipBitsAudioMetadataFlashVoicingStyle::kZeal ||
         style == FlipBitsAudioMetadataFlashVoicingStyle::kVoid;
}

bool IsSupportedMiniSpeedStyle(FlipBitsAudioMetadataMiniSpeedStyle style) {
  return style == FlipBitsAudioMetadataMiniSpeedStyle::kSlow ||
         style == FlipBitsAudioMetadataMiniSpeedStyle::kStandard ||
         style == FlipBitsAudioMetadataMiniSpeedStyle::kFast;
}

bool IsIsoUtcTimestampChar(char value, std::size_t index) {
  switch (index) {
    case 4:
    case 7:
      return value == '-';
    case 10:
      return value == 'T';
    case 13:
    case 16:
      return value == ':';
    case 19:
      return value == 'Z';
    default:
      return value >= '0' && value <= '9';
  }
}

bool IsSupportedMetadataVersion(std::uint8_t version) {
  return version == kFlipBitsMetadataVersionV7;
}

bool IsValidCreatedAtIsoUtc(const std::string& created_at_iso_utc) {
  if (created_at_iso_utc.size() != kFlipBitsMetadataIsoUtcByteCount) {
    return false;
  }
  for (std::size_t index = 0; index < created_at_iso_utc.size(); ++index) {
    if (!IsIsoUtcTimestampChar(created_at_iso_utc[index], index)) {
      return false;
    }
  }
  return true;
}

bool IsValidVersionText(const std::string& version_text) {
  if (version_text.empty() ||
      version_text.size() > kFlipBitsMetadataVersionTextByteCount) {
    return false;
  }
  for (char value : version_text) {
    if (value < 0x20 || value > 0x7Eu) {
      return false;
    }
  }
  return true;
}

bool IsSupportedInputSourceKind(FlipBitsAudioMetadataInputSourceKind kind) {
  return kind == FlipBitsAudioMetadataInputSourceKind::kManual ||
         kind == FlipBitsAudioMetadataInputSourceKind::kSample;
}

bool IsValidSegmentSampleCounts(const FlipBitsAudioMetadata& metadata) {
  if (metadata.segment_count == 0u) {
    return false;
  }
  if (metadata.segment_sample_counts.empty()) {
    return metadata.segment_count == 1u;
  }
  if (metadata.segment_sample_counts.size() != metadata.segment_count) {
    return false;
  }

  std::uint64_t total_segment_samples = 0;
  for (std::uint32_t sample_count : metadata.segment_sample_counts) {
    if (sample_count == 0u) {
      return false;
    }
    total_segment_samples += sample_count;
  }
  return total_segment_samples == metadata.pcm_sample_count;
}

std::string ReadPaddedAsciiString(const std::uint8_t* data,
                                  std::size_t byte_count) {
  std::size_t length = 0;
  while (length < byte_count && data[length] != 0u) {
    ++length;
  }
  return std::string(reinterpret_cast<const char*>(data), length);
}

bool HasRange(std::size_t total_size, std::size_t offset,
              std::size_t byte_count) {
  return offset <= total_size && byte_count <= (total_size - offset);
}

std::uint32_t ReadU32Le(const std::uint8_t* data) {
  return static_cast<std::uint32_t>(
      static_cast<std::uint32_t>(data[0]) |
      (static_cast<std::uint32_t>(data[1]) << 8) |
      (static_cast<std::uint32_t>(data[2]) << 16) |
      (static_cast<std::uint32_t>(data[3]) << 24));
}

bool MatchesTag(const std::uint8_t* data, const char* expected) {
  return data[0] == static_cast<std::uint8_t>(expected[0]) &&
         data[1] == static_cast<std::uint8_t>(expected[1]) &&
         data[2] == static_cast<std::uint8_t>(expected[2]) &&
         data[3] == static_cast<std::uint8_t>(expected[3]);
}

}  // namespace

FlipBitsAudioMetadataParseResult ParseFlipBitsMetadataChunk(
    const std::uint8_t* chunk_data, std::size_t chunk_size) {
  if (chunk_data == nullptr && chunk_size > 0) {
    return {FlipBitsAudioMetadataStatus::kInvalidArgument, {}};
  }
  if (chunk_size < kFlipBitsMetadataFixedPayloadSize) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }

  FlipBitsAudioMetadata metadata{};
  metadata.version = chunk_data[kFlipBitsMetadataVersionOffset];
  metadata.mode = static_cast<FlipBitsAudioMetadataMode>(
      chunk_data[kFlipBitsMetadataModeOffset]);
  metadata.has_flash_voicing_style =
      chunk_data[kFlipBitsMetadataHasFlashStyleOffset] != 0u;
  metadata.flash_voicing_style =
      static_cast<FlipBitsAudioMetadataFlashVoicingStyle>(
          chunk_data[kFlipBitsMetadataFlashStyleOffset]);

  if (!IsSupportedMetadataVersion(metadata.version)) {
    return {FlipBitsAudioMetadataStatus::kUnsupportedVersion, {}};
  }
  metadata.input_source_kind =
      static_cast<FlipBitsAudioMetadataInputSourceKind>(
          chunk_data[kFlipBitsMetadataInputSourceKindOffset]);
  metadata.has_mini_speed_style =
      chunk_data[kFlipBitsMetadataHasMiniSpeedStyleOffset] != 0u;
  metadata.mini_speed_style = static_cast<FlipBitsAudioMetadataMiniSpeedStyle>(
      chunk_data[kFlipBitsMetadataMiniSpeedStyleOffset]);
  metadata.duration_ms =
      ReadU32Le(chunk_data + kFlipBitsMetadataDurationOffset);
  metadata.created_at_iso_utc.assign(
      reinterpret_cast<const char*>(chunk_data +
                                    kFlipBitsMetadataCreatedAtOffset),
      kFlipBitsMetadataIsoUtcByteCount);
  metadata.sample_rate_hz =
      ReadU32Le(chunk_data + kFlipBitsMetadataSampleRateHzOffset);
  metadata.frame_samples =
      ReadU32Le(chunk_data + kFlipBitsMetadataFrameSamplesOffset);
  metadata.pcm_sample_count =
      ReadU32Le(chunk_data + kFlipBitsMetadataPcmSampleCountOffset);
  metadata.payload_byte_count =
      ReadU32Le(chunk_data + kFlipBitsMetadataPayloadByteCountOffset);
  if (!IsValidCreatedAtIsoUtc(metadata.created_at_iso_utc)) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }
  metadata.segment_count =
      ReadU32Le(chunk_data + kFlipBitsMetadataSegmentCountOffset);
  const std::uint32_t segment_sample_count_count =
      ReadU32Le(chunk_data + kFlipBitsMetadataSegmentSampleCountCountOffset);
  const std::size_t expected_size =
      kFlipBitsMetadataFixedPayloadSize +
      (static_cast<std::size_t>(segment_sample_count_count) * 4u);
  if (chunk_size != expected_size) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }
  metadata.segment_sample_counts.reserve(segment_sample_count_count);
  std::size_t read_offset = kFlipBitsMetadataHeaderSize;
  for (std::uint32_t index = 0; index < segment_sample_count_count; ++index) {
    metadata.segment_sample_counts.push_back(
        ReadU32Le(chunk_data + read_offset));
    read_offset += 4;
  }
  metadata.app_version =
      ReadPaddedAsciiString(chunk_data + kFlipBitsMetadataAppVersionOffset,
                            kFlipBitsMetadataVersionTextByteCount);
  metadata.core_version =
      ReadPaddedAsciiString(chunk_data + kFlipBitsMetadataCoreVersionOffset,
                            kFlipBitsMetadataVersionTextByteCount);
  if (!IsValidVersionText(metadata.app_version) ||
      !IsValidVersionText(metadata.core_version)) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }
  if (metadata.segment_count == 0u) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }
  if (!IsValidSegmentSampleCounts(metadata)) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }
  if (!IsSupportedMode(metadata.mode)) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }
  if (metadata.has_flash_voicing_style) {
    if (metadata.mode != FlipBitsAudioMetadataMode::kFlash ||
        !IsSupportedFlashVoicingStyle(metadata.flash_voicing_style)) {
      return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
    }
  } else {
    metadata.flash_voicing_style =
        FlipBitsAudioMetadataFlashVoicingStyle::kUnknown;
  }
  if (metadata.has_mini_speed_style) {
    if (metadata.mode != FlipBitsAudioMetadataMode::kMini ||
        !IsSupportedMiniSpeedStyle(metadata.mini_speed_style)) {
      return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
    }
  } else {
    metadata.mini_speed_style = FlipBitsAudioMetadataMiniSpeedStyle::kUnknown;
  }
  if (metadata.sample_rate_hz == 0u ||
      !IsSupportedInputSourceKind(metadata.input_source_kind)) {
    return {FlipBitsAudioMetadataStatus::kInvalidMetadata, {}};
  }

  return {FlipBitsAudioMetadataStatus::kOk, metadata};
}

FlipBitsAudioMetadataParseResult ParseFlipBitsAudioMetadataBytes(
    const std::uint8_t* wav_bytes, std::size_t wav_byte_count) {
  if (wav_bytes == nullptr && wav_byte_count > 0) {
    return {FlipBitsAudioMetadataStatus::kInvalidArgument, {}};
  }
  if (wav_byte_count < kRiffHeaderSize) {
    return {FlipBitsAudioMetadataStatus::kInvalidHeader, {}};
  }
  if (!MatchesTag(wav_bytes, "RIFF") || !MatchesTag(wav_bytes + 8, "WAVE")) {
    return {FlipBitsAudioMetadataStatus::kInvalidHeader, {}};
  }

  std::size_t chunk_offset = kRiffHeaderSize;
  while (HasRange(wav_byte_count, chunk_offset, kChunkHeaderSize)) {
    const std::uint8_t* chunk_header = wav_bytes + chunk_offset;
    const std::uint32_t chunk_size = ReadU32Le(chunk_header + 4);
    const std::size_t chunk_data_offset = chunk_offset + kChunkHeaderSize;
    if (MatchesTag(chunk_header, "data")) {
      return {FlipBitsAudioMetadataStatus::kNotFound, {}};
    }
    if (!HasRange(wav_byte_count, chunk_data_offset, chunk_size)) {
      return {FlipBitsAudioMetadataStatus::kTruncatedData, {}};
    }
    if (MatchesTag(chunk_header, "WBAG")) {
      return ParseFlipBitsMetadataChunk(wav_bytes + chunk_data_offset,
                                        chunk_size);
    }
    chunk_offset = chunk_data_offset + chunk_size + (chunk_size % 2u);
  }

  return {FlipBitsAudioMetadataStatus::kNotFound, {}};
}

}  // namespace audio_io::detail::bytes_impl
