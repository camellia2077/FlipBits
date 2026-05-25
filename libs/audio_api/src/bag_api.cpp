#include "bag_api.h"

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

import bag.common.config;
import bag.common.version;
import bag.flash.codec;
import bag.flash.signal;
import bag.transport.facade;

#include "bag_api_impl.inc"
