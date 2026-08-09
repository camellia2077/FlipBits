namespace bag::pro {
using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {
constexpr double kPi = 3.14159265358979323846;
float LerpProgress(float begin, float end, float ratio) {
  return begin + (end - begin) * std::clamp(ratio, 0.0f, 1.0f);
}

constexpr std::size_t kProgressReportStrideSamples = 256;
constexpr std::size_t kSymbolsPerPayloadByteForProgress = 2;

void ReportCompletedSampleProgress(const EncodeProgressSink* progress_sink,
                                   EncodeProgressPhase phase,
                                   float progress_begin, float progress_end,
                                   std::size_t completed_samples,
                                   std::size_t total_samples) {
  const float ratio =
      total_samples == 0
          ? 1.0f
          : static_cast<float>(std::min(completed_samples, total_samples)) /
                static_cast<float>(total_samples);
  ReportEncodeProgress(progress_sink, phase,
                       LerpProgress(progress_begin, progress_end, ratio));
}

void MaybeReportCompletedSampleProgress(
    const EncodeProgressSink* progress_sink, EncodeProgressPhase phase,
    float progress_begin, float progress_end, std::size_t completed_samples,
    std::size_t total_samples, std::size_t* last_reported_samples,
    bool force = false) {
  if (last_reported_samples == nullptr) {
    return;
  }

  const std::size_t clamped_completed_samples =
      total_samples == 0 ? 0 : std::min(completed_samples, total_samples);
  if (!force && clamped_completed_samples < total_samples &&
      clamped_completed_samples - *last_reported_samples <
          kProgressReportStrideSamples) {
    return;
  }

  ReportCompletedSampleProgress(progress_sink, phase, progress_begin,
                                progress_end, clamped_completed_samples,
                                total_samples);
  *last_reported_samples = clamped_completed_samples;
}

std::size_t NonZeroWork(std::size_t work) {
  return std::max(work, static_cast<std::size_t>(1));
}

struct ProEncodeWorkBudget {
  std::size_t prepare_payload_work = 1;
  std::size_t prepare_symbol_work = 1;
  std::size_t render_work = 1;
  std::size_t finalize_work = 1;
  std::size_t total_work = 4;
};

ProEncodeWorkBudget BuildEncodeWorkBudget(std::size_t payload_size,
                                          int symbol_samples) {
  const std::size_t symbol_count =
      payload_size * kSymbolsPerPayloadByteForProgress;

  ProEncodeWorkBudget budget{};
  budget.prepare_payload_work = NonZeroWork(payload_size);
  budget.prepare_symbol_work = NonZeroWork(symbol_count);
  budget.render_work = NonZeroWork(
      symbol_count * static_cast<std::size_t>(std::max(symbol_samples, 0)));
  budget.finalize_work = 1;
  budget.total_work = budget.prepare_payload_work + budget.prepare_symbol_work +
                      budget.render_work + budget.finalize_work;
  return budget;
}

using SymbolTemplateBank = std::array<std::vector<int16_t>, 16>;

SymbolTemplateBank BuildSymbolTemplateBank(const DualToneConfig& config) {
  SymbolTemplateBank templates{};
  for (std::size_t symbol = 0; symbol < templates.size(); ++symbol) {
    auto& pcm = templates[symbol];
    pcm.resize(static_cast<std::size_t>(config.symbol_samples));
    const double low_freq_hz = config.low_freqs_hz[symbol / 4];
    const double high_freq_hz = config.high_freqs_hz[symbol % 4];
    const double low_omega = 2.0 * kPi * low_freq_hz;
    const double high_omega = 2.0 * kPi * high_freq_hz;
    for (int index = 0; index < config.symbol_samples; ++index) {
      const double time = static_cast<double>(index) /
                          static_cast<double>(config.sample_rate_hz);
      const double low_sample = std::sin(low_omega * time);
      const double high_sample = std::sin(high_omega * time);
      const double sample = config.amplitude * 0.5 * (low_sample + high_sample);
      const double clamped = std::max(-1.0, std::min(1.0, sample));
      pcm[static_cast<std::size_t>(index)] =
          static_cast<int16_t>(std::lround(clamped * 32767.0));
    }
  }
  return templates;
}
}  // namespace

class SymbolRenderer::Impl {
 public:
  Impl(std::uint8_t symbol, std::size_t write_offset,
       const DualToneConfig& config, std::vector<int16_t>* out_pcm)
      : symbol_(symbol),
        write_offset_(write_offset),
        config_(config),
        out_pcm_(out_pcm) {
    if (out_pcm_ == nullptr || !IsValidDualToneConfig(config_) || symbol_ > 0x0F) {
      throw std::invalid_argument("Invalid pro symbol renderer state.");
    }
    const std::size_t total_work =
        static_cast<std::size_t>(std::max(config_.symbol_samples, 0));
    if (write_offset_ + total_work > out_pcm_->size()) {
      throw std::invalid_argument("Pro symbol renderer exceeds PCM bounds.");
    }
  }

  std::size_t TotalWork() const {
    return static_cast<std::size_t>(std::max(config_.symbol_samples, 0));
  }

  bool Finished() const { return sample_index_ >= TotalWork(); }

  SymbolRenderProgress Pump(std::size_t work_budget) {
    const std::size_t normalized_budget =
        std::max(work_budget, static_cast<std::size_t>(1));
    const double low_freq_hz = config_.low_freqs_hz[symbol_ / 4];
    const double high_freq_hz = config_.high_freqs_hz[symbol_ % 4];
    constexpr double kPi = 3.14159265358979323846;
    const double low_omega = 2.0 * kPi * low_freq_hz;
    const double high_omega = 2.0 * kPi * high_freq_hz;
    std::size_t advanced = 0;
    while (!Finished() && advanced < normalized_budget) {
      const double time = static_cast<double>(sample_index_) /
                          static_cast<double>(config_.sample_rate_hz);
      const double sample =
          config_.amplitude * 0.5 *
          (std::sin(low_omega * time) + std::sin(high_omega * time));
      (*out_pcm_)[write_offset_ + sample_index_] = static_cast<int16_t>(
          std::lround(std::clamp(sample, -1.0, 1.0) * 32767.0));
      ++sample_index_;
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
  DualToneConfig config_{};
  std::vector<int16_t>* out_pcm_ = nullptr;
  std::size_t sample_index_ = 0;
};

SymbolRenderer::SymbolRenderer(std::uint8_t symbol, std::size_t write_offset,
                               const DualToneConfig& config,
                               std::vector<int16_t>* out_pcm)
    : impl_(std::make_unique<Impl>(symbol, write_offset, config, out_pcm)) {}

SymbolRenderer::~SymbolRenderer() = default;

std::size_t SymbolRenderer::TotalWork() const {
  return impl_->TotalWork();
}

bool SymbolRenderer::Finished() const {
  return impl_->Finished();
}

SymbolRenderProgress SymbolRenderer::Pump(std::size_t work_budget) {
  return impl_->Pump(work_budget);
}

ErrorCode EncodeSymbolsToPcm16(const std::vector<uint8_t>& symbols,
                               const DualToneConfig& config,
                               std::vector<int16_t>* out_pcm,
                               const EncodeProgressSink* progress_sink,
                               float progress_begin, float progress_end) {
  if (out_pcm == nullptr || !IsValidDualToneConfig(config)) {
    return ErrorCode::kInvalidArgument;
  }

  out_pcm->clear();
  const std::size_t total_samples =
      symbols.size() * static_cast<std::size_t>(config.symbol_samples);
  out_pcm->resize(total_samples);
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_begin);
  if (total_samples == 0) {
    ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                         progress_end);
    return ErrorCode::kOk;
  }

  std::size_t rendered_samples = 0;
  std::size_t last_reported_samples = 0;
  std::size_t write_offset = 0;
  for (size_t symbol_index = 0; symbol_index < symbols.size(); ++symbol_index) {
    if (ShouldCancelEncode(progress_sink)) {
      out_pcm->clear();
      return ErrorCode::kCancelled;
    }
    const uint8_t symbol = symbols[symbol_index];
    if (symbol > 0x0F) {
      return ErrorCode::kInvalidArgument;
    }
    SymbolRenderer renderer(symbol, write_offset, config, out_pcm);
    while (!renderer.Finished()) {
      if (ShouldCancelEncode(progress_sink)) {
        out_pcm->clear();
        return ErrorCode::kCancelled;
      }
      const SymbolRenderProgress progress =
          renderer.Pump(kProgressReportStrideSamples);
      rendered_samples = write_offset + progress.completed_work;
      MaybeReportCompletedSampleProgress(
          progress_sink, EncodeProgressPhase::kRenderingPcm, progress_begin,
          progress_end, rendered_samples, total_samples, &last_reported_samples,
          progress.finished);
    }
    write_offset += renderer.TotalWork();
    MaybeReportCompletedSampleProgress(
        progress_sink, EncodeProgressPhase::kRenderingPcm, progress_begin,
        progress_end, rendered_samples, total_samples, &last_reported_samples,
        true);
  }

  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_end);
  return ErrorCode::kOk;
}
}  // namespace bag::pro
