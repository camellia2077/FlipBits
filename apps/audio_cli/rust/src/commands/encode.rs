use super::metadata::{build_cli_metadata, flash_style_for_mode};
use crate::audio_io_api;
use crate::bag_api;
use crate::presentation::{resolve_encode_text, write_binary_file, EncodeProgressBar, RunOutput};
use crate::{CliError, EncodeArgs};

pub(super) fn run(args: EncodeArgs) -> Result<RunOutput, CliError> {
    let text = resolve_encode_text(&args)?;
    let mut config = bag_api::CodecConfig::for_mode(args.mode);
    config.flash_style = flash_style_for_mode(args.mode, args.flash_style);
    let mut progress_bar = EncodeProgressBar::new();
    let pcm_samples = bag_api::encode_text_with_progress(&config, &text, |progress| {
        progress_bar.update(args.mode, progress);
    });
    progress_bar.finish();
    let pcm_samples = pcm_samples.map_err(|error| {
        CliError::Api(format!(
            "failed to encode `{}` text payload: {error}",
            args.mode
        ))
    })?;
    let metadata = build_cli_metadata(args.mode, &config, pcm_samples.len());
    let wav_bytes = audio_io_api::encode_mono_pcm16_wav_with_metadata(
        config.sample_rate_hz,
        &pcm_samples,
        &metadata,
    )
    .map_err(|error| {
        CliError::Api(format!(
            "failed to serialize `{}` PCM into WAV: {error}",
            args.mode
        ))
    })?;
    write_binary_file(&args.out, &wav_bytes, "failed to write WAV output")?;
    Ok(RunOutput::Message(format!(
        "Output WAV: {}\nMode: {}\nFormat: mono PCM16 WAV",
        args.out.display(),
        args.mode
    )))
}
