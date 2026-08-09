#include "jni_bridge_internal.h"

#include <algorithm>

namespace jni_bridge {

template <typename T>
void DeleteLocalRefIfNotNull(JNIEnv* env, T ref) {
    if (env != nullptr && ref != nullptr) {
        env->DeleteLocalRef(ref);
    }
}

jclass FindClassOrNull(JNIEnv* env, const char* name) {
    return env->FindClass(name);
}

jobject NewArrayList(JNIEnv* env, jint initial_capacity) {
    jclass list_class = FindClassOrNull(env, "java/util/ArrayList");
    if (list_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(list_class, "<init>", "(I)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jobject list = env->NewObject(list_class, ctor, initial_capacity);
    DeleteLocalRefIfNotNull(env, list_class);
    return list;
}

bool AddToList(JNIEnv* env, jobject list, jobject item) {
    if (list == nullptr || item == nullptr) {
        return false;
    }
    jclass list_class = FindClassOrNull(env, "java/util/ArrayList");
    if (list_class == nullptr) {
        return false;
    }
    jmethodID add = env->GetMethodID(list_class, "add", "(Ljava/lang/Object;)Z");
    if (add == nullptr) {
        return false;
    }
    const bool ok = env->CallBooleanMethod(list, add, item) == JNI_TRUE;
    DeleteLocalRefIfNotNull(env, list_class);
    return ok;
}

struct ArrayListMethods {
    jclass list_class = nullptr;
    jmethodID ctor = nullptr;
    jmethodID add = nullptr;
};

ArrayListMethods ResolveArrayListMethods(JNIEnv* env) {
    ArrayListMethods methods{};
    methods.list_class = FindClassOrNull(env, "java/util/ArrayList");
    if (methods.list_class == nullptr) {
        return methods;
    }
    methods.ctor = env->GetMethodID(methods.list_class, "<init>", "(I)V");
    methods.add = env->GetMethodID(methods.list_class, "add", "(Ljava/lang/Object;)Z");
    if (methods.ctor == nullptr || methods.add == nullptr) {
        DeleteLocalRefIfNotNull(env, methods.list_class);
        return {};
    }
    return methods;
}

jobject NewArrayList(JNIEnv* env, jint initial_capacity, const ArrayListMethods& methods) {
    if (methods.list_class == nullptr || methods.ctor == nullptr) {
        return nullptr;
    }
    jobject list = env->NewObject(methods.list_class, methods.ctor, initial_capacity);
    DeleteLocalRefIfNotNull(env, methods.list_class);
    return list;
}

bool AddToList(JNIEnv* env, jobject list, const ArrayListMethods& methods, jobject item) {
    if (list == nullptr || item == nullptr || methods.add == nullptr) {
        return false;
    }
    return env->CallBooleanMethod(list, methods.add, item) == JNI_TRUE;
}

jobject NewStringList(JNIEnv* env, const std::vector<std::string>& values) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(values.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& value : values) {
        jstring item = env->NewStringUTF(value.c_str());
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewByteTimelineList(JNIEnv* env,
                            const std::vector<bag_payload_follow_byte_entry>& entries) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewPayloadFollowByteEntry(env, entry);
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewBinaryTimelineList(
    JNIEnv* env,
    const std::vector<bag_payload_follow_binary_group_entry>& entries) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewPayloadFollowBinaryGroupEntry(env, entry);
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewUltraFrameTimelineList(
    JNIEnv* env,
    const std::vector<bag_ultra_frame_symbol_entry>& entries) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewUltraFrameSymbolEntry(env, entry);
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewTextTimelineList(JNIEnv* env,
                            const std::vector<bag_text_follow_token_entry>& entries) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowTimelineEntry(env, entry);
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewTextCharacterList(JNIEnv* env,
                             const std::vector<bag_text_follow_character_entry>& entries,
                             const std::string& text_character_text) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item =
            NewTextFollowCharacterViewData(env, entry, text_character_text);
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewTextRawSegmentList(JNIEnv* env,
                              const std::vector<bag_text_follow_raw_segment_entry>& entries,
                              const std::vector<std::string>& hex_tokens,
                              const std::string& compact_bits) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowRawSegmentViewData(env, entry, hex_tokens, compact_bits);
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewTextRawDisplayUnitList(
    JNIEnv* env,
    const std::vector<bag_text_follow_raw_display_unit_entry>& entries,
    const std::vector<std::string>& hex_tokens,
    const std::string& compact_bits) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowRawDisplayUnitViewData(env, entry, hex_tokens, compact_bits);
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewLyricLineTimelineList(JNIEnv* env,
                                 const std::vector<bag_text_follow_lyric_line_entry>& entries) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowLyricLineTimelineEntry(env, entry);
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewLineTokenRangeList(
    JNIEnv* env,
    const std::vector<bag_text_follow_line_token_range_entry>& entries) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowLineTokenRangeViewData(env, entry);
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

jobject NewLineRawSegmentList(JNIEnv* env,
                              const std::vector<bag_text_follow_line_raw_segment_entry>& entries,
                              const std::vector<std::string>& hex_tokens,
                              const std::string& compact_bits) {
    const ArrayListMethods methods = ResolveArrayListMethods(env);
    if (methods.list_class == nullptr || methods.ctor == nullptr || methods.add == nullptr) {
        return nullptr;
    }
    jobject list = NewArrayList(env, static_cast<jint>(entries.size()), methods);
    if (list == nullptr) {
        return nullptr;
    }
    for (const auto& entry : entries) {
        jobject item = NewTextFollowLineRawSegmentViewData(env, entry, hex_tokens, compact_bits);
        if (item == nullptr || !AddToList(env, list, methods, item)) {
            return nullptr;
        }
        env->DeleteLocalRef(item);
    }
    return list;
}

}  // namespace jni_bridge
