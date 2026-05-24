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

jdoubleArray NewEncodeOperationSnapshotArray(JNIEnv* env,
                                             const bag_encode_operation_progress& progress);
jdoubleArray NewEncodeOperationWorkPlanArray(JNIEnv* env,
                                             const bag_encode_operation_work_plan& work_plan);

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

jint NativeValidateDecodeConfig(JNIEnv* env,
                                jint sample_rate_hz,
                                jint frame_samples,
                                jint mode,
                                jint flash_signal_profile,
                                jint flash_voicing_flavor);

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
