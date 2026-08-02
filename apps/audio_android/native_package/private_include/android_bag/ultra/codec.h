#pragma once

#include <cstddef>
#include <cstdint>
#include <array>
#include <string>
#include <vector>

#include "android_bag/common/error_code.h"

namespace bag::ultra {

enum class Mfsk16Speed : std::uint8_t {
    k15_625Bd = 0,
    k31_25Bd = 1,
};

inline constexpr double kMfsK16SymbolRateBaud = 15.625;
inline constexpr double kMfsK16ToneSpacingHz = 15.625;
inline constexpr double kMfsK16FastSymbolRateBaud = 31.25;
inline constexpr double kMfsK16FastToneSpacingHz = 31.25;
inline constexpr std::size_t kMfsK16PreambleSymbolCount = 8;
inline constexpr std::size_t kMfsK16TailSymbolCount = 4;
inline constexpr std::array<std::uint8_t, 16> kMfsK16ToneToNibble = {
    0x0, 0x1, 0x3, 0x2, 0x6, 0x7, 0x5, 0x4,
    0xC, 0xD, 0xF, 0xE, 0xA, 0xB, 0x9, 0x8};

ErrorCode EncodeTextToPayload(const std::string& text, std::vector<std::uint8_t>* out_payload);
ErrorCode DecodePayloadToText(const std::vector<std::uint8_t>& payload, std::string* out_text);
ErrorCode EncodePayloadToVaricodeBits(
    const std::vector<std::uint8_t>& payload,
    std::vector<std::uint8_t>* out_bits);
ErrorCode DecodeVaricodeBits(const std::vector<std::uint8_t>& bits,
                             std::vector<std::uint8_t>* out_payload);

}  // namespace bag::ultra
