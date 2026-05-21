#pragma once

#include <cstddef>
#include <cstdint>
#include <limits>
#include <string>
#include <vector>

#include "bag/interface/common/config.h"

namespace bag {

struct PcmBlock {
  const std::int16_t* samples = nullptr;
  std::size_t sample_count = 0;
  std::int64_t timestamp_ms = 0;
};

struct IrPacket {
  std::vector<std::uint8_t> bits;
  std::int64_t timestamp_ms = 0;
  float confidence = 0.0f;
};

struct TextResult {
  std::string text;
  bool complete = false;
  float confidence = 0.0f;
  TransportMode mode = TransportMode::kFlash;
};

struct PayloadFollowByteEntry {
  std::size_t start_sample = 0;
  std::size_t sample_count = 0;
  std::size_t byte_index = 0;
};

struct PayloadFollowBinaryGroupEntry {
  std::size_t start_sample = 0;
  std::size_t sample_count = 0;
  std::size_t group_index = 0;
  std::size_t bit_offset = 0;
  std::size_t bit_count = 0;
  double carrier_freq_hz = 0.0;
};

enum class UltraFrameSection {
  kPreamble = 0,
  kSync = 1,
  kVersion = 2,
  kFlags = 3,
  kPayloadLength = 4,
  kPayload = 5,
  kCrc16 = 6,
};

struct UltraFrameSymbolEntry {
  std::size_t start_sample = 0;
  std::size_t sample_count = 0;
  std::size_t frame_byte_index = 0;
  std::size_t nibble_index_in_byte = 0;
  std::size_t nibble_value = 0;
  double carrier_freq_hz = 0.0;
  UltraFrameSection section = UltraFrameSection::kPayload;
  bool is_payload = false;
  std::size_t payload_byte_index = std::numeric_limits<std::size_t>::max();
};

struct TextFollowTokenTimelineEntry {
  std::size_t start_sample = 0;
  std::size_t sample_count = 0;
  std::size_t token_index = 0;
};

// Character-level follow semantics inside a token. Frontends use this to
// decide whether a character should render as visible text or as a blank
// timing slot that still owns payload bytes.
enum class TextFollowCharacterKind {
  kVisible = 0,         // Visible text glyphs such as letters, digits, CJK.
  kSpace = 1,           // Space-like separators that should keep layout width.
  kNewline = 2,         // Newline separators attached to the preceding token.
  kSeparatorOther = 3,  // Other non-visible separators not covered above.
};

struct TextFollowCharacterEntry {
  std::size_t start_sample = 0;
  std::size_t sample_count = 0;
  std::size_t token_index = 0;
  std::size_t character_index_within_token = 0;
  std::size_t byte_index_within_token = 0;
  std::size_t byte_count = 0;
  TextFollowCharacterKind kind = TextFollowCharacterKind::kVisible;
  std::string text;
};

struct TextFollowRawSegmentEntry {
  std::size_t start_sample = 0;
  std::size_t sample_count = 0;
  std::size_t token_index = 0;
  std::size_t byte_offset = 0;
  std::size_t byte_count = 0;
};

struct TextFollowRawDisplayUnitEntry {
  std::size_t start_sample = 0;
  std::size_t sample_count = 0;
  std::size_t token_index = 0;
  std::size_t byte_index_within_token = 0;
  std::size_t byte_offset = 0;
  std::size_t byte_count = 0;
  std::size_t character_index_within_token = 0;
  std::size_t byte_index_within_character = 0;
  std::size_t character_byte_count = 0;
  bool is_character_start = false;
  bool is_character_end = false;
};

struct TextFollowLyricLineTimelineEntry {
  std::size_t start_sample = 0;
  std::size_t sample_count = 0;
  std::size_t line_index = 0;
};

struct TextFollowLineTokenRangeEntry {
  std::size_t line_index = 0;
  std::size_t token_begin_index = 0;
  std::size_t token_count = 0;
};

struct TextFollowLineRawSegmentEntry {
  std::size_t start_sample = 0;
  std::size_t sample_count = 0;
  std::size_t line_index = 0;
  std::size_t byte_offset = 0;
  std::size_t byte_count = 0;
};

struct TextFollowData {
  std::vector<std::string> text_tokens;
  std::vector<TextFollowTokenTimelineEntry> text_token_timeline;
  std::vector<TextFollowCharacterEntry> text_characters;
  std::vector<TextFollowRawSegmentEntry> token_raw_segments;
  std::vector<TextFollowRawDisplayUnitEntry> token_raw_display_units;
  std::vector<std::string> lyric_lines;
  std::vector<TextFollowLyricLineTimelineEntry> lyric_line_timeline;
  std::vector<TextFollowLineTokenRangeEntry> line_token_ranges;
  std::vector<TextFollowLineRawSegmentEntry> line_raw_segments;
  bool available = false;
};

struct PayloadFollowData {
  std::vector<std::uint8_t> raw_payload_bytes;
  std::vector<PayloadFollowByteEntry> byte_timeline;
  std::vector<PayloadFollowBinaryGroupEntry> binary_group_timeline;
  std::vector<UltraFrameSymbolEntry> ultra_frame_timeline;
  std::size_t payload_begin_sample = 0;
  std::size_t payload_sample_count = 0;
  std::size_t total_pcm_sample_count = 0;
  bool available = false;
};

struct EncodedPcmFollowResult {
  std::vector<std::int16_t> pcm;
  PayloadFollowData follow_data;
  TextFollowData text_follow_data;
};

enum class DecodeContentStatus {
  kOk = 0,
  kUnavailable = 1,
  kInvalidTextPayload = 2,
  kBufferTooSmall = 3,
  kInternalError = 4,
};

struct DecodeResult {
  std::string text;
  std::vector<std::uint8_t> raw_payload_bytes;
  bool raw_payload_available = false;
  PayloadFollowData follow_data;
  bool follow_available = false;
  TextFollowData text_follow_data;
  bool text_follow_available = false;
  bool complete = false;
  float confidence = 0.0f;
  TransportMode mode = TransportMode::kFlash;
  DecodeContentStatus text_status = DecodeContentStatus::kUnavailable;
};

}  // namespace bag
