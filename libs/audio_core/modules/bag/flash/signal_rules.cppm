module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.flash.signal_rules;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.config;
export import bag.common.types;

export namespace bag::flash {

struct BfskConfig {
  double low_freq_hz = 300.0;
  double high_freq_hz = 600.0;
  int sample_rate_hz = 44100;
  std::size_t samples_per_bit = 2205;
  std::size_t samples_per_silence_slot = 2205;
  double bit_duration_sec = 0.05;
  double amplitude = 0.8;
  bool collapse_tone_runs_when_skipping_silence = false;
};

FlashVoicingFlavor NormalizeSignalVoicingFlavor(FlashVoicingFlavor flavor);

BfskConfig MakeBfskConfig(const CoreConfig& config);
BfskConfig MakeBfskConfigForSignalProfile(const CoreConfig& config,
                                          FlashSignalProfile signal_profile);

}  // namespace bag::flash
