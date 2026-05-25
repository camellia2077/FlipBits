#include "jni_bridge_internal.h"

#include <algorithm>

namespace jni_bridge {

namespace {

template <typename T>
void DeleteLocalRefIfNotNull(JNIEnv* env, T ref) {
    if (env != nullptr && ref != nullptr) {
        env->DeleteLocalRef(ref);
    }
}

template <typename T>
std::vector<T> CopyApiEntries(const T* buffer,
                              std::size_t count,
                              std::size_t buffer_count) {
    if (buffer == nullptr || count == 0 || buffer_count == 0) {
        return {};
    }
    const std::size_t safe_count = std::min(count, buffer_count);
    return std::vector<T>(buffer, buffer + safe_count);
}

}  // namespace

jobject NewEncodeFollowHydrationPayloadResultFromEncodeResult(
    JNIEnv* env,
    const bag_encode_result& result) {
    const std::string text_tokens = CopyApiString(
        result.text_follow_data.text_tokens_buffer,
        result.text_follow_data.text_tokens_size,
        result.text_follow_data.text_tokens_buffer_size);
    const std::string text_character_text = CopyApiString(
        result.text_follow_data.text_character_text_buffer,
        result.text_follow_data.text_character_text_size,
        result.text_follow_data.text_character_text_buffer_size);
    const std::string lyric_lines = CopyApiString(
        result.text_follow_data.lyric_lines_buffer,
        result.text_follow_data.lyric_lines_size,
        result.text_follow_data.lyric_lines_buffer_size);
    const std::string raw_bytes_hex =
        CopyApiString(
            result.raw_bytes_hex_buffer,
            result.raw_bytes_hex_size,
            result.raw_bytes_hex_buffer_size);
    const std::string raw_bits_binary = CopyApiString(
        result.raw_bits_binary_buffer,
        result.raw_bits_binary_size,
        result.raw_bits_binary_buffer_size);
    const std::vector<bag_text_follow_token_entry> text_entries =
        CopyApiEntries(
            result.text_follow_data.text_token_timeline_buffer,
            result.text_follow_data.text_token_timeline_count,
            result.text_follow_data.text_token_timeline_buffer_count);
    const std::vector<bag_text_follow_character_entry> text_characters =
        CopyApiEntries(
            result.text_follow_data.text_characters_buffer,
            result.text_follow_data.text_characters_count,
            result.text_follow_data.text_characters_buffer_count);
    const std::vector<bag_text_follow_raw_segment_entry> text_raw_segments =
        CopyApiEntries(
            result.text_follow_data.token_raw_segments_buffer,
            result.text_follow_data.token_raw_segments_count,
            result.text_follow_data.token_raw_segments_buffer_count);
    const std::vector<bag_text_follow_raw_display_unit_entry> text_raw_display_units =
        CopyApiEntries(
            result.text_follow_data.token_raw_display_units_buffer,
            result.text_follow_data.token_raw_display_units_count,
            result.text_follow_data.token_raw_display_units_buffer_count);
    const std::vector<bag_text_follow_lyric_line_entry> line_entries =
        CopyApiEntries(
            result.text_follow_data.lyric_line_timeline_buffer,
            result.text_follow_data.lyric_line_timeline_count,
            result.text_follow_data.lyric_line_timeline_buffer_count);
    const std::vector<bag_text_follow_line_token_range_entry> line_token_ranges =
        CopyApiEntries(
            result.text_follow_data.line_token_ranges_buffer,
            result.text_follow_data.line_token_ranges_count,
            result.text_follow_data.line_token_ranges_buffer_count);
    const std::vector<bag_text_follow_line_raw_segment_entry> line_raw_segments =
        CopyApiEntries(
            result.text_follow_data.line_raw_segments_buffer,
            result.text_follow_data.line_raw_segments_count,
            result.text_follow_data.line_raw_segments_buffer_count);
    const std::vector<bag_payload_follow_byte_entry> byte_entries =
        CopyApiEntries(
            result.follow_data.byte_timeline_buffer,
            result.follow_data.byte_timeline_count,
            result.follow_data.byte_timeline_buffer_count);
    const std::vector<bag_payload_follow_binary_group_entry> binary_entries =
        CopyApiEntries(
            result.follow_data.binary_group_timeline_buffer,
            result.follow_data.binary_group_timeline_count,
            result.follow_data.binary_group_timeline_buffer_count);
    const std::vector<bag_ultra_frame_symbol_entry> ultra_frame_entries =
        CopyApiEntries(
            result.follow_data.ultra_frame_timeline_buffer,
            result.follow_data.ultra_frame_timeline_count,
            result.follow_data.ultra_frame_timeline_buffer_count);
    jshortArray empty_pcm = env->NewShortArray(0);
    jobject follow_data = NewPayloadFollowViewData(
        env,
        text_tokens,
        text_character_text,
        lyric_lines,
        raw_bytes_hex,
        raw_bits_binary,
        text_entries,
        text_characters,
        text_raw_segments,
        text_raw_display_units,
        line_entries,
        line_token_ranges,
        line_raw_segments,
        byte_entries,
        binary_entries,
        ultra_frame_entries,
        result.text_follow_data.available != 0 ? JNI_TRUE : JNI_FALSE,
        (result.text_follow_data.available != 0 &&
         result.text_follow_data.lyric_line_timeline_count > 0 &&
         result.text_follow_data.line_token_ranges_count > 0)
            ? JNI_TRUE
            : JNI_FALSE,
        static_cast<jint>(result.follow_data.payload_begin_sample),
        static_cast<jint>(result.follow_data.payload_sample_count),
        static_cast<jint>(result.follow_data.total_pcm_sample_count),
        result.follow_data.available != 0 ? JNI_TRUE : JNI_FALSE);
    jobject out =
        NewEncodedAudioPayloadResult(env, empty_pcm, "", "", follow_data, kBagErrorOk);
    DeleteLocalRefIfNotNull(env, follow_data);
    return out;
}

jobject NewEmptyEncodeFollowHydrationPayloadResult(JNIEnv* env, jint terminal_code) {
    return NewEmptyEncodedPayloadResult(env, terminal_code);
}

}  // namespace jni_bridge

