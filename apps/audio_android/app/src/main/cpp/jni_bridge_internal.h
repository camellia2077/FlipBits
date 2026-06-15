#pragma once

#include <jni.h>

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "bag_api.h"

namespace jni_bridge {

inline constexpr int kDefaultSampleRateHz = 44100;
inline constexpr int kDefaultFrameSamples = 2205;
inline constexpr jint kBagErrorOk = 0;
inline constexpr jint kBagErrorInternal = 4;
inline constexpr jint kBagErrorEncodedAudioTooLarge = 6;
inline constexpr std::size_t kMaxJvmEncodePcmSamples =
    static_cast<std::size_t>(kDefaultSampleRateHz) * 60U * 10U;

std::string JStringToStdString(JNIEnv* env, jstring value);
std::string CopyApiString(const char* buffer, std::size_t size);
std::string CopyApiString(const char* buffer, std::size_t size, std::size_t buffer_size);

int NormalizeSampleRate(int sample_rate_hz);
int NormalizeFrameSamples(int sample_rate_hz, int frame_samples);

bag_encoder_config MakeEncoderConfig(int sample_rate_hz,
                                     int frame_samples,
                                     int flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_STANDARD,
                                     int flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_STANDARD);

bag_decoder_config MakeDecoderConfig(int sample_rate_hz,
                                     int frame_samples,
                                     int flash_signal_profile = BAG_FLASH_SIGNAL_PROFILE_STANDARD,
                                     int flash_voicing_flavor = BAG_FLASH_VOICING_FLAVOR_STANDARD);

bag_encode_operation* HandleToEncodeOperation(jlong handle);
bag_decode_operation* HandleToDecodeOperation(jlong handle);

jdoubleArray NewEncodeOperationSnapshotArray(JNIEnv* env,
                                             const bag_encode_operation_progress& progress);
jdoubleArray NewEncodeOperationWorkPlanArray(JNIEnv* env,
                                             const bag_encode_operation_work_plan& work_plan);
jdoubleArray NewDecodeOperationSnapshotArray(JNIEnv* env,
                                             const bag_decode_operation_progress& progress);
jdoubleArray NewDecodeOperationWorkPlanArray(JNIEnv* env,
                                             const bag_decode_operation_work_plan& work_plan);

jobject NewPayloadFollowByteEntry(JNIEnv* env,
                                  const bag_payload_follow_byte_entry& entry);
jobject NewPayloadFollowBinaryGroupEntry(
    JNIEnv* env,
    const bag_payload_follow_binary_group_entry& entry);
jobject NewUltraFrameSymbolEntry(JNIEnv* env,
                                 const bag_ultra_frame_symbol_entry& entry);
jobject NewTextFollowTimelineEntry(JNIEnv* env,
                                   const bag_text_follow_token_entry& entry);
jobject NewTextFollowCharacterViewData(
    JNIEnv* env,
    const bag_text_follow_character_entry& entry,
    const std::string& text_character_text);
jobject NewTextFollowRawSegmentViewData(
    JNIEnv* env,
    const bag_text_follow_raw_segment_entry& entry,
    const std::vector<std::string>& hex_tokens,
    const std::string& compact_bits);
jobject NewTextFollowRawDisplayUnitViewData(
    JNIEnv* env,
    const bag_text_follow_raw_display_unit_entry& entry,
    const std::vector<std::string>& hex_tokens,
    const std::string& compact_bits);
jobject NewTextFollowLyricLineTimelineEntry(
    JNIEnv* env,
    const bag_text_follow_lyric_line_entry& entry);
jobject NewTextFollowLineTokenRangeViewData(
    JNIEnv* env,
    const bag_text_follow_line_token_range_entry& entry);
jobject NewTextFollowLineRawSegmentViewData(
    JNIEnv* env,
    const bag_text_follow_line_raw_segment_entry& entry,
    const std::vector<std::string>& hex_tokens,
    const std::string& compact_bits);

jobject NewStringList(JNIEnv* env, const std::vector<std::string>& values);
jobject NewByteTimelineList(JNIEnv* env,
                            const std::vector<bag_payload_follow_byte_entry>& entries);
jobject NewBinaryTimelineList(
    JNIEnv* env,
    const std::vector<bag_payload_follow_binary_group_entry>& entries);
jobject NewUltraFrameTimelineList(
    JNIEnv* env,
    const std::vector<bag_ultra_frame_symbol_entry>& entries);
jobject NewTextTimelineList(JNIEnv* env,
                            const std::vector<bag_text_follow_token_entry>& entries);
jobject NewTextCharacterList(
    JNIEnv* env,
    const std::vector<bag_text_follow_character_entry>& entries,
    const std::string& text_character_text);
jobject NewTextRawSegmentList(
    JNIEnv* env,
    const std::vector<bag_text_follow_raw_segment_entry>& entries,
    const std::vector<std::string>& hex_tokens,
    const std::string& compact_bits);
jobject NewTextRawDisplayUnitList(
    JNIEnv* env,
    const std::vector<bag_text_follow_raw_display_unit_entry>& entries,
    const std::vector<std::string>& hex_tokens,
    const std::string& compact_bits);
jobject NewLyricLineTimelineList(
    JNIEnv* env,
    const std::vector<bag_text_follow_lyric_line_entry>& entries);
jobject NewLineTokenRangeList(
    JNIEnv* env,
    const std::vector<bag_text_follow_line_token_range_entry>& entries);
jobject NewLineRawSegmentList(
    JNIEnv* env,
    const std::vector<bag_text_follow_line_raw_segment_entry>& entries,
    const std::vector<std::string>& hex_tokens,
    const std::string& compact_bits);

jobject NewPayloadFollowViewData(JNIEnv* env,
                                 const std::string& text_tokens,
                                 const std::string& text_character_text,
                                 const std::string& lyric_lines,
                                 const std::string& raw_bytes_hex,
                                 const std::string& raw_bits_binary,
                                 const std::vector<bag_text_follow_token_entry>& text_entries,
                                 const std::vector<bag_text_follow_character_entry>& text_characters,
                                 const std::vector<bag_text_follow_raw_segment_entry>& text_raw_segments,
                                 const std::vector<bag_text_follow_raw_display_unit_entry>& text_raw_display_units,
                                 const std::vector<bag_text_follow_lyric_line_entry>& line_entries,
                                 const std::vector<bag_text_follow_line_token_range_entry>& line_token_ranges,
                                 const std::vector<bag_text_follow_line_raw_segment_entry>& line_raw_segments,
                                 const std::vector<bag_payload_follow_byte_entry>& byte_entries,
                                 const std::vector<bag_payload_follow_binary_group_entry>& binary_entries,
                                 const std::vector<bag_ultra_frame_symbol_entry>& ultra_frame_entries,
                                 jboolean text_follow_available,
                                 jboolean lyric_line_follow_available,
                                 jint payload_begin_sample,
                                 jint payload_sample_count,
                                 jint total_pcm_sample_count,
                                 jboolean follow_available);

jobject NewDecodedPayloadViewData(JNIEnv* env,
                                  const std::string& text,
                                  const std::string& raw_bytes_hex,
                                  const std::string& raw_bits_binary,
                                  jint text_decode_status_code,
                                  jboolean raw_payload_available);

jobject NewDecodedAudioPayloadResult(JNIEnv* env,
                                     jobject decoded_payload,
                                     jobject follow_data);
jobject NewEncodedAudioPayloadResult(JNIEnv* env,
                                     jshortArray pcm,
                                     const std::string& raw_bytes_hex,
                                     const std::string& raw_bits_binary,
                                     jobject follow_data,
                                     jint terminal_code);

jobject NewFlashSignalInfo(JNIEnv* env,
                           const std::string& low_carrier_hz,
                           const std::string& high_carrier_hz,
                           const std::string& bit_duration_samples,
                           const std::string& payload_silence,
                           const std::string& decode_path,
                           jboolean available);

jshortArray NewShortArrayFromPcmResult(JNIEnv* env, const bag_pcm16_result& result);
jobject NewEmptyPayloadFollowViewData(JNIEnv* env);
jobject NewEmptyDecodedAudioPayloadResult(JNIEnv* env,
                                          jint text_decode_status_code,
                                          jboolean raw_payload_available);
jobject NewEmptyEncodedPayloadResult(JNIEnv* env, jint terminal_code);
jobject NewEmptyEncodeOperationPcmPayloadResult(JNIEnv* env,
                                                jint terminal_code = kBagErrorInternal);
jobject NewEncodeOperationPcmPayloadResultFromEncodeResult(
    JNIEnv* env,
    const bag_encode_result& result);
jobject NewEmptyEncodeFollowHydrationPayloadResult(JNIEnv* env,
                                                   jint terminal_code = kBagErrorInternal);
jobject NewEncodeFollowHydrationPayloadResultFromEncodeResult(
    JNIEnv* env,
    const bag_encode_result& result);
bool IsPcmSampleCountWithinJvmLimit(std::size_t sample_count);

jint NativeValidateEncodeRequest(JNIEnv* env,
                                 jstring text,
                                 jint sample_rate_hz,
                                 jint frame_samples,
                                 jint mode,
                                 jint flash_signal_profile,
                                 jint flash_voicing_flavor);

jobject NativeBuildEncodeFollowData(JNIEnv* env,
                                    jstring text,
                                    jint sample_rate_hz,
                                    jint frame_samples,
                                    jint mode,
                                    jint flash_signal_profile,
                                    jint flash_voicing_flavor);

jobject NativeDescribeFlashSignal(JNIEnv* env,
                                  jstring text,
                                  jint sample_rate_hz,
                                  jint frame_samples,
                                  jint flash_signal_profile,
                                  jint flash_voicing_flavor);

jobject NativeDecodeGeneratedPcm(JNIEnv* env,
                                 jshortArray pcm,
                                 jint sample_rate_hz,
                                 jint frame_samples,
                                 jint mode,
                                 jint flash_signal_profile,
                                 jint flash_voicing_flavor);

jobject NativeDecodePcmFileSegment(JNIEnv* env,
                                   jstring pcm_file_path,
                                   jlong start_sample,
                                   jint sample_count,
                                   jint sample_rate_hz,
                                   jint frame_samples,
                                   jint mode,
                                   jint flash_signal_profile,
                                   jint flash_voicing_flavor);

jlong NativeCreateDecodeOperation(JNIEnv* env,
                                  jshortArray pcm,
                                  jint sample_rate_hz,
                                  jint frame_samples,
                                  jint mode,
                                  jint flash_signal_profile,
                                  jint flash_voicing_flavor);
jint NativePumpDecodeOperation(jlong handle,
                               jint max_work_units,
                               jint max_wall_time_ms,
                               jboolean* did_progress);
jdoubleArray NativeGetDecodeOperationWorkPlan(JNIEnv* env, jlong handle);
jdoubleArray NativePollDecodeOperation(JNIEnv* env, jlong handle);
jobject NativeTakeDecodeOperationResult(JNIEnv* env, jlong handle);
jint NativeCancelDecodeOperation(jlong handle);
void NativeDestroyDecodeOperation(jlong handle);

jint NativeValidateDecodeConfig(JNIEnv* env,
                                jint sample_rate_hz,
                                jint frame_samples,
                                jint mode,
                                jint flash_signal_profile,
                                jint flash_voicing_flavor);

jobject NativeApplyVoiceFx(JNIEnv* env,
                           jshortArray pcm,
                           jint sample_rate_hz,
                           jint preset,
                           jint enable_diagnostics,
                           jint subvoice_style);
jlong NativeCreateVoiceFxProcessor(JNIEnv* env,
                                   jint sample_rate_hz,
                                   jint preset,
                                   jint enable_diagnostics,
                                   jint subvoice_style);
jobject NativeProcessVoiceFxBlock(JNIEnv* env,
                                  jlong handle,
                                  jshortArray pcm);
jobject NativeFlushVoiceFxProcessor(JNIEnv* env, jlong handle);
void NativeDestroyVoiceFxProcessor(jlong handle);

jstring NativeGetCoreVersion(JNIEnv* env);

jlong NativeCreateEncodeOperation(JNIEnv* env,
                               jstring text,
                               jint sample_rate_hz,
                               jint frame_samples,
                               jint mode,
                               jint flash_signal_profile,
                               jint flash_voicing_flavor);

jint NativePumpEncodeOperation(jlong handle,
                               jint max_work_units,
                               jint max_wall_time_ms,
                               jboolean* did_progress);
jdoubleArray NativeGetEncodeOperationWorkPlan(JNIEnv* env, jlong handle);
jdoubleArray NativePollEncodeOperation(JNIEnv* env, jlong handle);
jobject NativeTakeEncodeOperationResult(JNIEnv* env, jlong handle);
jint NativeCancelEncodeOperation(jlong handle);
void NativeDestroyEncodeOperation(jlong handle);

}  // namespace jni_bridge
