#include "jni_bridge_internal.h"

#include <algorithm>

namespace jni_bridge {

template <typename T>
void DeleteLocalRefIfNotNull(JNIEnv* env, T ref) {
    if (env != nullptr && ref != nullptr) {
        env->DeleteLocalRef(ref);
    }
}

std::string JStringToStdString(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        return {};
    }
    const std::string out(chars);
    env->ReleaseStringUTFChars(value, chars);
    return out;
}

int NormalizeSampleRate(int sample_rate_hz) {
    return sample_rate_hz > 0 ? sample_rate_hz : kDefaultSampleRateHz;
}

int NormalizeFrameSamples(int sample_rate_hz, int frame_samples) {
    if (frame_samples > 0) {
        return frame_samples;
    }
    const int normalized_sample_rate = NormalizeSampleRate(sample_rate_hz);
    return normalized_sample_rate > 0 ? normalized_sample_rate / 20 : kDefaultFrameSamples;
}

bag_encoder_config MakeEncoderConfig(int sample_rate_hz,
                                     int frame_samples,
                                     int flash_signal_profile,
                                     int flash_voicing_flavor) {
    bag_encoder_config config{};
    config.sample_rate_hz = NormalizeSampleRate(sample_rate_hz);
    config.frame_samples = NormalizeFrameSamples(sample_rate_hz, frame_samples);
    config.enable_diagnostics = 0;
    config.mode = BAG_TRANSPORT_FLASH;
    config.flash_signal_profile = static_cast<bag_flash_signal_profile>(flash_signal_profile);
    config.flash_voicing_flavor = static_cast<bag_flash_voicing_flavor>(flash_voicing_flavor);
    config.reserved = 0;
    return config;
}

bag_decoder_config MakeDecoderConfig(int sample_rate_hz,
                                     int frame_samples,
                                     int flash_signal_profile,
                                     int flash_voicing_flavor) {
    bag_decoder_config config{};
    config.sample_rate_hz = NormalizeSampleRate(sample_rate_hz);
    config.frame_samples = NormalizeFrameSamples(sample_rate_hz, frame_samples);
    config.enable_diagnostics = 0;
    config.mode = BAG_TRANSPORT_FLASH;
    config.flash_signal_profile = static_cast<bag_flash_signal_profile>(flash_signal_profile);
    config.flash_voicing_flavor = static_cast<bag_flash_voicing_flavor>(flash_voicing_flavor);
    config.reserved = 0;
    return config;
}

bag_encode_operation* HandleToEncodeOperation(jlong handle) {
    return reinterpret_cast<bag_encode_operation*>(static_cast<intptr_t>(handle));
}

jdoubleArray NewEncodeOperationSnapshotArray(
    JNIEnv* env,
    const bag_encode_operation_progress& progress) {
    jdoubleArray out = env->NewDoubleArray(13);
    if (out == nullptr) {
        return nullptr;
    }

    const jdouble values[13] = {
        static_cast<jdouble>(progress.state),
        static_cast<jdouble>(progress.phase),
        static_cast<jdouble>(progress.overall_progress_0_to_1),
        static_cast<jdouble>(progress.phase_progress_0_to_1),
        static_cast<jdouble>(progress.completed_work_units),
        static_cast<jdouble>(progress.total_work_units),
        static_cast<jdouble>(progress.phase_completed_work_units),
        static_cast<jdouble>(progress.phase_total_work_units),
        static_cast<jdouble>(progress.terminal_code),
        static_cast<jdouble>(progress.estimated_pcm_sample_count),
        static_cast<jdouble>(progress.payload_byte_count),
        static_cast<jdouble>(progress.segment_count),
        static_cast<jdouble>(progress.current_segment_index),
    };
    env->SetDoubleArrayRegion(out, 0, 13, values);
    return out;
}

jdoubleArray NewEncodeOperationWorkPlanArray(
    JNIEnv* env,
    const bag_encode_operation_work_plan& work_plan) {
    jdoubleArray out = env->NewDoubleArray(8);
    if (out == nullptr) {
        return nullptr;
    }

    const jdouble values[8] = {
        static_cast<jdouble>(work_plan.preparing_input_work_units),
        static_cast<jdouble>(work_plan.rendering_pcm_work_units),
        static_cast<jdouble>(work_plan.postprocessing_work_units),
        static_cast<jdouble>(work_plan.finalizing_work_units),
        static_cast<jdouble>(work_plan.total_work_units),
        static_cast<jdouble>(work_plan.estimated_pcm_sample_count),
        static_cast<jdouble>(work_plan.payload_byte_count),
        static_cast<jdouble>(work_plan.segment_count),
    };
    env->SetDoubleArrayRegion(out, 0, 8, values);
    return out;
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

jobject NewDecodedAudioPayloadResult(JNIEnv* env,
                                     jobject decoded_payload,
                                     jobject follow_data) {
    jclass result_class = FindClassOrNull(env, "com/bag/audioandroid/domain/DecodedAudioPayloadResult");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(
            result_class,
            "<init>",
            "(Lcom/bag/audioandroid/domain/DecodedPayloadViewData;Lcom/bag/audioandroid/domain/PayloadFollowViewData;)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jobject out = env->NewObject(result_class, ctor, decoded_payload, follow_data);
    DeleteLocalRefIfNotNull(env, result_class);
    return out;
}

jobject NewEncodedAudioPayloadResult(JNIEnv* env,
                                     jshortArray pcm,
                                     const std::string& raw_bytes_hex,
                                     const std::string& raw_bits_binary,
                                     jobject follow_data,
                                     jint terminal_code) {
    jclass result_class = FindClassOrNull(env, "com/bag/audioandroid/domain/EncodedAudioPayloadResult");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(
            result_class,
            "<init>",
            "([SLjava/lang/String;Ljava/lang/String;Lcom/bag/audioandroid/domain/PayloadFollowViewData;I)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jstring raw_bytes_hex_value = env->NewStringUTF(raw_bytes_hex.c_str());
    jstring raw_bits_binary_value = env->NewStringUTF(raw_bits_binary.c_str());
    jobject out = env->NewObject(
        result_class,
        ctor,
        pcm,
        raw_bytes_hex_value,
        raw_bits_binary_value,
        follow_data,
        terminal_code);
    if (raw_bytes_hex_value != nullptr) {
        env->DeleteLocalRef(raw_bytes_hex_value);
    }
    if (raw_bits_binary_value != nullptr) {
        env->DeleteLocalRef(raw_bits_binary_value);
    }
    DeleteLocalRefIfNotNull(env, result_class);
    return out;
}

jobject NewFlashSignalInfo(JNIEnv* env,
                           const std::string& low_carrier_hz,
                           const std::string& high_carrier_hz,
                           const std::string& bit_duration_samples,
                           const std::string& payload_silence,
                           const std::string& decode_path,
                           jboolean available) {
    jclass result_class = FindClassOrNull(env, "com/bag/audioandroid/domain/FlashSignalInfo");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(
            result_class,
            "<init>",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jstring low_value = env->NewStringUTF(low_carrier_hz.c_str());
    jstring high_value = env->NewStringUTF(high_carrier_hz.c_str());
    jstring bit_value = env->NewStringUTF(bit_duration_samples.c_str());
    jstring silence_value = env->NewStringUTF(payload_silence.c_str());
    jstring decode_value = env->NewStringUTF(decode_path.c_str());
    jobject out = env->NewObject(
        result_class,
        ctor,
        low_value,
        high_value,
        bit_value,
        silence_value,
        decode_value,
        available);
    DeleteLocalRefIfNotNull(env, low_value);
    DeleteLocalRefIfNotNull(env, high_value);
    DeleteLocalRefIfNotNull(env, bit_value);
    DeleteLocalRefIfNotNull(env, silence_value);
    DeleteLocalRefIfNotNull(env, decode_value);
    DeleteLocalRefIfNotNull(env, result_class);
    return out;
}

jshortArray NewShortArrayFromPcmResult(JNIEnv* env, const bag_pcm16_result& result) {
    if (result.sample_count > 0 && result.samples == nullptr) {
        return env->NewShortArray(0);
    }
    jshortArray out = env->NewShortArray(static_cast<jsize>(result.sample_count));
    if (out != nullptr && result.sample_count > 0) {
        env->SetShortArrayRegion(
            out, 0, static_cast<jsize>(result.sample_count),
            reinterpret_cast<const jshort*>(result.samples));
    }
    return out;
}

jobject NewEmptyDecodedAudioPayloadResult(JNIEnv* env,
                                          jint text_decode_status_code,
                                          jboolean raw_payload_available) {
    jobject decoded_payload = NewDecodedPayloadViewData(
        env, "", "", "", text_decode_status_code, raw_payload_available);
    jobject follow_data = NewEmptyPayloadFollowViewData(env);
    jobject out = NewDecodedAudioPayloadResult(env, decoded_payload, follow_data);
    DeleteLocalRefIfNotNull(env, decoded_payload);
    DeleteLocalRefIfNotNull(env, follow_data);
    return out;
}

jobject NewEmptyEncodedPayloadResult(JNIEnv* env, jint terminal_code) {
    jshortArray empty_pcm = env->NewShortArray(0);
    jobject follow_data = NewEmptyPayloadFollowViewData(env);
    jobject out = NewEncodedAudioPayloadResult(env, empty_pcm, "", "", follow_data, terminal_code);
    DeleteLocalRefIfNotNull(env, follow_data);
    return out;
}

std::string CopyApiString(const char* buffer, std::size_t size) {
    if (buffer == nullptr || size == 0) {
        return {};
    }
    return std::string(buffer, size);
}

std::string CopyApiString(const char* buffer, std::size_t size, std::size_t buffer_size) {
    if (buffer == nullptr || size == 0 || buffer_size == 0) {
        return {};
    }
    return std::string(buffer, std::min(size, buffer_size));
}

jobject NewEncodeOperationPcmPayloadResultFromEncodeResult(
    JNIEnv* env,
    const bag_encode_result& result) {
    bag_pcm16_result pcm_result{};
    pcm_result.samples = result.samples;
    pcm_result.sample_count = result.sample_count;
    jshortArray pcm = NewShortArrayFromPcmResult(env, pcm_result);
    jobject follow_data = NewEmptyPayloadFollowViewData(env);
    jobject out = NewEncodedAudioPayloadResult(env, pcm, "", "", follow_data, kBagErrorOk);
    DeleteLocalRefIfNotNull(env, follow_data);
    return out;
}

jobject NewEmptyEncodeOperationPcmPayloadResult(JNIEnv* env, jint terminal_code) {
    return NewEmptyEncodedPayloadResult(env, terminal_code);
}

bool IsPcmSampleCountWithinJvmLimit(std::size_t sample_count) {
    return sample_count <= kMaxJvmEncodePcmSamples;
}

}  // namespace jni_bridge
