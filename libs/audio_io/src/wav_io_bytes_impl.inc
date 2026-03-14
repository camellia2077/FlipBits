namespace audio_io::detail::bytes_impl {
namespace {

constexpr std::size_t kRiffHeaderSize = 12;
constexpr std::size_t kChunkHeaderSize = 8;
constexpr std::size_t kFmtChunkSize = 16;
constexpr std::size_t kCanonicalWavHeaderSize = 44;
constexpr std::uint16_t kPcmFormatCode = 1;
constexpr std::uint16_t kMonoChannelCount = 1;
constexpr std::uint16_t kBitsPerSample = 16;
constexpr std::size_t kBytesPerSample = 2;
constexpr std::uint32_t kMaxChunkByteCount = 0xFFFFFFFFu;

bool HasRange(std::size_t total_size, std::size_t offset, std::size_t byte_count) {
    return offset <= total_size && byte_count <= (total_size - offset);
}

std::uint16_t ReadU16Le(const std::uint8_t* data) {
    return static_cast<std::uint16_t>(
        static_cast<std::uint16_t>(data[0]) |
        (static_cast<std::uint16_t>(data[1]) << 8));
}

std::uint32_t ReadU32Le(const std::uint8_t* data) {
    return static_cast<std::uint32_t>(
        static_cast<std::uint32_t>(data[0]) |
        (static_cast<std::uint32_t>(data[1]) << 8) |
        (static_cast<std::uint32_t>(data[2]) << 16) |
        (static_cast<std::uint32_t>(data[3]) << 24));
}

void WriteU16Le(std::uint8_t* data, std::uint16_t value) {
    data[0] = static_cast<std::uint8_t>(value & 0xFFu);
    data[1] = static_cast<std::uint8_t>((value >> 8) & 0xFFu);
}

void WriteU32Le(std::uint8_t* data, std::uint32_t value) {
    data[0] = static_cast<std::uint8_t>(value & 0xFFu);
    data[1] = static_cast<std::uint8_t>((value >> 8) & 0xFFu);
    data[2] = static_cast<std::uint8_t>((value >> 16) & 0xFFu);
    data[3] = static_cast<std::uint8_t>((value >> 24) & 0xFFu);
}

bool MatchesTag(const std::uint8_t* data, const char* expected) {
    return data[0] == static_cast<std::uint8_t>(expected[0]) &&
        data[1] == static_cast<std::uint8_t>(expected[1]) &&
        data[2] == static_cast<std::uint8_t>(expected[2]) &&
        data[3] == static_cast<std::uint8_t>(expected[3]);
}

}  // namespace

std::vector<std::uint8_t> SerializeMonoPcm16WavBytes(
    int sample_rate_hz,
    const std::vector<std::int16_t>& pcm
) {
    if (sample_rate_hz <= 0) {
        return {};
    }

    const std::size_t data_byte_count = pcm.size() * kBytesPerSample;
    const std::size_t total_byte_count = kCanonicalWavHeaderSize + data_byte_count;
    if (data_byte_count > kMaxChunkByteCount || total_byte_count > (static_cast<std::size_t>(kMaxChunkByteCount) + 8u)) {
        return {};
    }

    std::vector<std::uint8_t> wav_bytes(total_byte_count, 0);
    wav_bytes[0] = 'R';
    wav_bytes[1] = 'I';
    wav_bytes[2] = 'F';
    wav_bytes[3] = 'F';
    WriteU32Le(wav_bytes.data() + 4, static_cast<std::uint32_t>(total_byte_count - 8));
    wav_bytes[8] = 'W';
    wav_bytes[9] = 'A';
    wav_bytes[10] = 'V';
    wav_bytes[11] = 'E';
    wav_bytes[12] = 'f';
    wav_bytes[13] = 'm';
    wav_bytes[14] = 't';
    wav_bytes[15] = ' ';
    WriteU32Le(wav_bytes.data() + 16, static_cast<std::uint32_t>(kFmtChunkSize));
    WriteU16Le(wav_bytes.data() + 20, kPcmFormatCode);
    WriteU16Le(wav_bytes.data() + 22, kMonoChannelCount);
    WriteU32Le(wav_bytes.data() + 24, static_cast<std::uint32_t>(sample_rate_hz));
    WriteU32Le(
        wav_bytes.data() + 28,
        static_cast<std::uint32_t>(sample_rate_hz * kMonoChannelCount * kBytesPerSample));
    WriteU16Le(
        wav_bytes.data() + 32,
        static_cast<std::uint16_t>(kMonoChannelCount * kBytesPerSample));
    WriteU16Le(wav_bytes.data() + 34, kBitsPerSample);
    wav_bytes[36] = 'd';
    wav_bytes[37] = 'a';
    wav_bytes[38] = 't';
    wav_bytes[39] = 'a';
    WriteU32Le(wav_bytes.data() + 40, static_cast<std::uint32_t>(data_byte_count));

    std::size_t write_offset = kCanonicalWavHeaderSize;
    for (const std::int16_t sample : pcm) {
        WriteU16Le(
            wav_bytes.data() + write_offset,
            static_cast<std::uint16_t>(sample));
        write_offset += kBytesPerSample;
    }
    return wav_bytes;
}

WavPcm16ParseResult ParseMonoPcm16WavBytes(
    const std::uint8_t* wav_bytes,
    std::size_t wav_byte_count
) {
    if (wav_bytes == nullptr && wav_byte_count > 0) {
        return {WavPcm16Status::kInvalidArgument, {}};
    }
    if (wav_byte_count < kRiffHeaderSize) {
        return {WavPcm16Status::kInvalidHeader, {}};
    }
    if (!MatchesTag(wav_bytes, "RIFF") || !MatchesTag(wav_bytes + 8, "WAVE")) {
        return {WavPcm16Status::kInvalidHeader, {}};
    }

    bool saw_fmt_chunk = false;
    bool saw_data_chunk = false;
    int sample_rate_hz = 0;
    int channels = 0;
    std::size_t data_offset = 0;
    std::size_t data_byte_count = 0;

    std::size_t chunk_offset = kRiffHeaderSize;
    while (HasRange(wav_byte_count, chunk_offset, kChunkHeaderSize)) {
        const std::uint8_t* chunk_header = wav_bytes + chunk_offset;
        const std::uint32_t chunk_size = ReadU32Le(chunk_header + 4);
        const std::size_t chunk_data_offset = chunk_offset + kChunkHeaderSize;
        if (!HasRange(wav_byte_count, chunk_data_offset, chunk_size)) {
            return {WavPcm16Status::kTruncatedData, {}};
        }

        if (MatchesTag(chunk_header, "fmt ")) {
            if (chunk_size < kFmtChunkSize) {
                return {WavPcm16Status::kInvalidHeader, {}};
            }
            const std::uint8_t* fmt = wav_bytes + chunk_data_offset;
            const std::uint16_t format_code = ReadU16Le(fmt);
            const std::uint16_t fmt_channels = ReadU16Le(fmt + 2);
            const std::uint32_t fmt_sample_rate_hz = ReadU32Le(fmt + 4);
            const std::uint16_t block_align = ReadU16Le(fmt + 12);
            const std::uint16_t bits_per_sample = ReadU16Le(fmt + 14);

            if (format_code != kPcmFormatCode ||
                fmt_channels != kMonoChannelCount ||
                bits_per_sample != kBitsPerSample ||
                block_align != (kMonoChannelCount * kBytesPerSample) ||
                fmt_sample_rate_hz == 0u) {
                return {WavPcm16Status::kUnsupportedFormat, {}};
            }

            saw_fmt_chunk = true;
            sample_rate_hz = static_cast<int>(fmt_sample_rate_hz);
            channels = static_cast<int>(fmt_channels);
        } else if (MatchesTag(chunk_header, "data")) {
            if ((chunk_size % kBytesPerSample) != 0u) {
                return {WavPcm16Status::kTruncatedData, {}};
            }
            saw_data_chunk = true;
            data_offset = chunk_data_offset;
            data_byte_count = chunk_size;
        }

        chunk_offset = chunk_data_offset + chunk_size + (chunk_size % 2u);
    }

    if (!saw_fmt_chunk || !saw_data_chunk) {
        return {WavPcm16Status::kInvalidHeader, {}};
    }

    WavPcm16 wav{};
    wav.sample_rate_hz = sample_rate_hz;
    wav.channels = channels;
    wav.mono_pcm.reserve(data_byte_count / kBytesPerSample);
    for (std::size_t offset = 0; offset < data_byte_count; offset += kBytesPerSample) {
        wav.mono_pcm.push_back(static_cast<std::int16_t>(ReadU16Le(wav_bytes + data_offset + offset)));
    }
    return {WavPcm16Status::kOk, wav};
}

}  // namespace audio_io::detail::bytes_impl
