module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.ultra.phy_clean;

import bag.ultra.codec;
import bag.transport.follow;

// Ultra decode also supports the first-stage auto speed contract.
#include "phy_clean_impl.inc"
