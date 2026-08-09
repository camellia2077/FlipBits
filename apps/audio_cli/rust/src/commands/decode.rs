use crate::audio_io_api;
use crate::bag_api;
use crate::presentation::{read_binary_file, write_text_file, RunOutput};
use crate::{CliError, DecodeArgs, FlashStyle};

pub(super) fn run(args: DecodeArgs) -> Result<RunOutput, CliError> {
    let wav_bytes = read_binary_file(&args.input)?;
    let decoded = audio_io_api::decode_mono_pcm16_wav(&wav_bytes)
        .map_err(|error| CliError::Api(format!("failed to parse WAV input: {error}")))?;
    let metadata = decoded.metadata.map_err(|error| {
        CliError::Api(format!(
            "failed to read FlipBits metadata from WAV input: {error}"
        ))
    })?;
    let config = bag_api::CodecConfig {
        sample_rate_hz: decoded.sample_rate_hz,
        frame_samples: metadata.frame_samples,
        mode: metadata.mode,
        flash_style: metadata.flash_voicing_style.unwrap_or(FlashStyle::Standard),
    };
    let text = bag_api::decode_pcm(&config, &decoded.pcm_samples).map_err(|error| {
        CliError::Api(format!(
            "failed to decode WAV payload in `{}` mode: {error}",
            metadata.mode
        ))
    })?;
    if let Some(out_text) = &args.out_text {
        write_text_file(out_text, &text)?;
    }
    Ok(RunOutput::DecodedText(text))
}
