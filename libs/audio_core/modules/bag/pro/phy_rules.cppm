module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.pro.phy_rules;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.types;

export namespace bag::pro {

struct DualToneConfig {
  std::array<double, 4> low_freqs_hz = {697.0, 770.0, 852.0, 941.0};
  std::array<double, 4> high_freqs_hz = {1209.0, 1336.0, 1477.0, 1633.0};
  int sample_rate_hz = 44100;
  int symbol_samples = 2205;
  double amplitude = 0.8;
};

DualToneConfig MakeDualToneConfig(const CoreConfig& config);
bool IsValidDualToneConfig(const DualToneConfig& config);

}  // namespace bag::pro
