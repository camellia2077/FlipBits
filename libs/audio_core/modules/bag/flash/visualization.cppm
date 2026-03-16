module;

export module bag.flash.visualization;

import std;

export import bag.common.error_code;
export import bag.common.types;
export import bag.flash.voicing;

export namespace bag::flash {

ErrorCode AnalyzeVisualization(const CoreConfig& config,
                               const std::vector<std::int16_t>& pcm,
                               VisualizationResult* out_result);

}  // namespace bag::flash
