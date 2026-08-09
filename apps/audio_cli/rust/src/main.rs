use clap::Parser;
use flipbits::{print_error, print_output, run, Cli};

fn main() {
    let cli = Cli::parse();
    match run(cli) {
        Ok(output) => print_output(output),
        Err(error) => print_error(error),
    }
}
