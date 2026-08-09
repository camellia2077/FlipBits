use crate::CliError;

#[derive(Debug, Eq, PartialEq)]
pub enum RunOutput {
    Message(String),
    DecodedText(String),
}

pub fn print_output(output: RunOutput) {
    match output {
        RunOutput::Message(message) | RunOutput::DecodedText(message) => {
            println!("{message}");
        }
    }
}

pub fn print_error(error: CliError) -> ! {
    eprintln!("Error: {error}");
    std::process::exit(1);
}
