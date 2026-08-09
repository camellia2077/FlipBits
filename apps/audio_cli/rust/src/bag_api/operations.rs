use crate::CliError;

use super::abi::{
    bag_validate_encode_request, BagEncoderConfig, BAG_INTERNAL, BAG_NOT_READY, BAG_VALIDATION_OK,
    ENCODE_OPERATION_IDLE_SLEEP_MS,
};
use super::config::{
    make_encoder_config, CodecConfig, EncodeOperationProgress, EncodeOperationState,
};
use super::errors::{error_code_message, validation_issue_message};
use super::guards::EncodeOperationGuard;
use std::ffi::CString;
use std::thread;
use std::time::Duration;

pub fn encode_text_with_progress<F>(
    config: &CodecConfig,
    text: &str,
    mut on_progress: F,
) -> Result<Vec<i16>, CliError>
where
    F: FnMut(EncodeOperationProgress),
{
    let c_text = CString::new(text)
        .map_err(|_| CliError::Api("encode text contains an interior NUL byte".to_string()))?;
    let raw_config = make_encoder_config(config);

    validate_encode_request(&raw_config, &c_text)?;
    let operation = EncodeOperationGuard::create(&raw_config, &c_text)?;
    loop {
        let progress = operation.poll_progress()?;
        on_progress(progress);
        match progress.state {
            EncodeOperationState::Queued | EncodeOperationState::Running => {
                if !operation.pump()? {
                    thread::sleep(Duration::from_millis(ENCODE_OPERATION_IDLE_SLEEP_MS));
                }
            }
            EncodeOperationState::Succeeded => {
                return operation.take_result();
            }
            EncodeOperationState::Failed | EncodeOperationState::Cancelled => {
                let code = if progress.terminal_code == BAG_NOT_READY {
                    BAG_INTERNAL
                } else {
                    progress.terminal_code
                };
                return Err(CliError::Api(error_code_message(code)));
            }
        }
    }
}

pub(super) fn validate_encode_request(
    config: &BagEncoderConfig,
    text: &CString,
) -> Result<(), CliError> {
    let validation = unsafe {
        // SAFETY: `config` and `text` remain valid for the duration of the FFI call.
        bag_validate_encode_request(config, text.as_ptr())
    };
    if validation != BAG_VALIDATION_OK {
        return Err(CliError::Api(validation_issue_message(validation)));
    }
    Ok(())
}
