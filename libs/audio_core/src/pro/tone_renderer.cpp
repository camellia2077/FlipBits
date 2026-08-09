module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.pro.tone_renderer;

import bag.pro.phy_rules;

#include "tone_renderer_impl.inc"
