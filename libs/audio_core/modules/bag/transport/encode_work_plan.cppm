module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.transport.encode_work_plan;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.config;
export import bag.common.types;

export namespace bag {

EncodeWorkPlan BuildEncodeWorkPlan(const CoreConfig& config,
                                   const std::string& text);

}  // namespace bag
