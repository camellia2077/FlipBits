use crate::{CliError, FlashStyle, TransportMode};

use super::abi::{
    AudioIoMetadata, AudioIoMetadataFlashVoicingStyle, AudioIoMetadataMiniSpeedStyle,
    AudioIoMetadataMode, AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE,
    AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILITY, AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY,
    AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STANDARD, AUDIO_IO_METADATA_FLASH_VOICING_STYLE_VOID,
    AUDIO_IO_METADATA_FLASH_VOICING_STYLE_ZEAL, AUDIO_IO_METADATA_MINI_SPEED_STYLE_FAST,
    AUDIO_IO_METADATA_MINI_SPEED_STYLE_SLOW, AUDIO_IO_METADATA_MINI_SPEED_STYLE_STANDARD,
    AUDIO_IO_METADATA_MODE_FLASH, AUDIO_IO_METADATA_MODE_MINI, AUDIO_IO_METADATA_MODE_PRO,
    AUDIO_IO_METADATA_MODE_ULTRA,
};
use super::raw::owned_string_to_string;

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

pub(super) fn convert_metadata(raw: &AudioIoMetadata) -> Result<FlipBitsMetadata, CliError> {
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

pub(super) fn to_metadata_mode(mode: TransportMode) -> AudioIoMetadataMode {
    match mode {
        TransportMode::Flash => AUDIO_IO_METADATA_MODE_FLASH,
        TransportMode::Pro => AUDIO_IO_METADATA_MODE_PRO,
        TransportMode::Ultra => AUDIO_IO_METADATA_MODE_ULTRA,
        TransportMode::Mini => AUDIO_IO_METADATA_MODE_MINI,
    }
}

pub(super) fn from_metadata_mode(mode: AudioIoMetadataMode) -> Result<TransportMode, CliError> {
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

pub(super) fn to_flash_voicing_style(style: FlashStyle) -> AudioIoMetadataFlashVoicingStyle {
    match style {
        FlashStyle::Standard => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STANDARD,
        FlashStyle::Hostility => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILITY,
        FlashStyle::Litany => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_LITANY,
        FlashStyle::Collapse => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_COLLAPSE,
        FlashStyle::Zeal => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_ZEAL,
        FlashStyle::Void => AUDIO_IO_METADATA_FLASH_VOICING_STYLE_VOID,
    }
}

pub(super) fn from_flash_voicing_style(
    style: AudioIoMetadataFlashVoicingStyle,
) -> Result<FlashStyle, CliError> {
    match style {
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_STANDARD => Ok(FlashStyle::Standard),
        AUDIO_IO_METADATA_FLASH_VOICING_STYLE_HOSTILITY => Ok(FlashStyle::Hostility),
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

pub(super) fn to_mini_speed_style(style: MiniSpeedStyle) -> AudioIoMetadataMiniSpeedStyle {
    match style {
        MiniSpeedStyle::Slow => AUDIO_IO_METADATA_MINI_SPEED_STYLE_SLOW,
        MiniSpeedStyle::Standard => AUDIO_IO_METADATA_MINI_SPEED_STYLE_STANDARD,
        MiniSpeedStyle::Fast => AUDIO_IO_METADATA_MINI_SPEED_STYLE_FAST,
    }
}

pub(super) fn from_mini_speed_style(
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
