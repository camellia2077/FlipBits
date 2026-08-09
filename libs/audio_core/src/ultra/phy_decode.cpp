module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.ultra.phy_decode;

import bag.ultra.codec;
import bag.ultra.phy_rules;
import bag.transport.decoder;
import bag.transport.follow;

#include "phy_decode_impl.inc"
