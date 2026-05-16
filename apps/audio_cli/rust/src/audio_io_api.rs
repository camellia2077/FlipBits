use crate::util::c_str_to_string;
use crate::{CliError, FlashStyle, TransportMode};
use std::os::raw::{c_char, c_int};
use std::ptr;

type AudioIoWavStatus = c_int;
type AudioIoMetadataStatus = c_int;
type AudioIoMetadataMode = c_int;
type AudioIoMetadataFlashVoicingStyle = c_int;
type AudioIoMetadataMiniSpeedStyle = c_int;
type AudioIoMetadataInputSourceKind = c_int;

const AUDIO_IO_WAV_OK: AudioIoWavStatus = 0;
const AUDIO_IO_METADATA_OK: AudioIoMetadataStatus = 0;
const AUDIO_IO_METADATA_MODE_MINI: AudioIoMetadataMode = 1;
const AUDIO_IO_METADATA_MODE_FLASH: AudioIoMetadataMode = 2;
const AUDIO_IO_METADATA_MODE_PRO: AudioIoMetadataMode = 3;
const AUDIO_IO_METADATA_MODE_ULTRA: AudioIoMetadataMode = 4;
const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STANDARD: AudioIoMetadataFlashVoicingStyle = 1;
const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY: AudioIoMetadataFlashVoicingStyle = 2;
const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILE: AudioIoMetadataFlashVoicingStyle = 4;
const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE: AudioIoMetadataFlashVoicingStyle = 5;
const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_ZEAL: AudioIoMetadataFlashVoicingStyle = 6;
const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_VOID: AudioIoMetadataFlashVoicingStyle = 7;
const AUDIO_IO_METADATA_MINI_SPEED_STYLE_SLOW: AudioIoMetadataMiniSpeedStyle = 1;
const AUDIO_IO_METADATA_MINI_SPEED_STYLE_STANDARD: AudioIoMetadataMiniSpeedStyle = 2;
const AUDIO_IO_METADATA_MINI_SPEED_STYLE_FAST: AudioIoMetadataMiniSpeedStyle = 3;
const AUDIO_IO_METADATA_INPUT_SOURCE_KIND_MANUAL: AudioIoMetadataInputSourceKind = 1;

#[repr(C)]
struct AudioIoStringView {
    data: *const c_char,
    size: usize,
}

#[repr(C)]
struct AudioIoOwnedString {
    data: *mut c_char,
    size: usize,
}

#[repr(C)]
struct AudioIoByteBuffer {
    data: *mut u8,
    size: usize,
}

#[repr(C)]
struct AudioIoMetadataView {
    version: u8,
    mode: AudioIoMetadataMode,
    has_flash_voicing_style: u8,
    flash_voicing_style: AudioIoMetadataFlashVoicingStyle,
    has_mini_speed_style: u8,
    mini_speed_style: AudioIoMetadataMiniSpeedStyle,
    created_at_iso_utc: AudioIoStringView,
    duration_ms: u32,
    sample_rate_hz: u32,
    frame_samples: u32,
    pcm_sample_count: u32,
    payload_byte_count: u32,
    input_source_kind: AudioIoMetadataInputSourceKind,
    segment_count: u32,
    // Keep the Rust FFI layout aligned with the native audio_io C ABI even
    // when the CLI itself only writes single-segment metadata today.
    segment_sample_counts: *const u32,
    segment_sample_count_count: usize,
    app_version: AudioIoStringView,
    core_version: AudioIoStringView,
}

#[repr(C)]
struct AudioIoMetadata {
    version: u8,
    mode: AudioIoMetadataMode,
    has_flash_voicing_style: u8,
    flash_voicing_style: AudioIoMetadataFlashVoicingStyle,
    has_mini_speed_style: u8,
    mini_speed_style: AudioIoMetadataMiniSpeedStyle,
    created_at_iso_utc: AudioIoOwnedString,
    duration_ms: u32,
    sample_rate_hz: u32,
    frame_samples: u32,
    pcm_sample_count: u32,
    payload_byte_count: u32,
    input_source_kind: AudioIoMetadataInputSourceKind,
    segment_count: u32,
    segment_sample_counts: *mut u32,
    segment_sample_count_count: usize,
    app_version: AudioIoOwnedString,
    core_version: AudioIoOwnedString,
}

#[repr(C)]
struct AudioIoDecodedWav {
    sample_rate_hz: i32,
    channels: i32,
    samples: *mut i16,
    sample_count: usize,
    metadata_status: AudioIoMetadataStatus,
    metadata: AudioIoMetadata,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct FlipBitsMetadata {
    pub version: u8,
    pub mode: TransportMode,
    pub flash_voicing_style: Option<FlashStyle>,
    pub mini_speed_style: Option<MiniSpeedStyle>,
    pub created_at_iso_utc: String,
    pub duration_ms: u32,
    pub sample_rate_hz: i32,
    pub frame_samples: i32,
    pub pcm_sample_count: usize,
    pub payload_byte_count: u32,
    pub app_version: String,
    pub core_version: String,
}

#[derive(Debug)]
pub struct DecodedWav {
    pub sample_rate_hz: i32,
    pub pcm_samples: Vec<i16>,
    pub metadata: Result<FlipBitsMetadata, String>,
}

unsafe extern "C" {
    fn audio_io_encode_mono_pcm16_wav_with_metadata(
        sample_rate_hz: i32,
        pcm: *const i16,
        sample_count: usize,
        metadata: *const AudioIoMetadataView,
        out_wav_bytes: *mut AudioIoByteBuffer,
    ) -> AudioIoWavStatus;
    fn audio_io_decode_mono_pcm16_wav(
        wav_bytes: *const u8,
        wav_byte_count: usize,
        out_result: *mut AudioIoDecodedWav,
    ) -> AudioIoWavStatus;
    fn audio_io_wav_status_message(status: AudioIoWavStatus) -> *const i8;
    fn audio_io_metadata_status_message(status: AudioIoMetadataStatus) -> *const i8;
    fn audio_io_free_byte_buffer(buffer: *mut AudioIoByteBuffer);
    #[cfg(test)]
    fn audio_io_free_metadata(metadata: *mut AudioIoMetadata);
    fn audio_io_free_decoded_wav(decoded: *mut AudioIoDecodedWav);
}

struct ByteBufferGuard(AudioIoByteBuffer);

impl ByteBufferGuard {
    fn new() -> Self {
        Self(AudioIoByteBuffer {
            data: ptr::null_mut(),
            size: 0,
        })
    }

    fn as_mut_ptr(&mut self) -> *mut AudioIoByteBuffer {
        &mut self.0
    }

    fn to_vec(&self) -> Vec<u8> {
        raw_bytes_to_vec(self.0.data.cast_const(), self.0.size)
    }
}

impl Drop for ByteBufferGuard {
    fn drop(&mut self) {
        unsafe {
            // SAFETY: The native API documents that output buffers returned through
            // `audio_io_encode_mono_pcm16_wav_with_metadata` are released with
            // `audio_io_free_byte_buffer`. Null/empty buffers are accepted.
            audio_io_free_byte_buffer(&mut self.0);
        }
    }
}

struct DecodedWavGuard(AudioIoDecodedWav);

impl DecodedWavGuard {
    fn new() -> Self {
        Self(AudioIoDecodedWav {
            sample_rate_hz: 0,
            channels: 1,
            samples: ptr::null_mut(),
            sample_count: 0,
            metadata_status: 1,
            metadata: AudioIoMetadata {
                version: 0,
                mode: 0,
                has_flash_voicing_style: 0,
                flash_voicing_style: 0,
                has_mini_speed_style: 0,
                mini_speed_style: 0,
                created_at_iso_utc: AudioIoOwnedString {
                    data: ptr::null_mut(),
                    size: 0,
                },
                duration_ms: 0,
                sample_rate_hz: 0,
                frame_samples: 0,
                pcm_sample_count: 0,
                payload_byte_count: 0,
                input_source_kind: 0,
                segment_count: 1,
                segment_sample_counts: ptr::null_mut(),
                segment_sample_count_count: 0,
                app_version: AudioIoOwnedString {
                    data: ptr::null_mut(),
                    size: 0,
                },
                core_version: AudioIoOwnedString {
                    data: ptr::null_mut(),
                    size: 0,
                },
            },
        })
    }

    fn as_mut_ptr(&mut self) -> *mut AudioIoDecodedWav {
        &mut self.0
    }

    fn sample_rate_hz(&self) -> i32 {
        self.0.sample_rate_hz
    }

    fn pcm_samples(&self) -> Vec<i16> {
        raw_slice_to_vec(self.0.samples.cast_const(), self.0.sample_count)
    }

    fn metadata(&self) -> Result<FlipBitsMetadata, CliError> {
        convert_metadata(&self.0.metadata)
    }

    fn metadata_status(&self) -> AudioIoMetadataStatus {
        self.0.metadata_status
    }
}

impl Drop for DecodedWavGuard {
    fn drop(&mut self) {
        unsafe {
            // SAFETY: The native API initializes `AudioIoDecodedWav` outputs for
            // `audio_io_decode_mono_pcm16_wav` and requires cleanup with
            // `audio_io_free_decoded_wav`. The free function tolerates the zeroed
            // / null state used for initialization.
            audio_io_free_decoded_wav(&mut self.0);
        }
    }
}

pub fn encode_mono_pcm16_wav_with_metadata(
    sample_rate_hz: i32,
    pcm_samples: &[i16],
    metadata: &FlipBitsMetadata,
) -> Result<Vec<u8>, CliError> {
    let created_at_bytes = metadata.created_at_iso_utc.as_bytes();
    let app_version_bytes = metadata.app_version.as_bytes();
    let core_version_bytes = metadata.core_version.as_bytes();
    let raw_metadata = AudioIoMetadataView {
        version: metadata.version,
        mode: to_metadata_mode(metadata.mode),
        has_flash_voicing_style: metadata.flash_voicing_style.is_some() as u8,
        flash_voicing_style: metadata
            .flash_voicing_style
            .map(to_flash_voicing_style)
            .unwrap_or(0),
        has_mini_speed_style: metadata.mini_speed_style.is_some() as u8,
        mini_speed_style: metadata
            .mini_speed_style
            .map(to_mini_speed_style)
            .unwrap_or(0),
        created_at_iso_utc: AudioIoStringView {
            data: created_at_bytes.as_ptr() as *const c_char,
            size: created_at_bytes.len(),
        },
        duration_ms: metadata.duration_ms,
        sample_rate_hz: metadata.sample_rate_hz as u32,
        frame_samples: metadata.frame_samples as u32,
        pcm_sample_count: metadata.pcm_sample_count as u32,
        payload_byte_count: metadata.payload_byte_count,
        input_source_kind: AUDIO_IO_METADATA_INPUT_SOURCE_KIND_MANUAL,
        segment_count: 1,
        segment_sample_counts: ptr::null(),
        segment_sample_count_count: 0,
        app_version: AudioIoStringView {
            data: app_version_bytes.as_ptr() as *const c_char,
            size: app_version_bytes.len(),
        },
        core_version: AudioIoStringView {
            data: core_version_bytes.as_ptr() as *const c_char,
            size: core_version_bytes.len(),
        },
    };
    let mut out = ByteBufferGuard::new();
    let status = unsafe {
        // SAFETY: `pcm_samples` and the metadata string slices stay alive for the
        // duration of the FFI call, and `out` points to writable storage for the
        // native output buffer descriptor.
        audio_io_encode_mono_pcm16_wav_with_metadata(
            sample_rate_hz,
            pcm_samples.as_ptr(),
            pcm_samples.len(),
            &raw_metadata,
            out.as_mut_ptr(),
        )
    };
    if status != AUDIO_IO_WAV_OK {
        return Err(CliError::Api(wav_status_message(status)));
    }

    Ok(out.to_vec())
}

pub fn decode_mono_pcm16_wav(wav_bytes: &[u8]) -> Result<DecodedWav, CliError> {
    let mut out = DecodedWavGuard::new();
    let wav_status = unsafe {
        // SAFETY: `wav_bytes` is a valid immutable byte slice for the duration of
        // the call, and `out` points to writable storage for the native decoded
        // result descriptor.
        audio_io_decode_mono_pcm16_wav(wav_bytes.as_ptr(), wav_bytes.len(), out.as_mut_ptr())
    };
    if wav_status != AUDIO_IO_WAV_OK {
        return Err(CliError::Api(wav_status_message(wav_status)));
    }

    let pcm_samples = out.pcm_samples();
    let metadata = if out.metadata_status() == AUDIO_IO_METADATA_OK {
        Ok(out.metadata()?)
    } else {
        Err(metadata_status_message(out.metadata_status()))
    };
    let decoded = DecodedWav {
        sample_rate_hz: out.sample_rate_hz(),
        pcm_samples,
        metadata,
    };
    Ok(decoded)
}

#[cfg(test)]
pub fn free_empty_metadata_for_contract_test() {
    let mut metadata = AudioIoMetadata {
        version: 0,
        mode: 0,
        has_flash_voicing_style: 0,
        flash_voicing_style: 0,
        has_mini_speed_style: 0,
        mini_speed_style: 0,
        created_at_iso_utc: AudioIoOwnedString {
            data: ptr::null_mut(),
            size: 0,
        },
        duration_ms: 0,
        sample_rate_hz: 0,
        frame_samples: 0,
        pcm_sample_count: 0,
        payload_byte_count: 0,
        input_source_kind: 0,
        segment_count: 1,
        segment_sample_counts: ptr::null_mut(),
        segment_sample_count_count: 0,
        app_version: AudioIoOwnedString {
            data: ptr::null_mut(),
            size: 0,
        },
        core_version: AudioIoOwnedString {
            data: ptr::null_mut(),
            size: 0,
        },
    };
    unsafe {
        audio_io_free_metadata(&mut metadata);
    }
}

fn convert_metadata(raw: &AudioIoMetadata) -> Result<FlipBitsMetadata, CliError> {
    Ok(FlipBitsMetadata {
        version: raw.version,
        mode: from_metadata_mode(raw.mode)?,
        flash_voicing_style: if raw.has_flash_voicing_style != 0 {
            Some(from_flash_voicing_style(raw.flash_voicing_style)?)
        } else {
            None
        },
        mini_speed_style: if raw.has_mini_speed_style != 0 {
            Some(from_mini_speed_style(raw.mini_speed_style)?)
        } else {
            None
        },
        created_at_iso_utc: owned_string_to_string(&raw.created_at_iso_utc),
        duration_ms: raw.duration_ms,
        sample_rate_hz: raw.sample_rate_hz as i32,
        frame_samples: raw.frame_samples as i32,
        pcm_sample_count: raw.pcm_sample_count as usize,
        payload_byte_count: raw.payload_byte_count,
        app_version: owned_string_to_string(&raw.app_version),
        core_version: owned_string_to_string(&raw.core_version),
    })
}

fn owned_string_to_string(raw: &AudioIoOwnedString) -> String {
    if raw.data.is_null() || raw.size == 0 {
        String::new()
    } else {
        let bytes = raw_slice(raw.data.cast_const().cast::<u8>(), raw.size);
        String::from_utf8_lossy(bytes).into_owned()
    }
}

fn wav_status_message(status: AudioIoWavStatus) -> String {
    let raw = unsafe {
        // SAFETY: The native function returns a process-lifetime message pointer
        // for any status code value.
        audio_io_wav_status_message(status)
    };
    c_str_to_string(raw)
}

fn metadata_status_message(status: AudioIoMetadataStatus) -> String {
    let raw = unsafe {
        // SAFETY: The native function returns a process-lifetime message pointer
        // for any metadata status code value.
        audio_io_metadata_status_message(status)
    };
    c_str_to_string(raw)
}

fn raw_bytes_to_vec(data: *const u8, size: usize) -> Vec<u8> {
    raw_slice(data, size).to_vec()
}

fn raw_slice_to_vec<T: Copy>(data: *const T, size: usize) -> Vec<T> {
    raw_slice(data, size).to_vec()
}

fn raw_slice<'a, T>(data: *const T, size: usize) -> &'a [T] {
    if data.is_null() || size == 0 {
        &[]
    } else {
        unsafe {
            // SAFETY: Callers only pass pointers/lengths obtained from the native
            // `audio_io` API, which guarantees readable contiguous storage for the
            // reported element count until the matching free function runs.
            std::slice::from_raw_parts(data, size)
        }
    }
}

fn to_metadata_mode(mode: TransportMode) -> AudioIoMetadataMode {
    match mode {
        TransportMode::Flash => AUDIO_IO_METADATA_MODE_FLASH,
        TransportMode::Pro => AUDIO_IO_METADATA_MODE_PRO,
        TransportMode::Ultra => AUDIO_IO_METADATA_MODE_ULTRA,
        TransportMode::Mini => AUDIO_IO_METADATA_MODE_MINI,
    }
}

fn from_metadata_mode(mode: AudioIoMetadataMode) -> Result<TransportMode, CliError> {
    match mode {
        AUDIO_IO_METADATA_MODE_FLASH => Ok(TransportMode::Flash),
        AUDIO_IO_METADATA_MODE_PRO => Ok(TransportMode::Pro),
        AUDIO_IO_METADATA_MODE_ULTRA => Ok(TransportMode::Ultra),
        AUDIO_IO_METADATA_MODE_MINI => Ok(TransportMode::Mini),
        _ => Err(CliError::Api(
            "WAV metadata contained an unknown transport mode".to_string(),
        )),
    }
}

fn to_flash_voicing_style(style: FlashStyle) -> AudioIoMetadataFlashVoicingStyle {
    match style {
        FlashStyle::Standard => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STANDARD,
        FlashStyle::Hostile => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILE,
        FlashStyle::Litany => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY,
        FlashStyle::Collapse => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE,
        FlashStyle::Zeal => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_ZEAL,
        FlashStyle::Void => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_VOID,
    }
}

fn from_flash_voicing_style(
    style: AudioIoMetadataFlashVoicingStyle,
) -> Result<FlashStyle, CliError> {
    match style {
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STANDARD => Ok(FlashStyle::Standard),
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILE => Ok(FlashStyle::Hostile),
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY => Ok(FlashStyle::Litany),
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE => Ok(FlashStyle::Collapse),
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_ZEAL => Ok(FlashStyle::Zeal),
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_VOID => Ok(FlashStyle::Void),
        _ => Err(CliError::Api(
            "WAV metadata contained an unknown flash voicing style".to_string(),
        )),
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum MiniSpeedStyle {
    Slow,
    Standard,
    Fast,
}

fn to_mini_speed_style(style: MiniSpeedStyle) -> AudioIoMetadataMiniSpeedStyle {
    match style {
        MiniSpeedStyle::Slow => AUDIO_IO_METADATA_MINI_SPEED_STYLE_SLOW,
        MiniSpeedStyle::Standard => AUDIO_IO_METADATA_MINI_SPEED_STYLE_STANDARD,
        MiniSpeedStyle::Fast => AUDIO_IO_METADATA_MINI_SPEED_STYLE_FAST,
    }
}

fn from_mini_speed_style(
    style: AudioIoMetadataMiniSpeedStyle,
) -> Result<MiniSpeedStyle, CliError> {
    match style {
        AUDIO_IO_METADATA_MINI_SPEED_STYLE_SLOW => Ok(MiniSpeedStyle::Slow),
        AUDIO_IO_METADATA_MINI_SPEED_STYLE_STANDARD => Ok(MiniSpeedStyle::Standard),
        AUDIO_IO_METADATA_MINI_SPEED_STYLE_FAST => Ok(MiniSpeedStyle::Fast),
        _ => Err(CliError::Api(
            "WAV metadata contained an unknown mini speed style".to_string(),
        )),
    }
}
