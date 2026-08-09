module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.flash.signal_decode;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.flash.signal_layout;

export namespace bag::flash {

std::vector<std::uint8_t> DecodePcm16ToBytes(
    const std::vector<std::int16_t>& pcm, const BfskConfig& config = {});
std::vector<std::uint8_t> DecodePcm16ToBytesWithPayloadLayout(
    const std::vector<std::int16_t>& pcm, const BfskConfig& config,
    FlashVoicingFlavor flavor);
std::vector<std::uint8_t> DecodePcm16ToBytesSkippingSilence(
    const std::vector<std::int16_t>& pcm, const BfskConfig& config = {});
std::vector<std::uint8_t> DecodePcm16ToBytesSkippingSilenceWithCarrierSchedule(
    const std::vector<std::int16_t>& pcm, const BfskConfig& config,
    FlashVoicingFlavor flavor);
std::vector<std::uint8_t> DecodeZealPcm16ToBytes(
    const std::vector<std::int16_t>& pcm, const BfskConfig& config = {});

}  // namespace bag::flash
