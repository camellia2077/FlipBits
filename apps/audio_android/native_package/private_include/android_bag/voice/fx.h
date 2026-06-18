#pragma once

#include <cstdint>
#include <memory>
#include <vector>

#include "android_bag/common/error_code.h"

namespace bag {

enum class VoiceFxPreset {
    kMachineVoice = 0,
    kBinaricCant = 1,
    kSignalCant = 2,
    kRobotVox = 3,
    kRawConstant = 4,
    kVoiceTrigger = 5,
};

enum class VoiceFxSubvoiceStyle {
    kStandard = 0,
    kLitany = 1,
    kHostility = 3,
    kCollapse = 4,
    kZeal = 5,
    kVoid = 6,
};

struct VoiceFxConfig {
    int sample_rate_hz = 0;
    bool enable_diagnostics = false;
    VoiceFxPreset preset = VoiceFxPreset::kMachineVoice;
    VoiceFxSubvoiceStyle subvoice_style = VoiceFxSubvoiceStyle::kStandard;
};

struct VoiceFxResult {
    std::vector<std::int16_t> final_mix;
    std::vector<std::int16_t> main_voice;
    std::vector<std::int16_t> subvoice;
    std::vector<std::int16_t> signal_overlay;
};

class VoiceFxProcessor {
  public:
    explicit VoiceFxProcessor(const VoiceFxConfig& config);
    ~VoiceFxProcessor();
    VoiceFxProcessor(VoiceFxProcessor&&) noexcept;
    VoiceFxProcessor& operator=(VoiceFxProcessor&&) noexcept;
    VoiceFxProcessor(const VoiceFxProcessor&) = delete;
    VoiceFxProcessor& operator=(const VoiceFxProcessor&) = delete;

    ErrorCode ProcessBlock(const std::vector<std::int16_t>& input_pcm,
                           VoiceFxResult* out_result);
    ErrorCode Flush(VoiceFxResult* out_result);
    std::size_t LookbackSamples() const;

  private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

ErrorCode ApplyVoiceFx(const VoiceFxConfig& config,
                       const std::vector<std::int16_t>& input_pcm,
                       VoiceFxResult* out_result);

}  // namespace bag

