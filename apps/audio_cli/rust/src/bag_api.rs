use crate::util::c_str_to_string;
use crate::{
    CliError, FlashStyle, TransportMode, DEFAULT_FRAME_RATE_DIVISOR, DEFAULT_SAMPLE_RATE_HZ,
};
use std::ffi::{c_void, CString};
use std::os::raw::c_int;
use std::ptr;
use std::thread;
use std::time::Duration;

type BagErrorCode = c_int;
type BagTransportMode = c_int;
type BagFlashSignalProfile = c_int;
type BagFlashVoicingFlavor = c_int;
type BagValidationIssue = c_int;
type BagDecodeContentStatus = c_int;

const BAG_OK: BagErrorCode = 0;
const BAG_NOT_READY: BagErrorCode = 2;
const BAG_INTERNAL: BagErrorCode = 4;
const BAG_TRANSPORT_MINI: BagTransportMode = 0;
const BAG_TRANSPORT_FLASH: BagTransportMode = 1;
const BAG_TRANSPORT_PRO: BagTransportMode = 2;
const BAG_TRANSPORT_ULTRA: BagTransportMode = 3;
const BAG_FLASH_SIGNAL_PROFILE_STANDARD: BagFlashSignalProfile = 0;
const BAG_FLASH_SIGNAL_PROFILE_LITANY: BagFlashSignalProfile = 1;
const BAG_FLASH_SIGNAL_PROFILE_HOSTILITY: BagFlashSignalProfile = 3;
const BAG_FLASH_SIGNAL_PROFILE_COLLAPSE: BagFlashSignalProfile = 4;
const BAG_FLASH_SIGNAL_PROFILE_ZEAL: BagFlashSignalProfile = 5;
const BAG_FLASH_SIGNAL_PROFILE_VOID: BagFlashSignalProfile = 6;
const BAG_FLASH_VOICING_FLAVOR_STANDARD: BagFlashVoicingFlavor = 0;
const BAG_FLASH_VOICING_FLAVOR_LITANY: BagFlashVoicingFlavor = 1;
const BAG_FLASH_VOICING_FLAVOR_HOSTILITY: BagFlashVoicingFlavor = 3;
const BAG_FLASH_VOICING_FLAVOR_COLLAPSE: BagFlashVoicingFlavor = 4;
const BAG_FLASH_VOICING_FLAVOR_ZEAL: BagFlashVoicingFlavor = 5;
const BAG_FLASH_VOICING_FLAVOR_VOID: BagFlashVoicingFlavor = 6;
const BAG_VALIDATION_OK: BagValidationIssue = 0;
const BAG_ENCODE_OPERATION_QUEUED: c_int = 0;
const BAG_ENCODE_OPERATION_RUNNING: c_int = 1;
const BAG_ENCODE_OPERATION_SUCCEEDED: c_int = 2;
const BAG_ENCODE_OPERATION_FAILED: c_int = 3;
const BAG_ENCODE_OPERATION_CANCELLED: c_int = 4;
const BAG_ENCODE_OPERATION_PHASE_PREPARING_INPUT: c_int = 0;
const BAG_ENCODE_OPERATION_PHASE_RENDERING_PCM: c_int = 1;
const BAG_ENCODE_OPERATION_PHASE_POSTPROCESSING: c_int = 2;
const BAG_ENCODE_OPERATION_PHASE_FINALIZING: c_int = 3;
const ENCODE_OPERATION_PUMP_MAX_WORK_UNITS: u64 = 64;
const ENCODE_OPERATION_PUMP_MAX_WALL_TIME_MS: u32 = 5;
const ENCODE_OPERATION_IDLE_SLEEP_MS: u64 = 1;

#[repr(C)]
struct BagEncoderConfig {
    sample_rate_hz: i32,
    frame_samples: i32,
    enable_diagnostics: i32,
    mode: BagTransportMode,
    flash_signal_profile: BagFlashSignalProfile,
    flash_voicing_flavor: BagFlashVoicingFlavor,
    reserved: i32,
}

#[repr(C)]
struct BagDecoderConfig {
    sample_rate_hz: i32,
    frame_samples: i32,
    enable_diagnostics: i32,
    mode: BagTransportMode,
    flash_signal_profile: BagFlashSignalProfile,
    flash_voicing_flavor: BagFlashVoicingFlavor,
    reserved: i32,
}

#[repr(C)]
struct BagTextResult {
    buffer: *mut i8,
    buffer_size: usize,
    text_size: usize,
    complete: i32,
    confidence: f32,
    mode: BagTransportMode,
}

#[repr(C)]
struct BagPayloadFollowData {
    byte_timeline_buffer: *mut c_void,
    byte_timeline_buffer_count: usize,
    byte_timeline_count: usize,
    byte_timeline_status: BagDecodeContentStatus,
    binary_group_timeline_buffer: *mut c_void,
    binary_group_timeline_buffer_count: usize,
    binary_group_timeline_count: usize,
    binary_group_timeline_status: BagDecodeContentStatus,
    ultra_frame_timeline_buffer: *mut c_void,
    ultra_frame_timeline_buffer_count: usize,
    ultra_frame_timeline_count: usize,
    ultra_frame_timeline_status: BagDecodeContentStatus,
    payload_begin_sample: usize,
    payload_sample_count: usize,
    total_pcm_sample_count: usize,
    available: c_int,
}

#[repr(C)]
struct BagTextFollowData {
    text_tokens_buffer: *mut i8,
    text_tokens_buffer_size: usize,
    text_tokens_size: usize,
    text_tokens_status: BagDecodeContentStatus,
    text_token_timeline_buffer: *mut c_void,
    text_token_timeline_buffer_count: usize,
    text_token_timeline_count: usize,
    text_token_timeline_status: BagDecodeContentStatus,
    text_character_text_buffer: *mut i8,
    text_character_text_buffer_size: usize,
    text_character_text_size: usize,
    text_character_text_status: BagDecodeContentStatus,
    text_characters_buffer: *mut c_void,
    text_characters_buffer_count: usize,
    text_characters_count: usize,
    text_characters_status: BagDecodeContentStatus,
    token_raw_segments_buffer: *mut c_void,
    token_raw_segments_buffer_count: usize,
    token_raw_segments_count: usize,
    token_raw_segments_status: BagDecodeContentStatus,
    token_raw_display_units_buffer: *mut c_void,
    token_raw_display_units_buffer_count: usize,
    token_raw_display_units_count: usize,
    token_raw_display_units_status: BagDecodeContentStatus,
    lyric_lines_buffer: *mut i8,
    lyric_lines_buffer_size: usize,
    lyric_lines_size: usize,
    lyric_lines_status: BagDecodeContentStatus,
    lyric_line_timeline_buffer: *mut c_void,
    lyric_line_timeline_buffer_count: usize,
    lyric_line_timeline_count: usize,
    lyric_line_timeline_status: BagDecodeContentStatus,
    line_token_ranges_buffer: *mut c_void,
    line_token_ranges_buffer_count: usize,
    line_token_ranges_count: usize,
    line_token_ranges_status: BagDecodeContentStatus,
    line_raw_segments_buffer: *mut c_void,
    line_raw_segments_buffer_count: usize,
    line_raw_segments_count: usize,
    line_raw_segments_status: BagDecodeContentStatus,
    available: c_int,
}

#[repr(C)]
struct BagEncodeResult {
    samples: *mut i16,
    sample_count: usize,
    raw_bytes_hex_buffer: *mut i8,
    raw_bytes_hex_buffer_size: usize,
    raw_bytes_hex_size: usize,
    raw_bits_binary_buffer: *mut i8,
    raw_bits_binary_buffer_size: usize,
    raw_bits_binary_size: usize,
    raw_bytes_hex_status: BagDecodeContentStatus,
    raw_bits_binary_status: BagDecodeContentStatus,
    raw_payload_available: c_int,
    follow_data: BagPayloadFollowData,
    text_follow_data: BagTextFollowData,
}

#[repr(C)]
struct BagEncodeOperationProgress {
    state: c_int,
    phase: c_int,
    overall_progress_0_to_1: f32,
    phase_progress_0_to_1: f32,
    completed_work_units: u64,
    total_work_units: u64,
    phase_completed_work_units: u64,
    phase_total_work_units: u64,
    terminal_code: BagErrorCode,
    estimated_pcm_sample_count: usize,
    payload_byte_count: usize,
    segment_count: usize,
    current_segment_index: usize,
}

#[repr(C)]
struct BagEncodeOperationPumpBudget {
    max_work_units: u64,
    max_wall_time_ms: u32,
}

#[allow(non_camel_case_types)]
enum BagDecoder {}

#[allow(non_camel_case_types)]
enum BagEncodeOperation {}

unsafe extern "C" {
    fn bag_validate_encode_request(
        config: *const BagEncoderConfig,
        text: *const i8,
    ) -> BagValidationIssue;
    fn bag_validate_decoder_config(config: *const BagDecoderConfig) -> BagValidationIssue;
    fn bag_validation_issue_message(issue: BagValidationIssue) -> *const i8;
    fn bag_error_code_message(code: BagErrorCode) -> *const i8;
    fn bag_create_encode_operation(
        config: *const BagEncoderConfig,
        text: *const i8,
        out_operation: *mut *mut BagEncodeOperation,
    ) -> BagErrorCode;
    fn bag_pump_encode_operation(
        operation: *mut BagEncodeOperation,
        budget: BagEncodeOperationPumpBudget,
        out_did_progress: *mut c_int,
    ) -> BagErrorCode;
    fn bag_poll_encode_operation(
        operation: *const BagEncodeOperation,
        out_progress: *mut BagEncodeOperationProgress,
    ) -> BagErrorCode;
    fn bag_cancel_encode_operation(operation: *mut BagEncodeOperation) -> BagErrorCode;
    fn bag_take_encode_operation_result(
        operation: *const BagEncodeOperation,
        out_result: *mut BagEncodeResult,
    ) -> BagErrorCode;
    fn bag_destroy_encode_operation(operation: *mut BagEncodeOperation);
    fn bag_free_encode_result(result: *mut BagEncodeResult);
    fn bag_create_decoder(
        config: *const BagDecoderConfig,
        out_decoder: *mut *mut BagDecoder,
    ) -> BagErrorCode;
    fn bag_destroy_decoder(decoder: *mut BagDecoder);
    fn bag_push_pcm(
        decoder: *mut BagDecoder,
        samples: *const i16,
        sample_count: usize,
        timestamp_ms: i64,
    ) -> BagErrorCode;
    fn bag_poll_result(decoder: *mut BagDecoder, out_result: *mut BagTextResult) -> BagErrorCode;
    fn bag_core_version() -> *const i8;
}

struct EncodeOperationGuard(*mut BagEncodeOperation);

impl EncodeOperationGuard {
    fn create(config: &BagEncoderConfig, text: &CString) -> Result<Self, CliError> {
        let mut raw_operation = ptr::null_mut();
        let create_code = unsafe {
            // SAFETY: `config` and `text` remain valid for the duration of the FFI
            // call, and `raw_operation` points to writable storage for the handle.
            bag_create_encode_operation(config, text.as_ptr(), &mut raw_operation)
        };
        if create_code != BAG_OK || raw_operation.is_null() {
            return Err(CliError::Api(error_code_message(create_code)));
        }
        Ok(Self(raw_operation))
    }

    fn pump(&self) -> Result<bool, CliError> {
        let mut did_progress = 0;
        let code = unsafe {
            // SAFETY: `self.0` is a live operation handle and `did_progress`
            // points to writable storage for the native progress flag.
            bag_pump_encode_operation(
                self.0,
                BagEncodeOperationPumpBudget {
                    max_work_units: ENCODE_OPERATION_PUMP_MAX_WORK_UNITS,
                    max_wall_time_ms: ENCODE_OPERATION_PUMP_MAX_WALL_TIME_MS,
                },
                &mut did_progress,
            )
        };
        if code != BAG_OK {
            return Err(CliError::Api(error_code_message(code)));
        }
        Ok(did_progress != 0)
    }

    fn poll_progress(&self) -> Result<EncodeOperationProgress, CliError> {
        poll_encode_operation(self.0)
    }

    fn take_result(&self) -> Result<Vec<i16>, CliError> {
        take_encode_operation_result(self.0)
    }
}

impl Drop for EncodeOperationGuard {
    fn drop(&mut self) {
        if !self.0.is_null() {
            unsafe {
                // SAFETY: The handle is owned by this guard and is destroyed
                // exactly once. Cancellation is idempotent for terminal operations.
                bag_cancel_encode_operation(self.0);
                bag_destroy_encode_operation(self.0);
            }
        }
    }
}

struct DecoderGuard(*mut BagDecoder);

impl DecoderGuard {
    fn create(config: &BagDecoderConfig) -> Result<Self, CliError> {
        let mut decoder = ptr::null_mut();
        let create_code = unsafe {
            // SAFETY: `config` remains valid for the duration of the FFI call, and
            // `decoder` points to writable storage for the returned handle.
            bag_create_decoder(config, &mut decoder)
        };
        if create_code != BAG_OK || decoder.is_null() {
            return Err(CliError::Api(error_code_message(create_code)));
        }
        Ok(Self(decoder))
    }

    fn push_pcm(&self, pcm_samples: &[i16]) -> Result<(), CliError> {
        let push_code = unsafe {
            // SAFETY: `pcm_samples` is a valid immutable slice for the duration of
            // the call, and `self.0` is a live decoder handle owned by this guard.
            bag_push_pcm(self.0, pcm_samples.as_ptr(), pcm_samples.len(), 0)
        };
        if push_code != BAG_OK {
            return Err(CliError::Api(error_code_message(push_code)));
        }
        Ok(())
    }

    fn poll_text_result(&self, result: &mut BagTextResult) -> Result<(), CliError> {
        let poll_code = unsafe {
            // SAFETY: `self.0` is a live decoder handle and `result` points to
            // writable storage whose buffer fields reference `text_buffer`.
            bag_poll_result(self.0, result)
        };
        if poll_code != BAG_OK {
            return Err(CliError::Api(error_code_message(poll_code)));
        }
        Ok(())
    }
}

impl Drop for DecoderGuard {
    fn drop(&mut self) {
        if !self.0.is_null() {
            unsafe {
                // SAFETY: The native API requires decoder handles returned from
                // `bag_create_decoder` to be destroyed exactly once.
                bag_destroy_decoder(self.0);
            }
        }
    }
}

struct EncodeResultGuard(BagEncodeResult);

impl EncodeResultGuard {
    fn new() -> Self {
        // SAFETY: `bag_encode_result` is a C POD result descriptor made only of
        // integer fields, status codes, and nullable caller/API-owned pointers.
        Self(unsafe { std::mem::zeroed() })
    }

    fn as_mut_ptr(&mut self) -> *mut BagEncodeResult {
        &mut self.0
    }

    fn to_vec(&self) -> Vec<i16> {
        raw_slice(self.0.samples.cast_const(), self.0.sample_count).to_vec()
    }
}

impl Drop for EncodeResultGuard {
    fn drop(&mut self) {
        unsafe {
            // SAFETY: Results produced by `bag_take_encode_operation_result`
            // are released with `bag_free_encode_result`; null fields are accepted.
            bag_free_encode_result(&mut self.0);
        }
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct CodecConfig {
    pub sample_rate_hz: i32,
    pub frame_samples: i32,
    pub mode: TransportMode,
    pub flash_style: FlashStyle,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum EncodeOperationState {
    Queued,
    Running,
    Succeeded,
    Failed,
    Cancelled,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum EncodeOperationPhase {
    PreparingInput,
    RenderingPcm,
    Postprocessing,
    Finalizing,
}

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct EncodeOperationProgress {
    pub state: EncodeOperationState,
    pub phase: EncodeOperationPhase,
    pub overall_progress_0_to_1: f32,
    pub terminal_code: BagErrorCode,
}

impl CodecConfig {
    pub fn for_mode(mode: TransportMode) -> Self {
        Self {
            sample_rate_hz: DEFAULT_SAMPLE_RATE_HZ,
            frame_samples: default_frame_samples(DEFAULT_SAMPLE_RATE_HZ),
            mode,
            flash_style: FlashStyle::Standard,
        }
    }
}

pub fn core_version() -> Option<String> {
    let raw = unsafe {
        // SAFETY: The native function returns a process-lifetime version string
        // pointer or null when unavailable.
        bag_core_version()
    };
    if raw.is_null() {
        None
    } else {
        Some(c_str_to_string(raw))
    }
}

pub fn encode_text_with_progress<F>(
    config: &CodecConfig,
    text: &str,
    mut on_progress: F,
) -> Result<Vec<i16>, CliError>
where
    F: FnMut(EncodeOperationProgress),
{
    let c_text = CString::new(text)
        .map_err(|_| CliError::Api("encode text contains an interior NUL byte".to_string()))?;
    let raw_config = make_encoder_config(config);

    validate_encode_request(&raw_config, &c_text)?;
    let operation = EncodeOperationGuard::create(&raw_config, &c_text)?;
    loop {
        let progress = operation.poll_progress()?;
        on_progress(progress);
        match progress.state {
            EncodeOperationState::Queued | EncodeOperationState::Running => {
                if !operation.pump()? {
                    thread::sleep(Duration::from_millis(ENCODE_OPERATION_IDLE_SLEEP_MS));
                }
            }
            EncodeOperationState::Succeeded => {
                return operation.take_result();
            }
            EncodeOperationState::Failed | EncodeOperationState::Cancelled => {
                let code = if progress.terminal_code == BAG_NOT_READY {
                    BAG_INTERNAL
                } else {
                    progress.terminal_code
                };
                return Err(CliError::Api(error_code_message(code)));
            }
        }
    }
}

pub fn decode_pcm(config: &CodecConfig, pcm_samples: &[i16]) -> Result<String, CliError> {
    let raw_config = make_decoder_config(config);
    validate_decoder_config(&raw_config)?;
    let decoder = DecoderGuard::create(&raw_config)?;
    decoder.push_pcm(pcm_samples)?;

    let mut text_buffer = vec![0u8; pcm_samples.len().max(4096)];
    let mut result = BagTextResult {
        buffer: text_buffer.as_mut_ptr() as *mut i8,
        buffer_size: text_buffer.len(),
        text_size: 0,
        complete: 0,
        confidence: 0.0,
        mode: to_bag_mode(config.mode),
    };
    decoder.poll_text_result(&mut result)?;

    String::from_utf8(text_buffer[..result.text_size].to_vec())
        .map_err(|_| CliError::Api("decoded text is not valid UTF-8".to_string()))
}

fn default_frame_samples(sample_rate_hz: i32) -> i32 {
    sample_rate_hz / DEFAULT_FRAME_RATE_DIVISOR
}

fn make_encoder_config(config: &CodecConfig) -> BagEncoderConfig {
    let (flash_signal_profile, flash_voicing_flavor) = flash_style_pair(config.flash_style);
    BagEncoderConfig {
        sample_rate_hz: config.sample_rate_hz,
        frame_samples: config.frame_samples,
        enable_diagnostics: 0,
        mode: to_bag_mode(config.mode),
        flash_signal_profile,
        flash_voicing_flavor,
        reserved: 0,
    }
}

fn make_decoder_config(config: &CodecConfig) -> BagDecoderConfig {
    let (flash_signal_profile, flash_voicing_flavor) = flash_style_pair(config.flash_style);
    BagDecoderConfig {
        sample_rate_hz: config.sample_rate_hz,
        frame_samples: config.frame_samples,
        enable_diagnostics: 0,
        mode: to_bag_mode(config.mode),
        flash_signal_profile,
        flash_voicing_flavor,
        reserved: 0,
    }
}

fn flash_style_pair(style: FlashStyle) -> (BagFlashSignalProfile, BagFlashVoicingFlavor) {
    match style {
        FlashStyle::Standard => (
            BAG_FLASH_SIGNAL_PROFILE_STANDARD,
            BAG_FLASH_VOICING_FLAVOR_STANDARD,
        ),
        FlashStyle::Hostility => (
            BAG_FLASH_SIGNAL_PROFILE_HOSTILITY,
            BAG_FLASH_VOICING_FLAVOR_HOSTILITY,
        ),
        FlashStyle::Litany => (
            BAG_FLASH_SIGNAL_PROFILE_LITANY,
            BAG_FLASH_VOICING_FLAVOR_LITANY,
        ),
        FlashStyle::Collapse => (
            BAG_FLASH_SIGNAL_PROFILE_COLLAPSE,
            BAG_FLASH_VOICING_FLAVOR_COLLAPSE,
        ),
        FlashStyle::Zeal => (BAG_FLASH_SIGNAL_PROFILE_ZEAL, BAG_FLASH_VOICING_FLAVOR_ZEAL),
        FlashStyle::Void => (BAG_FLASH_SIGNAL_PROFILE_VOID, BAG_FLASH_VOICING_FLAVOR_VOID),
    }
}

fn to_bag_mode(mode: TransportMode) -> BagTransportMode {
    match mode {
        TransportMode::Flash => BAG_TRANSPORT_FLASH,
        TransportMode::Pro => BAG_TRANSPORT_PRO,
        TransportMode::Ultra => BAG_TRANSPORT_ULTRA,
        TransportMode::Mini => BAG_TRANSPORT_MINI,
    }
}

fn validate_encode_request(config: &BagEncoderConfig, text: &CString) -> Result<(), CliError> {
    let validation = unsafe {
        // SAFETY: `config` and `text` remain valid for the duration of the FFI
        // call and satisfy the native API's pointer requirements.
        bag_validate_encode_request(config, text.as_ptr())
    };
    if validation != BAG_VALIDATION_OK {
        return Err(CliError::Api(validation_issue_message(validation)));
    }
    Ok(())
}

fn validate_decoder_config(config: &BagDecoderConfig) -> Result<(), CliError> {
    let validation = unsafe {
        // SAFETY: `config` remains valid for the duration of the FFI call and
        // satisfies the native API's pointer requirements.
        bag_validate_decoder_config(config)
    };
    if validation != BAG_VALIDATION_OK {
        return Err(CliError::Api(validation_issue_message(validation)));
    }
    Ok(())
}

fn poll_encode_operation(
    operation: *mut BagEncodeOperation,
) -> Result<EncodeOperationProgress, CliError> {
    let mut progress = BagEncodeOperationProgress {
        state: BAG_ENCODE_OPERATION_FAILED,
        phase: BAG_ENCODE_OPERATION_PHASE_FINALIZING,
        overall_progress_0_to_1: 0.0,
        phase_progress_0_to_1: 0.0,
        completed_work_units: 0,
        total_work_units: 0,
        phase_completed_work_units: 0,
        phase_total_work_units: 0,
        terminal_code: BAG_NOT_READY,
        estimated_pcm_sample_count: 0,
        payload_byte_count: 0,
        segment_count: 0,
        current_segment_index: 0,
    };
    let code = unsafe {
        // SAFETY: `operation` is a live encode operation handle and `progress`
        // points to writable storage for the polled progress struct.
        bag_poll_encode_operation(operation, &mut progress)
    };
    if code != BAG_OK {
        return Err(CliError::Api(error_code_message(code)));
    }

    Ok(EncodeOperationProgress {
        state: match progress.state {
            BAG_ENCODE_OPERATION_QUEUED => EncodeOperationState::Queued,
            BAG_ENCODE_OPERATION_RUNNING => EncodeOperationState::Running,
            BAG_ENCODE_OPERATION_SUCCEEDED => EncodeOperationState::Succeeded,
            BAG_ENCODE_OPERATION_CANCELLED => EncodeOperationState::Cancelled,
            _ => EncodeOperationState::Failed,
        },
        phase: match progress.phase {
            BAG_ENCODE_OPERATION_PHASE_PREPARING_INPUT => EncodeOperationPhase::PreparingInput,
            BAG_ENCODE_OPERATION_PHASE_RENDERING_PCM => EncodeOperationPhase::RenderingPcm,
            BAG_ENCODE_OPERATION_PHASE_POSTPROCESSING => EncodeOperationPhase::Postprocessing,
            _ => EncodeOperationPhase::Finalizing,
        },
        overall_progress_0_to_1: progress.overall_progress_0_to_1.clamp(0.0, 1.0),
        terminal_code: progress.terminal_code,
    })
}

fn take_encode_operation_result(operation: *mut BagEncodeOperation) -> Result<Vec<i16>, CliError> {
    let mut result = EncodeResultGuard::new();
    let code = unsafe {
        // SAFETY: `operation` is a live encode operation handle and `result`
        // points to writable storage for the returned result descriptor.
        bag_take_encode_operation_result(operation, result.as_mut_ptr())
    };
    if code != BAG_OK {
        return Err(CliError::Api(error_code_message(code)));
    }
    Ok(result.to_vec())
}

fn validation_issue_message(issue: BagValidationIssue) -> String {
    let raw = unsafe {
        // SAFETY: The native function returns a process-lifetime message pointer
        // for any validation issue code.
        bag_validation_issue_message(issue)
    };
    c_str_to_string(raw)
}

fn error_code_message(code: BagErrorCode) -> String {
    let raw = unsafe {
        // SAFETY: The native function returns a process-lifetime message pointer
        // for any error code value.
        bag_error_code_message(code)
    };
    c_str_to_string(raw)
}

fn raw_slice<'a, T>(data: *const T, size: usize) -> &'a [T] {
    if data.is_null() || size == 0 {
        &[]
    } else {
        unsafe {
            // SAFETY: Callers only pass pointers/lengths obtained from the native
            // `bag_api` ABI, which guarantees readable contiguous storage for the
            // reported element count until the matching free function runs.
            std::slice::from_raw_parts(data, size)
        }
    }
}
