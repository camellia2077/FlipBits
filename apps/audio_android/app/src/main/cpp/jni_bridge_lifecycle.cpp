#include "jni_bridge_internal.h"

#include <android/log.h>

#include <chrono>
#include <cstdio>

namespace jni_bridge {

namespace {

using DecodeTimingClock = std::chrono::steady_clock;

std::int64_t ElapsedMsSince(DecodeTimingClock::time_point started) {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
               DecodeTimingClock::now() - started)
        .count();
}

void LogDecodeTiming(const char* message) {
    __android_log_print(ANDROID_LOG_ERROR, "SavedAudioDecodeProgress", "%s", message);
}

}  // namespace

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
        failed.state = BAG_ENCODE_OPERATION_FAILED;
        failed.phase = BAG_ENCODE_OPERATION_PHASE_PREPARING_INPUT;
        failed.terminal_code = BAG_INVALID_ARGUMENT;
        return NewEncodeOperationSnapshotArray(env, failed);
    }

    bag_encode_operation_progress progress{};
    if (bag_poll_encode_operation(operation, &progress) != BAG_OK) {
        bag_encode_operation_progress failed{};
        failed.state = BAG_ENCODE_OPERATION_FAILED;
        failed.phase = BAG_ENCODE_OPERATION_PHASE_PREPARING_INPUT;
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

jlong NativeCreateDecodeOperation(
    JNIEnv* env,
    jshortArray pcm,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    const auto total_started = DecodeTimingClock::now();
    if (pcm == nullptr) {
        return 0L;
    }

    const jsize len = env->GetArrayLength(pcm);
    if (len <= 0) {
        return 0L;
    }
    const auto allocate_started = DecodeTimingClock::now();
    std::vector<int16_t> buffer(static_cast<size_t>(len), 0);
    const std::int64_t allocate_ms = ElapsedMsSince(allocate_started);
    const auto copy_started = DecodeTimingClock::now();
    env->GetShortArrayRegion(pcm, 0, len, reinterpret_cast<jshort*>(buffer.data()));
    const std::int64_t copy_ms = ElapsedMsSince(copy_started);

    bag_decoder_config config =
        MakeDecoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);

    bag_decode_operation* operation = nullptr;
    const auto create_started = DecodeTimingClock::now();
    const bag_error_code create_code = bag_create_decode_operation(&config, buffer.data(), buffer.size(), &operation);
    const std::int64_t create_ms = ElapsedMsSince(create_started);
    const std::int64_t total_ms = ElapsedMsSince(total_started);
    char timing_message[256];
    std::snprintf(
        timing_message,
        sizeof(timing_message),
        "jniNativeCreateDecodeOperation samples=%d allocateMs=%lld copyShortArrayMs=%lld bagCreateMs=%lld totalMs=%lld code=%d",
        static_cast<int>(len),
        static_cast<long long>(allocate_ms),
        static_cast<long long>(copy_ms),
        static_cast<long long>(create_ms),
        static_cast<long long>(total_ms),
        static_cast<int>(create_code));
    LogDecodeTiming(timing_message);
    if (create_code != BAG_OK ||
        operation == nullptr) {
        return 0L;
    }

    return static_cast<jlong>(reinterpret_cast<intptr_t>(operation));
}

jint NativePumpDecodeOperation(
    jlong handle,
    jint max_work_units,
    jint max_wall_time_ms,
    jboolean* did_progress) {
    if (did_progress != nullptr) {
        *did_progress = JNI_FALSE;
    }
    bag_decode_operation* operation = HandleToDecodeOperation(handle);
    if (operation == nullptr) {
        return static_cast<jint>(BAG_INVALID_ARGUMENT);
    }

    int progressed = 0;
    const bag_decode_operation_pump_budget budget{
        .max_work_units = max_work_units > 0 ? static_cast<std::uint64_t>(max_work_units) : 0ULL,
        .max_wall_time_ms = max_wall_time_ms > 0 ? static_cast<std::uint32_t>(max_wall_time_ms) : 0U,
    };
    const auto pump_started = DecodeTimingClock::now();
    const bag_error_code code = bag_pump_decode_operation(operation, budget, &progressed);
    const std::int64_t pump_ms = ElapsedMsSince(pump_started);
    if (pump_ms >= 50) {
        char timing_message[192];
        std::snprintf(
            timing_message,
            sizeof(timing_message),
            "jniNativePumpDecodeOperation slow elapsedMs=%lld code=%d progressed=%d maxWorkUnits=%d maxWallTimeMs=%d",
            static_cast<long long>(pump_ms),
            static_cast<int>(code),
            progressed,
            static_cast<int>(max_work_units),
            static_cast<int>(max_wall_time_ms));
        LogDecodeTiming(timing_message);
    }
    if (did_progress != nullptr) {
        *did_progress = progressed != 0 ? JNI_TRUE : JNI_FALSE;
    }
    return static_cast<jint>(code);
}

jdoubleArray NativeGetDecodeOperationWorkPlan(JNIEnv* env, jlong handle) {
    bag_decode_operation* operation = HandleToDecodeOperation(handle);
    if (operation == nullptr) {
        return NewDecodeOperationWorkPlanArray(env, {});
    }

    bag_decode_operation_work_plan work_plan{};
    if (bag_get_decode_operation_work_plan(operation, &work_plan) != BAG_OK) {
        return NewDecodeOperationWorkPlanArray(env, {});
    }

    return NewDecodeOperationWorkPlanArray(env, work_plan);
}

jdoubleArray NativePollDecodeOperation(JNIEnv* env, jlong handle) {
    bag_decode_operation* operation = HandleToDecodeOperation(handle);
    if (operation == nullptr) {
        bag_decode_operation_progress failed{};
        failed.state = BAG_DECODE_OPERATION_FAILED;
        failed.phase = BAG_DECODE_OPERATION_PHASE_PREPARING_INPUT;
        failed.terminal_code = BAG_INVALID_ARGUMENT;
        return NewDecodeOperationSnapshotArray(env, failed);
    }

    bag_decode_operation_progress progress{};
    if (bag_poll_decode_operation(operation, &progress) != BAG_OK) {
        bag_decode_operation_progress failed{};
        failed.state = BAG_DECODE_OPERATION_FAILED;
        failed.phase = BAG_DECODE_OPERATION_PHASE_PREPARING_INPUT;
        failed.terminal_code = BAG_INVALID_ARGUMENT;
        return NewDecodeOperationSnapshotArray(env, failed);
    }

    return NewDecodeOperationSnapshotArray(env, progress);
}

jobject NativeTakeDecodeOperationResult(JNIEnv* env, jlong handle) {
    bag_decode_operation* operation = HandleToDecodeOperation(handle);
    if (operation == nullptr) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }

    bag_decode_result probe{};
    const bag_error_code probe_code = bag_take_decode_operation_result(operation, &probe);
    if (probe_code != BAG_OK) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }

    std::vector<char> text_buffer(probe.text_size + 1, '\0');
    std::vector<char> raw_bytes_hex_buffer(probe.raw_bytes_hex_size + 1, '\0');
    std::vector<char> raw_bits_binary_buffer(probe.raw_bits_binary_size + 1, '\0');
    std::vector<char> text_tokens_buffer(probe.text_follow_data.text_tokens_size + 1, '\0');
    std::vector<char> text_character_text_buffer(
        probe.text_follow_data.text_character_text_size + 1, '\0');
    std::vector<char> lyric_lines_buffer(probe.text_follow_data.lyric_lines_size + 1, '\0');
    std::vector<bag_text_follow_token_entry> text_entries(
        probe.text_follow_data.text_token_timeline_count);
    std::vector<bag_text_follow_character_entry> text_characters(
        probe.text_follow_data.text_characters_count);
    std::vector<bag_text_follow_raw_segment_entry> text_raw_segments(
        probe.text_follow_data.token_raw_segments_count);
    std::vector<bag_text_follow_raw_display_unit_entry> text_raw_display_units(
        probe.text_follow_data.token_raw_display_units_count);
    std::vector<bag_text_follow_lyric_line_entry> line_entries(
        probe.text_follow_data.lyric_line_timeline_count);
    std::vector<bag_text_follow_line_token_range_entry> line_token_ranges(
        probe.text_follow_data.line_token_ranges_count);
    std::vector<bag_text_follow_line_raw_segment_entry> line_raw_segments(
        probe.text_follow_data.line_raw_segments_count);
    std::vector<bag_payload_follow_byte_entry> byte_entries(
        probe.follow_data.byte_timeline_count);
    std::vector<bag_payload_follow_binary_group_entry> binary_entries(
        probe.follow_data.binary_group_timeline_count);
    std::vector<bag_ultra_frame_symbol_entry> ultra_frame_entries(
        probe.follow_data.ultra_frame_timeline_count);

    bag_decode_result result{};
    result.text_buffer = text_buffer.data();
    result.text_buffer_size = text_buffer.size();
    result.raw_bytes_hex_buffer = raw_bytes_hex_buffer.data();
    result.raw_bytes_hex_buffer_size = raw_bytes_hex_buffer.size();
    result.raw_bits_binary_buffer = raw_bits_binary_buffer.data();
    result.raw_bits_binary_buffer_size = raw_bits_binary_buffer.size();
    result.text_follow_data.text_tokens_buffer = text_tokens_buffer.data();
    result.text_follow_data.text_tokens_buffer_size = text_tokens_buffer.size();
    result.text_follow_data.text_character_text_buffer = text_character_text_buffer.data();
    result.text_follow_data.text_character_text_buffer_size = text_character_text_buffer.size();
    result.text_follow_data.lyric_lines_buffer = lyric_lines_buffer.data();
    result.text_follow_data.lyric_lines_buffer_size = lyric_lines_buffer.size();
    result.text_follow_data.text_token_timeline_buffer = text_entries.data();
    result.text_follow_data.text_token_timeline_buffer_count = text_entries.size();
    result.text_follow_data.text_characters_buffer = text_characters.data();
    result.text_follow_data.text_characters_buffer_count = text_characters.size();
    result.text_follow_data.token_raw_segments_buffer = text_raw_segments.data();
    result.text_follow_data.token_raw_segments_buffer_count = text_raw_segments.size();
    result.text_follow_data.token_raw_display_units_buffer = text_raw_display_units.data();
    result.text_follow_data.token_raw_display_units_buffer_count = text_raw_display_units.size();
    result.text_follow_data.lyric_line_timeline_buffer = line_entries.data();
    result.text_follow_data.lyric_line_timeline_buffer_count = line_entries.size();
    result.text_follow_data.line_token_ranges_buffer = line_token_ranges.data();
    result.text_follow_data.line_token_ranges_buffer_count = line_token_ranges.size();
    result.text_follow_data.line_raw_segments_buffer = line_raw_segments.data();
    result.text_follow_data.line_raw_segments_buffer_count = line_raw_segments.size();
    result.follow_data.byte_timeline_buffer = byte_entries.data();
    result.follow_data.byte_timeline_buffer_count = byte_entries.size();
    result.follow_data.binary_group_timeline_buffer = binary_entries.data();
    result.follow_data.binary_group_timeline_buffer_count = binary_entries.size();
    result.follow_data.ultra_frame_timeline_buffer = ultra_frame_entries.data();
    result.follow_data.ultra_frame_timeline_buffer_count = ultra_frame_entries.size();
    if (bag_take_decode_operation_result(operation, &result) != BAG_OK) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }

    text_entries.resize(result.text_follow_data.text_token_timeline_count);
    text_characters.resize(result.text_follow_data.text_characters_count);
    text_raw_segments.resize(result.text_follow_data.token_raw_segments_count);
    text_raw_display_units.resize(result.text_follow_data.token_raw_display_units_count);
    line_entries.resize(result.text_follow_data.lyric_line_timeline_count);
    line_token_ranges.resize(result.text_follow_data.line_token_ranges_count);
    line_raw_segments.resize(result.text_follow_data.line_raw_segments_count);
    byte_entries.resize(result.follow_data.byte_timeline_count);
    binary_entries.resize(result.follow_data.binary_group_timeline_count);
    ultra_frame_entries.resize(result.follow_data.ultra_frame_timeline_count);
    const std::string text = CopyApiString(text_buffer.data(), result.text_size, text_buffer.size());
    const std::string text_tokens =
        CopyApiString(text_tokens_buffer.data(), result.text_follow_data.text_tokens_size, text_tokens_buffer.size());
    const std::string text_character_text =
        CopyApiString(
            text_character_text_buffer.data(),
            result.text_follow_data.text_character_text_size,
            text_character_text_buffer.size());
    const std::string lyric_lines =
        CopyApiString(lyric_lines_buffer.data(), result.text_follow_data.lyric_lines_size, lyric_lines_buffer.size());
    const std::string raw_bytes_hex =
        CopyApiString(raw_bytes_hex_buffer.data(), result.raw_bytes_hex_size, raw_bytes_hex_buffer.size());
    const std::string raw_bits_binary =
        CopyApiString(raw_bits_binary_buffer.data(), result.raw_bits_binary_size, raw_bits_binary_buffer.size());

    jobject decoded_payload = NewDecodedPayloadViewData(
        env,
        text,
        raw_bytes_hex,
        raw_bits_binary,
        static_cast<jint>(result.text_decode_status),
        result.raw_payload_available != 0 ? JNI_TRUE : JNI_FALSE);
    jobject follow_data = NewPayloadFollowViewData(
        env,
        text_tokens,
        text_character_text,
        lyric_lines,
        raw_bytes_hex,
        raw_bits_binary,
        text_entries,
        text_characters,
        text_raw_segments,
        text_raw_display_units,
        line_entries,
        line_token_ranges,
        line_raw_segments,
        byte_entries,
        binary_entries,
        ultra_frame_entries,
        result.text_follow_data.available != 0 ? JNI_TRUE : JNI_FALSE,
        (result.text_follow_data.available != 0 &&
         result.text_follow_data.lyric_line_timeline_count > 0 &&
         result.text_follow_data.line_token_ranges_count > 0)
            ? JNI_TRUE
            : JNI_FALSE,
        static_cast<jint>(result.follow_data.payload_begin_sample),
        static_cast<jint>(result.follow_data.payload_sample_count),
        static_cast<jint>(result.follow_data.total_pcm_sample_count),
        result.follow_data.available != 0 ? JNI_TRUE : JNI_FALSE);
    return NewDecodedAudioPayloadResult(env, decoded_payload, follow_data);
}

jint NativeCancelDecodeOperation(jlong handle) {
    bag_decode_operation* operation = HandleToDecodeOperation(handle);
    if (operation == nullptr) {
        return static_cast<jint>(BAG_INVALID_ARGUMENT);
    }
    return static_cast<jint>(bag_cancel_decode_operation(operation));
}

void NativeDestroyDecodeOperation(jlong handle) {
    bag_destroy_decode_operation(HandleToDecodeOperation(handle));
}

}  // namespace jni_bridge
