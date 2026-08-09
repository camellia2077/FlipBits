module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.ultra.phy_encode;

import bag.ultra.codec;
import bag.ultra.phy_rules;
import bag.ultra.tone_renderer;

#include "phy_encode_impl.inc"
