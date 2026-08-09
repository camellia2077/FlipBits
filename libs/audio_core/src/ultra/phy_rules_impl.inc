namespace bag::ultra {
using std::size_t;

bool IsValidMfsk16Config(const Mfsk16Config& config) {
  const bool supported_symbol_rate =
      config.symbol_rate_baud == kMfsK16SymbolRateBaud ||
      config.symbol_rate_baud == kMfsK16FastSymbolRateBaud;
  return config.sample_rate_hz >= 16 && config.symbol_samples > 0 &&
         supported_symbol_rate && config.amplitude > 0.0 &&
         config.amplitude <= 1.0;
}

namespace {
bool TryBoundaryParameters(double symbol_rate_baud,
                           std::uint64_t* out_sample_multiplier) {
  if (out_sample_multiplier == nullptr) {
    return false;
  }
  if (symbol_rate_baud == kMfsK16SymbolRateBaud) {
    *out_sample_multiplier = 8U;
    return true;
  }
  if (symbol_rate_baud == kMfsK16FastSymbolRateBaud) {
    *out_sample_multiplier = 4U;
    return true;
  }
  return false;
}

bool TrySymbolBoundarySample(int sample_rate_hz, size_t symbol_index,
                             double symbol_rate_baud, size_t* out_sample) {
  std::uint64_t sample_multiplier = 0;
  if (out_sample == nullptr || sample_rate_hz < 16 ||
      !TryBoundaryParameters(symbol_rate_baud, &sample_multiplier)) {
    return false;
  }
  // The supported rates are exactly 125/8 and 125/4 symbols per second. Use
  // integer arithmetic so the encoder and decoder share identical boundaries.
  constexpr std::uint64_t kBoundaryDenominator = 125U;
  constexpr std::uint64_t kRoundHalf = kBoundaryDenominator / 2U;
  const std::uint64_t boundary_numerator =
      static_cast<std::uint64_t>(sample_rate_hz) * sample_multiplier;
  const std::uint64_t max_sample =
      static_cast<std::uint64_t>(std::numeric_limits<size_t>::max());
  if (symbol_index >
      (max_sample - kRoundHalf) / boundary_numerator) {
    return false;
  }
  const std::uint64_t rounded_sample =
      (boundary_numerator * static_cast<std::uint64_t>(symbol_index) +
       kRoundHalf) /
      kBoundaryDenominator;
  *out_sample = static_cast<size_t>(rounded_sample);
  return true;
}

bool TrySymbolCountForSamples(int sample_rate_hz, size_t sample_count,
                              double symbol_rate_baud,
                              size_t* out_symbol_count) {
  if (out_symbol_count == nullptr || sample_rate_hz < 16) {
    return false;
  }
  size_t low = 0;
  size_t high = sample_count;
  while (low < high) {
    const size_t middle = low + (high - low) / 2U;
    size_t boundary = 0;
    if (!TrySymbolBoundarySample(sample_rate_hz, middle, symbol_rate_baud,
                                 &boundary)) {
      return false;
    }
    if (boundary < sample_count) {
      low = middle + 1U;
    } else {
      high = middle;
    }
  }
  size_t boundary = 0;
  if (!TrySymbolBoundarySample(sample_rate_hz, low, symbol_rate_baud,
                               &boundary) || boundary != sample_count) {
    return false;
  }
  *out_symbol_count = low;
  return true;
}
}  // namespace

int NominalSymbolSamples(int sample_rate_hz, double symbol_rate_baud) {
  std::uint64_t unused_multiplier = 0;
  if (!TryBoundaryParameters(symbol_rate_baud, &unused_multiplier)) {
    return 0;
  }
  return sample_rate_hz > 0
             ? static_cast<int>(std::lround(
                   static_cast<double>(sample_rate_hz) /
                   symbol_rate_baud))
             : 0;
}

std::size_t SymbolBoundarySample(int sample_rate_hz,
                                  std::size_t symbol_index,
                                  double symbol_rate_baud) {
  std::size_t boundary = 0;
  return TrySymbolBoundarySample(sample_rate_hz, symbol_index,
                                 symbol_rate_baud, &boundary)
             ? boundary
             : 0;
}

std::size_t TotalSamplesForSymbols(int sample_rate_hz,
                                    std::size_t symbol_count,
                                    double symbol_rate_baud) {
  return SymbolBoundarySample(sample_rate_hz, symbol_count, symbol_rate_baud);
}

Mfsk16Config MakeMfsk16Config(const CoreConfig& config, Mfsk16Speed speed) {
  Mfsk16Config out{};
  const bool fast = speed == Mfsk16Speed::k31_25Bd;
  const double symbol_rate_baud =
      fast ? kMfsK16FastSymbolRateBaud : kMfsK16SymbolRateBaud;
  const double tone_spacing_hz =
      fast ? kMfsK16FastToneSpacingHz : kMfsK16ToneSpacingHz;
  out.sample_rate_hz = config.sample_rate_hz;
  out.symbol_samples =
      NominalSymbolSamples(config.sample_rate_hz, symbol_rate_baud);
  out.symbol_rate_baud = symbol_rate_baud;
  for (size_t index = 0; index < out.freqs_hz.size(); ++index) {
    out.freqs_hz[index] = 1000.0 + static_cast<double>(index) *
                                         tone_spacing_hz;
  }
  return out;
}

Mfsk16Config MakeMfsk16Config(const CoreConfig& config) {
  const int fast_symbol_samples =
      NominalSymbolSamples(config.sample_rate_hz,
                           kMfsK16FastSymbolRateBaud);
  const Mfsk16Speed speed =
      config.mode == TransportMode::kUltra &&
              config.frame_samples == fast_symbol_samples
          ? Mfsk16Speed::k31_25Bd
          : Mfsk16Speed::k15_625Bd;
  return MakeMfsk16Config(config, speed);
}
}  // namespace bag::ultra
