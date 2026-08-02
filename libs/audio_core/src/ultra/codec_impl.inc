namespace bag::ultra {

namespace {

constexpr std::array<std::string_view, 256> kMfsK16Varicode = {{
    "11101011100",
    "11101100000",
    "11101101000",
    "11101101100",
    "11101110000",
    "11101110100",
    "11101111000",
    "11101111100",
    "10101000",
    "11110000000",
    "11110100000",
    "11110101000",
    "11110101100",
    "10101100",
    "11110110000",
    "11110110100",
    "11110111000",
    "11110111100",
    "11111000000",
    "11111010000",
    "11111010100",
    "11111011000",
    "11111011100",
    "11111100000",
    "11111101000",
    "11111101100",
    "11111110000",
    "11111110100",
    "11111111000",
    "11111111100",
    "100000000000",
    "101000000000",
    "100",
    "111000000",
    "111111100",
    "1011011000",
    "1010101000",
    "1010100000",
    "1000000000",
    "110111100",
    "111110100",
    "111110000",
    "1010110100",
    "111100000",
    "10100000",
    "111011000",
    "111010100",
    "111101000",
    "11100000",
    "11110000",
    "101000000",
    "101010100",
    "101110100",
    "101100000",
    "101101100",
    "110100000",
    "110000000",
    "110101100",
    "111101100",
    "111111000",
    "1011000000",
    "111011100",
    "1010111100",
    "111010000",
    "1010000000",
    "10111100",
    "100000000",
    "11010100",
    "11011100",
    "10111000",
    "11111000",
    "101010000",
    "101011000",
    "11000000",
    "110110100",
    "101111100",
    "11110100",
    "11101000",
    "11111100",
    "11010000",
    "11101100",
    "110110000",
    "11011000",
    "10110100",
    "10110000",
    "101011100",
    "110101000",
    "101101000",
    "101110000",
    "101111000",
    "110111000",
    "1011101000",
    "1011010000",
    "1011101100",
    "1011010100",
    "1010110000",
    "1010101100",
    "10100",
    "1100000",
    "111000",
    "110100",
    "1000",
    "1010000",
    "1011000",
    "110000",
    "11000",
    "10000000",
    "1110000",
    "101100",
    "1000000",
    "11100",
    "10000",
    "1010100",
    "1111000",
    "100000",
    "101000",
    "1100",
    "111100",
    "1101100",
    "1101000",
    "1110100",
    "1011100",
    "1111100",
    "1011011100",
    "1010111000",
    "1011100000",
    "1011110000",
    "101010000000",
    "101010100000",
    "101010101000",
    "101010101100",
    "101010110000",
    "101010110100",
    "101010111000",
    "101010111100",
    "101011000000",
    "101011010000",
    "101011010100",
    "101011011000",
    "101011011100",
    "101011100000",
    "101011101000",
    "101011101100",
    "101011110000",
    "101011110100",
    "101011111000",
    "101011111100",
    "101100000000",
    "101101000000",
    "101101010000",
    "101101010100",
    "101101011000",
    "101101011100",
    "101101100000",
    "101101101000",
    "101101101100",
    "101101110000",
    "101101110100",
    "101101111000",
    "101101111100",
    "1011110100",
    "1011111000",
    "1011111100",
    "1100000000",
    "1101000000",
    "1101010000",
    "1101010100",
    "1101011000",
    "1101011100",
    "1101100000",
    "1101101000",
    "1101101100",
    "1101110000",
    "1101110100",
    "1101111000",
    "1101111100",
    "1110000000",
    "1110100000",
    "1110101000",
    "1110101100",
    "1110110000",
    "1110110100",
    "1110111000",
    "1110111100",
    "1111000000",
    "1111010000",
    "1111010100",
    "1111011000",
    "1111011100",
    "1111100000",
    "1111101000",
    "1111101100",
    "1111110000",
    "1111110100",
    "1111111000",
    "1111111100",
    "10000000000",
    "10100000000",
    "10101000000",
    "10101010000",
    "10101010100",
    "10101011000",
    "10101011100",
    "10101100000",
    "10101101000",
    "10101101100",
    "10101110000",
    "10101110100",
    "10101111000",
    "10101111100",
    "10110000000",
    "10110100000",
    "10110101000",
    "10110101100",
    "10110110000",
    "10110110100",
    "10110111000",
    "10110111100",
    "10111000000",
    "10111010000",
    "10111010100",
    "10111011000",
    "10111011100",
    "10111100000",
    "10111101000",
    "10111101100",
    "10111110000",
    "10111110100",
    "10111111000",
    "10111111100",
    "11000000000",
    "11010000000",
    "11010100000",
    "11010101000",
    "11010101100",
    "11010110000",
    "11010110100",
    "11010111000",
    "11010111100",
    "11011000000",
    "11011010000",
    "11011010100",
    "11011011000",
    "11011011100",
    "11011100000",
    "11011101000",
    "11011101100",
    "11011110000",
    "11011110100",
    "11011111000",
    "11011111100",
    "11100000000",
    "11101000000",
    "11101010000",
    "11101010100",
    "11101011000"
}};

int VaricodeIndexFor(std::string_view code) {
  for (std::size_t index = 0; index < kMfsK16Varicode.size(); ++index) {
    if (kMfsK16Varicode[index] == code) {
      return static_cast<int>(index);
    }
  }
  return -1;
}

}  // namespace

ErrorCode EncodeTextToPayload(const std::string& text,
                              std::vector<std::uint8_t>* out_payload) {
  if (out_payload == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  out_payload->assign(text.begin(), text.end());
  return ErrorCode::kOk;
}

ErrorCode DecodePayloadToText(const std::vector<std::uint8_t>& payload,
                              std::string* out_text) {
  if (out_text == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  out_text->assign(payload.begin(), payload.end());
  return ErrorCode::kOk;
}

ErrorCode EncodePayloadToVaricodeBits(
    const std::vector<std::uint8_t>& payload,
    std::vector<std::uint8_t>* out_bits) {
  if (out_bits == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  out_bits->clear();
  for (const std::uint8_t value : payload) {
    const std::string_view code = kMfsK16Varicode[value];
    for (const char bit : code) {
      out_bits->push_back(static_cast<std::uint8_t>(bit == '1'));
    }
  }
  // MFSK16 identifies the inter-character gap as 00 followed by the first
  // one bit of the following character. Supply that look-ahead bit for the
  // final character so a finite clean recording has the same boundary as a
  // live stream followed by the next character.
  out_bits->push_back(1);
  return ErrorCode::kOk;
}

ErrorCode DecodeVaricodeBits(const std::vector<std::uint8_t>& bits,
                             std::vector<std::uint8_t>* out_payload) {
  if (out_payload == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  out_payload->clear();
  std::string code;
  code.reserve(12);
  for (std::size_t bit_index = 0; bit_index < bits.size(); ++bit_index) {
    const std::uint8_t bit = bits[bit_index];
    if (bit > 1) {
      return ErrorCode::kInvalidArgument;
    }
    if (code.empty() && bit == 0) {
      continue;
    }
    code.push_back(bit == 0 ? '0' : '1');
    if (code.size() < 3 || code.compare(code.size() - 3, 3, "001") != 0) {
      continue;
    }
    code.pop_back();
    const int value = VaricodeIndexFor(code);
    if (value < 0) {
      code.clear();
      continue;
    }
    out_payload->push_back(static_cast<std::uint8_t>(value));
    // Preserve the look-ahead 1 as the first bit of the next codeword.
    code = "1";
  }
  return out_payload->empty() ? ErrorCode::kInvalidArgument
                              : ErrorCode::kOk;
}

}  // namespace bag::ultra
