module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.flash.phy_rules;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.config;
export import bag.common.error_code;
export import bag.common.types;
export import bag.flash.signal;
export import bag.flash.voicing;

export namespace bag::flash {

struct FlashEncodeWorkBudget {
  std::size_t prepare_text_work = 1;
  std::size_t prepare_layout_work = 1;
  std::size_t render_work = 1;
  std::size_t postprocess_work = 1;
  std::size_t finalize_work = 1;
  std::size_t total_work = 5;
};

FlashSignalProfile NormalizeFormalSignalProfile(
    FlashSignalProfile signal_profile);
FlashVoicingFlavor NormalizeFormalVoicingFlavor(FlashVoicingFlavor flavor);
bool IsFormalVoicingCompatible(const FlashVoicingResult& voiced_payload,
                               std::size_t clean_payload_sample_count,
                               const FlashVoicingConfig& config);
std::vector<std::int16_t> TrimFormalPayloadPcmForFlavor(
    const CoreConfig& config, FlashVoicingFlavor flavor,
    const std::vector<std::int16_t>& pcm);
FlashEncodeWorkBudget BuildFlashEncodeWorkBudget(
    const std::vector<std::uint8_t>& bytes,
    const FlashPayloadLayout& payload_layout,
    const FlashVoicingConfig& voicing_config);
std::vector<std::uint8_t> DecodeFormalPayloadBytes(
    const CoreConfig& config, FlashSignalProfile signal_profile,
    FlashVoicingFlavor flavor,
    const std::vector<std::int16_t>& trimmed_payload_pcm);

}  // namespace bag::flash
