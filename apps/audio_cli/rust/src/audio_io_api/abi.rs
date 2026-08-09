use std::os::raw::{c_char, c_int};

pub(super) type AudioIoWavStatus = c_int;
pub(super) type AudioIoMetadataStatus = c_int;
pub(super) type AudioIoMetadataMode = c_int;
pub(super) type AudioIoMetadataFlashVoicingStyle = c_int;
pub(super) type AudioIoMetadataMiniSpeedStyle = c_int;
pub(super) type AudioIoMetadataInputSourceKind = c_int;

pub(super) const AUDIO_IO_WAV_OK: AudioIoWavStatus = 0;
pub(super) const AUDIO_IO_METADATA_OK: AudioIoMetadataStatus = 0;
pub(super) const AUDIO_IO_METADATA_MODE_MINI: AudioIoMetadataMode = 1;
pub(super) const AUDIO_IO_METADATA_MODE_FLASH: AudioIoMetadataMode = 2;
pub(super) const AUDIO_IO_METADATA_MODE_PRO: AudioIoMetadataMode = 3;
pub(super) const AUDIO_IO_METADATA_MODE_ULTRA: AudioIoMetadataMode = 4;
pub(super) const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STANDARD: AudioIoMetadataFlashVoicingStyle =
    1;
pub(super) const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY: AudioIoMetadataFlashVoicingStyle = 2;
pub(super) const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILITY: AudioIoMetadataFlashVoicingStyle =
    4;
pub(super) const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE: AudioIoMetadataFlashVoicingStyle =
    5;
pub(super) const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_ZEAL: AudioIoMetadataFlashVoicingStyle = 6;
pub(super) const AUDIO_IO_METADATA_FLASH_VOICING_STYLE_VOID: AudioIoMetadataFlashVoicingStyle = 7;
pub(super) const AUDIO_IO_METADATA_MINI_SPEED_STYLE_SLOW: AudioIoMetadataMiniSpeedStyle = 1;
pub(super) const AUDIO_IO_METADATA_MINI_SPEED_STYLE_STANDARD: AudioIoMetadataMiniSpeedStyle = 2;
pub(super) const AUDIO_IO_METADATA_MINI_SPEED_STYLE_FAST: AudioIoMetadataMiniSpeedStyle = 3;
pub(super) const AUDIO_IO_METADATA_INPUT_SOURCE_KIND_MANUAL: AudioIoMetadataInputSourceKind = 1;

#[repr(C)]
pub(super) struct AudioIoStringView {
    pub(super) data: *const c_char,
    pub(super) size: usize,
}

#[repr(C)]
pub(super) struct AudioIoOwnedString {
    pub(super) data: *mut c_char,
    pub(super) size: usize,
}

#[repr(C)]
pub(super) struct AudioIoByteBuffer {
    pub(super) data: *mut u8,
    pub(super) size: usize,
}

#[repr(C)]
pub(super) struct AudioIoMetadataView {
    pub(super) version: u8,
    pub(super) mode: AudioIoMetadataMode,
    pub(super) has_flash_voicing_style: u8,
    pub(super) flash_voicing_style: AudioIoMetadataFlashVoicingStyle,
    pub(super) has_mini_speed_style: u8,
    pub(super) mini_speed_style: AudioIoMetadataMiniSpeedStyle,
    pub(super) created_at_iso_utc: AudioIoStringView,
    pub(super) duration_ms: u32,
    pub(super) sample_rate_hz: u32,
    pub(super) frame_samples: u32,
    pub(super) pcm_sample_count: u32,
    pub(super) payload_byte_count: u32,
    pub(super) input_source_kind: AudioIoMetadataInputSourceKind,
    pub(super) segment_count: u32,
    // Keep the Rust FFI layout aligned with the native audio_io C ABI even
    // when the CLI itself only writes single-segment metadata today.
    pub(super) segment_sample_counts: *const u32,
    pub(super) segment_sample_count_count: usize,
    pub(super) app_version: AudioIoStringView,
    pub(super) core_version: AudioIoStringView,
}

#[repr(C)]
pub(super) struct AudioIoMetadata {
    pub(super) version: u8,
    pub(super) mode: AudioIoMetadataMode,
    pub(super) has_flash_voicing_style: u8,
    pub(super) flash_voicing_style: AudioIoMetadataFlashVoicingStyle,
    pub(super) has_mini_speed_style: u8,
    pub(super) mini_speed_style: AudioIoMetadataMiniSpeedStyle,
    pub(super) created_at_iso_utc: AudioIoOwnedString,
    pub(super) duration_ms: u32,
    pub(super) sample_rate_hz: u32,
    pub(super) frame_samples: u32,
    pub(super) pcm_sample_count: u32,
    pub(super) payload_byte_count: u32,
    pub(super) input_source_kind: AudioIoMetadataInputSourceKind,
    pub(super) segment_count: u32,
    pub(super) segment_sample_counts: *mut u32,
    pub(super) segment_sample_count_count: usize,
    pub(super) app_version: AudioIoOwnedString,
    pub(super) core_version: AudioIoOwnedString,
}

#[repr(C)]
pub(super) struct AudioIoDecodedWav {
    pub(super) sample_rate_hz: i32,
    pub(super) channels: i32,
    pub(super) samples: *mut i16,
    pub(super) sample_count: usize,
    pub(super) metadata_status: AudioIoMetadataStatus,
    pub(super) metadata: AudioIoMetadata,
}

unsafe extern "C" {
    pub(super) fn audio_io_encode_mono_pcm16_wav_with_metadata(
        sample_rate_hz: i32,
        pcm: *const i16,
        sample_count: usize,
        metadata: *const AudioIoMetadataView,
        out_wav_bytes: *mut AudioIoByteBuffer,
    ) -> AudioIoWavStatus;
    pub(super) fn audio_io_decode_mono_pcm16_wav(
        wav_bytes: *const u8,
        wav_byte_count: usize,
        out_result: *mut AudioIoDecodedWav,
    ) -> AudioIoWavStatus;
    pub(super) fn audio_io_wav_status_message(status: AudioIoWavStatus) -> *const i8;
    pub(super) fn audio_io_metadata_status_message(status: AudioIoMetadataStatus) -> *const i8;
    pub(super) fn audio_io_free_byte_buffer(buffer: *mut AudioIoByteBuffer);
    #[cfg(test)]
    pub(super) fn audio_io_free_metadata(metadata: *mut AudioIoMetadata);
    pub(super) fn audio_io_free_decoded_wav(decoded: *mut AudioIoDecodedWav);
}
