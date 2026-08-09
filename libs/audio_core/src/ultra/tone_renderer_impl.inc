namespace bag::ultra {
using std::int16_t;
using std::size_t;
using std::uint8_t;

constexpr double kPi = 3.14159265358979323846;

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
    if (out_pcm_ == nullptr || !IsValidMfsk16Config(config_) ||
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

ErrorCode EncodeSymbolsToPcm16(const std::vector<uint8_t>& symbols,
                               const Mfsk16Config& config,
                               std::vector<int16_t>* out_pcm,
                               const EncodeProgressSink* progress_sink,
                               float progress_begin, float progress_end) {
  if (out_pcm == nullptr || !IsValidMfsk16Config(config)) {
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
}  // namespace bag::ultra
