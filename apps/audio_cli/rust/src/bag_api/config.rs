use crate::{FlashStyle, TransportMode, DEFAULT_FRAME_RATE_DIVISOR, DEFAULT_SAMPLE_RATE_HZ};

use super::abi::{
    BagDecoderConfig, BagEncoderConfig, BagErrorCode, BagFlashSignalProfile, BagFlashVoicingFlavor,
    BagTransportMode, BAG_FLASH_SIGNAL_PROFILE_COLLAPSE, BAG_FLASH_SIGNAL_PROFILE_HOSTILITY,
    BAG_FLASH_SIGNAL_PROFILE_LITANY, BAG_FLASH_SIGNAL_PROFILE_STANDARD,
    BAG_FLASH_SIGNAL_PROFILE_VOID, BAG_FLASH_SIGNAL_PROFILE_ZEAL,
    BAG_FLASH_VOICING_FLAVOR_COLLAPSE, BAG_FLASH_VOICING_FLAVOR_HOSTILITY,
    BAG_FLASH_VOICING_FLAVOR_LITANY, BAG_FLASH_VOICING_FLAVOR_STANDARD,
    BAG_FLASH_VOICING_FLAVOR_VOID, BAG_FLASH_VOICING_FLAVOR_ZEAL, BAG_TRANSPORT_FLASH,
    BAG_TRANSPORT_MINI, BAG_TRANSPORT_PRO, BAG_TRANSPORT_ULTRA,
};

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

pub(super) fn default_frame_samples(sample_rate_hz: i32) -> i32 {
    sample_rate_hz / DEFAULT_FRAME_RATE_DIVISOR
}

pub(super) fn make_encoder_config(config: &CodecConfig) -> BagEncoderConfig {
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

pub(super) fn make_decoder_config(config: &CodecConfig) -> BagDecoderConfig {
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

pub(super) fn flash_style_pair(
    style: FlashStyle,
) -> (BagFlashSignalProfile, BagFlashVoicingFlavor) {
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

pub(super) fn to_bag_mode(mode: TransportMode) -> BagTransportMode {
    match mode {
        TransportMode::Flash => BAG_TRANSPORT_FLASH,
        TransportMode::Pro => BAG_TRANSPORT_PRO,
        TransportMode::Ultra => BAG_TRANSPORT_ULTRA,
        TransportMode::Mini => BAG_TRANSPORT_MINI,
    }
}
