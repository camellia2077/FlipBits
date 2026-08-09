#pragma once

#include <cstddef>
#include <cstdint>
#include <string>

#include "wav_io.h"

namespace audio_io::detail::wav_metadata_parse_rules {

bool IsSupportedMode(FlipBitsAudioMetadataMode mode);
bool IsSupportedFlashVoicingStyle(
    FlipBitsAudioMetadataFlashVoicingStyle style);
bool IsSupportedMiniSpeedStyle(FlipBitsAudioMetadataMiniSpeedStyle style);
bool IsSupportedMetadataVersion(std::uint8_t version);
bool IsValidCreatedAtIsoUtc(const std::string& created_at_iso_utc);
bool IsValidVersionText(const std::string& version_text);
bool IsSupportedInputSourceKind(FlipBitsAudioMetadataInputSourceKind kind);
bool IsValidSegmentSampleCounts(const FlipBitsAudioMetadata& metadata);

FlipBitsAudioMetadataParseResult ParseFlipBitsMetadataChunk(
    const std::uint8_t* chunk_data, std::size_t chunk_size);
FlipBitsAudioMetadataParseResult ParseFlipBitsAudioMetadataBytes(
    const std::uint8_t* wav_bytes, std::size_t wav_byte_count);

}  // namespace audio_io::detail::wav_metadata_parse_rules
