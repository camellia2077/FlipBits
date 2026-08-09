module;

#include "bag/common/build_features.h"

#if !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include "bag/common/std_compat.h"
#endif

export module bag.ultra.phy_rules;

#if FLIPBITS_HAS_STD_MODULE_PROVIDER
import std;
#endif

export import bag.common.types;
export import bag.ultra.codec;

export namespace bag::ultra {

struct Mfsk16Config {
  std::array<double, 16> freqs_hz = {
      1000.000, 1015.625, 1031.250, 1046.875, 1062.500, 1078.125,
      1093.750, 1109.375, 1125.000, 1140.625, 1156.250, 1171.875,
      1187.500, 1203.125, 1218.750, 1234.375};
  int sample_rate_hz = 44100;
  int symbol_samples = 2822;
  double symbol_rate_baud = kMfsK16SymbolRateBaud;
  double amplitude = 0.8;
};

Mfsk16Config MakeMfsk16Config(const CoreConfig& config);
Mfsk16Config MakeMfsk16Config(const CoreConfig& config, Mfsk16Speed speed);
bool IsValidMfsk16Config(const Mfsk16Config& config);
int NominalSymbolSamples(int sample_rate_hz,
                         double symbol_rate_baud = kMfsK16SymbolRateBaud);
std::size_t SymbolBoundarySample(
    int sample_rate_hz, std::size_t symbol_index,
    double symbol_rate_baud = kMfsK16SymbolRateBaud);
std::size_t TotalSamplesForSymbols(
    int sample_rate_hz, std::size_t symbol_count,
    double symbol_rate_baud = kMfsK16SymbolRateBaud);

}  // namespace bag::ultra
