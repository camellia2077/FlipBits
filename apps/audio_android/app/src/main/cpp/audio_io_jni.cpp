#include <jni.h>

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>

#include "audio_io_api.h"

namespace {

std::vector<std::int16_t> ShortArrayToVector(JNIEnv* env, jshortArray pcm_array) {
    if (pcm_array == nullptr) {
        return {};
    }
    const auto length = env->GetArrayLength(pcm_array);
    std::vector<std::int16_t> pcm(static_cast<std::size_t>(length), 0);
    if (length > 0) {
        env->GetShortArrayRegion(
            pcm_array,
            0,
            length,
            reinterpret_cast<jshort*>(pcm.data()));
    }
    return pcm;
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

audio_io_string_view ToStringView(const std::string& value) {
    return audio_io_string_view{value.data(), value.size()};
}

jobject NewIntegerList(JNIEnv* env, const std::uint32_t* values, std::size_t count) {
    jclass array_list_class = env->FindClass("java/util/ArrayList");
    jclass integer_class = env->FindClass("java/lang/Integer");
    if (array_list_class == nullptr || integer_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(array_list_class, "<init>", "(I)V");
    jmethodID add = env->GetMethodID(array_list_class, "add", "(Ljava/lang/Object;)Z");
    jmethodID value_of = env->GetStaticMethodID(integer_class, "valueOf", "(I)Ljava/lang/Integer;");
    if (ctor == nullptr || add == nullptr || value_of == nullptr) {
        return nullptr;
    }

    jobject list = env->NewObject(array_list_class, ctor, static_cast<jint>(count));
    if (list == nullptr) {
        return nullptr;
    }
    for (std::size_t index = 0; index < count; ++index) {
        jobject boxed = env->CallStaticObjectMethod(
            integer_class,
            value_of,
            static_cast<jint>(values[index]));
        env->CallBooleanMethod(list, add, boxed);
    }
    return list;
}

jobject FindFlashStyleById(JNIEnv* env, jobjectArray flash_entries, const char* target_id) {
    if (flash_entries == nullptr || target_id == nullptr) {
        return nullptr;
    }
    jclass flash_style_class = env->FindClass("com/bag/audioandroid/ui/model/FlashVoicingStyleOption");
    if (flash_style_class == nullptr) {
        return nullptr;
    }
    jmethodID get_id = env->GetMethodID(
        flash_style_class,
        "getId",
        "()Ljava/lang/String;");
    if (get_id == nullptr) {
        return nullptr;
    }
    const jsize count = env->GetArrayLength(flash_entries);
    for (jsize index = 0; index < count; ++index) {
        jobject entry = env->GetObjectArrayElement(flash_entries, index);
        if (entry == nullptr) {
            continue;
        }
        jstring id = static_cast<jstring>(env->CallObjectMethod(entry, get_id));
        const std::string id_text = JStringToStdString(env, id);
        if (id_text == target_id) {
            return entry;
        }
    }
    return nullptr;
}

jobject FindTransportModeByWireName(JNIEnv* env, jobjectArray transport_entries, const char* target_wire_name) {
    if (transport_entries == nullptr || target_wire_name == nullptr) {
        return nullptr;
    }
    jclass transport_mode_class = env->FindClass("com/bag/audioandroid/ui/model/TransportModeOption");
    if (transport_mode_class == nullptr) {
        return nullptr;
    }
    jmethodID get_wire_name = env->GetMethodID(
        transport_mode_class,
        "getWireName",
        "()Ljava/lang/String;");
    if (get_wire_name == nullptr) {
        return nullptr;
    }
    const jsize count = env->GetArrayLength(transport_entries);
    for (jsize index = 0; index < count; ++index) {
        jobject entry = env->GetObjectArrayElement(transport_entries, index);
        if (entry == nullptr) {
            continue;
        }
        jstring wire_name = static_cast<jstring>(env->CallObjectMethod(entry, get_wire_name));
        const std::string wire_name_text = JStringToStdString(env, wire_name);
        if (wire_name_text == target_wire_name) {
            return entry;
        }
    }
    return nullptr;
}

bool ReadIntegerList(
    JNIEnv* env,
    jobject list_object,
    std::vector<std::uint32_t>* out_values
) {
    if (out_values == nullptr) {
        return false;
    }
    out_values->clear();
    if (list_object == nullptr) {
        return true;
    }

    jclass list_class = env->FindClass("java/util/List");
    jclass integer_class = env->FindClass("java/lang/Integer");
    if (list_class == nullptr || integer_class == nullptr) {
        return false;
    }
    jmethodID size = env->GetMethodID(list_class, "size", "()I");
    jmethodID get = env->GetMethodID(list_class, "get", "(I)Ljava/lang/Object;");
    jmethodID int_value = env->GetMethodID(integer_class, "intValue", "()I");
    if (size == nullptr || get == nullptr || int_value == nullptr) {
        return false;
    }

    const jint count = env->CallIntMethod(list_object, size);
    out_values->reserve(static_cast<std::size_t>(std::max<jint>(0, count)));
    for (jint index = 0; index < count; ++index) {
        jobject boxed = env->CallObjectMethod(list_object, get, index);
        if (boxed == nullptr) {
            return false;
        }
        const jint value = env->CallIntMethod(boxed, int_value);
        if (value <= 0) {
            return false;
        }
        out_values->push_back(static_cast<std::uint32_t>(value));
    }
    return true;
}

jstring ToJString(JNIEnv* env, const audio_io_owned_string& value) {
    if (value.data == nullptr || value.size == 0) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(value.data);
}

jobject ToGeneratedAudioMetadata(JNIEnv* env, const audio_io_metadata& metadata, jint fallback_sample_rate_hz) {
    jclass metadata_class = env->FindClass("com/bag/audioandroid/domain/GeneratedAudioMetadata");
    jclass transport_mode_class = env->FindClass("com/bag/audioandroid/ui/model/TransportModeOption");
    jclass flash_style_class = env->FindClass("com/bag/audioandroid/ui/model/FlashVoicingStyleOption");
    jclass input_source_kind_class = env->FindClass("com/bag/audioandroid/domain/GeneratedAudioInputSourceKind");
    if (metadata_class == nullptr || transport_mode_class == nullptr || flash_style_class == nullptr ||
        input_source_kind_class == nullptr) {
        return nullptr;
    }

    jmethodID transport_values = env->GetStaticMethodID(
        transport_mode_class,
        "values",
        "()[Lcom/bag/audioandroid/ui/model/TransportModeOption;");
    jmethodID flash_values = env->GetStaticMethodID(
        flash_style_class,
        "values",
        "()[Lcom/bag/audioandroid/ui/model/FlashVoicingStyleOption;");
    jmethodID input_source_values = env->GetStaticMethodID(
        input_source_kind_class,
        "values",
        "()[Lcom/bag/audioandroid/domain/GeneratedAudioInputSourceKind;");
    if (transport_values == nullptr || flash_values == nullptr || input_source_values == nullptr) {
        return nullptr;
    }

    jobjectArray transport_entries = static_cast<jobjectArray>(
        env->CallStaticObjectMethod(transport_mode_class, transport_values));
    jobjectArray flash_entries = static_cast<jobjectArray>(
        env->CallStaticObjectMethod(flash_style_class, flash_values));
    jobjectArray input_source_entries = static_cast<jobjectArray>(
        env->CallStaticObjectMethod(input_source_kind_class, input_source_values));
    if (transport_entries == nullptr || flash_entries == nullptr || input_source_entries == nullptr) {
        return nullptr;
    }

    jobject mode_object = nullptr;
    switch (metadata.mode) {
        case AUDIO_IO_METADATA_MODE_MINI:
            mode_object = FindTransportModeByWireName(env, transport_entries, "mini");
            break;
        case AUDIO_IO_METADATA_MODE_FLASH:
            mode_object = FindTransportModeByWireName(env, transport_entries, "flash");
            break;
        case AUDIO_IO_METADATA_MODE_PRO:
            mode_object = FindTransportModeByWireName(env, transport_entries, "pro");
            break;
        case AUDIO_IO_METADATA_MODE_ULTRA:
            mode_object = FindTransportModeByWireName(env, transport_entries, "ultra");
            break;
        default:
            return nullptr;
    }
    if (mode_object == nullptr) {
        return nullptr;
    }

    jobject flash_style_object = nullptr;
    if (metadata.has_flash_voicing_style != 0u) {
        switch (metadata.flash_voicing_style) {
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STEADY:
                flash_style_object = FindFlashStyleById(env, flash_entries, "steady");
                break;
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY:
                flash_style_object = FindFlashStyleById(env, flash_entries, "litany");
                break;
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILE:
                flash_style_object = FindFlashStyleById(env, flash_entries, "hostile");
                break;
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE:
                flash_style_object = FindFlashStyleById(env, flash_entries, "collapse");
                break;
            default:
                return nullptr;
        }
        if (flash_style_object == nullptr) {
            return nullptr;
        }
    }

    jobject input_source_object = nullptr;
    switch (metadata.input_source_kind) {
        case AUDIO_IO_METADATA_INPUT_SOURCE_KIND_MANUAL:
            input_source_object = env->GetObjectArrayElement(input_source_entries, 0);
            break;
        case AUDIO_IO_METADATA_INPUT_SOURCE_KIND_SAMPLE:
            input_source_object = env->GetObjectArrayElement(input_source_entries, 1);
            break;
        default:
            input_source_object = env->GetObjectArrayElement(input_source_entries, 0);
            break;
    }

    jmethodID ctor = env->GetMethodID(
        metadata_class,
        "<init>",
        "(ILcom/bag/audioandroid/ui/model/TransportModeOption;Lcom/bag/audioandroid/ui/model/FlashVoicingStyleOption;Ljava/lang/String;JIIIILcom/bag/audioandroid/domain/GeneratedAudioInputSourceKind;ILjava/lang/String;Ljava/lang/String;Ljava/util/List;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    jstring created_at_string = ToJString(env, metadata.created_at_iso_utc);
    jstring app_version_string = ToJString(env, metadata.app_version);
    jstring core_version_string = ToJString(env, metadata.core_version);
    jobject segment_sample_counts =
        NewIntegerList(env, metadata.segment_sample_counts, metadata.segment_sample_count_count);

    return env->NewObject(
        metadata_class,
        ctor,
        static_cast<jint>(metadata.version),
        mode_object,
        flash_style_object,
        created_at_string,
        static_cast<jlong>(metadata.duration_ms),
        static_cast<jint>(metadata.sample_rate_hz != 0u ? metadata.sample_rate_hz : fallback_sample_rate_hz),
        static_cast<jint>(metadata.frame_samples),
        static_cast<jint>(metadata.pcm_sample_count),
        static_cast<jint>(metadata.payload_byte_count),
        input_source_object,
        static_cast<jint>(metadata.segment_count),
        app_version_string,
        core_version_string,
        segment_sample_counts);
}

jshortArray VectorToShortArray(JNIEnv* env, const std::int16_t* pcm_samples, std::size_t sample_count) {
    jshortArray out = env->NewShortArray(static_cast<jsize>(sample_count));
    if (out != nullptr && sample_count > 0) {
        env->SetShortArrayRegion(
            out,
            0,
            static_cast<jsize>(sample_count),
            reinterpret_cast<const jshort*>(pcm_samples));
    }
    return out;
}

audio_io_metadata_mode MapTransportMode(JNIEnv* env, jobject mode_object) {
    if (mode_object == nullptr) {
        return AUDIO_IO_METADATA_MODE_UNKNOWN;
    }

    jclass transport_mode_class = env->FindClass("com/bag/audioandroid/ui/model/TransportModeOption");
    jmethodID get_wire_name = env->GetMethodID(
        transport_mode_class,
        "getWireName",
        "()Ljava/lang/String;");
    if (get_wire_name == nullptr) {
        return AUDIO_IO_METADATA_MODE_UNKNOWN;
    }

    jstring wire_name = static_cast<jstring>(env->CallObjectMethod(mode_object, get_wire_name));
    const std::string wire_name_text = JStringToStdString(env, wire_name);
    if (wire_name_text == "flash") {
        return AUDIO_IO_METADATA_MODE_FLASH;
    }
    if (wire_name_text == "pro") {
        return AUDIO_IO_METADATA_MODE_PRO;
    }
    if (wire_name_text == "ultra") {
        return AUDIO_IO_METADATA_MODE_ULTRA;
    }
    if (wire_name_text == "mini") {
        return AUDIO_IO_METADATA_MODE_MINI;
    }
    return AUDIO_IO_METADATA_MODE_UNKNOWN;
}

audio_io_metadata_flash_voicing_style MapFlashVoicingStyle(JNIEnv* env, jobject flash_style_object) {
    if (flash_style_object == nullptr) {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
    }

    jclass flash_style_class = env->FindClass("com/bag/audioandroid/ui/model/FlashVoicingStyleOption");
    jmethodID get_id = env->GetMethodID(
        flash_style_class,
        "getId",
        "()Ljava/lang/String;");
    if (get_id == nullptr) {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
    }

    jstring style_id = static_cast<jstring>(env->CallObjectMethod(flash_style_object, get_id));
    const std::string style_id_text = JStringToStdString(env, style_id);
    if (style_id_text == "steady") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STEADY;
    }
    if (style_id_text == "litany") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY;
    }
    if (style_id_text == "hostile") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILE;
    }
    if (style_id_text == "collapse") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE;
    }
    return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
}

audio_io_metadata_input_source_kind MapInputSourceKind(JNIEnv* env, jobject input_source_object) {
    if (input_source_object == nullptr) {
        return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_UNKNOWN;
    }

    jclass input_source_kind_class = env->FindClass("com/bag/audioandroid/domain/GeneratedAudioInputSourceKind");
    jmethodID get_name = env->GetMethodID(
        input_source_kind_class,
        "name",
        "()Ljava/lang/String;");
    if (get_name == nullptr) {
        return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_UNKNOWN;
    }

    jstring kind_name = static_cast<jstring>(env->CallObjectMethod(input_source_object, get_name));
    const std::string kind_name_text = JStringToStdString(env, kind_name);
    if (kind_name_text == "Manual") {
        return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_MANUAL;
    }
    if (kind_name_text == "Sample") {
        return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_SAMPLE;
    }
    return AUDIO_IO_METADATA_INPUT_SOURCE_KIND_UNKNOWN;
}

}  // namespace

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_bag_audioandroid_NativeAudioIoBridge_nativeEncodeMonoPcm16ToWavBytes(
    JNIEnv* env,
    jobject /*thiz*/,
    jint sample_rate_hz,
    jshortArray pcm,
    jobject metadata_object
) {
    const auto pcm_samples = ShortArrayToVector(env, pcm);

    audio_io_byte_buffer wav_bytes{};
    audio_io_wav_status status = AUDIO_IO_WAV_INVALID_ARGUMENT;
    std::string created_at_iso_utc;
    std::string app_version;
    std::string core_version;
    std::vector<std::uint32_t> segment_sample_counts;
    audio_io_metadata_view metadata{};

    if (metadata_object == nullptr) {
        status = audio_io_encode_mono_pcm16_wav(
            sample_rate_hz,
            pcm_samples.data(),
            pcm_samples.size(),
            &wav_bytes);
    } else {
        jclass metadata_class = env->GetObjectClass(metadata_object);
        jfieldID version_field = env->GetFieldID(metadata_class, "version", "I");
        jfieldID mode_field = env->GetFieldID(
            metadata_class,
            "mode",
            "Lcom/bag/audioandroid/ui/model/TransportModeOption;");
        jfieldID flash_style_field = env->GetFieldID(
            metadata_class,
            "flashVoicingStyle",
            "Lcom/bag/audioandroid/ui/model/FlashVoicingStyleOption;");
        jfieldID created_at_field = env->GetFieldID(
            metadata_class,
            "createdAtIsoUtc",
            "Ljava/lang/String;");
        jfieldID duration_field = env->GetFieldID(
            metadata_class,
            "durationMs",
            "J");
        jfieldID sample_rate_hz_field = env->GetFieldID(
            metadata_class,
            "sampleRateHz",
            "I");
        jfieldID frame_samples_field = env->GetFieldID(
            metadata_class,
            "frameSamples",
            "I");
        jfieldID pcm_sample_count_field = env->GetFieldID(
            metadata_class,
            "pcmSampleCount",
            "I");
        jfieldID payload_byte_count_field = env->GetFieldID(
            metadata_class,
            "payloadByteCount",
            "I");
        jfieldID input_source_kind_field = env->GetFieldID(
            metadata_class,
            "inputSourceKind",
            "Lcom/bag/audioandroid/domain/GeneratedAudioInputSourceKind;");
        jfieldID segment_count_field = env->GetFieldID(
            metadata_class,
            "segmentCount",
            "I");
        jfieldID app_version_field = env->GetFieldID(
            metadata_class,
            "appVersion",
            "Ljava/lang/String;");
        jfieldID core_version_field = env->GetFieldID(
            metadata_class,
            "coreVersion",
            "Ljava/lang/String;");
        jfieldID segment_sample_counts_field = env->GetFieldID(
            metadata_class,
            "segmentSampleCounts",
            "Ljava/util/List;");
        if (version_field == nullptr ||
            mode_field == nullptr ||
            flash_style_field == nullptr ||
            created_at_field == nullptr ||
            duration_field == nullptr ||
            sample_rate_hz_field == nullptr ||
            frame_samples_field == nullptr ||
            pcm_sample_count_field == nullptr ||
            payload_byte_count_field == nullptr ||
            input_source_kind_field == nullptr ||
            segment_count_field == nullptr ||
            app_version_field == nullptr ||
            core_version_field == nullptr ||
            segment_sample_counts_field == nullptr) {
            return env->NewByteArray(0);
        }

        jobject mode_object = env->GetObjectField(metadata_object, mode_field);
        jobject flash_style_object = env->GetObjectField(metadata_object, flash_style_field);
        jobject input_source_kind_object = env->GetObjectField(metadata_object, input_source_kind_field);
        created_at_iso_utc = JStringToStdString(
            env,
            static_cast<jstring>(env->GetObjectField(metadata_object, created_at_field)));
        app_version = JStringToStdString(
            env,
            static_cast<jstring>(env->GetObjectField(metadata_object, app_version_field)));
        core_version = JStringToStdString(
            env,
            static_cast<jstring>(env->GetObjectField(metadata_object, core_version_field)));
        if (!ReadIntegerList(
                env,
                env->GetObjectField(metadata_object, segment_sample_counts_field),
                &segment_sample_counts)) {
            return env->NewByteArray(0);
        }

        metadata.version = static_cast<std::uint8_t>(env->GetIntField(metadata_object, version_field));
        metadata.mode = MapTransportMode(env, mode_object);
        metadata.has_flash_voicing_style = flash_style_object != nullptr ? 1u : 0u;
        metadata.flash_voicing_style = MapFlashVoicingStyle(env, flash_style_object);
        metadata.created_at_iso_utc = ToStringView(created_at_iso_utc);
        metadata.duration_ms =
            static_cast<std::uint32_t>(std::max<jlong>(0, env->GetLongField(metadata_object, duration_field)));
        metadata.sample_rate_hz =
            static_cast<std::uint32_t>(std::max<jint>(0, env->GetIntField(metadata_object, sample_rate_hz_field)));
        metadata.frame_samples =
            static_cast<std::uint32_t>(std::max<jint>(0, env->GetIntField(metadata_object, frame_samples_field)));
        metadata.pcm_sample_count =
            static_cast<std::uint32_t>(std::max<jint>(0, env->GetIntField(metadata_object, pcm_sample_count_field)));
        metadata.payload_byte_count =
            static_cast<std::uint32_t>(std::max<jint>(0, env->GetIntField(metadata_object, payload_byte_count_field)));
        metadata.input_source_kind = MapInputSourceKind(env, input_source_kind_object);
        metadata.segment_count =
            static_cast<std::uint32_t>(std::max<jint>(1, env->GetIntField(metadata_object, segment_count_field)));
        metadata.segment_sample_counts = segment_sample_counts.data();
        metadata.segment_sample_count_count = segment_sample_counts.size();
        metadata.app_version = ToStringView(app_version);
        metadata.core_version = ToStringView(core_version);

        status = audio_io_encode_mono_pcm16_wav_with_metadata(
            sample_rate_hz,
            pcm_samples.data(),
            pcm_samples.size(),
            &metadata,
            &wav_bytes);
    }

    if (status != AUDIO_IO_WAV_OK) {
        audio_io_free_byte_buffer(&wav_bytes);
        return env->NewByteArray(0);
    }

    jbyteArray out = env->NewByteArray(static_cast<jsize>(wav_bytes.size));
    if (out != nullptr && wav_bytes.size > 0) {
        env->SetByteArrayRegion(
            out,
            0,
            static_cast<jsize>(wav_bytes.size),
            reinterpret_cast<const jbyte*>(wav_bytes.data));
    }
    audio_io_free_byte_buffer(&wav_bytes);
    return out;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeAudioIoBridge_nativeDecodeMonoPcm16WavBytes(
    JNIEnv* env,
    jobject /*thiz*/,
    jbyteArray wav_bytes_array
) {
    std::vector<std::uint8_t> wav_bytes;
    if (wav_bytes_array != nullptr) {
        const auto length = env->GetArrayLength(wav_bytes_array);
        wav_bytes.resize(static_cast<std::size_t>(length), 0);
        if (length > 0) {
            env->GetByteArrayRegion(
                wav_bytes_array,
                0,
                length,
                reinterpret_cast<jbyte*>(wav_bytes.data()));
        }
    }

    audio_io_decoded_wav decoded{};
    const auto wav_status = audio_io_decode_mono_pcm16_wav(
        wav_bytes.data(),
        wav_bytes.size(),
        &decoded);

    jclass result_class = env->FindClass("com/bag/audioandroid/domain/DecodedAudioData");
    if (result_class == nullptr) {
        audio_io_free_decoded_wav(&decoded);
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(
        result_class,
        "<init>",
        "(IIII[SLcom/bag/audioandroid/domain/GeneratedAudioMetadata;)V");
    if (ctor == nullptr) {
        audio_io_free_decoded_wav(&decoded);
        return nullptr;
    }

    jshortArray pcm_array = VectorToShortArray(env, decoded.samples, decoded.sample_count);
    jobject metadata_object = nullptr;
    if (decoded.metadata_status == AUDIO_IO_METADATA_OK) {
        metadata_object = ToGeneratedAudioMetadata(env, decoded.metadata, static_cast<jint>(decoded.sample_rate_hz));
    }
    jobject result = env->NewObject(
        result_class,
        ctor,
        static_cast<jint>(wav_status),
        static_cast<jint>(decoded.metadata_status),
        static_cast<jint>(decoded.sample_rate_hz),
        static_cast<jint>(decoded.channels),
        pcm_array,
        metadata_object);
    audio_io_free_decoded_wav(&decoded);
    return result;
}
