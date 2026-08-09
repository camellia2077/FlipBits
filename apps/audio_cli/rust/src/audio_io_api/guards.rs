use crate::CliError;

use super::abi::{
    audio_io_free_byte_buffer, audio_io_free_decoded_wav, AudioIoByteBuffer, AudioIoDecodedWav,
    AudioIoMetadata, AudioIoMetadataStatus, AudioIoOwnedString,
};
use super::metadata::{convert_metadata, FlipBitsMetadata};
use super::raw::{raw_bytes_to_vec, raw_slice_to_vec};
use std::ptr;

pub(super) struct ByteBufferGuard(AudioIoByteBuffer);

impl ByteBufferGuard {
    pub(super) fn new() -> Self {
        Self(AudioIoByteBuffer {
            data: ptr::null_mut(),
            size: 0,
        })
    }

    pub(super) fn as_mut_ptr(&mut self) -> *mut AudioIoByteBuffer {
        &mut self.0
    }

    pub(super) fn to_vec(&self) -> Vec<u8> {
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

pub(super) struct DecodedWavGuard(AudioIoDecodedWav);

impl DecodedWavGuard {
    pub(super) fn new() -> Self {
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

    pub(super) fn as_mut_ptr(&mut self) -> *mut AudioIoDecodedWav {
        &mut self.0
    }

    pub(super) fn sample_rate_hz(&self) -> i32 {
        self.0.sample_rate_hz
    }

    pub(super) fn pcm_samples(&self) -> Vec<i16> {
        raw_slice_to_vec(self.0.samples.cast_const(), self.0.sample_count)
    }

    pub(super) fn metadata(&self) -> Result<FlipBitsMetadata, CliError> {
        convert_metadata(&self.0.metadata)
    }

    pub(super) fn metadata_status(&self) -> AudioIoMetadataStatus {
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
