module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.flash.phy_clean;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.flash.phy_encode;
export import bag.flash.phy_decode;
export import bag.flash.phy_rules;
export import bag.flash.signal;
export import bag.flash.voicing;
