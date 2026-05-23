module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.transport.facade;

import bag.flash.phy_clean;
import bag.flash.codec;
import bag.mini.phy_clean;
import bag.mini.codec;
import bag.pro.phy_clean;
import bag.pro.codec;
import bag.transport.follow;
import bag.ultra.phy_clean;
import bag.ultra.codec;

#include "transport_impl.inc"
