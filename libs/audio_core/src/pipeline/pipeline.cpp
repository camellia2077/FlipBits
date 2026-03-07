#include "bag/pipeline/pipeline.h"

#include <vector>

#include "bag/fsk/fsk_codec.h"

namespace bag {

namespace {
class BasicFskPipeline final : public IPipeline {
public:
    explicit BasicFskPipeline(CoreConfig config) : config_(config) {
        if (config_.sample_rate_hz <= 0) {
            config_.sample_rate_hz = 44100;
        }
        if (config_.frame_samples <= 0) {
            config_.frame_samples = 2205;
        }
        fsk_config_.sample_rate_hz = config_.sample_rate_hz;
        fsk_config_.bit_duration_sec =
            static_cast<double>(config_.frame_samples) / static_cast<double>(config_.sample_rate_hz);
    }

    ErrorCode PushPcm(const PcmBlock& block) override {
        if (block.samples == nullptr || block.sample_count == 0) {
            return ErrorCode::kInvalidArgument;
        }
        buffered_pcm_.insert(buffered_pcm_.end(), block.samples, block.samples + block.sample_count);
        has_pending_result_ = true;
        return ErrorCode::kOk;
    }

    ErrorCode PollTextResult(TextResult* out_result) override {
        if (out_result == nullptr) {
            return ErrorCode::kInvalidArgument;
        }
        if (!has_pending_result_ || buffered_pcm_.empty()) {
            out_result->text.clear();
            out_result->complete = false;
            out_result->confidence = 0.0f;
            return ErrorCode::kNotReady;
        }

        try {
            out_result->text = fsk::DecodePcm16ToText(buffered_pcm_, fsk_config_);
            out_result->complete = true;
            out_result->confidence = 1.0f;
            has_pending_result_ = false;
            return ErrorCode::kOk;
        } catch (...) {
            out_result->text.clear();
            out_result->complete = false;
            out_result->confidence = 0.0f;
            return ErrorCode::kInternal;
        }
    }

    void Reset() override {
        buffered_pcm_.clear();
        has_pending_result_ = false;
    }

private:
    CoreConfig config_;
    fsk::FskConfig fsk_config_{};
    std::vector<int16_t> buffered_pcm_;
    bool has_pending_result_ = false;
};
}  // namespace

std::unique_ptr<IPipeline> CreatePipeline(const CoreConfig& config) {
    return std::make_unique<BasicFskPipeline>(config);
}

}  // namespace bag
