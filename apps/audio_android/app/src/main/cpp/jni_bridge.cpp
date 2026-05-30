#include "bag_api.h"
#include "jni_bridge_internal.h"

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeValidateEncodeRequest(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeValidateEncodeRequest(
        env,
        text,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeCreateEncodeOperation(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeCreateEncodeOperation(
        env,
        text,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeGetEncodeOperationWorkPlan(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativeGetEncodeOperationWorkPlan(env, handle);
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativePollEncodeOperation(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativePollEncodeOperation(env, handle);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativePumpEncodeOperation(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jint max_work_units,
    jint max_wall_time_ms) {
    jboolean did_progress = JNI_FALSE;
    return jni_bridge::NativePumpEncodeOperation(
        handle,
        max_work_units,
        max_wall_time_ms,
        &did_progress);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeTakeEncodeOperationResult(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativeTakeEncodeOperationResult(env, handle);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeBuildEncodeFollowData(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeBuildEncodeFollowData(
        env,
        text,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDescribeFlashSignal(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeDescribeFlashSignal(
        env,
        text,
        sample_rate_hz,
        frame_samples,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeCancelEncodeOperation(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativeCancelEncodeOperation(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDestroyEncodeOperation(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    jni_bridge::NativeDestroyEncodeOperation(handle);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDecodeGeneratedPcm(
    JNIEnv* env,
    jobject /*thiz*/,
    jshortArray pcm,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeDecodeGeneratedPcm(
        env,
        pcm,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDecodePcmFileSegment(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring pcm_file_path,
    jlong start_sample,
    jint sample_count,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeDecodePcmFileSegment(
        env,
        pcm_file_path,
        start_sample,
        sample_count,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeCreateDecodeOperation(
    JNIEnv* env,
    jobject /*thiz*/,
    jshortArray pcm,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeCreateDecodeOperation(
        env,
        pcm,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativePumpDecodeOperation(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jint max_work_units,
    jint max_wall_time_ms) {
    jboolean did_progress = JNI_FALSE;
    return jni_bridge::NativePumpDecodeOperation(
        handle,
        max_work_units,
        max_wall_time_ms,
        &did_progress);
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeGetDecodeOperationWorkPlan(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativeGetDecodeOperationWorkPlan(env, handle);
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativePollDecodeOperation(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativePollDecodeOperation(env, handle);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeTakeDecodeOperationResult(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativeTakeDecodeOperationResult(env, handle);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeCancelDecodeOperation(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    return jni_bridge::NativeCancelDecodeOperation(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeDestroyDecodeOperation(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle) {
    jni_bridge::NativeDestroyDecodeOperation(handle);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeValidateDecodeConfig(
    JNIEnv* env,
    jobject /*thiz*/,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    return jni_bridge::NativeValidateDecodeConfig(
        env,
        sample_rate_hz,
        frame_samples,
        mode,
        flash_signal_profile,
        flash_voicing_flavor);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bag_audioandroid_NativeBagBridge_nativeGetCoreVersion(
    JNIEnv* env,
    jobject /*thiz*/) {
    return jni_bridge::NativeGetCoreVersion(env);
}
