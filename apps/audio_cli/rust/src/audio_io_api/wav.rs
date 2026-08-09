use crate::util::c_str_to_string;
use crate::CliError;

use super::abi::{
    audio_io_decode_mono_pcm16_wav, audio_io_encode_mono_pcm16_wav_with_metadata,
    audio_io_metadata_status_message, audio_io_wav_status_message, AudioIoMetadataStatus,
    AudioIoMetadataView, AudioIoStringView, AudioIoWavStatus,
    AUDIO_IO_METADATA_INPUT_SOURCE_KIND_MANUAL, AUDIO_IO_METADATA_OK, AUDIO_IO_WAV_OK,
};
#[cfg(test)]
use super::abi::{audio_io_free_metadata, AudioIoMetadata, AudioIoOwnedString};
use super::guards::{ByteBufferGuard, DecodedWavGuard};
use super::metadata::{
    to_flash_voicing_style, to_metadata_mode, to_mini_speed_style, FlipBitsMetadata,
};
use std::os::raw::c_char;
use std::ptr;

#[derive(Debug)]
pub struct DecodedWav {
    pub sample_rate_hz: i32,
    pub pcm_samples: Vec<i16>,
    pub metadata: Result<FlipBitsMetadata, String>,
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
