module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.transport.encode_operation_steps;

import bag.flash.codec;
import bag.flash.signal;
import bag.flash.voicing;
import bag.mini.codec;
import bag.mini.morse_rules;
import bag.mini.tone_renderer;
import bag.pro.codec;
import bag.pro.phy_rules;
import bag.pro.tone_renderer;
import bag.transport.follow;
import bag.ultra.codec;
import bag.ultra.phy_encode;
import bag.ultra.phy_rules;
import bag.ultra.tone_renderer;

#include "encode_operation_steps_impl.inc"
