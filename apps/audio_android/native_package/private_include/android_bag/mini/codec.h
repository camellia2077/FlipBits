#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <string_view>
#include <vector>

#include "android_bag/common/error_code.h"

namespace bag::mini_mode {

inline constexpr std::size_t kMorseDotUnits = 1;
inline constexpr std::size_t kMorseDashUnits = 3;
inline constexpr std::size_t kMorseElementGapUnits = 1;
inline constexpr std::size_t kMorseLetterGapUnits = 3;
inline constexpr std::size_t kMorseWordGapUnits = 7;

bool IsMorseText(std::string_view text);
ErrorCode EncodeTextToPayload(const std::string& text, std::vector<std::uint8_t>* out_payload);
ErrorCode DecodePayloadToText(const std::vector<std::uint8_t>& payload, std::string* out_text);
std::string_view MorsePatternForChar(char ch);
char CharForMorsePattern(std::string_view pattern);
std::size_t MorseUnitCountForPayloadByte(std::uint8_t value);
std::size_t MorseTrailingGapUnits(const std::vector<std::uint8_t>& payload, std::size_t byte_index);

}  // namespace bag::mini_mode
