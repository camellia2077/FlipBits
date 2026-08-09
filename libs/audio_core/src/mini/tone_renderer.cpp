module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.mini.tone_renderer;

import bag.mini.codec;
import bag.mini.morse_rules;

#include "tone_renderer_impl.inc"
