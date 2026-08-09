module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.mini.morse_rules;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.types;

export namespace bag::mini_mode {

struct MorseToneConfig {
  double tone_freq_hz = 700.0;
  int sample_rate_hz = 44100;
  int unit_samples = 2205;
  double amplitude = 0.75;
};

MorseToneConfig MakeMorseToneConfig(const CoreConfig& config);
bool IsValidMorseToneConfig(const MorseToneConfig& config);
std::size_t EncodedPayloadUnitCount(
    const std::vector<std::uint8_t>& payload);

}  // namespace bag::mini_mode
