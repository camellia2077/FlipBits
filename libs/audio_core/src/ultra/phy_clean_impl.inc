namespace bag::ultra {

using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {

constexpr double kPi = 3.14159265358979323846;
constexpr uint8_t kFecTapOne = 0b0110101;
constexpr uint8_t kFecTapTwo = 0b1001111;
constexpr int kInfiniteMetric = 1'000'000'000;

uint8_t Parity(uint8_t value) {
  value ^= static_cast<uint8_t>(value >> 4);
  value ^= static_cast<uint8_t>(value >> 2);
  value ^= static_cast<uint8_t>(value >> 1);
  return static_cast<uint8_t>(value & 1U);
}

uint8_t EncodeFecPair(uint8_t state, uint8_t input_bit) {
  const uint8_t register_value =
      static_cast<uint8_t>(((state << 1U) | input_bit) & 0x7FU);
  const uint8_t first = Parity(static_cast<uint8_t>(register_value &
                                                    kFecTapOne));
  const uint8_t second = Parity(static_cast<uint8_t>(register_value &
                                                     kFecTapTwo));
  return static_cast<uint8_t>((first << 1U) | second);
}

std::vector<uint8_t> ConvolutionalEncode(const std::vector<uint8_t>& bits) {
  std::vector<uint8_t> coded;
  coded.reserve(bits.size() * 2);
  uint8_t state = 0;
  for (const uint8_t bit : bits) {
    const uint8_t pair = EncodeFecPair(state, bit);
    coded.push_back(static_cast<uint8_t>((pair >> 1U) & 1U));
    coded.push_back(static_cast<uint8_t>(pair & 1U));
    state = static_cast<uint8_t>(((state << 1U) | bit) & 0x3FU);
  }
  return coded;
}

std::vector<uint8_t> Interleave(const std::vector<uint8_t>& input) {
  const size_t padded_size =
      ((input.size() + 3U) / 4U) * 4U;
  const size_t output_size = padded_size + 160U;
  std::vector<uint8_t> output(output_size, 0);
  for (size_t group = 0; group < padded_size / 4U; ++group) {
    for (size_t bit = 0; bit < 4U; ++bit) {
      const size_t input_index = group * 4U + bit;
      // The receiver writes four received bits into consecutive positions of
      // a 160-bit ring. To make the bit appear on the diagonal read after
      // ten, twenty, or thirty groups, place it in the corresponding future
      // group and bit lane.
      const size_t output_group = group + bit * 10U;
      const size_t output_index = output_group * 4U + bit;
      output[output_index] =
          input_index < input.size() ? input[input_index] : 0;
    }
  }
  return output;
}

std::vector<uint8_t> Deinterleave(const std::vector<uint8_t>& input) {
  std::array<uint8_t, 160> register_bits{};
  std::vector<uint8_t> output(input.size(), 0);
  size_t index = 0;
  for (size_t offset = 0; offset + 4U <= input.size(); offset += 4U) {
    for (size_t bit = 0; bit < 4U; ++bit) {
      output[offset + bit] =
          register_bits[(index + bit * 41U) % register_bits.size()];
    }
    for (size_t bit = 0; bit < 4U; ++bit) {
      register_bits[(index + bit) % register_bits.size()] =
          input[offset + bit];
    }
    index = (index + 4U) % register_bits.size();
  }
  return output;
}

bool IsValidConfig(const Mfsk16Config& config) {
  const bool supported_symbol_rate =
      config.symbol_rate_baud == kMfsK16SymbolRateBaud ||
      config.symbol_rate_baud == kMfsK16FastSymbolRateBaud;
  return config.sample_rate_hz >= 16 && config.symbol_samples > 0 &&
         supported_symbol_rate && config.amplitude > 0.0 &&
         config.amplitude <= 1.0;
}

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

size_t ToneIndexForNibble(uint8_t nibble) {
  for (size_t tone = 0; tone < kMfsK16ToneToNibble.size(); ++tone) {
    if (kMfsK16ToneToNibble[tone] == nibble) {
      return tone;
    }
  }
  return 0;
}

std::vector<uint8_t> MfsK16BitsToSymbols(
    const std::vector<uint8_t>& bits) {
  std::vector<uint8_t> coded = ConvolutionalEncode(bits);
  const std::vector<uint8_t> interleaved = Interleave(coded);
  std::vector<uint8_t> symbols;
  symbols.reserve(kMfsK16PreambleSymbolCount +
                 interleaved.size() / 4U + kMfsK16TailSymbolCount);
  symbols.insert(symbols.end(), kMfsK16PreambleSymbolCount, 0);
  for (size_t offset = 0; offset + 4U <= interleaved.size(); offset += 4U) {
    const uint8_t nibble = static_cast<uint8_t>(
        (interleaved[offset] << 3U) |
        (interleaved[offset + 1U] << 2U) |
        (interleaved[offset + 2U] << 1U) |
        interleaved[offset + 3U]);
    symbols.push_back(static_cast<uint8_t>(ToneIndexForNibble(nibble)));
  }
  symbols.insert(symbols.end(), kMfsK16TailSymbolCount, 0);
  return symbols;
}

bool ViterbiDecode(const std::vector<uint8_t>& coded_bits,
                   std::vector<uint8_t>* out_bits) {
  if (out_bits == nullptr || coded_bits.size() % 2U != 0) {
    return false;
  }
  std::array<int, 64> metrics{};
  metrics.fill(kInfiniteMetric);
  metrics[0] = 0;
  std::vector<std::array<uint8_t, 64>> previous_states;
  std::vector<std::array<uint8_t, 64>> previous_bits;
  previous_states.reserve(coded_bits.size() / 2U);
  previous_bits.reserve(coded_bits.size() / 2U);

  for (size_t offset = 0; offset < coded_bits.size(); offset += 2U) {
    std::array<int, 64> next_metrics{};
    next_metrics.fill(kInfiniteMetric);
    std::array<uint8_t, 64> next_states{};
    std::array<uint8_t, 64> next_bits{};
    for (int state = 0; state < 64; ++state) {
      if (metrics[static_cast<size_t>(state)] >= kInfiniteMetric) {
        continue;
      }
      for (uint8_t input_bit = 0; input_bit <= 1; ++input_bit) {
        const uint8_t pair = EncodeFecPair(
            static_cast<uint8_t>(state), input_bit);
        const int next_state =
            static_cast<int>(((static_cast<uint8_t>(state) << 1U) |
                              input_bit) &
                             0x3FU);
        const int metric =
            metrics[static_cast<size_t>(state)] +
            (static_cast<int>((pair >> 1U) & 1U) !=
                     static_cast<int>(coded_bits[offset])
                 ? 1
                 : 0) +
            (static_cast<int>(pair & 1U) !=
                     static_cast<int>(coded_bits[offset + 1U])
                 ? 1
                 : 0);
        if (metric < next_metrics[static_cast<size_t>(next_state)]) {
          next_metrics[static_cast<size_t>(next_state)] = metric;
          next_states[static_cast<size_t>(next_state)] =
              static_cast<uint8_t>(state);
          next_bits[static_cast<size_t>(next_state)] = input_bit;
        }
      }
    }
    metrics = next_metrics;
    previous_states.push_back(next_states);
    previous_bits.push_back(next_bits);
  }

  // The MFSK16 stream is not terminated with a convolutional zero-tail;
  // the four idle symbols are outside the FEC bitstream. Select the best
  // surviving state rather than assuming state zero at the end.
  int state = static_cast<int>(std::distance(
      metrics.begin(), std::min_element(metrics.begin(), metrics.end())));
  out_bits->assign(coded_bits.size() / 2U, 0);
  for (size_t step = previous_states.size(); step-- > 0;) {
    const size_t index = step;
    (*out_bits)[index] = previous_bits[index][static_cast<size_t>(state)];
    state = previous_states[index][static_cast<size_t>(state)];
  }
  return true;
}

size_t FindStrongestSymbol(const int16_t* chunk, size_t chunk_size,
                           const Mfsk16Config& config) {
  std::array<double, 16> powers{};
  for (size_t index = 0; index < config.freqs_hz.size(); ++index) {
    double q1 = 0.0;
    double q2 = 0.0;
    const double normalized =
        config.freqs_hz[index] / static_cast<double>(config.sample_rate_hz);
    const double coefficient = 2.0 * std::cos(2.0 * kPi * normalized);
    for (size_t sample_index = 0; sample_index < chunk_size; ++sample_index) {
      const double sample =
          static_cast<double>(chunk[sample_index]) / 32767.0;
      const double q0 = coefficient * q1 - q2 + sample;
      q2 = q1;
      q1 = q0;
    }
    powers[index] = q1 * q1 + q2 * q2 - coefficient * q1 * q2;
  }
  return static_cast<size_t>(std::distance(
      powers.begin(), std::max_element(powers.begin(), powers.end())));
}

ErrorCode DecodeSymbolsToPayloadImpl(const std::vector<uint8_t>& symbols,
                                     std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr ||
      symbols.size() < kMfsK16PreambleSymbolCount +
                           kMfsK16TailSymbolCount) {
    return ErrorCode::kInvalidArgument;
  }
  for (size_t index = 0; index < kMfsK16PreambleSymbolCount; ++index) {
    if (symbols[index] != 0) {
      return ErrorCode::kInvalidArgument;
    }
  }
  for (size_t index = symbols.size() - kMfsK16TailSymbolCount;
       index < symbols.size(); ++index) {
    if (symbols[index] != 0) {
      return ErrorCode::kInvalidArgument;
    }
  }
  const size_t data_symbol_begin = kMfsK16PreambleSymbolCount;
  const size_t data_symbol_end = symbols.size() - kMfsK16TailSymbolCount;
  if (data_symbol_end <= data_symbol_begin) {
    return ErrorCode::kInvalidArgument;
  }
  std::vector<uint8_t> interleaved_bits;
  interleaved_bits.reserve((data_symbol_end - data_symbol_begin) * 4U);
  for (size_t index = kMfsK16PreambleSymbolCount;
       index < data_symbol_end; ++index) {
    if (symbols[index] >= kMfsK16ToneToNibble.size()) {
      return ErrorCode::kInvalidArgument;
    }
    const uint8_t nibble = kMfsK16ToneToNibble[symbols[index]];
    interleaved_bits.push_back(static_cast<uint8_t>((nibble >> 3U) & 1U));
    interleaved_bits.push_back(static_cast<uint8_t>((nibble >> 2U) & 1U));
    interleaved_bits.push_back(static_cast<uint8_t>((nibble >> 1U) & 1U));
    interleaved_bits.push_back(static_cast<uint8_t>(nibble & 1U));
  }
  const std::vector<uint8_t> deinterleaved_bits =
      Deinterleave(interleaved_bits);
  if (deinterleaved_bits.size() <= 160U) {
    return ErrorCode::kInvalidArgument;
  }
  const std::vector<uint8_t> coded_bits(
      deinterleaved_bits.begin() + 160, deinterleaved_bits.end());

  // The interleaver pads the coded stream to a whole 4-bit MFSK symbol. Try
  // the possible 0..3 padding dibits and retain the candidate whose decoded
  // Varicode reproduces the non-padding bitstream. This is not a frame
  // format; it only removes symbol packing padding.
  std::vector<uint8_t> best_payload;
  std::size_t best_encoded_bit_count = 0;
  for (std::size_t padding_bits = 0; padding_bits <= 6; padding_bits += 2) {
    if (coded_bits.size() <= padding_bits) {
      continue;
    }
    const std::vector<uint8_t> candidate_coded(
        coded_bits.begin(), coded_bits.end() -
                                static_cast<std::ptrdiff_t>(padding_bits));
    std::vector<uint8_t> decoded_bits;
    if (!ViterbiDecode(candidate_coded, &decoded_bits)) {
      continue;
    }
    std::vector<uint8_t> candidate_payload;
    if (DecodeVaricodeBits(decoded_bits, &candidate_payload) !=
        ErrorCode::kOk) {
      continue;
    }
    std::vector<uint8_t> encoded_candidate_bits;
    if (EncodePayloadToVaricodeBits(candidate_payload,
                                    &encoded_candidate_bits) !=
        ErrorCode::kOk ||
        encoded_candidate_bits.size() > decoded_bits.size() ||
        !std::equal(encoded_candidate_bits.begin(),
                    encoded_candidate_bits.end(), decoded_bits.begin()) ||
        !std::all_of(decoded_bits.begin() +
                         static_cast<std::ptrdiff_t>(encoded_candidate_bits.size()),
                     decoded_bits.end(), [](const uint8_t bit) {
                       return bit == 0;
                     })) {
      continue;
    }
    if (encoded_candidate_bits.size() > best_encoded_bit_count) {
      best_encoded_bit_count = encoded_candidate_bits.size();
      best_payload = std::move(candidate_payload);
    }
  }
  if (best_payload.empty()) {
    return ErrorCode::kInvalidArgument;
  }
  *out_payload = std::move(best_payload);
  return ErrorCode::kOk;
}

}  // namespace

ErrorCode DecodeSymbolsToPayload(const std::vector<uint8_t>& symbols,
                                 std::vector<uint8_t>* out_payload) {
  return DecodeSymbolsToPayloadImpl(symbols, out_payload);
}

class SymbolRenderer::Impl {
 public:
  Impl(std::uint8_t symbol, std::size_t write_offset,
       const Mfsk16Config& config, std::vector<int16_t>* out_pcm,
       std::size_t symbol_sample_count, double initial_phase)
      : symbol_(symbol),
        write_offset_(write_offset),
        config_(config),
        out_pcm_(out_pcm),
        symbol_sample_count_(symbol_sample_count),
        phase_(initial_phase) {
    if (out_pcm_ == nullptr || !IsValidConfig(config_) ||
        symbol_ >= kMfsK16ToneToNibble.size() ||
        symbol_sample_count_ == 0 || write_offset_ > out_pcm_->size() ||
        symbol_sample_count_ > out_pcm_->size() - write_offset_) {
      throw std::invalid_argument("Invalid MFSK16 symbol renderer state.");
    }
  }

  std::size_t TotalWork() const {
    return symbol_sample_count_;
  }

  bool Finished() const { return sample_index_ >= TotalWork(); }

  double FinalPhase() const { return phase_; }

  SymbolRenderProgress Pump(std::size_t work_budget) {
    const std::size_t normalized_budget = std::max(work_budget, size_t{1});
    const double frequency = config_.freqs_hz[symbol_];
    std::size_t advanced = 0;
    while (!Finished() && advanced < normalized_budget) {
      const double sample =
          config_.amplitude * std::sin(phase_);
      (*out_pcm_)[write_offset_ + sample_index_] =
          static_cast<int16_t>(std::lround(std::clamp(sample, -1.0, 1.0) *
                                           32767.0));
      ++sample_index_;
      phase_ += 2.0 * kPi * frequency /
                static_cast<double>(config_.sample_rate_hz);
      if (phase_ >= 2.0 * kPi) {
        phase_ = std::fmod(phase_, 2.0 * kPi);
      }
      ++advanced;
    }
    return SymbolRenderProgress{
        .completed_work = sample_index_,
        .total_work = TotalWork(),
        .finished = Finished(),
    };
  }

 private:
  std::uint8_t symbol_ = 0;
  std::size_t write_offset_ = 0;
  Mfsk16Config config_{};
  std::vector<int16_t>* out_pcm_ = nullptr;
  std::size_t symbol_sample_count_ = 0;
  std::size_t sample_index_ = 0;
  double phase_ = 0.0;
};

SymbolRenderer::SymbolRenderer(std::uint8_t symbol, std::size_t write_offset,
                               const Mfsk16Config& config,
                               std::vector<int16_t>* out_pcm,
                               double initial_phase)
    : impl_(std::make_unique<Impl>(symbol, write_offset, config, out_pcm,
                                   static_cast<std::size_t>(config.symbol_samples),
                                   initial_phase)) {}

SymbolRenderer::SymbolRenderer(std::uint8_t symbol, std::size_t write_offset,
                               const Mfsk16Config& config,
                               std::vector<int16_t>* out_pcm,
                               std::size_t symbol_sample_count,
                               double initial_phase)
    : impl_(std::make_unique<Impl>(symbol, write_offset, config, out_pcm,
                                   symbol_sample_count, initial_phase)) {}

SymbolRenderer::~SymbolRenderer() = default;

std::size_t SymbolRenderer::TotalWork() const { return impl_->TotalWork(); }

bool SymbolRenderer::Finished() const { return impl_->Finished(); }

SymbolRenderProgress SymbolRenderer::Pump(std::size_t work_budget) {
  return impl_->Pump(work_budget);
}

double SymbolRenderer::FinalPhase() const { return impl_->FinalPhase(); }

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

ErrorCode EncodePayloadToSymbols(const std::vector<uint8_t>& payload,
                                 const Mfsk16Config& config,
                                 std::vector<uint8_t>* out_symbols) {
  if (out_symbols == nullptr || !IsValidConfig(config) || payload.empty()) {
    return ErrorCode::kInvalidArgument;
  }
  std::vector<uint8_t> varicode_bits;
  const ErrorCode bits_code =
      EncodePayloadToVaricodeBits(payload, &varicode_bits);
  if (bits_code != ErrorCode::kOk) {
    return bits_code;
  }
  *out_symbols = MfsK16BitsToSymbols(varicode_bits);
  return ErrorCode::kOk;
}

ErrorCode EncodeSymbolsToPcm16(const std::vector<uint8_t>& symbols,
                               const Mfsk16Config& config,
                               std::vector<int16_t>* out_pcm,
                               const EncodeProgressSink* progress_sink,
                               float progress_begin, float progress_end) {
  if (out_pcm == nullptr || !IsValidConfig(config)) {
    return ErrorCode::kInvalidArgument;
  }
  const std::size_t total_sample_count =
      TotalSamplesForSymbols(config.sample_rate_hz, symbols.size(),
                             config.symbol_rate_baud);
  if (!symbols.empty() && total_sample_count == 0) {
    return ErrorCode::kInvalidArgument;
  }
  out_pcm->assign(total_sample_count, 0);
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_begin);
  double phase = 0.0;
  for (size_t index = 0; index < symbols.size(); ++index) {
    if (ShouldCancelEncode(progress_sink)) {
      out_pcm->clear();
      return ErrorCode::kCancelled;
    }
    const std::size_t write_offset =
        SymbolBoundarySample(config.sample_rate_hz, index,
                             config.symbol_rate_baud);
    const std::size_t next_boundary =
        SymbolBoundarySample(config.sample_rate_hz, index + 1U,
                             config.symbol_rate_baud);
    if (next_boundary <= write_offset) {
      out_pcm->clear();
      return ErrorCode::kInvalidArgument;
    }
    SymbolRenderer renderer(symbols[index], write_offset, config, out_pcm,
                            next_boundary - write_offset, phase);
    while (!renderer.Finished()) {
      renderer.Pump(256);
    }
    phase = renderer.FinalPhase();
    ReportEncodeProgress(
        progress_sink, EncodeProgressPhase::kRenderingPcm,
        progress_begin +
            (progress_end - progress_begin) *
                static_cast<float>(index + 1U) /
                static_cast<float>(std::max(symbols.size(), size_t{1})));
  }
  return ErrorCode::kOk;
}

ErrorCode DecodePcm16ToSymbols(const std::vector<int16_t>& pcm,
                               const Mfsk16Config& config,
                               std::vector<uint8_t>* out_symbols) {
  if (out_symbols == nullptr || !IsValidConfig(config)) {
    return ErrorCode::kInvalidArgument;
  }
  std::size_t symbol_count = 0;
  if (pcm.empty() ||
      !TrySymbolCountForSamples(config.sample_rate_hz, pcm.size(),
                                config.symbol_rate_baud,
                                 &symbol_count)) {
    return ErrorCode::kInvalidArgument;
  }
  out_symbols->clear();
  out_symbols->reserve(symbol_count);
  for (size_t index = 0; index < symbol_count; ++index) {
    const size_t offset = SymbolBoundarySample(
        config.sample_rate_hz, index, config.symbol_rate_baud);
    const size_t next_offset =
        SymbolBoundarySample(config.sample_rate_hz, index + 1U,
                             config.symbol_rate_baud);
    out_symbols->push_back(static_cast<uint8_t>(FindStrongestSymbol(
        pcm.data() + offset, next_offset - offset, config)));
  }
  return ErrorCode::kOk;
}

struct DecodedPcmCandidate {
  Mfsk16Speed speed = Mfsk16Speed::k15_625Bd;
  Mfsk16Config config{};
  std::vector<uint8_t> payload;
  std::string text;
};

ErrorCode DecodePcmCandidate(const CoreConfig& core_config,
                             const std::vector<int16_t>& pcm,
                             Mfsk16Speed speed,
                             DecodedPcmCandidate* out_candidate) {
  if (out_candidate == nullptr) {
    return ErrorCode::kInvalidArgument;
  }

  DecodedPcmCandidate candidate{};
  candidate.speed = speed;
  candidate.config = MakeMfsk16Config(core_config, speed);
  std::vector<uint8_t> symbols;
  if (DecodePcm16ToSymbols(pcm, candidate.config, &symbols) !=
      ErrorCode::kOk) {
    return ErrorCode::kInvalidArgument;
  }
  if (DecodeSymbolsToPayloadImpl(symbols, &candidate.payload) !=
      ErrorCode::kOk ||
      DecodePayloadToText(candidate.payload, &candidate.text) !=
          ErrorCode::kOk) {
    return ErrorCode::kInvalidArgument;
  }

  *out_candidate = std::move(candidate);
  return ErrorCode::kOk;
}

ErrorCode ResolvePcmCandidate(const CoreConfig& core_config,
                              const std::vector<int16_t>& pcm,
                              DecodedPcmCandidate* out_candidate) {
  if (out_candidate == nullptr || core_config.sample_rate_hz <= 0) {
    return ErrorCode::kInvalidArgument;
  }

  const bool auto_speed = core_config.frame_samples == kAutoFrameSamples;
  const std::array<Mfsk16Speed, 2> speeds = {
      Mfsk16Speed::k15_625Bd, Mfsk16Speed::k31_25Bd};
  std::vector<DecodedPcmCandidate> candidates;
  for (const Mfsk16Speed speed : speeds) {
    if (!auto_speed &&
        speed != (MakeMfsk16Config(core_config).symbol_rate_baud ==
                          kMfsK16FastSymbolRateBaud
                      ? Mfsk16Speed::k31_25Bd
                      : Mfsk16Speed::k15_625Bd)) {
      continue;
    }
    DecodedPcmCandidate candidate{};
    if (DecodePcmCandidate(core_config, pcm, speed, &candidate) ==
        ErrorCode::kOk) {
      candidates.push_back(std::move(candidate));
    }
  }

  if (candidates.size() != 1U) {
    return ErrorCode::kInvalidArgument;
  }
  *out_candidate = std::move(candidates.front());
  return ErrorCode::kOk;
}

ErrorCode EncodePayloadToPcm16(const std::vector<uint8_t>& payload,
                               const Mfsk16Config& config,
                               std::vector<int16_t>* out_pcm) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  std::vector<uint8_t> symbols;
  const ErrorCode symbols_code =
      EncodePayloadToSymbols(payload, config, &symbols);
  if (symbols_code != ErrorCode::kOk) {
    return symbols_code;
  }
  return EncodeSymbolsToPcm16(symbols, config, out_pcm);
}

ErrorCode DecodePcm16ToPayloadForUnit(
    const std::vector<int16_t>& pcm, const Mfsk16Config& config,
    std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  std::vector<uint8_t> symbols;
  const ErrorCode symbols_code =
      DecodePcm16ToSymbols(pcm, config, &symbols);
  if (symbols_code != ErrorCode::kOk) {
    return symbols_code;
  }
  return DecodeSymbolsToPayloadImpl(symbols, out_payload);
}

ErrorCode DecodePcm16ToPayloadWithResolvedUnit(
    const std::vector<int16_t>& pcm, const Mfsk16Config& config,
    std::vector<uint8_t>* out_payload, int* out_unit_samples) {
  if (out_payload == nullptr || out_unit_samples == nullptr ||
      config.sample_rate_hz <= 0) {
    return ErrorCode::kInvalidArgument;
  }
  Mfsk16Config resolved_config = config;
  resolved_config.symbol_samples =
      NominalSymbolSamples(config.sample_rate_hz, config.symbol_rate_baud);
  const ErrorCode code =
      DecodePcm16ToPayloadForUnit(pcm, resolved_config, out_payload);
  if (code == ErrorCode::kOk) {
    *out_unit_samples = resolved_config.symbol_samples;
  }
  return code;
}

ErrorCode DecodePcm16ToPayload(const std::vector<int16_t>& pcm,
                               const Mfsk16Config& config,
                               std::vector<uint8_t>* out_payload) {
  int resolved_unit_samples = 0;
  return DecodePcm16ToPayloadWithResolvedUnit(
      pcm, config, out_payload, &resolved_unit_samples);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm) {
  return EncodeTextToPcm16(config, text, out_pcm, nullptr,
                          Mfsk16Speed::k15_625Bd);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            Mfsk16Speed speed) {
  return EncodeTextToPcm16(config, text, out_pcm, nullptr, speed);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink) {
  return EncodeTextToPcm16(config, text, out_pcm, progress_sink,
                           Mfsk16Speed::k15_625Bd);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink,
                            Mfsk16Speed speed) {
  if (out_pcm == nullptr || text.empty()) {
    return ErrorCode::kInvalidArgument;
  }
  std::vector<uint8_t> payload;
  const ErrorCode payload_code = EncodeTextToPayload(text, &payload);
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }
  return EncodePayloadToPcm16(payload, MakeMfsk16Config(config, speed),
                              out_pcm);
}

ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<int16_t>& pcm,
                            std::string* out_text) {
  if (config.frame_samples == kAutoFrameSamples) {
    if (out_text == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    DecodedPcmCandidate candidate{};
    const ErrorCode candidate_code =
        ResolvePcmCandidate(config, pcm, &candidate);
    if (candidate_code != ErrorCode::kOk) {
      return candidate_code;
    }
    *out_text = std::move(candidate.text);
    return ErrorCode::kOk;
  }
  return DecodePcm16ToText(config, pcm, out_text,
                           Mfsk16Speed::k15_625Bd);
}

ErrorCode DecodePcm16ToText(const CoreConfig& config,
                            const std::vector<int16_t>& pcm,
                            std::string* out_text, Mfsk16Speed speed) {
  if (out_text == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  std::vector<uint8_t> payload;
  const ErrorCode payload_code = DecodePcm16ToPayload(
      pcm, MakeMfsk16Config(config, speed), &payload);
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }
  return DecodePayloadToText(payload, out_text);
}

class UltraDecoder final : public ITransportDecoder {
 public:
  explicit UltraDecoder(CoreConfig config) : config_(config) {}

  ErrorCode PushPcm(const PcmBlock& block) override {
    if (block.samples == nullptr || block.sample_count == 0) {
      return ErrorCode::kInvalidArgument;
    }
    buffered_pcm_.insert(buffered_pcm_.end(), block.samples,
                         block.samples + block.sample_count);
    has_pending_result_ = true;
    return ErrorCode::kOk;
  }

  ErrorCode PollDecodeResult(DecodeResult* out_result) override {
    if (out_result == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    if (!has_pending_result_ || buffered_pcm_.empty()) {
      out_result->text.clear();
      out_result->raw_payload_bytes.clear();
      out_result->raw_payload_available = false;
      out_result->follow_data = PayloadFollowData{};
      out_result->follow_available = false;
      out_result->text_follow_data = TextFollowData{};
      out_result->text_follow_available = false;
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kUltra;
      out_result->text_status = DecodeContentStatus::kUnavailable;
      return ErrorCode::kNotReady;
    }

    DecodedPcmCandidate candidate{};
    const ErrorCode candidate_code =
        ResolvePcmCandidate(config_, buffered_pcm_, &candidate);
    // Push/Poll is an accumulation interface for a complete recording. A
    // partial symbol block or a recording without the clean tail is not a
    // decode failure yet; more PCM may still arrive.
    if (candidate_code != ErrorCode::kOk) {
      out_result->text.clear();
      out_result->raw_payload_bytes.clear();
      out_result->raw_payload_available = false;
      out_result->follow_data = PayloadFollowData{};
      out_result->follow_available = false;
      out_result->text_follow_data = TextFollowData{};
      out_result->text_follow_available = false;
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kUltra;
      out_result->text_status = DecodeContentStatus::kUnavailable;
      return ErrorCode::kNotReady;
    }

    out_result->raw_payload_bytes = std::move(candidate.payload);
    out_result->raw_payload_available = true;
    CoreConfig resolved_config = config_;
    resolved_config.frame_samples = candidate.config.symbol_samples;
    out_result->follow_data =
        BuildPayloadFollowData(resolved_config, out_result->raw_payload_bytes);
    out_result->follow_available = out_result->follow_data.available;
    out_result->text = std::move(candidate.text);
    out_result->text_status = DecodeContentStatus::kOk;
    out_result->text_follow_data =
        BuildTextFollowData(out_result->follow_data, out_result->text);
    out_result->text_follow_available = out_result->text_follow_data.available;
    out_result->complete = true;
    out_result->confidence = 1.0f;
    out_result->mode = TransportMode::kUltra;
    has_pending_result_ = false;
    return ErrorCode::kOk;
  }

  ErrorCode PollTextResult(TextResult* out_result) override {
    if (out_result == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    DecodeResult decode_result{};
    const ErrorCode code = PollDecodeResult(&decode_result);
    if (code != ErrorCode::kOk) {
      out_result->text.clear();
      out_result->complete = false;
      out_result->confidence = 0.0f;
      out_result->mode = TransportMode::kUltra;
      return code;
    }
    out_result->text = decode_result.text;
    out_result->complete = decode_result.complete;
    out_result->confidence = decode_result.confidence;
    out_result->mode = decode_result.mode;
    return ErrorCode::kOk;
  }

  void Reset() override {
    buffered_pcm_.clear();
    has_pending_result_ = false;
  }

 private:
  CoreConfig config_;
  std::vector<int16_t> buffered_pcm_;
  bool has_pending_result_ = false;
};

std::unique_ptr<ITransportDecoder> CreateDecoder(const CoreConfig& config) {
  return std::make_unique<UltraDecoder>(config);
}

}  // namespace bag::ultra
