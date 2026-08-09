mod decode;
mod encode;
mod metadata;

use crate::licenses;
use crate::presentation::{version_output, RunOutput};
use crate::{Cli, CliError, Command};

pub fn run(cli: Cli) -> Result<RunOutput, CliError> {
    match cli.command {
        Command::Version => Ok(RunOutput::Message(version_output())),
        Command::Licenses => Ok(RunOutput::Message(licenses::licenses_output())),
        Command::Encode(args) => encode::run(args),
        Command::Decode(args) => decode::run(args),
    }
}
