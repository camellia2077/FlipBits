module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.flash.signal_layout;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.flash.signal_rules;

export namespace bag::flash {

enum class FlashPayloadSegmentKind {
  kBit = 0,
  kSilence = 1,
};

struct FlashPayloadChunk {
  FlashPayloadSegmentKind kind = FlashPayloadSegmentKind::kBit;
  std::uint8_t bit_value = 0;
  std::uint8_t source_byte = 0;
  std::uint8_t bit_index_in_byte = 0;
  std::size_t byte_index = 0;
  std::size_t sample_offset = 0;
  std::size_t sample_count = 0;
  double carrier_freq_hz = 0.0;
};

struct FlashPayloadLayout {
  std::vector<FlashPayloadChunk> chunks;
  std::size_t payload_sample_count = 0;
};

struct FlashChunkRenderProgress {
  std::size_t completed_work = 0;
  std::size_t total_work = 0;
  bool finished = false;
};

class FlashChunkRenderer {
 public:
  FlashChunkRenderer(const FlashPayloadChunk& chunk, const BfskConfig& config,
                     std::vector<std::int16_t>* out_pcm);
  ~FlashChunkRenderer();

  std::size_t TotalWork() const;
  bool Finished() const;
  FlashChunkRenderProgress Pump(std::size_t work_budget);

 private:
  class Impl;
  std::unique_ptr<Impl> impl_;
};

FlashPayloadLayout BuildPayloadLayout(const std::vector<std::uint8_t>& bytes,
                                      const BfskConfig& config = {});
FlashPayloadLayout BuildPayloadLayoutForVoicing(
    const std::vector<std::uint8_t>& bytes, const BfskConfig& config,
    FlashVoicingFlavor flavor);

std::vector<std::int16_t> EncodePayloadLayoutToPcm16(
    const FlashPayloadLayout& layout, const BfskConfig& config = {},
    const EncodeProgressSink* progress_sink = nullptr,
    float progress_begin = 0.0f, float progress_end = 1.0f);

}  // namespace bag::flash
