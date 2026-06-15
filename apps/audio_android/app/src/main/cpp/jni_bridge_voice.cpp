#include "jni_bridge_internal.h"

#include <vector>

namespace jni_bridge {

namespace {

jshortArray NewTrackArray(JNIEnv* env, const bag_pcm16_result& pcm) {
    jshortArray out = NewShortArrayFromPcmResult(env, pcm);
    if (out != nullptr) {
        return out;
    }
    return env->NewShortArray(0);
}

jobject NewVoiceFxNativeResult(JNIEnv* env,
                               jshortArray final_mix,
                               jshortArray main_voice,
                               jshortArray subvoice,
                               jshortArray signal_overlay,
                               jint error_code) {
    jclass result_class = env->FindClass("com/bag/audioandroid/domain/VoiceFxNativeResult");
    if (result_class == nullptr) {
        return nullptr;
    }
    jmethodID ctor =
        env->GetMethodID(result_class, "<init>", "([S[S[S[SI)V");
    if (ctor == nullptr) {
        env->DeleteLocalRef(result_class);
        return nullptr;
    }
    jobject out =
        env->NewObject(result_class, ctor, final_mix, main_voice, subvoice,
                       signal_overlay, error_code);
    env->DeleteLocalRef(result_class);
    return out;
}

}  // namespace

namespace {

bag_voice_fx_config BuildVoiceFxConfig(const jint sample_rate_hz,
                                       const jint preset,
                                       const jint enable_diagnostics,
                                       const jint subvoice_style) {
    bag_voice_fx_config config{};
    config.sample_rate_hz = sample_rate_hz;
    config.enable_diagnostics = enable_diagnostics;
    config.preset = static_cast<bag_voice_fx_preset>(preset);
    config.subvoice_style =
        static_cast<bag_voice_fx_subvoice_style>(subvoice_style);
    config.reserved = 0;
    return config;
}

jobject NewVoiceFxNativeResultFromApi(JNIEnv* env,
                                      const bag_voice_fx_result& result,
                                      const jint error_code) {
    jshortArray out_final_mix = NewTrackArray(env, result.final_mix);
    jshortArray out_main_voice = NewTrackArray(env, result.main_voice);
    jshortArray out_subvoice = NewTrackArray(env, result.subvoice);
    jshortArray out_signal_overlay = NewTrackArray(env, result.signal_overlay);
    return NewVoiceFxNativeResult(
        env,
        out_final_mix,
        out_main_voice,
        out_subvoice,
        out_signal_overlay,
        error_code);
}

}  // namespace

jobject NativeApplyVoiceFx(JNIEnv* env,
                           jshortArray pcm,
                           jint sample_rate_hz,
                           jint preset,
                           jint enable_diagnostics,
                           jint subvoice_style) {
    if (env == nullptr) {
        return nullptr;
    }

    const jsize pcm_size = pcm == nullptr ? 0 : env->GetArrayLength(pcm);
    std::vector<std::int16_t> samples(static_cast<std::size_t>(pcm_size));
    if (pcm != nullptr && pcm_size > 0) {
        env->GetShortArrayRegion(pcm, 0, pcm_size,
                                 reinterpret_cast<jshort*>(samples.data()));
    }

    const bag_voice_fx_config config =
        BuildVoiceFxConfig(
            sample_rate_hz, preset, enable_diagnostics, subvoice_style);
    bag_voice_fx_result result{};
    const bag_error_code status =
        bag_apply_voice_fx(&config, samples.data(), samples.size(), &result);
    const jobject out =
        NewVoiceFxNativeResultFromApi(env, result, static_cast<jint>(status));
    bag_free_voice_fx_result(&result);
    return out;
}

jlong NativeCreateVoiceFxProcessor(JNIEnv* env,
                                   jint sample_rate_hz,
                                   jint preset,
                                   jint enable_diagnostics,
                                   jint subvoice_style) {
    if (env == nullptr) {
        return 0;
    }
    const bag_voice_fx_config config =
        BuildVoiceFxConfig(
            sample_rate_hz, preset, enable_diagnostics, subvoice_style);
    bag_voice_fx_processor* processor = nullptr;
    const bag_error_code status =
        bag_create_voice_fx_processor(&config, &processor);
    if (status != BAG_OK || processor == nullptr) {
        return 0;
    }
    return reinterpret_cast<jlong>(processor);
}

jobject NativeProcessVoiceFxBlock(JNIEnv* env,
                                  jlong handle,
                                  jshortArray pcm) {
    if (env == nullptr) {
        return nullptr;
    }
    auto* processor = reinterpret_cast<bag_voice_fx_processor*>(handle);
    const jsize pcm_size = pcm == nullptr ? 0 : env->GetArrayLength(pcm);
    std::vector<std::int16_t> samples(static_cast<std::size_t>(pcm_size));
    if (pcm != nullptr && pcm_size > 0) {
        env->GetShortArrayRegion(
            pcm, 0, pcm_size, reinterpret_cast<jshort*>(samples.data()));
    }
    bag_voice_fx_result result{};
    const bag_error_code status =
        bag_process_voice_fx_block(processor, samples.data(), samples.size(),
                                   &result);
    const jobject out =
        NewVoiceFxNativeResultFromApi(env, result, static_cast<jint>(status));
    bag_free_voice_fx_result(&result);
    return out;
}

jobject NativeFlushVoiceFxProcessor(JNIEnv* env, jlong handle) {
    if (env == nullptr) {
        return nullptr;
    }
    auto* processor = reinterpret_cast<bag_voice_fx_processor*>(handle);
    bag_voice_fx_result result{};
    const bag_error_code status =
        bag_flush_voice_fx_processor(processor, &result);
    const jobject out =
        NewVoiceFxNativeResultFromApi(env, result, static_cast<jint>(status));
    bag_free_voice_fx_result(&result);
    return out;
}

void NativeDestroyVoiceFxProcessor(jlong handle) {
    auto* processor = reinterpret_cast<bag_voice_fx_processor*>(handle);
    bag_destroy_voice_fx_processor(processor);
}

}  // namespace jni_bridge

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeApplyVoiceFx(
    JNIEnv* env,
    jobject /*thiz*/,
    jshortArray pcm,
    jint sample_rate_hz,
    jint preset,
    jint enable_diagnostics,
    jint subvoice_style) {
    return jni_bridge::NativeApplyVoiceFx(
        env,
        pcm,
        sample_rate_hz,
        preset,
        enable_diagnostics,
        subvoice_style);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeCreateVoiceFxProcessor(
    JNIEnv* env,
    jobject /*thiz*/,
    jint sample_rate_hz,
    jint preset,
    jint enable_diagnostics,
    jint subvoice_style) {
    return jni_bridge::NativeCreateVoiceFxProcessor(
        env,
        sample_rate_hz,
        preset,
        enable_diagnostics,
        subvoice_style);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeProcessVoiceFxBlock(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jshortArray pcm) {
    return jni_bridge::NativeProcessVoiceFxBlock(env, handle, pcm);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeFlushVoiceFxProcessor(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativeFlushVoiceFxProcessor(env, handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDestroyVoiceFxProcessor(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    jni_bridge::NativeDestroyVoiceFxProcessor(handle);
}
