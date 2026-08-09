use crate::audio_io_api;
use crate::bag_api;
use crate::{FlashStyle, TransportMode, CLI_PRESENTATION_VERSION};

const CLI_METADATA_CREATED_AT: &str = "1970-01-01T00:00:00Z";

pub(super) fn build_cli_metadata(
    mode: TransportMode,
    config: &bag_api::CodecConfig,
    pcm_sample_count: usize,
) -> audio_io_api::FlipBitsMetadata {
    let duration_ms = ((pcm_sample_count as u64).saturating_mul(1000)
        / config.sample_rate_hz.max(1) as u64) as u32;
    audio_io_api::FlipBitsMetadata {
        version: 7,
        mode,
        flash_voicing_style: if mode == TransportMode::Flash {
            Some(config.flash_style)
        } else {
            None
        },
        mini_speed_style: if mode == TransportMode::Mini {
            Some(mini_speed_style_for_frame_samples(
                config.frame_samples,
                config.sample_rate_hz / 20,
            ))
        } else {
            None
        },
        created_at_iso_utc: CLI_METADATA_CREATED_AT.to_string(),
        duration_ms,
        sample_rate_hz: config.sample_rate_hz,
        frame_samples: config.frame_samples,
        pcm_sample_count,
        payload_byte_count: 0,
        app_version: format!("FlipBits/{CLI_PRESENTATION_VERSION}"),
        core_version: bag_api::core_version().unwrap_or_else(|| "unknown".to_string()),
    }
}

pub(super) fn flash_style_for_mode(mode: TransportMode, requested_style: FlashStyle) -> FlashStyle {
    if mode == TransportMode::Flash {
        requested_style
    } else {
        FlashStyle::Standard
    }
}

fn mini_speed_style_for_frame_samples(
    frame_samples: i32,
    default_frame_samples: i32,
) -> audio_io_api::MiniSpeedStyle {
    let slow = ((default_frame_samples * 3) / 2).max(1);
    let standard = default_frame_samples.max(1);
    let fast = (default_frame_samples / 2).max(1);
    let distances = [
        (
            (frame_samples - slow).abs(),
            audio_io_api::MiniSpeedStyle::Slow,
        ),
        (
            (frame_samples - standard).abs(),
            audio_io_api::MiniSpeedStyle::Standard,
        ),
        (
            (frame_samples - fast).abs(),
            audio_io_api::MiniSpeedStyle::Fast,
        ),
    ];
    distances
        .into_iter()
        .min_by_key(|(distance, _)| *distance)
        .map(|(_, style)| style)
        .unwrap_or(audio_io_api::MiniSpeedStyle::Standard)
}
