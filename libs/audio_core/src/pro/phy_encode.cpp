module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.pro.phy_encode;

import bag.pro.codec;
import bag.pro.phy_rules;
import bag.pro.tone_renderer;

#include "phy_encode_impl.inc"
