#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "android_bag/common/config.h"

namespace bag {

enum class EncodeProgressPhase {
    kPreparingInput = 0,
    kRenderingPcm = 1,
    kPostprocessing = 2,
    kFinalizing = 3,
};

using EncodeProgressCallback = void (*)(void* user_data,
                                        EncodeProgressPhase phase,
                                        float progress_0_to_1);
using EncodeShouldCancelCallback = bool (*)(void* user_data);

struct EncodeProgressSink {
    void* user_data = nullptr;
    EncodeProgressCallback on_progress = nullptr;
    EncodeShouldCancelCallback should_cancel = nullptr;
};

struct EncodeCancelled {};

inline bool ShouldCancelEncode(const EncodeProgressSink* sink) {
    return sink != nullptr && sink->should_cancel != nullptr &&
           sink->should_cancel(sink->user_data);
}

inline void ReportEncodeProgress(const EncodeProgressSink* sink,
                                 EncodeProgressPhase phase,
                                 float progress_0_to_1) {
    if (sink != nullptr && sink->on_progress != nullptr) {
        sink->on_progress(sink->user_data, phase, progress_0_to_1);
    }
}

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
};

struct TextFollowTokenTimelineEntry {
    std::size_t start_sample = 0;
    std::size_t sample_count = 0;
    std::size_t token_index = 0;
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
