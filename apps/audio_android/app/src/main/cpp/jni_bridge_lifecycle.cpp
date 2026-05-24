#include "jni_bridge_internal.h"

namespace jni_bridge {

jlong NativeCreateEncodeOperation(
    JNIEnv* env,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    const std::string input = JStringToStdString(env, text);
    bag_encoder_config config =
        MakeEncoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);

    bag_encode_operation* operation = nullptr;
    if (bag_create_encode_operation(&config, input.c_str(), &operation) != BAG_OK ||
        operation == nullptr) {
        return 0L;
    }

    return static_cast<jlong>(reinterpret_cast<intptr_t>(operation));
}
jint NativePumpEncodeOperation(
    jlong handle,
    jint max_work_units,
    jint max_wall_time_ms,
    jboolean* did_progress) {
    if (did_progress != nullptr) {
        *did_progress = JNI_FALSE;
    }
    bag_encode_operation* operation = HandleToEncodeOperation(handle);
    if (operation == nullptr) {
        return static_cast<jint>(BAG_INVALID_ARGUMENT);
    }

    int progressed = 0;
    const bag_encode_operation_pump_budget budget{
        .max_work_units = max_work_units > 0 ? static_cast<std::uint64_t>(max_work_units) : 0ULL,
        .max_wall_time_ms = max_wall_time_ms > 0 ? static_cast<std::uint32_t>(max_wall_time_ms) : 0U,
    };
    const bag_error_code code = bag_pump_encode_operation(operation, budget, &progressed);
    if (did_progress != nullptr) {
        *did_progress = progressed != 0 ? JNI_TRUE : JNI_FALSE;
    }
    return static_cast<jint>(code);
}
jdoubleArray NativeGetEncodeOperationWorkPlan(
    JNIEnv* env,
    jlong handle) {
    bag_encode_operation* operation = HandleToEncodeOperation(handle);
    if (operation == nullptr) {
        return NewEncodeOperationWorkPlanArray(env, {});
    }

    bag_encode_operation_work_plan work_plan{};
    if (bag_get_encode_operation_work_plan(operation, &work_plan) != BAG_OK) {
        return NewEncodeOperationWorkPlanArray(env, {});
    }

    return NewEncodeOperationWorkPlanArray(env, work_plan);
}

jdoubleArray NativePollEncodeOperation(
    JNIEnv* env,
    jlong handle) {
    bag_encode_operation* operation = HandleToEncodeOperation(handle);
    if (operation == nullptr) {
        bag_encode_operation_progress failed{};
        failed.state = BAG_ENCODE_JOB_FAILED;
        failed.phase = BAG_ENCODE_JOB_PHASE_PREPARING_INPUT;
        failed.terminal_code = BAG_INVALID_ARGUMENT;
        return NewEncodeOperationSnapshotArray(env, failed);
    }

    bag_encode_operation_progress progress{};
    if (bag_poll_encode_operation(operation, &progress) != BAG_OK) {
        bag_encode_operation_progress failed{};
        failed.state = BAG_ENCODE_JOB_FAILED;
        failed.phase = BAG_ENCODE_JOB_PHASE_PREPARING_INPUT;
        failed.terminal_code = BAG_INVALID_ARGUMENT;
        return NewEncodeOperationSnapshotArray(env, failed);
    }

    return NewEncodeOperationSnapshotArray(env, progress);
}
jobject NativeTakeEncodeOperationResult(
    JNIEnv* env,
    jlong handle) {
    bag_encode_operation* operation = HandleToEncodeOperation(handle);
    if (operation == nullptr) {
        return NewEmptyEncodeOperationPcmPayloadResult(env);
    }

    bag_encode_result result{};
    const bag_error_code take_code = bag_take_encode_operation_result(operation, &result);
    if (take_code != BAG_OK) {
        return NewEmptyEncodeOperationPcmPayloadResult(env);
    }
    if (!IsPcmSampleCountWithinJvmLimit(result.sample_count)) {
        bag_free_encode_result(&result);
        return NewEmptyEncodeOperationPcmPayloadResult(env, kBagErrorEncodedAudioTooLarge);
    }
    jobject encoded_result = NewEncodeOperationPcmPayloadResultFromEncodeResult(env, result);
    bag_free_encode_result(&result);
    if (encoded_result == nullptr) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return NewEmptyEncodeOperationPcmPayloadResult(env, kBagErrorInternal);
    }
    return encoded_result;
}
jint NativeCancelEncodeOperation(
    jlong handle) {
    bag_encode_operation* operation = HandleToEncodeOperation(handle);
    if (operation == nullptr) {
        return static_cast<jint>(BAG_INVALID_ARGUMENT);
    }
    return static_cast<jint>(bag_cancel_encode_operation(operation));
}
void NativeDestroyEncodeOperation(
    jlong handle) {
    bag_destroy_encode_operation(HandleToEncodeOperation(handle));
}

}  // namespace jni_bridge
