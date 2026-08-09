module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.pro.tone_renderer;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.types;
export import bag.pro.phy_rules;

export namespace bag::pro {

struct SymbolRenderProgress {
  std::size_t completed_work = 0;
  std::size_t total_work = 0;
  bool finished = false;
};

class SymbolRenderer {
 public:
  SymbolRenderer(std::uint8_t symbol, std::size_t write_offset,
                 const DualToneConfig& config,
                 std::vector<std::int16_t>* out_pcm);
  ~SymbolRenderer();

  std::size_t TotalWork() const;
  bool Finished() const;
  SymbolRenderProgress Pump(std::size_t work_budget);

 private:
  class Impl;
  std::unique_ptr<Impl> impl_;
};

ErrorCode EncodeSymbolsToPcm16(
    const std::vector<std::uint8_t>& symbols, const DualToneConfig& config,
    std::vector<std::int16_t>* out_pcm,
    const EncodeProgressSink* progress_sink = nullptr,
    float progress_begin = 0.0f, float progress_end = 1.0f);

}  // namespace bag::pro
