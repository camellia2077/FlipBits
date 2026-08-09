use std::os::raw::{c_int, c_void};

pub(super) type BagErrorCode = c_int;
pub(super) type BagTransportMode = c_int;
pub(super) type BagFlashSignalProfile = c_int;
pub(super) type BagFlashVoicingFlavor = c_int;
pub(super) type BagValidationIssue = c_int;
pub(super) type BagDecodeContentStatus = c_int;

pub(super) const BAG_OK: BagErrorCode = 0;
pub(super) const BAG_NOT_READY: BagErrorCode = 2;
pub(super) const BAG_INTERNAL: BagErrorCode = 4;
pub(super) const BAG_TRANSPORT_MINI: BagTransportMode = 0;
pub(super) const BAG_TRANSPORT_FLASH: BagTransportMode = 1;
pub(super) const BAG_TRANSPORT_PRO: BagTransportMode = 2;
pub(super) const BAG_TRANSPORT_ULTRA: BagTransportMode = 3;
pub(super) const BAG_FLASH_SIGNAL_PROFILE_STANDARD: BagFlashSignalProfile = 0;
pub(super) const BAG_FLASH_SIGNAL_PROFILE_LITANY: BagFlashSignalProfile = 1;
pub(super) const BAG_FLASH_SIGNAL_PROFILE_HOSTILITY: BagFlashSignalProfile = 3;
pub(super) const BAG_FLASH_SIGNAL_PROFILE_COLLAPSE: BagFlashSignalProfile = 4;
pub(super) const BAG_FLASH_SIGNAL_PROFILE_ZEAL: BagFlashSignalProfile = 5;
pub(super) const BAG_FLASH_SIGNAL_PROFILE_VOID: BagFlashSignalProfile = 6;
pub(super) const BAG_FLASH_VOICING_FLAVOR_STANDARD: BagFlashVoicingFlavor = 0;
pub(super) const BAG_FLASH_VOICING_FLAVOR_LITANY: BagFlashVoicingFlavor = 1;
pub(super) const BAG_FLASH_VOICING_FLAVOR_HOSTILITY: BagFlashVoicingFlavor = 3;
pub(super) const BAG_FLASH_VOICING_FLAVOR_COLLAPSE: BagFlashVoicingFlavor = 4;
pub(super) const BAG_FLASH_VOICING_FLAVOR_ZEAL: BagFlashVoicingFlavor = 5;
pub(super) const BAG_FLASH_VOICING_FLAVOR_VOID: BagFlashVoicingFlavor = 6;
pub(super) const BAG_VALIDATION_OK: BagValidationIssue = 0;
pub(super) const BAG_ENCODE_OPERATION_QUEUED: c_int = 0;
pub(super) const BAG_ENCODE_OPERATION_RUNNING: c_int = 1;
pub(super) const BAG_ENCODE_OPERATION_SUCCEEDED: c_int = 2;
pub(super) const BAG_ENCODE_OPERATION_FAILED: c_int = 3;
pub(super) const BAG_ENCODE_OPERATION_CANCELLED: c_int = 4;
pub(super) const BAG_ENCODE_OPERATION_PHASE_PREPARING_INPUT: c_int = 0;
pub(super) const BAG_ENCODE_OPERATION_PHASE_RENDERING_PCM: c_int = 1;
pub(super) const BAG_ENCODE_OPERATION_PHASE_POSTPROCESSING: c_int = 2;
pub(super) const BAG_ENCODE_OPERATION_PHASE_FINALIZING: c_int = 3;
pub(super) const ENCODE_OPERATION_PUMP_MAX_WORK_UNITS: u64 = 64;
pub(super) const ENCODE_OPERATION_PUMP_MAX_WALL_TIME_MS: u32 = 5;
pub(super) const ENCODE_OPERATION_IDLE_SLEEP_MS: u64 = 1;

#[repr(C)]
pub(super) struct BagEncoderConfig {
    pub(super) sample_rate_hz: i32,
    pub(super) frame_samples: i32,
    pub(super) enable_diagnostics: i32,
    pub(super) mode: BagTransportMode,
    pub(super) flash_signal_profile: BagFlashSignalProfile,
    pub(super) flash_voicing_flavor: BagFlashVoicingFlavor,
    pub(super) reserved: i32,
}

#[repr(C)]
pub(super) struct BagDecoderConfig {
    pub(super) sample_rate_hz: i32,
    pub(super) frame_samples: i32,
    pub(super) enable_diagnostics: i32,
    pub(super) mode: BagTransportMode,
    pub(super) flash_signal_profile: BagFlashSignalProfile,
    pub(super) flash_voicing_flavor: BagFlashVoicingFlavor,
    pub(super) reserved: i32,
}

#[repr(C)]
pub(super) struct BagTextResult {
    pub(super) buffer: *mut i8,
    pub(super) buffer_size: usize,
    pub(super) text_size: usize,
    pub(super) complete: i32,
    pub(super) confidence: f32,
    pub(super) mode: BagTransportMode,
}

#[repr(C)]
pub(super) struct BagPayloadFollowData {
    pub(super) byte_timeline_buffer: *mut c_void,
    pub(super) byte_timeline_buffer_count: usize,
    pub(super) byte_timeline_count: usize,
    pub(super) byte_timeline_status: BagDecodeContentStatus,
    pub(super) binary_group_timeline_buffer: *mut c_void,
    pub(super) binary_group_timeline_buffer_count: usize,
    pub(super) binary_group_timeline_count: usize,
    pub(super) binary_group_timeline_status: BagDecodeContentStatus,
    pub(super) ultra_frame_timeline_buffer: *mut c_void,
    pub(super) ultra_frame_timeline_buffer_count: usize,
    pub(super) ultra_frame_timeline_count: usize,
    pub(super) ultra_frame_timeline_status: BagDecodeContentStatus,
    pub(super) payload_begin_sample: usize,
    pub(super) payload_sample_count: usize,
    pub(super) total_pcm_sample_count: usize,
    pub(super) available: c_int,
}

#[repr(C)]
pub(super) struct BagTextFollowData {
    pub(super) text_tokens_buffer: *mut i8,
    pub(super) text_tokens_buffer_size: usize,
    pub(super) text_tokens_size: usize,
    pub(super) text_tokens_status: BagDecodeContentStatus,
    pub(super) text_token_timeline_buffer: *mut c_void,
    pub(super) text_token_timeline_buffer_count: usize,
    pub(super) text_token_timeline_count: usize,
    pub(super) text_token_timeline_status: BagDecodeContentStatus,
    pub(super) text_character_text_buffer: *mut i8,
    pub(super) text_character_text_buffer_size: usize,
    pub(super) text_character_text_size: usize,
    pub(super) text_character_text_status: BagDecodeContentStatus,
    pub(super) text_characters_buffer: *mut c_void,
    pub(super) text_characters_buffer_count: usize,
    pub(super) text_characters_count: usize,
    pub(super) text_characters_status: BagDecodeContentStatus,
    pub(super) token_raw_segments_buffer: *mut c_void,
    pub(super) token_raw_segments_buffer_count: usize,
    pub(super) token_raw_segments_count: usize,
    pub(super) token_raw_segments_status: BagDecodeContentStatus,
    pub(super) token_raw_display_units_buffer: *mut c_void,
    pub(super) token_raw_display_units_buffer_count: usize,
    pub(super) token_raw_display_units_count: usize,
    pub(super) token_raw_display_units_status: BagDecodeContentStatus,
    pub(super) lyric_lines_buffer: *mut i8,
    pub(super) lyric_lines_buffer_size: usize,
    pub(super) lyric_lines_size: usize,
    pub(super) lyric_lines_status: BagDecodeContentStatus,
    pub(super) lyric_line_timeline_buffer: *mut c_void,
    pub(super) lyric_line_timeline_buffer_count: usize,
    pub(super) lyric_line_timeline_count: usize,
    pub(super) lyric_line_timeline_status: BagDecodeContentStatus,
    pub(super) line_token_ranges_buffer: *mut c_void,
    pub(super) line_token_ranges_buffer_count: usize,
    pub(super) line_token_ranges_count: usize,
    pub(super) line_token_ranges_status: BagDecodeContentStatus,
    pub(super) line_raw_segments_buffer: *mut c_void,
    pub(super) line_raw_segments_buffer_count: usize,
    pub(super) line_raw_segments_count: usize,
    pub(super) line_raw_segments_status: BagDecodeContentStatus,
    pub(super) available: c_int,
}

#[repr(C)]
pub(super) struct BagEncodeResult {
    pub(super) samples: *mut i16,
    pub(super) sample_count: usize,
    pub(super) raw_bytes_hex_buffer: *mut i8,
    pub(super) raw_bytes_hex_buffer_size: usize,
    pub(super) raw_bytes_hex_size: usize,
    pub(super) raw_bits_binary_buffer: *mut i8,
    pub(super) raw_bits_binary_buffer_size: usize,
    pub(super) raw_bits_binary_size: usize,
    pub(super) raw_bytes_hex_status: BagDecodeContentStatus,
    pub(super) raw_bits_binary_status: BagDecodeContentStatus,
    pub(super) raw_payload_available: c_int,
    pub(super) follow_data: BagPayloadFollowData,
    pub(super) text_follow_data: BagTextFollowData,
}

#[repr(C)]
pub(super) struct BagEncodeOperationProgress {
    pub(super) state: c_int,
    pub(super) phase: c_int,
    pub(super) overall_progress_0_to_1: f32,
    pub(super) phase_progress_0_to_1: f32,
    pub(super) completed_work_units: u64,
    pub(super) total_work_units: u64,
    pub(super) phase_completed_work_units: u64,
    pub(super) phase_total_work_units: u64,
    pub(super) terminal_code: BagErrorCode,
    pub(super) estimated_pcm_sample_count: usize,
    pub(super) payload_byte_count: usize,
    pub(super) segment_count: usize,
    pub(super) current_segment_index: usize,
}

#[repr(C)]
pub(super) struct BagEncodeOperationPumpBudget {
    pub(super) max_work_units: u64,
    pub(super) max_wall_time_ms: u32,
}

#[allow(non_camel_case_types)]
pub(super) enum BagDecoder {}

#[allow(non_camel_case_types)]
pub(super) enum BagEncodeOperation {}

unsafe extern "C" {
    pub(super) fn bag_validate_encode_request(
        config: *const BagEncoderConfig,
        text: *const i8,
    ) -> BagValidationIssue;
    pub(super) fn bag_validate_decoder_config(
        config: *const BagDecoderConfig,
    ) -> BagValidationIssue;
    pub(super) fn bag_validation_issue_message(issue: BagValidationIssue) -> *const i8;
    pub(super) fn bag_error_code_message(code: BagErrorCode) -> *const i8;
    pub(super) fn bag_create_encode_operation(
        config: *const BagEncoderConfig,
        text: *const i8,
        out_operation: *mut *mut BagEncodeOperation,
    ) -> BagErrorCode;
    pub(super) fn bag_pump_encode_operation(
        operation: *mut BagEncodeOperation,
        budget: BagEncodeOperationPumpBudget,
        out_did_progress: *mut c_int,
    ) -> BagErrorCode;
    pub(super) fn bag_poll_encode_operation(
        operation: *const BagEncodeOperation,
        out_progress: *mut BagEncodeOperationProgress,
    ) -> BagErrorCode;
    pub(super) fn bag_cancel_encode_operation(operation: *mut BagEncodeOperation) -> BagErrorCode;
    pub(super) fn bag_take_encode_operation_result(
        operation: *const BagEncodeOperation,
        out_result: *mut BagEncodeResult,
    ) -> BagErrorCode;
    pub(super) fn bag_destroy_encode_operation(operation: *mut BagEncodeOperation);
    pub(super) fn bag_free_encode_result(result: *mut BagEncodeResult);
    pub(super) fn bag_create_decoder(
        config: *const BagDecoderConfig,
        out_decoder: *mut *mut BagDecoder,
    ) -> BagErrorCode;
    pub(super) fn bag_destroy_decoder(decoder: *mut BagDecoder);
    pub(super) fn bag_push_pcm(
        decoder: *mut BagDecoder,
        samples: *const i16,
        sample_count: usize,
        timestamp_ms: i64,
    ) -> BagErrorCode;
    pub(super) fn bag_poll_result(
        decoder: *mut BagDecoder,
        out_result: *mut BagTextResult,
    ) -> BagErrorCode;
    pub(super) fn bag_core_version() -> *const i8;
}
