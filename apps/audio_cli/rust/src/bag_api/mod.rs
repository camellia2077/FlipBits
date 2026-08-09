use crate::util::c_str_to_string;

mod abi;
mod config;
mod decode;
mod errors;
mod guards;
mod operations;

pub use config::{CodecConfig, EncodeOperationPhase, EncodeOperationProgress};
pub use decode::decode_pcm;
pub use operations::encode_text_with_progress;

pub fn core_version() -> Option<String> {
    use abi::bag_core_version;

    let raw = unsafe {
        // SAFETY: The native function returns a process-lifetime version string
        // pointer or null when unavailable.
        bag_core_version()
    };
    if raw.is_null() {
        None
    } else {
        Some(c_str_to_string(raw))
    }
}
