module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.transport.follow;

import bag.flash.signal;
import bag.flash.voicing;
import bag.mini.codec;
import bag.mini.morse_rules;
import bag.ultra.codec;
import bag.ultra.phy_encode;
import bag.ultra.phy_rules;

#include "follow_impl.inc"
