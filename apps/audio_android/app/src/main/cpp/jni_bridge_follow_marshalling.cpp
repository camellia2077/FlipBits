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

jclass FindClassOrNull(JNIEnv* env, const char* name) {
    return env->FindClass(name);
}

}  // namespace

std::vector<std::string> SplitOnSpaces(const std::string& value) {
    std::vector<std::string> tokens;
    std::size_t token_begin = 0;
    while (token_begin < value.size()) {
        while (token_begin < value.size() && value[token_begin] == ' ') {
            ++token_begin;
        }
        if (token_begin >= value.size()) {
            break;
        }
        const std::size_t token_end = value.find(' ', token_begin);
        if (token_end == std::string::npos) {
            tokens.push_back(value.substr(token_begin));
            break;
        }
        tokens.push_back(value.substr(token_begin, token_end - token_begin));
        token_begin = token_end + 1;
    }
    return tokens;
}

std::vector<std::string> SplitOnLines(const std::string& value) {
    std::vector<std::string> tokens;
    std::size_t token_begin = 0;
    while (token_begin <= value.size()) {
        const std::size_t token_end = value.find('\n', token_begin);
        if (token_end == std::string::npos) {
            if (token_begin < value.size()) {
                tokens.push_back(value.substr(token_begin));
            }
            break;
        }
        tokens.push_back(value.substr(token_begin, token_end - token_begin));
        token_begin = token_end + 1;
    }
    return tokens;
}

std::string RemoveSpaces(const std::string& value) {
    std::string compact;
    compact.reserve(value.size());
    for (const char ch : value) {
        if (ch != ' ') {
            compact.push_back(ch);
        }
    }
    return compact;
}

jobject NewPayloadFollowViewData(JNIEnv* env,
                                 const std::string& text_tokens,
                                 const std::string& text_character_text,
                                 const std::string& lyric_lines,
                                 const std::string& raw_bytes_hex,
                                 const std::string& raw_bits_binary,
                                 const std::vector<bag_text_follow_token_entry>& text_entries,
                                 const std::vector<bag_text_follow_character_entry>& text_characters,
                                 const std::vector<bag_text_follow_raw_segment_entry>& text_raw_segments,
                                 const std::vector<bag_text_follow_raw_display_unit_entry>& text_raw_display_units,
                                 const std::vector<bag_text_follow_lyric_line_entry>& line_entries,
                                 const std::vector<bag_text_follow_line_token_range_entry>& line_token_ranges,
                                 const std::vector<bag_text_follow_line_raw_segment_entry>& line_raw_segments,
                                 const std::vector<bag_payload_follow_byte_entry>& byte_entries,
                                 const std::vector<bag_payload_follow_binary_group_entry>& binary_entries,
                                 const std::vector<bag_ultra_frame_symbol_entry>& ultra_frame_entries,
                                 jboolean text_follow_available,
                                 jboolean lyric_line_follow_available,
                                 jint payload_begin_sample,
                                 jint payload_sample_count,
                                 jint total_pcm_sample_count,
                                 jboolean follow_available) {
    jclass result_class = FindClassOrNull(env, "com/bag/audioandroid/domain/PayloadFollowViewData");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(
            result_class,
            "<init>",
            "(Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;ZLjava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;ZLjava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;IIIZLjava/util/List;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    std::vector<std::string> text_follow_tokens = SplitOnLines(text_tokens);
    std::vector<std::string> lyric_line_tokens = SplitOnLines(lyric_lines);
    std::vector<std::string> hex_tokens = SplitOnSpaces(raw_bytes_hex);
    std::vector<std::string> binary_tokens;
    const std::string compact_bits = RemoveSpaces(raw_bits_binary);
    binary_tokens.reserve(binary_entries.size());
    for (const auto& entry : binary_entries) {
        const std::size_t bit_offset = entry.bit_offset;
        const std::size_t bit_count = entry.bit_count;
        if (bit_offset >= compact_bits.size()) {
            binary_tokens.emplace_back();
            continue;
        }
        const std::size_t token_size =
            std::min(bit_count, compact_bits.size() - bit_offset);
        binary_tokens.push_back(compact_bits.substr(bit_offset, token_size));
    }

    jobject text_list = NewStringList(env, text_follow_tokens);
    jobject text_timeline_list = NewTextTimelineList(env, text_entries);
    jobject text_character_list =
        NewTextCharacterList(env, text_characters, text_character_text);
    jobject text_raw_segment_list =
        NewTextRawSegmentList(env, text_raw_segments, hex_tokens, compact_bits);
    jobject text_raw_display_unit_list =
        NewTextRawDisplayUnitList(env, text_raw_display_units, hex_tokens, compact_bits);
    jobject lyric_line_list = NewStringList(env, lyric_line_tokens);
    jobject lyric_line_timeline_list = NewLyricLineTimelineList(env, line_entries);
    jobject line_token_range_list = NewLineTokenRangeList(env, line_token_ranges);
    jobject line_raw_segment_list =
        NewLineRawSegmentList(env, line_raw_segments, hex_tokens, compact_bits);
    jobject hex_list = NewStringList(env, hex_tokens);
    jobject binary_list = NewStringList(env, binary_tokens);
    jobject byte_timeline_list = NewByteTimelineList(env, byte_entries);
    jobject binary_timeline_list = NewBinaryTimelineList(env, binary_entries);
    jobject ultra_frame_timeline_list = NewUltraFrameTimelineList(env, ultra_frame_entries);
    if (text_list == nullptr || text_timeline_list == nullptr ||
        text_character_list == nullptr ||
        text_raw_segment_list == nullptr || text_raw_display_unit_list == nullptr ||
        lyric_line_list == nullptr ||
        lyric_line_timeline_list == nullptr || line_token_range_list == nullptr ||
        line_raw_segment_list == nullptr ||
        hex_list == nullptr || binary_list == nullptr ||
        byte_timeline_list == nullptr || binary_timeline_list == nullptr ||
        ultra_frame_timeline_list == nullptr) {
        DeleteLocalRefIfNotNull(env, result_class);
        return nullptr;
    }
    jobject out = env->NewObject(
        result_class,
        ctor,
        text_list,
        text_timeline_list,
        text_character_list,
        text_raw_segment_list,
        text_raw_display_unit_list,
        text_follow_available,
        lyric_line_list,
        lyric_line_timeline_list,
        line_token_range_list,
        line_raw_segment_list,
        lyric_line_follow_available,
        hex_list,
        binary_list,
        byte_timeline_list,
        binary_timeline_list,
        payload_begin_sample,
        payload_sample_count,
        total_pcm_sample_count,
        follow_available,
        ultra_frame_timeline_list);
    DeleteLocalRefIfNotNull(env, text_list);
    DeleteLocalRefIfNotNull(env, text_timeline_list);
    DeleteLocalRefIfNotNull(env, text_character_list);
    DeleteLocalRefIfNotNull(env, text_raw_segment_list);
    DeleteLocalRefIfNotNull(env, text_raw_display_unit_list);
    DeleteLocalRefIfNotNull(env, lyric_line_list);
    DeleteLocalRefIfNotNull(env, lyric_line_timeline_list);
    DeleteLocalRefIfNotNull(env, line_token_range_list);
    DeleteLocalRefIfNotNull(env, line_raw_segment_list);
    DeleteLocalRefIfNotNull(env, hex_list);
    DeleteLocalRefIfNotNull(env, binary_list);
    DeleteLocalRefIfNotNull(env, byte_timeline_list);
    DeleteLocalRefIfNotNull(env, binary_timeline_list);
    DeleteLocalRefIfNotNull(env, ultra_frame_timeline_list);
    DeleteLocalRefIfNotNull(env, result_class);
    return out;
}

jobject NewDecodedPayloadViewData(JNIEnv* env,
                                  const std::string& text,
                                  const std::string& raw_bytes_hex,
                                  const std::string& raw_bits_binary,
                                  jint text_decode_status_code,
                                  jboolean raw_payload_available) {
    jclass result_class = env->FindClass("com/bag/audioandroid/domain/DecodedPayloadViewData");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(result_class, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IZ)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jstring text_value = env->NewStringUTF(text.c_str());
    jstring raw_bytes_hex_value = env->NewStringUTF(raw_bytes_hex.c_str());
    jstring raw_bits_binary_value = env->NewStringUTF(raw_bits_binary.c_str());
    jobject out = env->NewObject(
        result_class,
        ctor,
        text_value,
        raw_bytes_hex_value,
        raw_bits_binary_value,
        text_decode_status_code,
        raw_payload_available);
    DeleteLocalRefIfNotNull(env, text_value);
    DeleteLocalRefIfNotNull(env, raw_bytes_hex_value);
    DeleteLocalRefIfNotNull(env, raw_bits_binary_value);
    DeleteLocalRefIfNotNull(env, result_class);
    return out;
}

jobject NewEmptyPayloadFollowViewData(JNIEnv* env) {
    return NewPayloadFollowViewData(
        env, "", "", "", "", "", {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, JNI_FALSE, JNI_FALSE, 0, 0, 0, JNI_FALSE);
}

}  // namespace jni_bridge

