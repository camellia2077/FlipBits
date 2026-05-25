module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

export module bag.transport.follow;

export import bag.common.config;
export import bag.common.types;

import bag.flash.signal;
import bag.flash.voicing;
import bag.mini.codec;
import bag.pro.codec;
import bag.ultra.codec;

export namespace bag {

PayloadFollowData BuildPayloadFollowData(
    const CoreConfig& config,
    const std::vector<std::uint8_t>& raw_payload_bytes);

TextFollowData BuildTextFollowData(const PayloadFollowData& payload_follow_data,
                                   std::string_view text);

}  // namespace bag
