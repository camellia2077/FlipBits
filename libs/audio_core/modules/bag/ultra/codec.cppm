module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

export module bag.ultra.codec;

export import bag.common.error_code;

export namespace bag::ultra {

inline constexpr std::size_t kSymbolsPerPayloadByte = 2;
inline constexpr std::array<std::uint8_t, 8> kCleanFrameV1Preamble = {
    0xA5, 0x5A, 0xA5, 0x5A, 0xA5, 0x5A, 0xA5, 0x5A};
inline constexpr std::array<std::uint8_t, 2> kCleanFrameV1Sync = {0xD3, 0x91};
inline constexpr std::uint8_t kCleanFrameV1Version = 0x01;
inline constexpr std::uint8_t kCleanFrameV1Flags = 0x00;
inline constexpr std::size_t kCleanFrameV1FixedByteCount =
    kCleanFrameV1Preamble.size() + kCleanFrameV1Sync.size() + 1 + 1 + 4 + 2;

ErrorCode EncodeTextToPayload(const std::string& text,
                              std::vector<std::uint8_t>* out_payload);
ErrorCode DecodePayloadToText(const std::vector<std::uint8_t>& payload,
                              std::string* out_text);
ErrorCode EncodePayloadToFrame(const std::vector<std::uint8_t>& payload,
                               std::vector<std::uint8_t>* out_frame);
ErrorCode DecodeFrameToPayload(const std::vector<std::uint8_t>& frame,
                               std::vector<std::uint8_t>* out_payload);
ErrorCode EncodePayloadToSymbols(const std::vector<std::uint8_t>& payload,
                                 std::vector<std::uint8_t>* out_symbols);
ErrorCode DecodeSymbolsToPayload(const std::vector<std::uint8_t>& symbols,
                                 std::vector<std::uint8_t>* out_payload);

}  // namespace bag::ultra
