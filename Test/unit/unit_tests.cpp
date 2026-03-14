#include "test_std_support.h"
#include "test_audio_io.h"
#include "test_framework.h"
#include "test_fs.h"
#include "wav_io.h"

namespace {

void TestWavIoHeaderRoundTripContract() {
    const auto dir = test::MakeTempDir("unit");
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        const auto path = dir / (std::string(test_case.name) + ".wav");
        audio_io::WriteMonoPcm16Wav(path, test_case.sample_rate_hz, test_case.mono_pcm);
        const auto read_back = audio_io::ReadMonoPcm16Wav(path);
        test::AssertAudioIoRoundTripResult(read_back, test_case, "Header audio_io boundary");
    }
}

void TestWavIoHeaderBytesRoundTripContract() {
    for (const auto& test_case : test::AudioIoRoundTripCases()) {
        const auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
        const auto parsed = audio_io::ParseMonoPcm16Wav(wav_bytes);
        test::AssertEq(
            parsed.status,
            audio_io::WavPcm16Status::kOk,
            "Header bytes route should parse canonical mono PCM16 WAV bytes.");
        test::AssertAudioIoRoundTripResult(parsed.wav, test_case, "Header audio_io bytes boundary");
    }
}

void TestWavIoHeaderReadMissingFileFails() {
    const auto missing_path = test::MakeTempDir("unit") / "missing.wav";
    test::AssertThrows(
        [&] {
            (void)audio_io::ReadMonoPcm16Wav(missing_path);
        },
        "Header audio_io boundary should throw when the input file does not exist.");
}

void TestWavIoHeaderRejectsInvalidBytes() {
    const std::vector<std::uint8_t> bad_header = {'N', 'O', 'T', 'W', 'A', 'V', 'E'};
    const auto parsed = audio_io::ParseMonoPcm16Wav(bad_header);
    test::AssertEq(
        parsed.status,
        audio_io::WavPcm16Status::kInvalidHeader,
        "Header bytes route should reject invalid RIFF/WAVE bytes.");
}

void TestWavIoHeaderRejectsUnsupportedStereoBytes() {
    const auto test_case = test::AudioIoRoundTripCases().front();
    auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
    wav_bytes[22] = 0x02;
    wav_bytes[23] = 0x00;
    wav_bytes[32] = 0x04;
    wav_bytes[33] = 0x00;
    const auto stereo_byte_rate = static_cast<std::uint32_t>(test_case.sample_rate_hz * 4);
    wav_bytes[28] = static_cast<std::uint8_t>(stereo_byte_rate & 0xFFu);
    wav_bytes[29] = static_cast<std::uint8_t>((stereo_byte_rate >> 8) & 0xFFu);
    wav_bytes[30] = static_cast<std::uint8_t>((stereo_byte_rate >> 16) & 0xFFu);
    wav_bytes[31] = static_cast<std::uint8_t>((stereo_byte_rate >> 24) & 0xFFu);
    const auto parsed = audio_io::ParseMonoPcm16Wav(wav_bytes);
    test::AssertEq(
        parsed.status,
        audio_io::WavPcm16Status::kUnsupportedFormat,
        "Header bytes route should reject stereo WAV input.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Unit.WavIoHeaderRoundTripContract", TestWavIoHeaderRoundTripContract);
    runner.Add("Unit.WavIoHeaderBytesRoundTripContract", TestWavIoHeaderBytesRoundTripContract);
    runner.Add("Unit.WavIoHeaderReadMissingFileFails", TestWavIoHeaderReadMissingFileFails);
    runner.Add("Unit.WavIoHeaderRejectsInvalidBytes", TestWavIoHeaderRejectsInvalidBytes);
    runner.Add("Unit.WavIoHeaderRejectsUnsupportedStereoBytes", TestWavIoHeaderRejectsUnsupportedStereoBytes);
    return runner.Run();
}

