use clap::Parser;
use flipbits::{run, Cli, RunOutput};

fn main() {
    let cli = Cli::parse();
    match run(cli) {
        Ok(RunOutput::Message(message)) => {
            println!("{message}");
        }
        Ok(RunOutput::DecodedText(text)) => {
            println!("{text}");
        }
        Err(error) => {
            eprintln!("Error: {error}");
            std::process::exit(1);
        }
    }
}
