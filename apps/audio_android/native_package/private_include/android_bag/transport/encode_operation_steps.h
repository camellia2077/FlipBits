#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "android_bag/common/config.h"
#include "android_bag/common/error_code.h"
#include "android_bag/common/types.h"
#include "android_bag/flash/phy_clean.h"
#include "android_bag/mini/phy_clean.h"
#include "android_bag/pro/phy_clean.h"
#include "android_bag/ultra/phy_clean.h"

namespace bag {

enum class EncodeOperationInternalStage {
  kPreparingInput = 0,
  kRenderingPcm = 1,
  kPostprocessing = 2,
  kFinalizing = 3,
  kDone = 4,
};

enum class EncodeOperationPrepareSubstage {
  kPayload = 0,
  kLayoutOrFrame = 1,
  kSymbols = 2,
  kDone = 3,
};

struct EncodeOperationStepState {
  EncodeOperationInternalStage stage =
      EncodeOperationInternalStage::kPreparingInput;
  EncodeOperationPrepareSubstage prepare_substage =
      EncodeOperationPrepareSubstage::kPayload;
  std::uint64_t prepare_completed_units = 0;
  std::uint64_t render_completed_units = 0;
  std::uint64_t postprocess_completed_units = 0;
  std::vector<std::uint8_t> raw_payload_bytes;
  std::vector<std::uint8_t> frame_bytes;
  std::vector<std::uint8_t> render_symbols;
  std::vector<std::int16_t> stage_pcm;
  std::size_t render_index = 0;
  FlashSignalProfile flash_signal_profile = FlashSignalProfile::kStandard;
  FlashVoicingFlavor flash_voicing_flavor = FlashVoicingFlavor::kStandard;
  flash::BfskConfig flash_signal_config{};
  flash::FlashVoicingConfig flash_voicing_config{};
  flash::FlashPayloadLayout flash_payload_layout{};
  std::unique_ptr<flash::FlashChunkRenderer> flash_chunk_renderer;
  std::unique_ptr<flash::FlashVoicingStepper> flash_voicing_stepper;
  std::unique_ptr<pro::SymbolRenderer> pro_symbol_renderer;
  std::unique_ptr<ultra::SymbolRenderer> ultra_symbol_renderer;
  std::unique_ptr<mini_mode::ToneUnitRenderer> mini_tone_renderer;
  pro::DualToneConfig pro_config{};
  ultra::Mfsk16Config ultra_config{};
  mini_mode::MorseToneConfig mini_config{};
};

class IEncodeOperationStepHost {
 public:
  virtual ~IEncodeOperationStepHost() = default;
  virtual std::uint64_t PhaseCompletedWorkUnits() const = 0;
  virtual void UpdatePhaseProgress(EncodeProgressPhase phase,
                                   std::uint64_t phase_completed_units) = 0;
  virtual void MarkFailed(ErrorCode code) = 0;
  virtual void MarkSucceeded() = 0;
};

bool PumpEncodeOperationStep(const CoreConfig& config,
                             const std::string& text,
                             EncodeWorkPlan* work_plan,
                             EncodedPcmFollowResult* result,
                             EncodeOperationStepState* state,
                             IEncodeOperationStepHost* host);

}  // namespace bag
