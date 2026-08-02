module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.mini.phy_clean;

import bag.mini.codec;
import bag.transport.follow;

// Mini decode also supports the first-stage auto speed contract and element gaps.
#include "phy_clean_impl.inc"
