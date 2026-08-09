namespace bag::ultra {
using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {
constexpr uint8_t kFecTapOne = 0b0110101;
constexpr uint8_t kFecTapTwo = 0b1001111;

uint8_t Parity(uint8_t value) {
  value ^= static_cast<uint8_t>(value >> 4);
  value ^= static_cast<uint8_t>(value >> 2);
  value ^= static_cast<uint8_t>(value >> 1);
  return static_cast<uint8_t>(value & 1U);
}

uint8_t EncodeFecPair(uint8_t state, uint8_t input_bit) {
  const uint8_t register_value =
      static_cast<uint8_t>(((state << 1U) | input_bit) & 0x7FU);
  const uint8_t first = Parity(static_cast<uint8_t>(register_value &
                                                    kFecTapOne));
  const uint8_t second = Parity(static_cast<uint8_t>(register_value &
                                                     kFecTapTwo));
  return static_cast<uint8_t>((first << 1U) | second);
}

std::vector<uint8_t> ConvolutionalEncode(const std::vector<uint8_t>& bits) {
  std::vector<uint8_t> coded;
  coded.reserve(bits.size() * 2);
  uint8_t state = 0;
  for (const uint8_t bit : bits) {
    const uint8_t pair = EncodeFecPair(state, bit);
    coded.push_back(static_cast<uint8_t>((pair >> 1U) & 1U));
    coded.push_back(static_cast<uint8_t>(pair & 1U));
    state = static_cast<uint8_t>(((state << 1U) | bit) & 0x3FU);
  }
  return coded;
}

std::vector<uint8_t> Interleave(const std::vector<uint8_t>& input) {
  const size_t padded_size =
      ((input.size() + 3U) / 4U) * 4U;
  const size_t output_size = padded_size + 160U;
  std::vector<uint8_t> output(output_size, 0);
  for (size_t group = 0; group < padded_size / 4U; ++group) {
    for (size_t bit = 0; bit < 4U; ++bit) {
      const size_t input_index = group * 4U + bit;
      // The receiver writes four received bits into consecutive positions of
      // a 160-bit ring. To make the bit appear on the diagonal read after
      // ten, twenty, or thirty groups, place it in the corresponding future
      // group and bit lane.
      const size_t output_group = group + bit * 10U;
      const size_t output_index = output_group * 4U + bit;
      output[output_index] =
          input_index < input.size() ? input[input_index] : 0;
    }
  }
  return output;
}

std::vector<uint8_t> Deinterleave(const std::vector<uint8_t>& input) {
  std::array<uint8_t, 160> register_bits{};
  std::vector<uint8_t> output(input.size(), 0);
  size_t index = 0;
  for (size_t offset = 0; offset + 4U <= input.size(); offset += 4U) {
    for (size_t bit = 0; bit < 4U; ++bit) {
      output[offset + bit] =
          register_bits[(index + bit * 41U) % register_bits.size()];
    }
    for (size_t bit = 0; bit < 4U; ++bit) {
      register_bits[(index + bit) % register_bits.size()] =
          input[offset + bit];
    }
    index = (index + 4U) % register_bits.size();
  }
  return output;
}

size_t ToneIndexForNibble(uint8_t nibble) {
  for (size_t tone = 0; tone < kMfsK16ToneToNibble.size(); ++tone) {
    if (kMfsK16ToneToNibble[tone] == nibble) {
      return tone;
    }
  }
  return 0;
}

std::vector<uint8_t> MfsK16BitsToSymbols(
    const std::vector<uint8_t>& bits) {
  std::vector<uint8_t> coded = ConvolutionalEncode(bits);
  const std::vector<uint8_t> interleaved = Interleave(coded);
  std::vector<uint8_t> symbols;
  symbols.reserve(kMfsK16PreambleSymbolCount +
                 interleaved.size() / 4U + kMfsK16TailSymbolCount);
  symbols.insert(symbols.end(), kMfsK16PreambleSymbolCount, 0);
  for (size_t offset = 0; offset + 4U <= interleaved.size(); offset += 4U) {
    const uint8_t nibble = static_cast<uint8_t>(
        (interleaved[offset] << 3U) |
        (interleaved[offset + 1U] << 2U) |
        (interleaved[offset + 2U] << 1U) |
        interleaved[offset + 3U]);
    symbols.push_back(static_cast<uint8_t>(ToneIndexForNibble(nibble)));
  }
  symbols.insert(symbols.end(), kMfsK16TailSymbolCount, 0);
  return symbols;
}
}  // namespace

ErrorCode EncodePayloadToSymbols(const std::vector<uint8_t>& payload,
                                 const Mfsk16Config& config,
                                 std::vector<uint8_t>* out_symbols) {
  if (out_symbols == nullptr || !IsValidMfsk16Config(config) || payload.empty()) {
    return ErrorCode::kInvalidArgument;
  }
  std::vector<uint8_t> varicode_bits;
  const ErrorCode bits_code =
      EncodePayloadToVaricodeBits(payload, &varicode_bits);
  if (bits_code != ErrorCode::kOk) {
    return bits_code;
  }
  *out_symbols = MfsK16BitsToSymbols(varicode_bits);
  return ErrorCode::kOk;
}

ErrorCode EncodePayloadToPcm16(const std::vector<uint8_t>& payload,
                               const Mfsk16Config& config,
                               std::vector<int16_t>* out_pcm) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  std::vector<uint8_t> symbols;
  const ErrorCode symbols_code =
      EncodePayloadToSymbols(payload, config, &symbols);
  if (symbols_code != ErrorCode::kOk) {
    return symbols_code;
  }
  return EncodeSymbolsToPcm16(symbols, config, out_pcm);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm) {
  return EncodeTextToPcm16(config, text, out_pcm, nullptr,
                          Mfsk16Speed::k15_625Bd);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            Mfsk16Speed speed) {
  return EncodeTextToPcm16(config, text, out_pcm, nullptr, speed);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink) {
  return EncodeTextToPcm16(config, text, out_pcm, progress_sink,
                           Mfsk16Speed::k15_625Bd);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink,
                            Mfsk16Speed speed) {
  if (out_pcm == nullptr || text.empty()) {
    return ErrorCode::kInvalidArgument;
  }
  std::vector<uint8_t> payload;
  const ErrorCode payload_code = EncodeTextToPayload(text, &payload);
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }
  return EncodePayloadToPcm16(payload, MakeMfsk16Config(config, speed),
                              out_pcm);
}
}  // namespace bag::ultra
