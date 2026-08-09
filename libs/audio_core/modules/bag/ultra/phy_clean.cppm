module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.ultra.phy_clean;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.ultra.phy_rules;
export import bag.ultra.tone_renderer;
export import bag.ultra.phy_encode;
export import bag.ultra.phy_decode;
