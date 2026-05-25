#include "jni_bridge_internal.h"

#include <algorithm>
#include <limits>

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

jobject NewPayloadFollowByteEntry(JNIEnv* env, const bag_payload_follow_byte_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/PayloadFollowByteTimelineEntry");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(III)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jobject out = env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.byte_index));
    DeleteLocalRefIfNotNull(env, entry_class);
    return out;
}

jobject NewPayloadFollowBinaryGroupEntry(
    JNIEnv* env,
    const bag_payload_follow_binary_group_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/PayloadFollowBinaryGroupTimelineEntry");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(IIIIIF)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jobject out = env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.group_index),
        static_cast<jint>(entry.bit_offset),
        static_cast<jint>(entry.bit_count),
        static_cast<jfloat>(entry.carrier_freq_hz));
    DeleteLocalRefIfNotNull(env, entry_class);
    return out;
}

jint SizeToJIntOrMinusOne(std::size_t value) {
    if (value > static_cast<std::size_t>(std::numeric_limits<jint>::max())) {
        return -1;
    }
    return static_cast<jint>(value);
}

jobject NewUltraFrameSymbolEntry(
    JNIEnv* env,
    const bag_ultra_frame_symbol_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/UltraFrameSymbolTimelineEntry");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(IIIIIFIZI)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jobject out = env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.frame_byte_index),
        static_cast<jint>(entry.nibble_index_in_byte),
        static_cast<jint>(entry.nibble_value),
        static_cast<jfloat>(entry.carrier_freq_hz),
        static_cast<jint>(entry.section),
        entry.is_payload != 0 ? JNI_TRUE : JNI_FALSE,
        SizeToJIntOrMinusOne(entry.payload_byte_index));
    DeleteLocalRefIfNotNull(env, entry_class);
    return out;
}

jobject NewTextFollowTimelineEntry(JNIEnv* env, const bag_text_follow_token_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowTimelineEntry");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(III)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jobject out = env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.token_index));
    DeleteLocalRefIfNotNull(env, entry_class);
    return out;
}

jobject NewTextFollowCharacterViewData(JNIEnv* env,
                                       const bag_text_follow_character_entry& entry,
                                       const std::string& text_character_text) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowCharacterViewData");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(entry_class, "<init>", "(IIIIIIILjava/lang/String;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    std::string character_text;
    if (entry.text_size > 0 && entry.text_offset < text_character_text.size()) {
        const std::size_t clamped_text_size =
            std::min(entry.text_size, text_character_text.size() - entry.text_offset);
        character_text =
            text_character_text.substr(entry.text_offset, clamped_text_size);
    }
    jstring text_value = env->NewStringUTF(character_text.c_str());
    jobject out = env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.token_index),
        static_cast<jint>(entry.character_index_within_token),
        static_cast<jint>(entry.byte_index_within_token),
        static_cast<jint>(entry.byte_count),
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.kind),
        text_value);
    DeleteLocalRefIfNotNull(env, text_value);
    DeleteLocalRefIfNotNull(env, entry_class);
    return out;
}

jobject NewTextFollowRawSegmentViewData(JNIEnv* env,
                                        const bag_text_follow_raw_segment_entry& entry,
                                        const std::vector<std::string>& hex_tokens,
                                        const std::string& compact_bits) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowRawSegmentViewData");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(IIIIILjava/lang/String;Ljava/lang/String;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    std::string hex_text;
    for (std::size_t index = 0; index < entry.byte_count; ++index) {
        const std::size_t byte_index = entry.byte_offset + index;
        if (byte_index >= hex_tokens.size()) {
            break;
        }
        if (!hex_text.empty()) {
            hex_text.push_back(' ');
        }
        hex_text.append(hex_tokens[byte_index]);
    }

    std::string binary_text;
    const std::size_t bit_offset = entry.byte_offset * static_cast<std::size_t>(8);
    const std::size_t bit_count = entry.byte_count * static_cast<std::size_t>(8);
    if (bit_offset < compact_bits.size()) {
        const std::size_t clamped_bit_count =
            std::min(bit_count, compact_bits.size() - bit_offset);
        for (std::size_t index = 0; index < clamped_bit_count; ++index) {
            if (index > 0 && index % static_cast<std::size_t>(8) == 0) {
                binary_text.push_back(' ');
            }
            binary_text.push_back(compact_bits[bit_offset + index]);
        }
    }

    jstring hex_value = env->NewStringUTF(hex_text.c_str());
    jstring binary_value = env->NewStringUTF(binary_text.c_str());
    jobject out = env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.token_index),
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.byte_offset),
        static_cast<jint>(entry.byte_count),
        hex_value,
        binary_value);
    DeleteLocalRefIfNotNull(env, hex_value);
    DeleteLocalRefIfNotNull(env, binary_value);
    DeleteLocalRefIfNotNull(env, entry_class);
    return out;
}

jobject NewTextFollowRawDisplayUnitViewData(
    JNIEnv* env,
    const bag_text_follow_raw_display_unit_entry& entry,
    const std::vector<std::string>& hex_tokens,
    const std::string& compact_bits) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowRawDisplayUnitViewData");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(
            entry_class,
            "<init>",
            "(IIIIIIIIIZZLjava/lang/String;Ljava/lang/String;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    std::string hex_text;
    for (std::size_t index = 0; index < entry.byte_count; ++index) {
        const std::size_t byte_index = entry.byte_offset + index;
        if (byte_index >= hex_tokens.size()) {
            break;
        }
        if (!hex_text.empty()) {
            hex_text.push_back(' ');
        }
        hex_text.append(hex_tokens[byte_index]);
    }

    std::string binary_text;
    const std::size_t bit_offset = entry.byte_offset * static_cast<std::size_t>(8);
    const std::size_t bit_count = entry.byte_count * static_cast<std::size_t>(8);
    if (bit_offset < compact_bits.size()) {
        const std::size_t clamped_bit_count =
            std::min(bit_count, compact_bits.size() - bit_offset);
        for (std::size_t index = 0; index < clamped_bit_count; ++index) {
            if (index > 0 && index % static_cast<std::size_t>(8) == 0) {
                binary_text.push_back(' ');
            }
            binary_text.push_back(compact_bits[bit_offset + index]);
        }
    }

    jstring hex_value = env->NewStringUTF(hex_text.c_str());
    jstring binary_value = env->NewStringUTF(binary_text.c_str());
    jobject out = env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.token_index),
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.byte_index_within_token),
        static_cast<jint>(entry.byte_offset),
        static_cast<jint>(entry.byte_count),
        static_cast<jint>(entry.character_index_within_token),
        static_cast<jint>(entry.byte_index_within_character),
        static_cast<jint>(entry.character_byte_count),
        entry.is_character_start != 0 ? JNI_TRUE : JNI_FALSE,
        entry.is_character_end != 0 ? JNI_TRUE : JNI_FALSE,
        hex_value,
        binary_value);
    DeleteLocalRefIfNotNull(env, hex_value);
    DeleteLocalRefIfNotNull(env, binary_value);
    DeleteLocalRefIfNotNull(env, entry_class);
    return out;
}

jobject NewTextFollowLyricLineTimelineEntry(JNIEnv* env,
                                            const bag_text_follow_lyric_line_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowLyricLineTimelineEntry");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(III)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jobject out = env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.line_index));
    DeleteLocalRefIfNotNull(env, entry_class);
    return out;
}

jobject NewTextFollowLineTokenRangeViewData(
    JNIEnv* env,
    const bag_text_follow_line_token_range_entry& entry) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowLineTokenRangeViewData");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entry_class, "<init>", "(III)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jobject out = env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.line_index),
        static_cast<jint>(entry.token_begin_index),
        static_cast<jint>(entry.token_count));
    DeleteLocalRefIfNotNull(env, entry_class);
    return out;
}

jobject NewTextFollowLineRawSegmentViewData(JNIEnv* env,
                                            const bag_text_follow_line_raw_segment_entry& entry,
                                            const std::vector<std::string>& hex_tokens,
                                            const std::string& compact_bits) {
    jclass entry_class =
        FindClassOrNull(env, "com/bag/audioandroid/domain/TextFollowLineRawSegmentViewData");
    if (entry_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(entry_class, "<init>", "(IIIIILjava/lang/String;Ljava/lang/String;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    std::string hex_text;
    for (std::size_t index = 0; index < entry.byte_count; ++index) {
        const std::size_t byte_index = entry.byte_offset + index;
        if (byte_index >= hex_tokens.size()) {
            break;
        }
        if (!hex_text.empty()) {
            hex_text.push_back(' ');
        }
        hex_text.append(hex_tokens[byte_index]);
    }

    std::string binary_text;
    const std::size_t bit_offset = entry.byte_offset * static_cast<std::size_t>(8);
    const std::size_t bit_count = entry.byte_count * static_cast<std::size_t>(8);
    if (bit_offset < compact_bits.size()) {
        const std::size_t clamped_bit_count =
            std::min(bit_count, compact_bits.size() - bit_offset);
        for (std::size_t index = 0; index < clamped_bit_count; ++index) {
            if (index > 0 && index % static_cast<std::size_t>(8) == 0) {
                binary_text.push_back(' ');
            }
            binary_text.push_back(compact_bits[bit_offset + index]);
        }
    }

    jstring hex_value = env->NewStringUTF(hex_text.c_str());
    jstring binary_value = env->NewStringUTF(binary_text.c_str());
    jobject out = env->NewObject(
        entry_class,
        ctor,
        static_cast<jint>(entry.line_index),
        static_cast<jint>(entry.start_sample),
        static_cast<jint>(entry.sample_count),
        static_cast<jint>(entry.byte_offset),
        static_cast<jint>(entry.byte_count),
        hex_value,
        binary_value);
    DeleteLocalRefIfNotNull(env, hex_value);
    DeleteLocalRefIfNotNull(env, binary_value);
    DeleteLocalRefIfNotNull(env, entry_class);
    return out;
}

}  // namespace jni_bridge

