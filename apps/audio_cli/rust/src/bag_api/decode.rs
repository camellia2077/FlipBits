use crate::CliError;

use super::abi::{bag_validate_decoder_config, BagDecoderConfig, BagTextResult, BAG_VALIDATION_OK};
use super::config::{make_decoder_config, to_bag_mode, CodecConfig};
use super::errors::validation_issue_message;
use super::guards::DecoderGuard;

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

pub(super) fn validate_decoder_config(config: &BagDecoderConfig) -> Result<(), CliError> {
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
