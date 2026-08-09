module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.ultra.tone_renderer;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.types;
export import bag.ultra.phy_rules;

export namespace bag::ultra {

struct SymbolRenderProgress {
  std::size_t completed_work = 0;
  std::size_t total_work = 0;
  bool finished = false;
};

class SymbolRenderer {
 public:
  SymbolRenderer(std::uint8_t symbol, std::size_t write_offset,
                 const Mfsk16Config& config,
                 std::vector<std::int16_t>* out_pcm,
                 double initial_phase = 0.0);
  SymbolRenderer(std::uint8_t symbol, std::size_t write_offset,
                 const Mfsk16Config& config,
                 std::vector<std::int16_t>* out_pcm,
                 std::size_t symbol_sample_count, double initial_phase);
  ~SymbolRenderer();

  std::size_t TotalWork() const;
  bool Finished() const;
  double FinalPhase() const;
  SymbolRenderProgress Pump(std::size_t work_budget);

 private:
  class Impl;
  std::unique_ptr<Impl> impl_;
};

ErrorCode EncodeSymbolsToPcm16(
    const std::vector<std::uint8_t>& symbols, const Mfsk16Config& config,
    std::vector<std::int16_t>* out_pcm,
    const EncodeProgressSink* progress_sink = nullptr,
    float progress_begin = 0.0f, float progress_end = 1.0f);

}  // namespace bag::ultra
