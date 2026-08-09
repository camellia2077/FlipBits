module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.pro.phy_decode;

import bag.pro.codec;
import bag.pro.phy_rules;
import bag.transport.follow;

#include "phy_decode_impl.inc"
