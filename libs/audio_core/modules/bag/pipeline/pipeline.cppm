module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

export module bag.pipeline;


export import bag.common.config;
export import bag.common.error_code;
export import bag.common.types;

export namespace bag {

class IPipeline {
 public:
  virtual ~IPipeline() = default;

  virtual ErrorCode PushPcm(const PcmBlock& block) = 0;
  virtual ErrorCode PollDecodeResult(DecodeResult* out_result) = 0;
  virtual ErrorCode PollTextResult(TextResult* out_result) = 0;
  virtual void Reset() = 0;
};

std::unique_ptr<IPipeline> CreatePipeline(const CoreConfig& config);

}  // namespace bag
