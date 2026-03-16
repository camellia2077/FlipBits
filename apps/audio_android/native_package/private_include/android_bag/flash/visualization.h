#pragma once

#include <cstdint>
#include <vector>

#include "android_bag/common/error_code.h"
#include "android_bag/common/types.h"
#include "android_bag/flash/voicing.h"

namespace bag::flash {

ErrorCode AnalyzeVisualization(const CoreConfig& config,
                               const std::vector<std::int16_t>& pcm,
                               VisualizationResult* out_result);

}  // namespace bag::flash
