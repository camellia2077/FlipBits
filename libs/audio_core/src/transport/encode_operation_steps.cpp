module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.transport.encode_operation_steps;

import bag.flash.codec;
import bag.mini.codec;
import bag.pro.codec;
import bag.transport.follow;
import bag.ultra.codec;

#include "encode_operation_steps_impl.inc"
