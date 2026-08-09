module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.mini.phy_encode;

import bag.mini.codec;
import bag.mini.morse_rules;
import bag.mini.tone_renderer;

#include "phy_encode_impl.inc"
