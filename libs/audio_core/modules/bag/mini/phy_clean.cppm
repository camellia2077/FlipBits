module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.mini.phy_clean;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.mini.morse_rules;
export import bag.mini.tone_renderer;
export import bag.mini.phy_encode;
export import bag.mini.phy_decode;
