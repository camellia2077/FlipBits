#include <jni.h>

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

jstring ToJString(JNIEnv* env, const audio_io_owned_string& value) {
    if (value.data == nullptr || value.size == 0) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(value.data);
}

jobject ToGeneratedAudioMetadata(JNIEnv* env, const audio_io_metadata& metadata) {
    jclass metadata_class = env->FindClass("com/bag/audioandroid/domain/GeneratedAudioMetadata");
    jclass transport_mode_class = env->FindClass("com/bag/audioandroid/ui/model/TransportModeOption");
    jclass flash_style_class = env->FindClass("com/bag/audioandroid/ui/model/FlashVoicingStyleOption");
    if (metadata_class == nullptr || transport_mode_class == nullptr || flash_style_class == nullptr) {
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
    if (transport_values == nullptr || flash_values == nullptr) {
        return nullptr;
    }

    jobjectArray transport_entries = static_cast<jobjectArray>(
        env->CallStaticObjectMethod(transport_mode_class, transport_values));
    jobjectArray flash_entries = static_cast<jobjectArray>(
        env->CallStaticObjectMethod(flash_style_class, flash_values));
    if (transport_entries == nullptr || flash_entries == nullptr) {
        return nullptr;
    }

    jobject mode_object = nullptr;
    switch (metadata.mode) {
        case AUDIO_IO_METADATA_MODE_FLASH:
            mode_object = env->GetObjectArrayElement(transport_entries, 0);
            break;
        case AUDIO_IO_METADATA_MODE_PRO:
            mode_object = env->GetObjectArrayElement(transport_entries, 1);
            break;
        case AUDIO_IO_METADATA_MODE_ULTRA:
            mode_object = env->GetObjectArrayElement(transport_entries, 2);
            break;
        default:
            return nullptr;
    }

    jobject flash_style_object = nullptr;
    if (metadata.has_flash_voicing_style != 0u) {
        switch (metadata.flash_voicing_style) {
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_CODED_BURST:
                flash_style_object = env->GetObjectArrayElement(flash_entries, 0);
                break;
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_RITUAL_CHANT:
                flash_style_object = env->GetObjectArrayElement(flash_entries, 1);
                break;
            case AUDIO_IO_METADATA_FLASH_VOICING_STYLE_DEEP_RITUAL:
                flash_style_object = env->GetObjectArrayElement(flash_entries, 2);
                break;
            default:
                return nullptr;
        }
    }

    jmethodID ctor = env->GetMethodID(
        metadata_class,
        "<init>",
        "(ILcom/bag/audioandroid/ui/model/TransportModeOption;Lcom/bag/audioandroid/ui/model/FlashVoicingStyleOption;Ljava/lang/String;JIILjava/lang/String;Ljava/lang/String;)V");
    if (ctor == nullptr) {
        return nullptr;
    }

    jstring created_at_string = ToJString(env, metadata.created_at_iso_utc);
    jstring app_version_string = ToJString(env, metadata.app_version);
    jstring core_version_string = ToJString(env, metadata.core_version);

    return env->NewObject(
        metadata_class,
        ctor,
        static_cast<jint>(metadata.version),
        mode_object,
        flash_style_object,
        created_at_string,
        static_cast<jlong>(metadata.duration_ms),
        static_cast<jint>(metadata.frame_samples),
        static_cast<jint>(metadata.pcm_sample_count),
        app_version_string,
        core_version_string);
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
    if (style_id_text == "coded_burst") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_CODED_BURST;
    }
    if (style_id_text == "ritual_chant") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_RITUAL_CHANT;
    }
    if (style_id_text == "deep_ritual") {
        return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_DEEP_RITUAL;
    }
    return AUDIO_IO_METADATA_FLASH_VOICING_STYLE_UNKNOWN;
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
        jfieldID frame_samples_field = env->GetFieldID(
            metadata_class,
            "frameSamples",
            "I");
        jfieldID pcm_sample_count_field = env->GetFieldID(
            metadata_class,
            "pcmSampleCount",
            "I");
        jfieldID app_version_field = env->GetFieldID(
            metadata_class,
            "appVersion",
            "Ljava/lang/String;");
        jfieldID core_version_field = env->GetFieldID(
            metadata_class,
            "coreVersion",
            "Ljava/lang/String;");
        if (version_field == nullptr ||
            mode_field == nullptr ||
            flash_style_field == nullptr ||
            created_at_field == nullptr ||
            duration_field == nullptr ||
            frame_samples_field == nullptr ||
            pcm_sample_count_field == nullptr ||
            app_version_field == nullptr ||
            core_version_field == nullptr) {
            return env->NewByteArray(0);
        }

        jobject mode_object = env->GetObjectField(metadata_object, mode_field);
        jobject flash_style_object = env->GetObjectField(metadata_object, flash_style_field);
        created_at_iso_utc = JStringToStdString(
            env,
            static_cast<jstring>(env->GetObjectField(metadata_object, created_at_field)));
        app_version = JStringToStdString(
            env,
            static_cast<jstring>(env->GetObjectField(metadata_object, app_version_field)));
        core_version = JStringToStdString(
            env,
            static_cast<jstring>(env->GetObjectField(metadata_object, core_version_field)));

        metadata.version = static_cast<std::uint8_t>(env->GetIntField(metadata_object, version_field));
        metadata.mode = MapTransportMode(env, mode_object);
        metadata.has_flash_voicing_style = flash_style_object != nullptr ? 1u : 0u;
        metadata.flash_voicing_style = MapFlashVoicingStyle(env, flash_style_object);
        metadata.created_at_iso_utc = ToStringView(created_at_iso_utc);
        metadata.duration_ms =
            static_cast<std::uint32_t>(std::max<jlong>(0, env->GetLongField(metadata_object, duration_field)));
        metadata.frame_samples =
            static_cast<std::uint32_t>(std::max<jint>(0, env->GetIntField(metadata_object, frame_samples_field)));
        metadata.pcm_sample_count =
            static_cast<std::uint32_t>(std::max<jint>(0, env->GetIntField(metadata_object, pcm_sample_count_field)));
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
        metadata_object = ToGeneratedAudioMetadata(env, decoded.metadata);
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
