namespace bag::ultra {

using std::size_t;
using std::uint8_t;

namespace {

std::uint16_t Crc16CcittFalse(const std::vector<uint8_t>& bytes,
                              std::size_t offset, std::size_t end) {
  std::uint16_t crc = 0xFFFF;
  for (std::size_t index = offset; index < end; ++index) {
    crc ^= static_cast<std::uint16_t>(bytes[index]) << 8;
    for (int bit = 0; bit < 8; ++bit) {
      if ((crc & 0x8000u) != 0) {
        crc = static_cast<std::uint16_t>((crc << 1) ^ 0x1021u);
      } else {
        crc = static_cast<std::uint16_t>(crc << 1);
      }
    }
  }
  return crc;
}

void AppendUint32BigEndian(std::uint32_t value, std::vector<uint8_t>* out) {
  out->push_back(static_cast<uint8_t>((value >> 24) & 0xFFu));
  out->push_back(static_cast<uint8_t>((value >> 16) & 0xFFu));
  out->push_back(static_cast<uint8_t>((value >> 8) & 0xFFu));
  out->push_back(static_cast<uint8_t>(value & 0xFFu));
}

void AppendUint16BigEndian(std::uint16_t value, std::vector<uint8_t>* out) {
  out->push_back(static_cast<uint8_t>((value >> 8) & 0xFFu));
  out->push_back(static_cast<uint8_t>(value & 0xFFu));
}

std::uint32_t ReadUint32BigEndian(const std::vector<uint8_t>& bytes,
                                  std::size_t offset) {
  return (static_cast<std::uint32_t>(bytes[offset]) << 24) |
         (static_cast<std::uint32_t>(bytes[offset + 1]) << 16) |
         (static_cast<std::uint32_t>(bytes[offset + 2]) << 8) |
         static_cast<std::uint32_t>(bytes[offset + 3]);
}

std::uint16_t ReadUint16BigEndian(const std::vector<uint8_t>& bytes,
                                  std::size_t offset) {
  return static_cast<std::uint16_t>(
      (static_cast<std::uint16_t>(bytes[offset]) << 8) |
      static_cast<std::uint16_t>(bytes[offset + 1]));
}

}  // namespace

ErrorCode EncodeTextToPayload(const std::string& text,
                              std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  out_payload->assign(text.begin(), text.end());
  return ErrorCode::kOk;
}

ErrorCode DecodePayloadToText(const std::vector<uint8_t>& payload,
                              std::string* out_text) {
  if (out_text == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  out_text->assign(payload.begin(), payload.end());
  return ErrorCode::kOk;
}

ErrorCode EncodePayloadToFrame(const std::vector<uint8_t>& payload,
                               std::vector<uint8_t>* out_frame) {
  if (out_frame == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (payload.size() >
      static_cast<std::size_t>(std::numeric_limits<std::uint32_t>::max())) {
    return ErrorCode::kInvalidArgument;
  }

  out_frame->clear();
  out_frame->reserve(kCleanFrameV1FixedByteCount + payload.size());
  out_frame->insert(out_frame->end(), kCleanFrameV1Preamble.begin(),
                    kCleanFrameV1Preamble.end());
  out_frame->insert(out_frame->end(), kCleanFrameV1Sync.begin(),
                    kCleanFrameV1Sync.end());

  const std::size_t crc_begin = out_frame->size();
  out_frame->push_back(kCleanFrameV1Version);
  out_frame->push_back(kCleanFrameV1Flags);
  AppendUint32BigEndian(static_cast<std::uint32_t>(payload.size()), out_frame);
  out_frame->insert(out_frame->end(), payload.begin(), payload.end());
  AppendUint16BigEndian(
      Crc16CcittFalse(*out_frame, crc_begin, out_frame->size()), out_frame);
  return ErrorCode::kOk;
}

ErrorCode DecodeFrameToPayload(const std::vector<uint8_t>& frame,
                               std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  out_payload->clear();
  if (frame.size() < kCleanFrameV1FixedByteCount) {
    return ErrorCode::kInvalidArgument;
  }

  std::size_t offset = 0;
  for (std::uint8_t expected : kCleanFrameV1Preamble) {
    if (frame[offset++] != expected) {
      return ErrorCode::kInvalidArgument;
    }
  }
  for (std::uint8_t expected : kCleanFrameV1Sync) {
    if (frame[offset++] != expected) {
      return ErrorCode::kInvalidArgument;
    }
  }
  const std::size_t crc_begin = offset;
  if (frame[offset++] != kCleanFrameV1Version) {
    return ErrorCode::kInvalidArgument;
  }
  if (frame[offset++] != kCleanFrameV1Flags) {
    return ErrorCode::kInvalidArgument;
  }

  const std::uint32_t payload_size = ReadUint32BigEndian(frame, offset);
  offset += 4;
  const std::size_t payload_begin = offset;
  const std::size_t crc_offset =
      payload_begin + static_cast<std::size_t>(payload_size);
  if (crc_offset + 2 != frame.size()) {
    return ErrorCode::kInvalidArgument;
  }

  const std::uint16_t expected_crc = ReadUint16BigEndian(frame, crc_offset);
  const std::uint16_t actual_crc =
      Crc16CcittFalse(frame, crc_begin, crc_offset);
  if (actual_crc != expected_crc) {
    return ErrorCode::kInvalidArgument;
  }

  out_payload->assign(
      frame.begin() + static_cast<std::ptrdiff_t>(payload_begin),
      frame.begin() + static_cast<std::ptrdiff_t>(crc_offset));
  return ErrorCode::kOk;
}

ErrorCode EncodePayloadToSymbols(const std::vector<uint8_t>& payload,
                                 std::vector<uint8_t>* out_symbols) {
  if (out_symbols == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  out_symbols->clear();
  out_symbols->reserve(payload.size() * kSymbolsPerPayloadByte);
  for (uint8_t value : payload) {
    out_symbols->push_back(static_cast<uint8_t>((value >> 4) & 0x0F));
    out_symbols->push_back(static_cast<uint8_t>(value & 0x0F));
  }
  return ErrorCode::kOk;
}

ErrorCode DecodeSymbolsToPayload(const std::vector<uint8_t>& symbols,
                                 std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (symbols.size() % kSymbolsPerPayloadByte != 0) {
    return ErrorCode::kInvalidArgument;
  }

  out_payload->clear();
  out_payload->reserve(symbols.size() / kSymbolsPerPayloadByte);
  for (size_t index = 0; index < symbols.size();
       index += kSymbolsPerPayloadByte) {
    const uint8_t high_nibble = symbols[index];
    const uint8_t low_nibble = symbols[index + 1];
    if (high_nibble > 0x0F || low_nibble > 0x0F) {
      return ErrorCode::kInvalidArgument;
    }
    out_payload->push_back(
        static_cast<uint8_t>((high_nibble << 4) | low_nibble));
  }
  return ErrorCode::kOk;
}

}  // namespace bag::ultra
