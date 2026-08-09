module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.mini.tone_renderer;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.types;
export import bag.mini.morse_rules;

export namespace bag::mini_mode {

struct ToneUnitRenderProgress {
  std::size_t completed_work = 0;
  std::size_t total_work = 0;
  bool finished = false;
};

class ToneUnitRenderer {
 public:
  ToneUnitRenderer(const std::vector<std::uint8_t>& payload,
                   std::size_t payload_index, const MorseToneConfig& config,
                   std::vector<std::int16_t>* out_pcm);
  ~ToneUnitRenderer();

  std::size_t TotalWork() const;
  bool Finished() const;
  ToneUnitRenderProgress Pump(std::size_t work_budget);

 private:
  class Impl;
  std::unique_ptr<Impl> impl_;
};

ErrorCode EncodePayloadToPcm16(
    const std::vector<std::uint8_t>& payload, const MorseToneConfig& config,
    std::vector<std::int16_t>* out_pcm,
    const EncodeProgressSink* progress_sink = nullptr,
    float progress_begin = 0.0f, float progress_end = 1.0f);

}  // namespace bag::mini_mode
