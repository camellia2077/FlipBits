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

void TestWavIoMetadataRoundTripContract() {
    const auto test_case = test::AudioIoRoundTripCases().front();
    audio_io::WaveBitsAudioMetadata metadata{};
    metadata.version = 3;
    metadata.mode = audio_io::WaveBitsAudioMetadataMode::kFlash;
    metadata.has_flash_voicing_style = true;
    metadata.flash_voicing_style = audio_io::WaveBitsAudioMetadataFlashVoicingStyle::kRitualChant;
    metadata.created_at_iso_utc = "2026-03-17T09:45:00Z";
    metadata.duration_ms = 4321u;
    metadata.frame_samples = 2205u;
    metadata.pcm_sample_count = static_cast<std::uint32_t>(test_case.mono_pcm.size());
    metadata.app_version = "0.2.1";
    metadata.core_version = "0.4.0";
    const auto wav_bytes = audio_io::SerializeMonoPcm16WavWithMetadata(
        test_case.sample_rate_hz,
        test_case.mono_pcm,
        metadata);
    test::AssertTrue(!wav_bytes.empty(), "Metadata WAV serialization should succeed.");

    const auto parsed_wav = audio_io::ParseMonoPcm16Wav(wav_bytes);
    test::AssertEq(parsed_wav.status, audio_io::WavPcm16Status::kOk, "PCM parsing should still succeed.");
    test::AssertAudioIoRoundTripResult(parsed_wav.wav, test_case, "Metadata WAV PCM round trip");

    const auto parsed_metadata = audio_io::ParseWaveBitsAudioMetadata(wav_bytes);
    test::AssertEq(parsed_metadata.status, audio_io::WaveBitsAudioMetadataStatus::kOk, "Metadata parse should succeed.");
    test::AssertEq(parsed_metadata.metadata.version, 3u, "Metadata version should round-trip.");
    test::AssertEq(parsed_metadata.metadata.mode,
                   audio_io::WaveBitsAudioMetadataMode::kFlash,
                   "Metadata mode should round-trip.");
    test::AssertTrue(parsed_metadata.metadata.has_flash_voicing_style,
                     "Metadata should preserve the flash voicing style flag.");
    test::AssertEq(parsed_metadata.metadata.flash_voicing_style,
                   audio_io::WaveBitsAudioMetadataFlashVoicingStyle::kRitualChant,
                   "Metadata flash voicing style should round-trip.");
    test::AssertEq(parsed_metadata.metadata.created_at_iso_utc,
                   std::string("2026-03-17T09:45:00Z"),
                   "Metadata creation time should round-trip.");
    test::AssertEq(parsed_metadata.metadata.duration_ms,
                   static_cast<std::uint32_t>(4321u),
                   "Metadata duration should round-trip.");
    test::AssertEq(parsed_metadata.metadata.frame_samples,
                   static_cast<std::uint32_t>(2205u),
                   "Metadata frame sample count should round-trip.");
    test::AssertEq(parsed_metadata.metadata.pcm_sample_count,
                   static_cast<std::uint32_t>(test_case.mono_pcm.size()),
                   "Metadata PCM sample count should round-trip.");
    test::AssertEq(parsed_metadata.metadata.app_version,
                   std::string("0.2.1"),
                   "Metadata app version should round-trip.");
    test::AssertEq(parsed_metadata.metadata.core_version,
                   std::string("0.4.0"),
                   "Metadata core version should round-trip.");
}

void TestWavIoMetadataMissingOnCanonicalWav() {
    const auto test_case = test::AudioIoRoundTripCases().front();
    const auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
    const auto parsed_metadata = audio_io::ParseWaveBitsAudioMetadata(wav_bytes);
    test::AssertEq(parsed_metadata.status,
                   audio_io::WaveBitsAudioMetadataStatus::kNotFound,
                   "Canonical WAV without WBAG chunk should report metadata not found.");
}

void TestWavIoMetadataRespectsChunkPadding() {
    const auto test_case = test::AudioIoRoundTripCases().front();
    auto wav_bytes = audio_io::SerializeMonoPcm16Wav(test_case.sample_rate_hz, test_case.mono_pcm);
    std::vector<std::uint8_t> padded_bytes;
    padded_bytes.reserve(wav_bytes.size() + 18);
    padded_bytes.insert(padded_bytes.end(), wav_bytes.begin(), wav_bytes.begin() + 36);
    padded_bytes.push_back('J');
    padded_bytes.push_back('U');
    padded_bytes.push_back('N');
    padded_bytes.push_back('K');
    padded_bytes.push_back(1);
    padded_bytes.push_back(0);
    padded_bytes.push_back(0);
    padded_bytes.push_back(0);
    padded_bytes.push_back(0xAB);
    padded_bytes.push_back(0x00);
    padded_bytes.insert(padded_bytes.end(), wav_bytes.begin() + 36, wav_bytes.end());
    const auto riff_size = static_cast<std::uint32_t>(padded_bytes.size() - 8);
    padded_bytes[4] = static_cast<std::uint8_t>(riff_size & 0xFFu);
    padded_bytes[5] = static_cast<std::uint8_t>((riff_size >> 8) & 0xFFu);
    padded_bytes[6] = static_cast<std::uint8_t>((riff_size >> 16) & 0xFFu);
    padded_bytes[7] = static_cast<std::uint8_t>((riff_size >> 24) & 0xFFu);

    const auto parsed = audio_io::ParseMonoPcm16Wav(padded_bytes);
    test::AssertEq(parsed.status,
                   audio_io::WavPcm16Status::kOk,
                   "WAV parser should handle odd-sized chunk padding before data.");
    test::AssertAudioIoRoundTripResult(parsed.wav, test_case, "Chunk padding WAV PCM round trip");
}

void TestWavIoMetadataRejectsOlderVersions() {
    const auto test_case = test::AudioIoRoundTripCases().front();
    audio_io::WaveBitsAudioMetadata metadata_v2{};
    metadata_v2.version = 2;
    metadata_v2.mode = audio_io::WaveBitsAudioMetadataMode::kUltra;
    metadata_v2.created_at_iso_utc = "2026-03-17T09:45:00Z";
    metadata_v2.duration_ms = 4321u;
    const auto wav_bytes_v2 = audio_io::SerializeMonoPcm16WavWithMetadata(
        test_case.sample_rate_hz,
        test_case.mono_pcm,
        metadata_v2);
    test::AssertTrue(
        wav_bytes_v2.empty(),
        "Serialization should reject older metadata versions now that only v3 is supported.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Unit.WavIoHeaderRoundTripContract", TestWavIoHeaderRoundTripContract);
    runner.Add("Unit.WavIoHeaderBytesRoundTripContract", TestWavIoHeaderBytesRoundTripContract);
    runner.Add("Unit.WavIoHeaderReadMissingFileFails", TestWavIoHeaderReadMissingFileFails);
    runner.Add("Unit.WavIoHeaderRejectsInvalidBytes", TestWavIoHeaderRejectsInvalidBytes);
    runner.Add("Unit.WavIoHeaderRejectsUnsupportedStereoBytes", TestWavIoHeaderRejectsUnsupportedStereoBytes);
    runner.Add("Unit.WavIoMetadataRoundTripContract", TestWavIoMetadataRoundTripContract);
    runner.Add("Unit.WavIoMetadataMissingOnCanonicalWav", TestWavIoMetadataMissingOnCanonicalWav);
    runner.Add("Unit.WavIoMetadataRespectsChunkPadding", TestWavIoMetadataRespectsChunkPadding);
    runner.Add("Unit.WavIoMetadataRejectsOlderVersions", TestWavIoMetadataRejectsOlderVersions);
    return runner.Run();
}

