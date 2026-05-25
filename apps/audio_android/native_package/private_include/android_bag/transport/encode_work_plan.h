#pragma once

#include <string>

#include "android_bag/common/config.h"
#include "android_bag/common/types.h"

namespace bag {

EncodeWorkPlan BuildEncodeWorkPlan(const CoreConfig& config,
                                   const std::string& text);

}  // namespace bag
