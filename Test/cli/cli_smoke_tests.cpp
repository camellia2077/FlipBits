#include <filesystem>
#include <string>
#include <vector>

#include "test_framework.h"
#include "test_fs.h"
#include "test_process.h"

namespace {

std::filesystem::path GetCliPath(int argc, char* argv[]) {
    if (argc < 2) {
        test::Fail("CLI smoke test requires the CLI executable path as argv[1].");
    }
    return std::filesystem::path(argv[1]);
}

test::ProcessResult RunCli(const std::filesystem::path& cli_path,
                           const std::vector<std::string>& args,
                           const std::filesystem::path& temp_dir) {
    return test::RunProcess(cli_path, args, temp_dir);
}

void TestVersionCommand(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");

    const auto version_result = RunCli(cli_path, {"version"}, dir);
    test::AssertEq(version_result.exit_code, 0, "`version` should exit successfully.");
    test::AssertContains(
        version_result.output,
        "presentation: v0.1.1",
        "`version` output should contain the presentation version.");
    test::AssertContains(
        version_result.output,
        "core: v0.1.1",
        "`version` output should contain the core version.");

    const auto dash_version_result = RunCli(cli_path, {"--version"}, dir);
    test::AssertEq(dash_version_result.exit_code, 0, "`--version` should exit successfully.");
    test::AssertContains(
        dash_version_result.output,
        "presentation: v0.1.1",
        "`--version` output should contain the presentation version.");
    test::AssertContains(
        dash_version_result.output,
        "core: v0.1.1",
        "`--version` output should contain the core version.");
}

void TestEncodeDecodeDirectText(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto wav_path = dir / "smoke_direct.wav";

    const auto encode_result =
        RunCli(cli_path, {"encode", "--text", "Smoke-CLI", "--out", wav_path.string()}, dir);
    test::AssertEq(encode_result.exit_code, 0, "CLI encode from direct text should succeed.");
    test::AssertTrue(std::filesystem::exists(wav_path), "Encoded WAV file should exist.");
    test::AssertTrue(std::filesystem::file_size(wav_path) > 0, "Encoded WAV file should be non-empty.");

    const auto decode_result = RunCli(cli_path, {"decode", "--in", wav_path.string()}, dir);
    test::AssertEq(decode_result.exit_code, 0, "CLI decode should succeed for generated WAV.");
    test::AssertContains(
        decode_result.output,
        "Smoke-CLI",
        "CLI decode output should contain the original text.");
}

void TestEncodeTextFileAndDecodeToTextFile(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");
    const auto input_path = dir / "input.txt";
    const auto wav_path = dir / "smoke_file.wav";
    const auto output_text_path = dir / "decoded.txt";

    test::WriteTextFile(input_path, "WaveBits file smoke path.");

    const auto encode_result = RunCli(
        cli_path,
        {"encode", "--text-file", input_path.string(), "--out", wav_path.string()},
        dir);
    test::AssertEq(encode_result.exit_code, 0, "CLI encode from text file should succeed.");
    test::AssertTrue(std::filesystem::exists(wav_path), "CLI encode from text file should create WAV.");

    const auto decode_result = RunCli(
        cli_path,
        {"decode", "--in", wav_path.string(), "--out-text", output_text_path.string()},
        dir);
    test::AssertEq(decode_result.exit_code, 0, "CLI decode to text file should succeed.");
    test::AssertTrue(
        std::filesystem::exists(output_text_path),
        "CLI decode with --out-text should create a text file.");
    test::AssertEq(
        test::ReadTextFile(output_text_path),
        std::string("WaveBits file smoke path."),
        "Decoded text file content should match the original input.");
}

void TestInvalidArgumentsShowUsage(const std::filesystem::path& cli_path) {
    const auto dir = test::MakeTempDir("cli_smoke");

    const auto no_args_result = RunCli(cli_path, {}, dir);
    test::AssertTrue(no_args_result.exit_code != 0, "Invoking CLI without args should fail.");
    test::AssertContains(no_args_result.output, "Usage:", "CLI without args should print usage.");

    const auto invalid_result = RunCli(cli_path, {"encode", "--unknown", "value"}, dir);
    test::AssertTrue(invalid_result.exit_code != 0, "Invalid CLI arguments should fail.");
    test::AssertContains(invalid_result.output, "Usage:", "Invalid CLI arguments should print usage.");
}

}  // namespace

int main(int argc, char* argv[]) {
    const auto cli_path = GetCliPath(argc, argv);

    test::Runner runner;
    runner.Add("CliSmoke.VersionCommand", [&]() { TestVersionCommand(cli_path); });
    runner.Add("CliSmoke.EncodeDecodeDirectText", [&]() { TestEncodeDecodeDirectText(cli_path); });
    runner.Add(
        "CliSmoke.EncodeTextFileAndDecodeToTextFile",
        [&]() { TestEncodeTextFileAndDecodeToTextFile(cli_path); });
    runner.Add("CliSmoke.InvalidArgumentsShowUsage", [&]() { TestInvalidArgumentsShowUsage(cli_path); });
    return runner.Run();
}
