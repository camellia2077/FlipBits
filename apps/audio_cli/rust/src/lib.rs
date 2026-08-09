#![deny(unsafe_op_in_unsafe_fn)]
#![warn(clippy::undocumented_unsafe_blocks)]

mod audio_io_api;
mod bag_api;
mod commands;
mod licenses;
mod presentation;
mod util;

#[cfg(test)]
mod tests;

pub use commands::run;
pub use presentation::{
    clap_debug_assert, print_error, print_output, version_output, Cli, CliError, Command,
    DecodeArgs, EncodeArgs, FlashStyle, RunOutput, TransportMode,
};

pub const CLI_VERSION: &str = env!("CARGO_PKG_VERSION");
pub const CLI_PRESENTATION_VERSION: &str = CLI_VERSION;
pub(crate) const DEFAULT_SAMPLE_RATE_HZ: i32 = 44_100;
pub(crate) const DEFAULT_FRAME_RATE_DIVISOR: i32 = 20;
