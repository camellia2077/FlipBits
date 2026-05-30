#include "jni_bridge_internal.h"

#include <algorithm>
#include <array>
#include <fstream>

namespace jni_bridge {

jint NativeValidateEncodeRequest(
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
    const bag_validation_issue issue = bag_validate_encode_request(&config, input.c_str());
    return static_cast<jint>(issue);
}
jobject NativeBuildEncodeFollowData(
    JNIEnv* env,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    const std::string input = JStringToStdString(env, text);
    if (input.empty()) {
        return NewEmptyEncodeFollowHydrationPayloadResult(env);
    }

    bag_encoder_config config =
        MakeEncoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);

    bag_encode_result result{};
    std::array<char, 4096> raw_bytes_hex_buffer{};
    std::array<char, 32768> raw_bits_binary_buffer{};
    std::vector<char> text_tokens_buffer(input.size() * 4 + 1, '\0');
    std::vector<char> text_character_text_buffer(input.size() * 4 + 1, '\0');
    std::vector<char> lyric_lines_buffer(input.size() * 4 + 1, '\0');
    std::vector<bag_text_follow_token_entry> text_entries(input.size());
    std::vector<bag_text_follow_character_entry> text_characters(input.size());
    std::vector<bag_text_follow_raw_segment_entry> text_raw_segments(input.size());
    std::vector<bag_text_follow_raw_display_unit_entry> text_raw_display_units(input.size() * 4);
    std::vector<bag_text_follow_lyric_line_entry> line_entries(input.size());
    std::vector<bag_text_follow_line_token_range_entry> line_token_ranges(input.size());
    std::vector<bag_text_follow_line_raw_segment_entry> line_raw_segments(input.size());
    std::vector<bag_payload_follow_byte_entry> byte_entries(input.size() * 4);
    std::vector<bag_payload_follow_binary_group_entry> binary_entries(input.size() * 8);
    std::vector<bag_ultra_frame_symbol_entry> ultra_frame_entries(input.size() * 8 + 64);

    result.raw_bytes_hex_buffer = raw_bytes_hex_buffer.data();
    result.raw_bytes_hex_buffer_size = raw_bytes_hex_buffer.size();
    result.raw_bits_binary_buffer = raw_bits_binary_buffer.data();
    result.raw_bits_binary_buffer_size = raw_bits_binary_buffer.size();
    result.text_follow_data.text_tokens_buffer = text_tokens_buffer.data();
    result.text_follow_data.text_tokens_buffer_size = text_tokens_buffer.size();
    result.text_follow_data.text_character_text_buffer =
        text_character_text_buffer.data();
    result.text_follow_data.text_character_text_buffer_size =
        text_character_text_buffer.size();
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
    if (bag_build_encode_follow_data(&config, input.c_str(), &result) != BAG_OK) {
        return NewEmptyEncodeFollowHydrationPayloadResult(env);
    }

    return NewEncodeFollowHydrationPayloadResultFromEncodeResult(env, result);
}

jobject NativeDescribeFlashSignal(
    JNIEnv* env,
    jstring text,
    jint sample_rate_hz,
    jint frame_samples,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    const std::string input = JStringToStdString(env, text);
    if (input.empty()) {
        return NewFlashSignalInfo(env, "", "", "", "", "", JNI_FALSE);
    }

    bag_encoder_config config =
        MakeEncoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = BAG_TRANSPORT_FLASH;
    std::array<char, 128> low_buffer{};
    std::array<char, 128> high_buffer{};
    std::array<char, 256> bit_buffer{};
    std::array<char, 256> silence_buffer{};
    std::array<char, 256> decode_buffer{};
    bag_flash_signal_info info{};
    info.low_carrier_hz_buffer = low_buffer.data();
    info.low_carrier_hz_buffer_size = low_buffer.size();
    info.high_carrier_hz_buffer = high_buffer.data();
    info.high_carrier_hz_buffer_size = high_buffer.size();
    info.bit_duration_samples_buffer = bit_buffer.data();
    info.bit_duration_samples_buffer_size = bit_buffer.size();
    info.payload_silence_buffer = silence_buffer.data();
    info.payload_silence_buffer_size = silence_buffer.size();
    info.decode_path_buffer = decode_buffer.data();
    info.decode_path_buffer_size = decode_buffer.size();
    if (bag_describe_flash_signal(&config, input.c_str(), &info) != BAG_OK || info.available == 0) {
        return NewFlashSignalInfo(env, "", "", "", "", "", JNI_FALSE);
    }
    return NewFlashSignalInfo(
        env,
        CopyApiString(low_buffer.data(), info.low_carrier_hz_size, low_buffer.size()),
        CopyApiString(high_buffer.data(), info.high_carrier_hz_size, high_buffer.size()),
        CopyApiString(bit_buffer.data(), info.bit_duration_samples_size, bit_buffer.size()),
        CopyApiString(silence_buffer.data(), info.payload_silence_size, silence_buffer.size()),
        CopyApiString(decode_buffer.data(), info.decode_path_size, decode_buffer.size()),
        JNI_TRUE);
}
jobject NativeDecodeGeneratedPcm(
    JNIEnv* env,
    jshortArray pcm,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    if (pcm == nullptr) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_UNAVAILABLE), JNI_FALSE);
    }

    const jsize len = env->GetArrayLength(pcm);
    std::vector<int16_t> buffer(static_cast<size_t>(len), 0);
    env->GetShortArrayRegion(pcm, 0, len, reinterpret_cast<jshort*>(buffer.data()));

    bag_decoder_config config =
        MakeDecoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);
    bag_decoder* decoder = nullptr;
    if (bag_create_decoder(&config, &decoder) != BAG_OK || decoder == nullptr) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }

    (void)bag_push_pcm(decoder, buffer.data(), buffer.size(), 0);
    bag_decode_result probe{};
    const bag_error_code probe_code = bag_poll_decode_result(decoder, &probe);
    bag_destroy_decoder(decoder);
    if (probe_code != BAG_OK) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }

    decoder = nullptr;
    if (bag_create_decoder(&config, &decoder) != BAG_OK || decoder == nullptr) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }
    (void)bag_push_pcm(decoder, buffer.data(), buffer.size(), 0);

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
    result.text_follow_data.text_character_text_buffer =
        text_character_text_buffer.data();
    result.text_follow_data.text_character_text_buffer_size =
        text_character_text_buffer.size();
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
    if (bag_poll_decode_result(decoder, &result) != BAG_OK) {
        bag_destroy_decoder(decoder);
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }
    bag_destroy_decoder(decoder);
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
    const std::string text =
        CopyApiString(text_buffer.data(), result.text_size, text_buffer.size());
    const std::string text_tokens =
        CopyApiString(
            text_tokens_buffer.data(),
            result.text_follow_data.text_tokens_size,
            text_tokens_buffer.size());
    const std::string text_character_text =
        CopyApiString(
            text_character_text_buffer.data(),
            result.text_follow_data.text_character_text_size,
            text_character_text_buffer.size());
    const std::string lyric_lines =
        CopyApiString(
            lyric_lines_buffer.data(),
            result.text_follow_data.lyric_lines_size,
            lyric_lines_buffer.size());
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

jobject NativeDecodePcmFileSegment(
    JNIEnv* env,
    jstring pcm_file_path,
    jlong start_sample,
    jint sample_count,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    if (pcm_file_path == nullptr || start_sample < 0 || sample_count <= 0) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_UNAVAILABLE), JNI_FALSE);
    }

    const std::string path = JStringToStdString(env, pcm_file_path);
    constexpr std::size_t kDecodeFileChunkSamples = 32768;
    auto push_file_segment = [&](bag_decoder* decoder) -> bag_error_code {
        std::ifstream input(path, std::ios::binary);
        if (!input) {
            return BAG_INVALID_ARGUMENT;
        }
        const std::streamoff byte_offset =
            static_cast<std::streamoff>(start_sample) * static_cast<std::streamoff>(sizeof(int16_t));
        input.seekg(byte_offset, std::ios::beg);
        if (!input) {
            return BAG_INVALID_ARGUMENT;
        }
        std::vector<int16_t> buffer(kDecodeFileChunkSamples);
        std::size_t remaining = static_cast<std::size_t>(sample_count);
        std::int64_t pushed_samples = 0;
        while (remaining > 0) {
            const std::size_t chunk_samples = std::min<std::size_t>(remaining, buffer.size());
            const std::streamsize chunk_bytes =
                static_cast<std::streamsize>(chunk_samples * sizeof(int16_t));
            input.read(reinterpret_cast<char*>(buffer.data()), chunk_bytes);
            if (input.gcount() != chunk_bytes) {
                return BAG_INVALID_ARGUMENT;
            }
            const bag_error_code push_code =
                bag_push_pcm(decoder, buffer.data(), chunk_samples, pushed_samples);
            if (push_code != BAG_OK) {
                return push_code;
            }
            pushed_samples += static_cast<std::int64_t>(chunk_samples);
            remaining -= chunk_samples;
        }
        return BAG_OK;
    };

    bag_decoder_config config =
        MakeDecoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);
    bag_decoder* decoder = nullptr;
    if (bag_create_decoder(&config, &decoder) != BAG_OK || decoder == nullptr) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }
    const bag_error_code probe_push_code = push_file_segment(decoder);
    if (probe_push_code != BAG_OK) {
        bag_destroy_decoder(decoder);
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }
    bag_decode_result probe{};
    const bag_error_code probe_code = bag_poll_decode_result(decoder, &probe);
    bag_destroy_decoder(decoder);
    if (probe_code != BAG_OK) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }

    decoder = nullptr;
    if (bag_create_decoder(&config, &decoder) != BAG_OK || decoder == nullptr) {
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }
    const bag_error_code result_push_code = push_file_segment(decoder);
    if (result_push_code != BAG_OK) {
        bag_destroy_decoder(decoder);
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }

    std::vector<char> text_buffer(probe.text_size + 1, '\0');
    std::vector<char> raw_bytes_hex_buffer(probe.raw_bytes_hex_size + 1, '\0');
    std::vector<char> raw_bits_binary_buffer(probe.raw_bits_binary_size + 1, '\0');
    // Saved long-audio decode uses the file-backed path. It must request the
    // same follow buffers as the in-memory path or tokens/visuals decode as empty.
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
    result.text_follow_data.text_character_text_buffer =
        text_character_text_buffer.data();
    result.text_follow_data.text_character_text_buffer_size =
        text_character_text_buffer.size();
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
    if (bag_poll_decode_result(decoder, &result) != BAG_OK) {
        bag_destroy_decoder(decoder);
        return NewEmptyDecodedAudioPayloadResult(
            env, static_cast<jint>(BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR), JNI_FALSE);
    }
    bag_destroy_decoder(decoder);

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
    const std::string text =
        CopyApiString(text_buffer.data(), result.text_size, text_buffer.size());
    const std::string text_tokens =
        CopyApiString(
            text_tokens_buffer.data(),
            result.text_follow_data.text_tokens_size,
            text_tokens_buffer.size());
    const std::string text_character_text =
        CopyApiString(
            text_character_text_buffer.data(),
            result.text_follow_data.text_character_text_size,
            text_character_text_buffer.size());
    const std::string lyric_lines =
        CopyApiString(
            lyric_lines_buffer.data(),
            result.text_follow_data.lyric_lines_size,
            lyric_lines_buffer.size());
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
jint NativeValidateDecodeConfig(
    JNIEnv* env,
    jint sample_rate_hz,
    jint frame_samples,
    jint mode,
    jint flash_signal_profile,
    jint flash_voicing_flavor) {
    bag_decoder_config config =
        MakeDecoderConfig(sample_rate_hz, frame_samples, flash_signal_profile, flash_voicing_flavor);
    config.mode = static_cast<bag_transport_mode>(mode);
    const bag_validation_issue issue = bag_validate_decoder_config(&config);
    return static_cast<jint>(issue);
}
jstring NativeGetCoreVersion(JNIEnv* env) {
    const char* version = bag_core_version();
    if (version == nullptr) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(version);
}

}  // namespace jni_bridge
