use crate::CliError;

use super::abi::{
    bag_cancel_encode_operation, bag_create_decoder, bag_create_encode_operation,
    bag_destroy_decoder, bag_destroy_encode_operation, bag_free_encode_result,
    bag_poll_encode_operation, bag_poll_result, bag_pump_encode_operation, bag_push_pcm,
    bag_take_encode_operation_result, BagDecoder, BagDecoderConfig, BagEncodeOperation,
    BagEncodeOperationProgress, BagEncodeOperationPumpBudget, BagEncodeResult, BagEncoderConfig,
    BagTextResult, BAG_ENCODE_OPERATION_CANCELLED, BAG_ENCODE_OPERATION_FAILED,
    BAG_ENCODE_OPERATION_PHASE_FINALIZING, BAG_ENCODE_OPERATION_PHASE_POSTPROCESSING,
    BAG_ENCODE_OPERATION_PHASE_PREPARING_INPUT, BAG_ENCODE_OPERATION_PHASE_RENDERING_PCM,
    BAG_ENCODE_OPERATION_QUEUED, BAG_ENCODE_OPERATION_RUNNING, BAG_ENCODE_OPERATION_SUCCEEDED,
    BAG_NOT_READY, BAG_OK, ENCODE_OPERATION_PUMP_MAX_WALL_TIME_MS,
    ENCODE_OPERATION_PUMP_MAX_WORK_UNITS,
};
use super::config::{EncodeOperationPhase, EncodeOperationProgress, EncodeOperationState};
use super::errors::error_code_message;
use std::ffi::CString;
use std::ptr;

pub(super) struct EncodeOperationGuard(*mut BagEncodeOperation);

impl EncodeOperationGuard {
    pub(super) fn create(config: &BagEncoderConfig, text: &CString) -> Result<Self, CliError> {
        let mut raw_operation = ptr::null_mut();
        let create_code = unsafe {
            // SAFETY: `config` and `text` remain valid for the duration of the FFI
            // call, and `raw_operation` points to writable storage for the handle.
            bag_create_encode_operation(config, text.as_ptr(), &mut raw_operation)
        };
        if create_code != BAG_OK || raw_operation.is_null() {
            return Err(CliError::Api(error_code_message(create_code)));
        }
        Ok(Self(raw_operation))
    }

    pub(super) fn pump(&self) -> Result<bool, CliError> {
        let mut did_progress = 0;
        let code = unsafe {
            // SAFETY: `self.0` is a live operation handle and `did_progress`
            // points to writable storage for the native progress flag.
            bag_pump_encode_operation(
                self.0,
                BagEncodeOperationPumpBudget {
                    max_work_units: ENCODE_OPERATION_PUMP_MAX_WORK_UNITS,
                    max_wall_time_ms: ENCODE_OPERATION_PUMP_MAX_WALL_TIME_MS,
                },
                &mut did_progress,
            )
        };
        if code != BAG_OK {
            return Err(CliError::Api(error_code_message(code)));
        }
        Ok(did_progress != 0)
    }

    pub(super) fn poll_progress(&self) -> Result<EncodeOperationProgress, CliError> {
        let mut progress = BagEncodeOperationProgress {
            state: BAG_ENCODE_OPERATION_FAILED,
            phase: BAG_ENCODE_OPERATION_PHASE_FINALIZING,
            overall_progress_0_to_1: 0.0,
            phase_progress_0_to_1: 0.0,
            completed_work_units: 0,
            total_work_units: 0,
            phase_completed_work_units: 0,
            phase_total_work_units: 0,
            terminal_code: BAG_NOT_READY,
            estimated_pcm_sample_count: 0,
            payload_byte_count: 0,
            segment_count: 0,
            current_segment_index: 0,
        };
        let code = unsafe {
            // SAFETY: `self.0` is a live encode operation handle and `progress`
            // points to writable storage for the polled progress struct.
            bag_poll_encode_operation(self.0, &mut progress)
        };
        if code != BAG_OK {
            return Err(CliError::Api(error_code_message(code)));
        }

        Ok(EncodeOperationProgress {
            state: match progress.state {
                BAG_ENCODE_OPERATION_QUEUED => EncodeOperationState::Queued,
                BAG_ENCODE_OPERATION_RUNNING => EncodeOperationState::Running,
                BAG_ENCODE_OPERATION_SUCCEEDED => EncodeOperationState::Succeeded,
                BAG_ENCODE_OPERATION_CANCELLED => EncodeOperationState::Cancelled,
                _ => EncodeOperationState::Failed,
            },
            phase: match progress.phase {
                BAG_ENCODE_OPERATION_PHASE_PREPARING_INPUT => EncodeOperationPhase::PreparingInput,
                BAG_ENCODE_OPERATION_PHASE_RENDERING_PCM => EncodeOperationPhase::RenderingPcm,
                BAG_ENCODE_OPERATION_PHASE_POSTPROCESSING => EncodeOperationPhase::Postprocessing,
                _ => EncodeOperationPhase::Finalizing,
            },
            overall_progress_0_to_1: progress.overall_progress_0_to_1.clamp(0.0, 1.0),
            terminal_code: progress.terminal_code,
        })
    }

    pub(super) fn take_result(&self) -> Result<Vec<i16>, CliError> {
        let mut result = EncodeResultGuard::new();
        let code = unsafe {
            // SAFETY: `self.0` is a live encode operation handle and `result`
            // points to writable storage for the returned result descriptor.
            bag_take_encode_operation_result(self.0, result.as_mut_ptr())
        };
        if code != BAG_OK {
            return Err(CliError::Api(error_code_message(code)));
        }
        Ok(result.to_vec())
    }
}

impl Drop for EncodeOperationGuard {
    fn drop(&mut self) {
        if !self.0.is_null() {
            unsafe {
                // SAFETY: The handle is owned by this guard and is destroyed
                // exactly once. Cancellation is idempotent for terminal operations.
                bag_cancel_encode_operation(self.0);
                bag_destroy_encode_operation(self.0);
            }
        }
    }
}

pub(super) struct DecoderGuard(*mut BagDecoder);

impl DecoderGuard {
    pub(super) fn create(config: &BagDecoderConfig) -> Result<Self, CliError> {
        let mut decoder = ptr::null_mut();
        let create_code = unsafe {
            // SAFETY: `config` remains valid for the duration of the FFI call, and
            // `decoder` points to writable storage for the returned handle.
            bag_create_decoder(config, &mut decoder)
        };
        if create_code != BAG_OK || decoder.is_null() {
            return Err(CliError::Api(error_code_message(create_code)));
        }
        Ok(Self(decoder))
    }

    pub(super) fn push_pcm(&self, pcm_samples: &[i16]) -> Result<(), CliError> {
        let push_code = unsafe {
            // SAFETY: `pcm_samples` is a valid immutable slice for the duration of
            // the call, and `self.0` is a live decoder handle owned by this guard.
            bag_push_pcm(self.0, pcm_samples.as_ptr(), pcm_samples.len(), 0)
        };
        if push_code != BAG_OK {
            return Err(CliError::Api(error_code_message(push_code)));
        }
        Ok(())
    }

    pub(super) fn poll_text_result(&self, result: &mut BagTextResult) -> Result<(), CliError> {
        let poll_code = unsafe {
            // SAFETY: `self.0` is a live decoder handle and `result` points to
            // writable storage whose buffer fields reference `text_buffer`.
            bag_poll_result(self.0, result)
        };
        if poll_code != BAG_OK {
            return Err(CliError::Api(error_code_message(poll_code)));
        }
        Ok(())
    }
}

impl Drop for DecoderGuard {
    fn drop(&mut self) {
        if !self.0.is_null() {
            unsafe {
                // SAFETY: The native API requires decoder handles returned from
                // `bag_create_decoder` to be destroyed exactly once.
                bag_destroy_decoder(self.0);
            }
        }
    }
}

pub(super) struct EncodeResultGuard(BagEncodeResult);

impl EncodeResultGuard {
    pub(super) fn new() -> Self {
        // SAFETY: `bag_encode_result` is a C POD result descriptor made only of
        // integer fields, status codes, and nullable caller/API-owned pointers.
        Self(unsafe { std::mem::zeroed() })
    }

    pub(super) fn as_mut_ptr(&mut self) -> *mut BagEncodeResult {
        &mut self.0
    }

    pub(super) fn to_vec(&self) -> Vec<i16> {
        raw_slice(self.0.samples.cast_const(), self.0.sample_count).to_vec()
    }
}

fn raw_slice<'a, T>(data: *const T, size: usize) -> &'a [T] {
    if data.is_null() || size == 0 {
        &[]
    } else {
        unsafe {
            // SAFETY: Callers only pass pointers/lengths obtained from the native
            // `bag_api` ABI, which guarantees readable contiguous storage for the
            // reported element count until the matching free function runs.
            std::slice::from_raw_parts(data, size)
        }
    }
}

impl Drop for EncodeResultGuard {
    fn drop(&mut self) {
        unsafe {
            // SAFETY: Results produced by `bag_take_encode_operation_result`
            // are released with `bag_free_encode_result`; null fields are accepted.
            bag_free_encode_result(&mut self.0);
        }
    }
}
