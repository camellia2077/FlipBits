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

}  // namespace jni_bridge

