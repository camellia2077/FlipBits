#pragma once

#include <chrono>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <vector>

#include "android_bag/flash/signal.h"

namespace bag::flash {

struct FlashVoicingConfig {
    int sample_rate_hz = 0;
    double attack_ratio = 0.0;
    double release_ratio = 0.0;
    double second_harmonic_gain = 0.0;
    double third_harmonic_gain = 0.0;
    double boundary_click_gain = 0.0;
    bool enable_preamble = false;
    bool enable_epilogue = false;
    std::size_t preamble_sample_count = 0;
    std::size_t epilogue_sample_count = 0;
};

struct FlashVoicingDescriptor {
    std::size_t leading_nonpayload_samples = 0;
    std::size_t trailing_nonpayload_samples = 0;
    std::size_t payload_sample_count = 0;
};

struct FlashVoicingResult {
    std::vector<std::int16_t> pcm;
    FlashVoicingDescriptor descriptor;
};

struct FlashVoicingStepProgress {
    std::size_t completed_work = 0;
    std::size_t total_work = 0;
    bool finished = false;
};

struct FlashVoicingDiagnostics {
    double payload_prepare_ms = 0.0;
    double payload_sample_setup_ms = 0.0;
    double payload_envelope_ms = 0.0;
    double payload_articulation_ms = 0.0;
    double payload_harmonic_ms = 0.0;
    double payload_metallic_ms = 0.0;
    double payload_chant_resonance_ms = 0.0;
    double payload_chant_drone_ms = 0.0;
    double payload_mechanical_throat_ms = 0.0;
    double payload_standard_low_voice_ms = 0.0;
    double payload_hostility_edge_ms = 0.0;
    double payload_boundary_click_ms = 0.0;
    double payload_modulation_ms = 0.0;
    double payload_mix_shape_store_ms = 0.0;
    std::uint64_t payload_voiced_samples = 0;
    std::uint64_t payload_silence_samples = 0;
    std::uint64_t payload_profiled_samples = 0;
};

class FlashVoicingStepper {
 public:
    FlashVoicingStepper(const std::vector<std::int16_t>& clean_payload_pcm,
                        const FlashPayloadLayout& payload_layout,
                        FlashVoicingFlavor flavor,
                        const FlashVoicingConfig& config = {},
                        bool enable_diagnostics = false);
    ~FlashVoicingStepper();

    const FlashVoicingResult& Result() const;
    FlashVoicingDiagnostics Diagnostics() const;
    std::size_t TotalWork() const;
    bool Finished() const;
    FlashVoicingStepProgress Pump(std::size_t work_budget);

 private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

FlashVoicingDescriptor DescribeVoicingOutput(std::size_t total_sample_count,
                                             const FlashVoicingConfig& config = {});

FlashVoicingConfig MakeFormalVoicingConfigForFlavor(const CoreConfig& config,
                                                    FlashVoicingFlavor flavor);

std::vector<std::int16_t> TrimToPayloadPcm(const std::vector<std::int16_t>& voiced_pcm,
                                           const FlashVoicingDescriptor& descriptor);

FlashVoicingResult ApplyVoicingToPayloadWithFlavor(const std::vector<std::int16_t>& clean_payload_pcm,
                                                  const FlashPayloadLayout& payload_layout,
                                                  FlashVoicingFlavor flavor,
                                                  const FlashVoicingConfig& config = {},
                                                  const EncodeProgressSink* progress_sink = nullptr,
                                                  float progress_begin = 0.0f,
                                                  float progress_end = 1.0f);

FlashVoicingResult ApplyVoicingToPayload(const std::vector<std::int16_t>& clean_payload_pcm,
                                         const FlashPayloadLayout& payload_layout,
                                         const FlashVoicingConfig& config = {},
                                         const EncodeProgressSink* progress_sink = nullptr,
                                         float progress_begin = 0.0f,
                                         float progress_end = 1.0f);

}  // namespace bag::flash
