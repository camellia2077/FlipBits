module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.flash.facade;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.config;
export import bag.common.error_code;
export import bag.common.types;
export import bag.flash.codec;
export import bag.flash.phy_clean;
export import bag.flash.signal;
export import bag.flash.voicing;
