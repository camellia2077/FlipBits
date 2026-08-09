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

jint SizeToJIntOrMinusOne(std::size_t value) {
    if (value > static_cast<std::size_t>(std::numeric_limits<jint>::max())) {
        return -1;
    }
    return static_cast<jint>(value);
}

}  // namespace

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
