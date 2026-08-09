namespace bag::mini_mode {
using std::int16_t;
using std::size_t;
using std::uint8_t;

namespace {
constexpr double kPi = 3.14159265358979323846;
constexpr std::size_t kProgressReportStrideSamples = 256;
constexpr double kToneEnvelopeSeconds = 0.005;

float LerpProgress(float begin, float end, float ratio) {
  return begin + (end - begin) * std::clamp(ratio, 0.0f, 1.0f);
}

void ReportSampleProgress(const EncodeProgressSink* progress_sink,
                          float progress_begin, float progress_end,
                          std::size_t completed_samples,
                          std::size_t total_samples) {
  const float ratio =
      total_samples == 0
          ? 1.0f
          : static_cast<float>(std::min(completed_samples, total_samples)) /
                static_cast<float>(total_samples);
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       LerpProgress(progress_begin, progress_end, ratio));
}

void AppendSilenceUnits(std::size_t unit_count, const MorseToneConfig& config,
                        std::vector<int16_t>* out_pcm) {
  if (out_pcm == nullptr || unit_count == 0) {
    return;
  }
  out_pcm->insert(out_pcm->end(),
                  unit_count * static_cast<std::size_t>(config.unit_samples),
                  0);
}

std::size_t ToneEnvelopeRampSampleCount(const MorseToneConfig& config,
                                        std::size_t sample_count) {
  if (sample_count < 2 || config.sample_rate_hz <= 0) {
    return 0;
  }
  const std::size_t target_samples = static_cast<std::size_t>(std::lround(
      static_cast<double>(config.sample_rate_hz) * kToneEnvelopeSeconds));
  return std::min(target_samples, sample_count / static_cast<std::size_t>(2));
}

double ToneEnvelopeGain(std::size_t sample_index, std::size_t sample_count,
                        const MorseToneConfig& config) {
  const std::size_t ramp_samples =
      ToneEnvelopeRampSampleCount(config, sample_count);
  if (ramp_samples == 0 || sample_count == 0) {
    return 1.0;
  }
  double gain = 1.0;
  if (sample_index < ramp_samples) {
    const double ratio = static_cast<double>(sample_index) /
                         static_cast<double>(ramp_samples);
    gain = std::min(gain, 0.5 - 0.5 * std::cos(kPi * ratio));
  }
  const std::size_t samples_from_end = sample_count - sample_index - 1;
  if (samples_from_end < ramp_samples) {
    const double ratio = static_cast<double>(samples_from_end) /
                         static_cast<double>(ramp_samples);
    gain = std::min(gain, 0.5 - 0.5 * std::cos(kPi * ratio));
  }
  return gain;
}

int16_t RenderToneSample(std::size_t sample_index, std::size_t sample_count,
                         const MorseToneConfig& config) {
  const double omega = 2.0 * kPi * config.tone_freq_hz;
  const double time =
      static_cast<double>(sample_index) /
      static_cast<double>(config.sample_rate_hz);
  const double sample =
      config.amplitude * ToneEnvelopeGain(sample_index, sample_count, config) *
      std::sin(omega * time);
  const double clamped = std::clamp(sample, -1.0, 1.0);
  return static_cast<int16_t>(std::lround(clamped * 32767.0));
}

void AppendToneUnits(std::size_t unit_count, const MorseToneConfig& config,
                     std::vector<int16_t>* out_pcm) {
  if (out_pcm == nullptr || unit_count == 0) {
    return;
  }
  const std::size_t sample_count =
      unit_count * static_cast<std::size_t>(config.unit_samples);
  const std::size_t start_offset = out_pcm->size();
  out_pcm->resize(start_offset + sample_count);
  for (std::size_t index = 0; index < sample_count; ++index) {
    (*out_pcm)[start_offset + index] =
        RenderToneSample(index, sample_count, config);
  }
}
}  // namespace

class ToneUnitRenderer::Impl {
 public:
  Impl(const std::vector<uint8_t>& payload, std::size_t payload_index,
       const MorseToneConfig& config, std::vector<int16_t>* out_pcm)
      : payload_(payload),
        payload_index_(payload_index),
        config_(config),
        out_pcm_(out_pcm) {
    if (out_pcm_ == nullptr || !IsValidMorseToneConfig(config_) ||
        payload_index_ >= payload_.size()) {
      throw std::invalid_argument("Invalid mini tone renderer state.");
    }
    BuildSegments();
  }

  std::size_t TotalWork() const { return total_work_; }
  bool Finished() const { return completed_work_ >= total_work_; }

  ToneUnitRenderProgress Pump(std::size_t work_budget) {
    const std::size_t normalized_budget =
        std::max(work_budget, static_cast<std::size_t>(1));
    std::size_t advanced = 0;
    while (!Finished() && advanced < normalized_budget) {
      if (segment_index_ >= segments_.size()) {
        break;
      }
      const Segment& segment = segments_[segment_index_];
      const std::size_t write_index = start_offset_ + completed_work_;
      if (!segment.is_tone) {
        (*out_pcm_)[write_index] = 0;
      } else {
        (*out_pcm_)[write_index] =
            RenderToneSample(segment_sample_index_, segment.sample_count,
                             config_);
      }
      ++segment_sample_index_;
      ++completed_work_;
      ++advanced;
      if (segment_sample_index_ >= segment.sample_count) {
        ++segment_index_;
        segment_sample_index_ = 0;
      }
    }
    return ToneUnitRenderProgress{
        .completed_work = completed_work_,
        .total_work = total_work_,
        .finished = Finished(),
    };
  }

 private:
  struct Segment {
    bool is_tone = false;
    std::size_t sample_count = 0;
  };

  void AppendSegment(bool is_tone, std::size_t unit_count) {
    if (unit_count == 0) {
      return;
    }
    const std::size_t sample_count =
        unit_count * static_cast<std::size_t>(config_.unit_samples);
    segments_.push_back(
        Segment{.is_tone = is_tone, .sample_count = sample_count});
    total_work_ += sample_count;
  }

  void BuildSegments() {
    start_offset_ = out_pcm_->size();
    const char ch = static_cast<char>(payload_[payload_index_]);
    if (ch == ' ') {
      AppendSegment(false, kMorseWordGapUnits);
    } else {
      const std::string_view pattern = MorsePatternForChar(ch);
      if (pattern.empty()) {
        throw std::invalid_argument("Invalid mini Morse symbol.");
      }
      for (std::size_t index = 0; index < pattern.size(); ++index) {
        AppendSegment(true,
                      pattern[index] == '-' ? kMorseDashUnits : kMorseDotUnits);
        if (index + 1 < pattern.size()) {
          AppendSegment(false, kMorseElementGapUnits);
        }
      }
      AppendSegment(false, MorseTrailingGapUnits(payload_, payload_index_));
    }
    out_pcm_->resize(start_offset_ + total_work_);
  }

  const std::vector<uint8_t>& payload_;
  std::size_t payload_index_ = 0;
  MorseToneConfig config_{};
  std::vector<int16_t>* out_pcm_ = nullptr;
  std::vector<Segment> segments_;
  std::size_t start_offset_ = 0;
  std::size_t total_work_ = 0;
  std::size_t completed_work_ = 0;
  std::size_t segment_index_ = 0;
  std::size_t segment_sample_index_ = 0;
};

ToneUnitRenderer::ToneUnitRenderer(const std::vector<uint8_t>& payload,
                                   std::size_t payload_index,
                                   const MorseToneConfig& config,
                                   std::vector<int16_t>* out_pcm)
    : impl_(std::make_unique<Impl>(payload, payload_index, config, out_pcm)) {}

ToneUnitRenderer::~ToneUnitRenderer() = default;

std::size_t ToneUnitRenderer::TotalWork() const {
  return impl_->TotalWork();
}

bool ToneUnitRenderer::Finished() const {
  return impl_->Finished();
}

ToneUnitRenderProgress ToneUnitRenderer::Pump(std::size_t work_budget) {
  return impl_->Pump(work_budget);
}

ErrorCode EncodePayloadToPcm16(const std::vector<uint8_t>& payload,
                               const MorseToneConfig& config,
                               std::vector<int16_t>* out_pcm,
                               const EncodeProgressSink* progress_sink,
                               float progress_begin, float progress_end) {
  if (out_pcm == nullptr || !IsValidMorseToneConfig(config)) {
    return ErrorCode::kInvalidArgument;
  }

  out_pcm->clear();
  const std::size_t total_samples =
      EncodedPayloadUnitCount(payload) *
      static_cast<std::size_t>(config.unit_samples);
  out_pcm->reserve(total_samples);
  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_begin);

  std::size_t last_reported_samples = 0;
  for (std::size_t byte_index = 0; byte_index < payload.size(); ++byte_index) {
    if (ShouldCancelEncode(progress_sink)) {
      out_pcm->clear();
      return ErrorCode::kCancelled;
    }
    try {
      ToneUnitRenderer renderer(payload, byte_index, config, out_pcm);
      const std::size_t chunk_start_offset =
          out_pcm->size() - renderer.TotalWork();
      while (!renderer.Finished()) {
        if (ShouldCancelEncode(progress_sink)) {
          out_pcm->clear();
          return ErrorCode::kCancelled;
        }
        const ToneUnitRenderProgress progress =
            renderer.Pump(kProgressReportStrideSamples);
        const std::size_t rendered_samples =
            chunk_start_offset + progress.completed_work;
        if (rendered_samples - last_reported_samples >=
                kProgressReportStrideSamples ||
            progress.finished) {
          ReportSampleProgress(progress_sink, progress_begin, progress_end,
                               rendered_samples, total_samples);
          last_reported_samples = rendered_samples;
        }
      }
    } catch (const std::invalid_argument&) {
      out_pcm->clear();
      return ErrorCode::kInvalidArgument;
    }
  }

  ReportEncodeProgress(progress_sink, EncodeProgressPhase::kRenderingPcm,
                       progress_end);
  return ErrorCode::kOk;
}
}  // namespace bag::mini_mode
