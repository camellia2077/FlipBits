use crate::bag_api;
use crate::CLI_PRESENTATION_VERSION;

pub fn version_output() -> String {
    let core_version = bag_api::core_version().unwrap_or_else(|| "unknown".to_string());
    format!(
        "presentation: v{CLI_PRESENTATION_VERSION}\ncore: v{core_version}\nbuild: rust-wav\nstatus: bag_api + audio_io WAV build"
    )
}
