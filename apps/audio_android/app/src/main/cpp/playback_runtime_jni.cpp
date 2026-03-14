#include <jni.h>

#include "audio_runtime.h"

namespace {
constexpr jsize kPlaybackStateFieldCount = 7;

audio_runtime_playback_session_state StateFromArray(JNIEnv* env, jintArray state_array) {
    jint values[kPlaybackStateFieldCount] = {0};
    if (state_array != nullptr) {
        const auto length = env->GetArrayLength(state_array);
        const auto copy_length = length < kPlaybackStateFieldCount ? length : kPlaybackStateFieldCount;
        if (copy_length > 0) {
            env->GetIntArrayRegion(state_array, 0, copy_length, values);
        }
    }
    return {
        static_cast<audio_runtime_playback_phase>(values[0]),
        values[1],
        values[2],
        values[3],
        values[4],
        values[5],
        values[6]
    };
}

jintArray StateToArray(JNIEnv* env, const audio_runtime_playback_session_state& state) {
    jint values[kPlaybackStateFieldCount] = {
        static_cast<jint>(state.phase),
        static_cast<jint>(state.played_samples),
        static_cast<jint>(state.total_samples),
        static_cast<jint>(state.sample_rate_hz),
        static_cast<jint>(state.is_scrubbing),
        static_cast<jint>(state.scrub_target_samples),
        static_cast<jint>(state.resume_after_scrub)
    };
    jintArray out = env->NewIntArray(kPlaybackStateFieldCount);
    if (out != nullptr) {
        env->SetIntArrayRegion(out, 0, kPlaybackStateFieldCount, values);
    }
    return out;
}
}  // namespace

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeClearedState(
    JNIEnv* env,
    jobject /*thiz*/
) {
    return StateToArray(env, audio_runtime_cleared());
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeLoadState(
    JNIEnv* env,
    jobject /*thiz*/,
    jint total_samples,
    jint sample_rate_hz
) {
    return StateToArray(env, audio_runtime_load(total_samples, sample_rate_hz));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativePlayStarted(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    return StateToArray(env, audio_runtime_play_started(StateFromArray(env, state)));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativePaused(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    return StateToArray(env, audio_runtime_paused(StateFromArray(env, state)));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeResumed(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    return StateToArray(env, audio_runtime_resumed(StateFromArray(env, state)));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeProgress(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state,
    jint played_samples
) {
    return StateToArray(env, audio_runtime_progress(StateFromArray(env, state), played_samples));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeScrubStarted(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    return StateToArray(env, audio_runtime_scrub_started(StateFromArray(env, state)));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeScrubChanged(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state,
    jint target_samples
) {
    return StateToArray(env, audio_runtime_scrub_changed(StateFromArray(env, state), target_samples));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeScrubCommitted(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    return StateToArray(env, audio_runtime_scrub_committed(StateFromArray(env, state)));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeScrubCanceled(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    return StateToArray(env, audio_runtime_scrub_canceled(StateFromArray(env, state)));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeStopped(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    return StateToArray(env, audio_runtime_stopped(StateFromArray(env, state)));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeCompleted(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    return StateToArray(env, audio_runtime_completed(StateFromArray(env, state)));
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeFailed(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    return StateToArray(env, audio_runtime_failed(StateFromArray(env, state)));
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeProgressFraction(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    const auto runtime_state = StateFromArray(env, state);
    return audio_runtime_progress_fraction(&runtime_state);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeClampSamples(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint total_samples,
    jint sample_index
) {
    return audio_runtime_clamp_samples(total_samples, sample_index);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeFractionToSamples(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint total_samples,
    jfloat fraction
) {
    return audio_runtime_fraction_to_samples(total_samples, fraction);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeElapsedMs(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    const auto runtime_state = StateFromArray(env, state);
    return static_cast<jlong>(audio_runtime_elapsed_ms(&runtime_state));
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_bag_audioandroid_NativePlaybackRuntimeBridge_nativeTotalMs(
    JNIEnv* env,
    jobject /*thiz*/,
    jintArray state
) {
    const auto runtime_state = StateFromArray(env, state);
    return static_cast<jlong>(audio_runtime_total_ms(&runtime_state));
}
