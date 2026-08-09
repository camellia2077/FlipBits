module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.flash.phy_rules;

import bag.flash.codec;
import bag.flash.signal;
import bag.flash.voicing;

#include "phy_rules_impl.inc"
