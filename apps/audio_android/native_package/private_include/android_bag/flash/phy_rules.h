#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

#include "android_bag/common/types.h"
#include "android_bag/flash/signal.h"
#include "android_bag/flash/voicing.h"

namespace bag::flash {

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
