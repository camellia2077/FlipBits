#pragma once

#include <cstdint>
#include <vector>

#include "android_bag/common/config.h"
#include "android_bag/common/types.h"

namespace bag {

PayloadFollowData BuildPayloadFollowData(
    const CoreConfig& config,
    const std::vector<std::uint8_t>& raw_payload_bytes);

TextFollowData BuildTextFollowData(const PayloadFollowData& payload_follow_data,
                                   std::string_view text);

}  // namespace bag
