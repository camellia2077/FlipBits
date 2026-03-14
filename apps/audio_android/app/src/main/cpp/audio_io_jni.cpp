#include <jni.h>

#include <cstdint>
#include <vector>

#include "android_audio_io/audio_io_package.h"

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

jshortArray VectorToShortArray(JNIEnv* env, const std::vector<std::int16_t>& pcm_samples) {
    jshortArray out = env->NewShortArray(static_cast<jsize>(pcm_samples.size()));
    if (out != nullptr && !pcm_samples.empty()) {
        env->SetShortArrayRegion(
            out,
            0,
            static_cast<jsize>(pcm_samples.size()),
            reinterpret_cast<const jshort*>(pcm_samples.data()));
    }
    return out;
}

}  // namespace

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_bag_audioandroid_NativeAudioIoBridge_nativeEncodeMonoPcm16ToWavBytes(
    JNIEnv* env,
    jobject /*thiz*/,
    jint sample_rate_hz,
    jshortArray pcm
) {
    const auto pcm_samples = ShortArrayToVector(env, pcm);
    const auto wav_bytes = android_audio_io::EncodeMonoPcm16ToWavBytes(sample_rate_hz, pcm_samples);
    jbyteArray out = env->NewByteArray(static_cast<jsize>(wav_bytes.size()));
    if (out != nullptr && !wav_bytes.empty()) {
        env->SetByteArrayRegion(
            out,
            0,
            static_cast<jsize>(wav_bytes.size()),
            reinterpret_cast<const jbyte*>(wav_bytes.data()));
    }
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

    const auto decoded = android_audio_io::DecodeMonoPcm16WavBytes(wav_bytes);
    jclass result_class = env->FindClass("com/bag/audioandroid/domain/DecodedAudioData");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(result_class, "<init>", "(III[S)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    jshortArray pcm_array = VectorToShortArray(env, decoded.pcm_samples);
    return env->NewObject(
        result_class,
        ctor,
        static_cast<jint>(decoded.status),
        static_cast<jint>(decoded.sample_rate_hz),
        static_cast<jint>(decoded.channels),
        pcm_array);
}
