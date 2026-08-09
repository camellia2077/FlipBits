mod cli;
mod error;
mod fs_io;
mod output;
mod progress;
mod version;

pub use cli::{clap_debug_assert, Cli, Command, DecodeArgs, EncodeArgs, FlashStyle, TransportMode};
pub use error::CliError;
pub(crate) use fs_io::{read_binary_file, resolve_encode_text, write_binary_file, write_text_file};
pub use output::{print_error, print_output, RunOutput};
pub(crate) use progress::EncodeProgressBar;
pub use version::version_output;
