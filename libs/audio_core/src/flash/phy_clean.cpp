module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.flash.phy_clean;

import bag.flash.codec;
import bag.flash.signal;
import bag.flash.voicing;
import bag.transport.follow;

#include "phy_clean_impl.inc"
