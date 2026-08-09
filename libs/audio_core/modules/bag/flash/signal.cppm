module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.flash.signal;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.flash.signal_layout;
export import bag.flash.signal_decode;

export namespace bag::flash {
std::vector<std::int16_t> EncodeBytesToPcm16(
    const std::vector<std::uint8_t>& bytes, const BfskConfig& config = {},
    const EncodeProgressSink* progress_sink = nullptr,
    float progress_begin = 0.0f, float progress_end = 1.0f);
}  // namespace bag::flash
