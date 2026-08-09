module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.pro.phy_clean;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.pro.phy_rules;
export import bag.pro.tone_renderer;
export import bag.pro.phy_encode;
export import bag.pro.phy_decode;
