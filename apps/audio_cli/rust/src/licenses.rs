const RUNTIME_DEP_COUNT: usize = 14;
const BUILD_DEP_COUNT: usize = 6;
const TEST_DEP_COUNT: usize = 3;
const NOTICES_REPO_PATH: &str = "docs/legal/cli_third_party_notices.md";

pub fn licenses_output() -> String {
    format!(
        "FlipBits third-party notices\n\n\
runtime: {RUNTIME_DEP_COUNT}\n\
build: {BUILD_DEP_COUNT}\n\
test: {TEST_DEP_COUNT}\n\n\
See the full notices document at: {NOTICES_REPO_PATH}\n\n\
CLI-only scope. Android is not included. libsndfile is not included in this first-pass notice set."
    )
}
