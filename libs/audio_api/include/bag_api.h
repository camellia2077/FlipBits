#pragma once

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum bag_error_code {
  BAG_OK = 0,
  BAG_INVALID_ARGUMENT = 1,
  BAG_NOT_READY = 2,
  BAG_NOT_IMPLEMENTED = 3,
  BAG_INTERNAL = 4,
  BAG_CANCELLED = 5
} bag_error_code;

typedef enum bag_transport_mode {
  BAG_TRANSPORT_MINI = 0,
  BAG_TRANSPORT_FLASH = 1,
  BAG_TRANSPORT_PRO = 2,
  BAG_TRANSPORT_ULTRA = 3
} bag_transport_mode;

typedef enum bag_voice_fx_preset {
  BAG_VOICE_FX_MACHINE_VOICE = 0,
  BAG_VOICE_FX_BINARIC_CANT = 1,
  BAG_VOICE_FX_SIGNAL_CANT = 2,
  BAG_VOICE_FX_ROBOT_VOX = 3,
  BAG_VOICE_FX_RAW_CONSTANT = 4,
  BAG_VOICE_FX_VOICE_TRIGGER = 5
} bag_voice_fx_preset;

typedef enum bag_voice_fx_subvoice_style {
  BAG_VOICE_FX_SUBVOICE_STYLE_STANDARD = 0,
  BAG_VOICE_FX_SUBVOICE_STYLE_LITANY = 1,
  BAG_VOICE_FX_SUBVOICE_STYLE_HOSTILITY = 3,
  BAG_VOICE_FX_SUBVOICE_STYLE_COLLAPSE = 4,
  BAG_VOICE_FX_SUBVOICE_STYLE_ZEAL = 5,
  BAG_VOICE_FX_SUBVOICE_STYLE_VOID = 6
} bag_voice_fx_subvoice_style;

typedef enum bag_flash_signal_profile {
  BAG_FLASH_SIGNAL_PROFILE_STANDARD = 0,
  BAG_FLASH_SIGNAL_PROFILE_LITANY = 1,
  BAG_FLASH_SIGNAL_PROFILE_HOSTILITY = 3,
  BAG_FLASH_SIGNAL_PROFILE_COLLAPSE = 4,
  BAG_FLASH_SIGNAL_PROFILE_ZEAL = 5,
  BAG_FLASH_SIGNAL_PROFILE_VOID = 6
} bag_flash_signal_profile;

typedef enum bag_flash_voicing_flavor {
  BAG_FLASH_VOICING_FLAVOR_STANDARD = 0,
  BAG_FLASH_VOICING_FLAVOR_LITANY = 1,
  BAG_FLASH_VOICING_FLAVOR_HOSTILITY = 3,
  BAG_FLASH_VOICING_FLAVOR_COLLAPSE = 4,
  BAG_FLASH_VOICING_FLAVOR_ZEAL = 5,
  BAG_FLASH_VOICING_FLAVOR_VOID = 6
} bag_flash_voicing_flavor;

typedef enum bag_validation_issue {
  BAG_VALIDATION_OK = 0,
  BAG_VALIDATION_NULL_CONFIG = 1,
  BAG_VALIDATION_NULL_TEXT = 2,
  BAG_VALIDATION_NULL_DECODER_OUTPUT = 3,
  BAG_VALIDATION_INVALID_SAMPLE_RATE = 4,
  BAG_VALIDATION_INVALID_FRAME_SAMPLES = 5,
  BAG_VALIDATION_INVALID_MODE = 6,
  BAG_VALIDATION_PRO_ASCII_ONLY = 7,
  BAG_VALIDATION_PAYLOAD_TOO_LARGE = 8,
  BAG_VALIDATION_INVALID_FLASH_SIGNAL_PROFILE = 9,
  BAG_VALIDATION_INVALID_FLASH_VOICING_FLAVOR = 10,
  BAG_VALIDATION_MINI_MORSE_ONLY = 11,
  BAG_VALIDATION_EMPTY_TEXT = 12
} bag_validation_issue;

typedef struct bag_decoder bag_decoder;
typedef struct bag_encode_operation bag_encode_operation;
typedef struct bag_decode_operation bag_decode_operation;
typedef struct bag_voice_fx_processor bag_voice_fx_processor;

typedef struct bag_encoder_config {
  int sample_rate_hz;
  int frame_samples;
  int enable_diagnostics;
  bag_transport_mode mode;
  bag_flash_signal_profile flash_signal_profile;
  bag_flash_voicing_flavor flash_voicing_flavor;
  int reserved;
} bag_encoder_config;

typedef struct bag_decoder_config {
  int sample_rate_hz;
  int frame_samples;
  int enable_diagnostics;
  bag_transport_mode mode;
  bag_flash_signal_profile flash_signal_profile;
  bag_flash_voicing_flavor flash_voicing_flavor;
  int reserved;
} bag_decoder_config;

typedef struct bag_voice_fx_config {
  int sample_rate_hz;
  int enable_diagnostics;
  bag_voice_fx_preset preset;
  bag_voice_fx_subvoice_style subvoice_style;
  int reserved;
} bag_voice_fx_config;

typedef struct bag_text_result {
  char* buffer;
  size_t buffer_size;
  size_t text_size;
  int complete;
  float confidence;
  bag_transport_mode mode;
} bag_text_result;

typedef enum bag_decode_content_status {
  BAG_DECODE_CONTENT_STATUS_OK = 0,
  BAG_DECODE_CONTENT_STATUS_UNAVAILABLE = 1,
  BAG_DECODE_CONTENT_STATUS_INVALID_TEXT_PAYLOAD = 2,
  BAG_DECODE_CONTENT_STATUS_BUFFER_TOO_SMALL = 3,
  BAG_DECODE_CONTENT_STATUS_INTERNAL_ERROR = 4
} bag_decode_content_status;

typedef struct bag_payload_follow_byte_entry {
  size_t start_sample;
  size_t sample_count;
  size_t byte_index;
} bag_payload_follow_byte_entry;

typedef struct bag_payload_follow_binary_group_entry {
  size_t start_sample;
  size_t sample_count;
  size_t group_index;
  size_t bit_offset;
  size_t bit_count;
  double carrier_freq_hz;
} bag_payload_follow_binary_group_entry;

typedef enum bag_ultra_frame_section {
  BAG_ULTRA_FRAME_SECTION_PREAMBLE = 0,
  BAG_ULTRA_FRAME_SECTION_SYNC = 1,
  BAG_ULTRA_FRAME_SECTION_VERSION = 2,
  BAG_ULTRA_FRAME_SECTION_FLAGS = 3,
  BAG_ULTRA_FRAME_SECTION_PAYLOAD_LENGTH = 4,
  BAG_ULTRA_FRAME_SECTION_PAYLOAD = 5,
  BAG_ULTRA_FRAME_SECTION_CRC16 = 6
} bag_ultra_frame_section;

typedef struct bag_ultra_frame_symbol_entry {
  size_t start_sample;
  size_t sample_count;
  size_t frame_byte_index;
  size_t nibble_index_in_byte;
  size_t nibble_value;
  double carrier_freq_hz;
  bag_ultra_frame_section section;
  int is_payload;
  size_t payload_byte_index;
} bag_ultra_frame_symbol_entry;

// Follow-data arrays use caller-owned buffers. The *_count fields report the
// total available entries for sizing/probing even when the matching *_buffer is
// null or too small. Consumers may read entries only when the matching status
// is BAG_DECODE_CONTENT_STATUS_OK; otherwise the buffer contents are absent or
// partial and must not be inferred from *_count alone.
typedef struct bag_payload_follow_data {
  bag_payload_follow_byte_entry* byte_timeline_buffer;
  size_t byte_timeline_buffer_count;
  size_t byte_timeline_count;
  bag_decode_content_status byte_timeline_status;
  bag_payload_follow_binary_group_entry* binary_group_timeline_buffer;
  size_t binary_group_timeline_buffer_count;
  size_t binary_group_timeline_count;
  bag_decode_content_status binary_group_timeline_status;
  bag_ultra_frame_symbol_entry* ultra_frame_timeline_buffer;
  size_t ultra_frame_timeline_buffer_count;
  size_t ultra_frame_timeline_count;
  bag_decode_content_status ultra_frame_timeline_status;
  size_t payload_begin_sample;
  size_t payload_sample_count;
  size_t total_pcm_sample_count;
  int available;
} bag_payload_follow_data;

typedef struct bag_text_follow_token_entry {
  size_t start_sample;
  size_t sample_count;
  size_t token_index;
  size_t text_offset;
  size_t text_size;
} bag_text_follow_token_entry;

// Character-level follow semantics inside a token. Consumers should use this
// together with text_offset/text_size to decide whether a character is visible
// text or a separator that only needs timing/layout ownership.
typedef enum bag_text_follow_character_kind {
  BAG_TEXT_FOLLOW_CHARACTER_KIND_VISIBLE = 0,  // Visible glyphs.
  BAG_TEXT_FOLLOW_CHARACTER_KIND_SPACE = 1,    // Space-like separators.
  BAG_TEXT_FOLLOW_CHARACTER_KIND_NEWLINE = 2,  // Attached newline separators.
  BAG_TEXT_FOLLOW_CHARACTER_KIND_SEPARATOR_OTHER =
      3  // Other non-visible separators.
} bag_text_follow_character_kind;

typedef struct bag_text_follow_character_entry {
  size_t start_sample;
  size_t sample_count;
  size_t token_index;
  size_t character_index_within_token;
  size_t byte_index_within_token;
  size_t byte_count;
  bag_text_follow_character_kind kind;
  size_t text_offset;
  size_t text_size;
} bag_text_follow_character_entry;

typedef struct bag_text_follow_raw_segment_entry {
  size_t start_sample;
  size_t sample_count;
  size_t token_index;
  size_t byte_offset;
  size_t byte_count;
} bag_text_follow_raw_segment_entry;

typedef struct bag_text_follow_raw_display_unit_entry {
  size_t start_sample;
  size_t sample_count;
  size_t token_index;
  size_t byte_index_within_token;
  size_t byte_offset;
  size_t byte_count;
  size_t character_index_within_token;
  size_t byte_index_within_character;
  size_t character_byte_count;
  int is_character_start;
  int is_character_end;
} bag_text_follow_raw_display_unit_entry;

typedef struct bag_text_follow_lyric_line_entry {
  size_t start_sample;
  size_t sample_count;
  size_t line_index;
} bag_text_follow_lyric_line_entry;

typedef struct bag_text_follow_line_token_range_entry {
  size_t line_index;
  size_t token_begin_index;
  size_t token_count;
} bag_text_follow_line_token_range_entry;

typedef struct bag_text_follow_line_raw_segment_entry {
  size_t start_sample;
  size_t sample_count;
  size_t line_index;
  size_t byte_offset;
  size_t byte_count;
} bag_text_follow_line_raw_segment_entry;

// Text follow strings and arrays use caller-owned buffers. The *_size and
// *_count fields are the total required sizes/counts for sizing/probing. A
// non-zero size/count does not guarantee a readable buffer; consumers must
// check the corresponding status before reading each string or array.
typedef struct bag_text_follow_data {
  char* text_tokens_buffer;
  size_t text_tokens_buffer_size;
  size_t text_tokens_size;
  bag_decode_content_status text_tokens_status;
  bag_text_follow_token_entry* text_token_timeline_buffer;
  size_t text_token_timeline_buffer_count;
  size_t text_token_timeline_count;
  bag_decode_content_status text_token_timeline_status;
  char* text_character_text_buffer;
  size_t text_character_text_buffer_size;
  size_t text_character_text_size;
  bag_decode_content_status text_character_text_status;
  bag_text_follow_character_entry* text_characters_buffer;
  size_t text_characters_buffer_count;
  size_t text_characters_count;
  bag_decode_content_status text_characters_status;
  bag_text_follow_raw_segment_entry* token_raw_segments_buffer;
  size_t token_raw_segments_buffer_count;
  size_t token_raw_segments_count;
  bag_decode_content_status token_raw_segments_status;
  bag_text_follow_raw_display_unit_entry* token_raw_display_units_buffer;
  size_t token_raw_display_units_buffer_count;
  size_t token_raw_display_units_count;
  bag_decode_content_status token_raw_display_units_status;
  char* lyric_lines_buffer;
  size_t lyric_lines_buffer_size;
  size_t lyric_lines_size;
  bag_decode_content_status lyric_lines_status;
  bag_text_follow_lyric_line_entry* lyric_line_timeline_buffer;
  size_t lyric_line_timeline_buffer_count;
  size_t lyric_line_timeline_count;
  bag_decode_content_status lyric_line_timeline_status;
  bag_text_follow_line_token_range_entry* line_token_ranges_buffer;
  size_t line_token_ranges_buffer_count;
  size_t line_token_ranges_count;
  bag_decode_content_status line_token_ranges_status;
  bag_text_follow_line_raw_segment_entry* line_raw_segments_buffer;
  size_t line_raw_segments_buffer_count;
  size_t line_raw_segments_count;
  bag_decode_content_status line_raw_segments_status;
  int available;
} bag_text_follow_data;

typedef struct bag_decode_result {
  char* text_buffer;
  size_t text_buffer_size;
  size_t text_size;
  char* raw_bytes_hex_buffer;
  size_t raw_bytes_hex_buffer_size;
  size_t raw_bytes_hex_size;
  char* raw_bits_binary_buffer;
  size_t raw_bits_binary_buffer_size;
  size_t raw_bits_binary_size;
  int complete;
  float confidence;
  bag_transport_mode mode;
  bag_decode_content_status text_decode_status;
  bag_decode_content_status raw_bytes_hex_status;
  bag_decode_content_status raw_bits_binary_status;
  int raw_payload_available;
  bag_payload_follow_data follow_data;
  bag_text_follow_data text_follow_data;
} bag_decode_result;

typedef struct bag_pcm16_result {
  int16_t* samples;
  size_t sample_count;
} bag_pcm16_result;

typedef struct bag_voice_fx_result {
  bag_pcm16_result final_mix;
  bag_pcm16_result main_voice;
  bag_pcm16_result subvoice;
  bag_pcm16_result signal_overlay;
} bag_voice_fx_result;

// Encode results combine owned PCM output with optional caller-owned raw/follow
// buffers. `samples` is allocated by the API on success and must be released
// via bag_free_encode_result. Raw/follow buffers are never allocated by the
// API; their *_size and *_count fields may be non-zero as sizing probes even
// when the matching status is unavailable or buffer-too-small.
typedef struct bag_encode_result {
  int16_t* samples;
  size_t sample_count;
  char* raw_bytes_hex_buffer;
  size_t raw_bytes_hex_buffer_size;
  size_t raw_bytes_hex_size;
  char* raw_bits_binary_buffer;
  size_t raw_bits_binary_buffer_size;
  size_t raw_bits_binary_size;
  bag_decode_content_status raw_bytes_hex_status;
  bag_decode_content_status raw_bits_binary_status;
  int raw_payload_available;
  bag_payload_follow_data follow_data;
  bag_text_follow_data text_follow_data;
} bag_encode_result;

typedef struct bag_flash_signal_info {
  char* low_carrier_hz_buffer;
  size_t low_carrier_hz_buffer_size;
  size_t low_carrier_hz_size;
  bag_decode_content_status low_carrier_hz_status;
  char* high_carrier_hz_buffer;
  size_t high_carrier_hz_buffer_size;
  size_t high_carrier_hz_size;
  bag_decode_content_status high_carrier_hz_status;
  char* bit_duration_samples_buffer;
  size_t bit_duration_samples_buffer_size;
  size_t bit_duration_samples_size;
  bag_decode_content_status bit_duration_samples_status;
  char* payload_silence_buffer;
  size_t payload_silence_buffer_size;
  size_t payload_silence_size;
  bag_decode_content_status payload_silence_status;
  char* decode_path_buffer;
  size_t decode_path_buffer_size;
  size_t decode_path_size;
  bag_decode_content_status decode_path_status;
  int available;
} bag_flash_signal_info;

typedef struct bag_encode_result_layout {
  size_t sample_count;
  size_t raw_bytes_hex_size;
  size_t raw_bits_binary_size;
  int raw_payload_available;
  size_t byte_timeline_count;
  size_t binary_group_timeline_count;
  size_t ultra_frame_timeline_count;
  size_t text_tokens_size;
  size_t text_character_text_size;
  size_t text_token_timeline_count;
  size_t text_characters_count;
  size_t token_raw_segments_count;
  size_t token_raw_display_units_count;
  size_t lyric_lines_size;
  size_t lyric_line_timeline_count;
  size_t line_token_ranges_count;
  size_t line_raw_segments_count;
  int follow_available;
  int text_follow_available;
} bag_encode_result_layout;

// Stable lifecycle values for encode operation snapshots. Keep the numeric
// assignments fixed so Android/Web bindings can map them without introducing a
// second lifecycle table.
typedef enum bag_encode_operation_state {
  BAG_ENCODE_OPERATION_QUEUED = 0,
  BAG_ENCODE_OPERATION_RUNNING = 1,
  BAG_ENCODE_OPERATION_SUCCEEDED = 2,
  BAG_ENCODE_OPERATION_FAILED = 3,
  BAG_ENCODE_OPERATION_CANCELLED = 4
} bag_encode_operation_state;

typedef enum bag_encode_operation_phase {
  BAG_ENCODE_OPERATION_PHASE_PREPARING_INPUT = 0,
  BAG_ENCODE_OPERATION_PHASE_RENDERING_PCM = 1,
  BAG_ENCODE_OPERATION_PHASE_POSTPROCESSING = 2,
  BAG_ENCODE_OPERATION_PHASE_FINALIZING = 3
} bag_encode_operation_phase;

// Public encode-generation snapshot. This mirrors
// `libs/audio_core::EncodeProgressSnapshot` and is the source of truth for
// state, phase, work completion, and terminal code while an encode operation is
// running.
typedef struct bag_encode_operation_progress {
  bag_encode_operation_state state;
  bag_encode_operation_phase phase;
  float overall_progress_0_to_1;
  float phase_progress_0_to_1;
  uint64_t completed_work_units;
  uint64_t total_work_units;
  uint64_t phase_completed_work_units;
  uint64_t phase_total_work_units;
  bag_error_code terminal_code;
  size_t estimated_pcm_sample_count;
  size_t payload_byte_count;
  size_t segment_count;
  size_t current_segment_index;
} bag_encode_operation_progress;

// Public encode-generation work plan. This mirrors
// `libs/audio_core::EncodeWorkPlan` and is the static forecast for an encode
// operation. Consumers should read this directly instead of recomputing their
// own phase buckets or percent math.
typedef struct bag_encode_operation_work_plan {
  uint64_t preparing_input_work_units;
  uint64_t rendering_pcm_work_units;
  uint64_t postprocessing_work_units;
  uint64_t finalizing_work_units;
  uint64_t total_work_units;
  size_t estimated_pcm_sample_count;
  size_t payload_byte_count;
  size_t segment_count;
} bag_encode_operation_work_plan;

typedef struct bag_encode_operation_diagnostics {
  double flash_payload_prepare_ms;
  double flash_payload_sample_setup_ms;
  double flash_payload_envelope_ms;
  double flash_payload_articulation_ms;
  double flash_payload_harmonic_ms;
  double flash_payload_metallic_ms;
  double flash_payload_chant_resonance_ms;
  double flash_payload_chant_drone_ms;
  double flash_payload_mechanical_throat_ms;
  double flash_payload_standard_low_voice_ms;
  double flash_payload_hostility_edge_ms;
  double flash_payload_boundary_click_ms;
  double flash_payload_modulation_ms;
  double flash_payload_mix_shape_store_ms;
  uint64_t flash_payload_voiced_samples;
  uint64_t flash_payload_silence_samples;
  uint64_t flash_payload_profiled_samples;
} bag_encode_operation_diagnostics;

typedef struct bag_encode_operation_pump_budget {
  uint64_t max_work_units;
  uint32_t max_wall_time_ms;
} bag_encode_operation_pump_budget;

// Stable lifecycle values for decode operation snapshots. Numeric assignments
// intentionally match encode operation state values so Android/Web bindings can
// use the same terminal-state handling while keeping decode as its own ABI
// family.
typedef enum bag_decode_operation_state {
  BAG_DECODE_OPERATION_QUEUED = 0,
  BAG_DECODE_OPERATION_RUNNING = 1,
  BAG_DECODE_OPERATION_SUCCEEDED = 2,
  BAG_DECODE_OPERATION_FAILED = 3,
  BAG_DECODE_OPERATION_CANCELLED = 4
} bag_decode_operation_state;

typedef enum bag_decode_operation_phase {
  BAG_DECODE_OPERATION_PHASE_PREPARING_INPUT = 0,
  BAG_DECODE_OPERATION_PHASE_READING_PCM = 1,
  BAG_DECODE_OPERATION_PHASE_DECODING_PAYLOAD = 2,
  BAG_DECODE_OPERATION_PHASE_FINALIZING = 3
} bag_decode_operation_phase;

typedef struct bag_decode_operation_progress {
  bag_decode_operation_state state;
  bag_decode_operation_phase phase;
  float overall_progress_0_to_1;
  float phase_progress_0_to_1;
  uint64_t completed_work_units;
  uint64_t total_work_units;
  uint64_t phase_completed_work_units;
  uint64_t phase_total_work_units;
  bag_error_code terminal_code;
  size_t pcm_sample_count;
  size_t pushed_pcm_sample_count;
} bag_decode_operation_progress;

typedef struct bag_decode_operation_work_plan {
  uint64_t preparing_input_work_units;
  uint64_t reading_pcm_work_units;
  uint64_t decoding_payload_work_units;
  uint64_t finalizing_work_units;
  uint64_t total_work_units;
  size_t pcm_sample_count;
} bag_decode_operation_work_plan;

typedef struct bag_decode_operation_pump_budget {
  uint64_t max_work_units;
  uint32_t max_wall_time_ms;
} bag_decode_operation_pump_budget;

const char* bag_transport_mode_name(bag_transport_mode mode);
int bag_try_parse_transport_mode(const char* raw_mode,
                                 bag_transport_mode* out_mode);
bag_validation_issue bag_validate_encode_request(
    const bag_encoder_config* config, const char* text);
bag_validation_issue bag_validate_decoder_config(
    const bag_decoder_config* config);
const char* bag_validation_issue_message(bag_validation_issue issue);
const char* bag_error_code_message(bag_error_code code);

bag_error_code bag_encode_text(const bag_encoder_config* config,
                               const char* text, bag_pcm16_result* out_result);
bag_error_code bag_apply_voice_fx(const bag_voice_fx_config* config,
                                  const int16_t* samples,
                                  size_t sample_count,
                                  bag_voice_fx_result* out_result);
bag_error_code bag_create_voice_fx_processor(
    const bag_voice_fx_config* config,
    bag_voice_fx_processor** out_processor);
bag_error_code bag_process_voice_fx_block(
    bag_voice_fx_processor* processor,
    const int16_t* samples,
    size_t sample_count,
    bag_voice_fx_result* out_result);
bag_error_code bag_flush_voice_fx_processor(
    bag_voice_fx_processor* processor,
    bag_voice_fx_result* out_result);
void bag_destroy_voice_fx_processor(bag_voice_fx_processor* processor);
bag_error_code bag_encode_text_with_follow(const bag_encoder_config* config,
                                           const char* text,
                                           bag_encode_result* out_result);
bag_error_code bag_build_encode_follow_data(const bag_encoder_config* config,
                                            const char* text,
                                            bag_encode_result* out_result);
bag_error_code bag_create_encode_operation(
    const bag_encoder_config* config, const char* text,
    bag_encode_operation** out_operation);
bag_error_code bag_run_encode_operation(bag_encode_operation* operation);
bag_error_code bag_cancel_encode_operation(bag_encode_operation* operation);
bag_error_code bag_get_encode_operation_work_plan(
    const bag_encode_operation* operation,
    bag_encode_operation_work_plan* out_work_plan);
bag_error_code bag_get_encode_operation_diagnostics(
    const bag_encode_operation* operation,
    bag_encode_operation_diagnostics* out_diagnostics);
bag_error_code bag_pump_encode_operation(
    bag_encode_operation* operation, bag_encode_operation_pump_budget budget,
    int* out_did_progress);
bag_error_code bag_poll_encode_operation(
    const bag_encode_operation* operation,
    bag_encode_operation_progress* out_progress);
// Returns the operation terminal PCM result. Follow/raw sizes and counts may be
// populated as probes, but follow/raw payload buffers are optional and are not
// required for operation completion. Use bag_build_encode_follow_data when a
// presentation needs hydrated follow/raw timeline data.
bag_error_code bag_take_encode_operation_result(
    const bag_encode_operation* operation, bag_encode_result* out_result);
void bag_destroy_encode_operation(bag_encode_operation* operation);
bag_error_code bag_describe_flash_signal(const bag_encoder_config* config,
                                         const char* text,
                                         bag_flash_signal_info* out_info);
void bag_free_pcm16_result(bag_pcm16_result* result);
void bag_free_voice_fx_result(bag_voice_fx_result* result);
void bag_free_encode_result(bag_encode_result* result);

bag_error_code bag_create_decoder(const bag_decoder_config* config,
                                  bag_decoder** out_decoder);
void bag_destroy_decoder(bag_decoder* decoder);

bag_error_code bag_push_pcm(bag_decoder* decoder, const int16_t* samples,
                            size_t sample_count, int64_t timestamp_ms);

bag_error_code bag_poll_decode_result(bag_decoder* decoder,
                                      bag_decode_result* out_result);
bag_error_code bag_poll_result(bag_decoder* decoder,
                               bag_text_result* out_result);
void bag_reset(bag_decoder* decoder);

bag_error_code bag_create_decode_operation(
    const bag_decoder_config* config, const int16_t* samples,
    size_t sample_count, bag_decode_operation** out_operation);
bag_error_code bag_run_decode_operation(bag_decode_operation* operation);
bag_error_code bag_cancel_decode_operation(bag_decode_operation* operation);
bag_error_code bag_get_decode_operation_work_plan(
    const bag_decode_operation* operation,
    bag_decode_operation_work_plan* out_work_plan);
bag_error_code bag_pump_decode_operation(
    bag_decode_operation* operation, bag_decode_operation_pump_budget budget,
    int* out_did_progress);
bag_error_code bag_poll_decode_operation(
    const bag_decode_operation* operation,
    bag_decode_operation_progress* out_progress);
bag_error_code bag_take_decode_operation_result(
    const bag_decode_operation* operation, bag_decode_result* out_result);
void bag_destroy_decode_operation(bag_decode_operation* operation);
const char* bag_core_version(void);

#ifdef __cplusplus
}
#endif
