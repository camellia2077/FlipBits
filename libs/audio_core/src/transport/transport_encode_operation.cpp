module;

#include "bag/common/build_features.h"

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#else
#include "bag/common/std_compat.h"
#endif

module bag.transport.facade;

import bag.transport.encode_operation_steps;
import bag.transport.encode_work_plan;

#include "transport_encode_operation_impl.inc"
