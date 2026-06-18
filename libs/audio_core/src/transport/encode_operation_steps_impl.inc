namespace bag {
namespace {

constexpr std::size_t kRenderPumpWorkBudget = 2048;

std::string TextFollowSourceForPayload(
    TransportMode mode, const std::string& text,
    const std::vector<std::uint8_t>& payload) {
  if (mode != TransportMode::kMini) {
    return text;
  }
  std::string normalized_text;
  if (bag::mini_mode::DecodePayloadToText(payload, &normalized_text) ==
      ErrorCode::kOk) {
    return normalized_text;
  }
  return text;
}

FlashSignalProfile NormalizeFormalSignalProfile(
    FlashSignalProfile signal_profile) {
  switch (signal_profile) {
    case FlashSignalProfile::kStandard:
    case FlashSignalProfile::kLitany:
    case FlashSignalProfile::kHostility:
    case FlashSignalProfile::kCollapse:
    case FlashSignalProfile::kZeal:
    case FlashSignalProfile::kVoid:
      return signal_profile;
    default:
      return FlashSignalProfile::kStandard;
  }
}

FlashVoicingFlavor NormalizeFormalVoicingFlavor(FlashVoicingFlavor flavor) {
  switch (flavor) {
    case FlashVoicingFlavor::kStandard:
    case FlashVoicingFlavor::kLitany:
    case FlashVoicingFlavor::kHostility:
    case FlashVoicingFlavor::kCollapse:
    case FlashVoicingFlavor::kZeal:
    case FlashVoicingFlavor::kVoid:
      return flavor;
    default:
      return FlashVoicingFlavor::kStandard;
  }
}

bool PumpPrepareFlash(const CoreConfig& config, const std::string& text,
                      EncodeWorkPlan* work_plan,
                      EncodeOperationStepState* state,
                      IEncodeOperationStepHost* host) {
  if (state->prepare_substage == EncodeOperationPrepareSubstage::kPayload) {
    if (flash::EncodeTextToBytes(text, &state->raw_payload_bytes) !=
        ErrorCode::kOk) {
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
    }
    state->flash_signal_profile =
        NormalizeFormalSignalProfile(config.flash_signal_profile);
    state->flash_voicing_flavor =
        NormalizeFormalVoicingFlavor(config.flash_voicing_flavor);
    state->prepare_completed_units = std::min<std::uint64_t>(
        work_plan->preparing_input_work_units,
        std::max<std::uint64_t>(1, work_plan->preparing_input_work_units / 2));
    host->UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                              state->prepare_completed_units);
    state->prepare_substage = EncodeOperationPrepareSubstage::kLayoutOrFrame;
    return true;
  }
  if (state->prepare_substage ==
      EncodeOperationPrepareSubstage::kLayoutOrFrame) {
    state->flash_signal_config = flash::MakeBfskConfigForSignalProfile(
        config, state->flash_signal_profile);
    state->flash_voicing_config = flash::MakeFormalVoicingConfigForFlavor(
        config, state->flash_voicing_flavor);
    state->flash_payload_layout = flash::BuildPayloadLayoutForVoicing(
        state->raw_payload_bytes, state->flash_signal_config,
        state->flash_voicing_flavor);
    state->stage_pcm.assign(state->flash_payload_layout.payload_sample_count,
                            0);
    work_plan->rendering_pcm_work_units = std::max<std::uint64_t>(
        state->flash_payload_layout.payload_sample_count, 1);
    state->flash_voicing_stepper = std::make_unique<flash::FlashVoicingStepper>(
        state->stage_pcm, state->flash_payload_layout,
        state->flash_voicing_flavor, state->flash_voicing_config,
        config.enable_diagnostics);
    work_plan->postprocessing_work_units =
        std::max<std::uint64_t>(state->flash_voicing_stepper->TotalWork(), 1);
    work_plan->total_work_units = work_plan->preparing_input_work_units +
                                  work_plan->rendering_pcm_work_units +
                                  work_plan->postprocessing_work_units +
                                  work_plan->finalizing_work_units;
    work_plan->estimated_pcm_sample_count =
        state->flash_voicing_stepper->Result().pcm.size();
    state->prepare_completed_units = work_plan->preparing_input_work_units;
    host->UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                              state->prepare_completed_units);
    state->prepare_substage = EncodeOperationPrepareSubstage::kDone;
    state->stage = EncodeOperationInternalStage::kRenderingPcm;
    return true;
  }
  return false;
}

bool PumpPreparePro(const CoreConfig& config, const std::string& text,
                    EncodeWorkPlan* work_plan, EncodeOperationStepState* state,
                    IEncodeOperationStepHost* host) {
  if (state->prepare_substage == EncodeOperationPrepareSubstage::kPayload) {
    if (pro::EncodeTextToPayload(text, &state->raw_payload_bytes) !=
        ErrorCode::kOk) {
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
    }
    state->prepare_completed_units = std::max<std::uint64_t>(
        1, std::min<std::uint64_t>(work_plan->preparing_input_work_units,
                                   state->raw_payload_bytes.size()));
    host->UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                              state->prepare_completed_units);
    state->prepare_substage = EncodeOperationPrepareSubstage::kSymbols;
    return true;
  }
  if (state->prepare_substage == EncodeOperationPrepareSubstage::kSymbols) {
    if (pro::EncodePayloadToSymbols(state->raw_payload_bytes,
                                    &state->render_symbols) != ErrorCode::kOk) {
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
    }
    state->pro_config = pro::MakeDualToneConfig(config);
    state->stage_pcm.assign(state->render_symbols.size() *
                                static_cast<std::size_t>(std::max(
                                    state->pro_config.symbol_samples, 0)),
                            0);
    state->prepare_completed_units = work_plan->preparing_input_work_units;
    host->UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                              state->prepare_completed_units);
    state->prepare_substage = EncodeOperationPrepareSubstage::kDone;
    state->stage = EncodeOperationInternalStage::kRenderingPcm;
    return true;
  }
  return false;
}

bool PumpPrepareUltra(const CoreConfig& config, const std::string& text,
                      EncodeWorkPlan* work_plan,
                      EncodeOperationStepState* state,
                      IEncodeOperationStepHost* host) {
  if (state->prepare_substage == EncodeOperationPrepareSubstage::kPayload) {
    if (ultra::EncodeTextToPayload(text, &state->raw_payload_bytes) !=
        ErrorCode::kOk) {
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
    }
    state->prepare_completed_units = std::max<std::uint64_t>(
        1, std::min<std::uint64_t>(work_plan->preparing_input_work_units,
                                   state->raw_payload_bytes.size()));
    host->UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                              state->prepare_completed_units);
    state->prepare_substage = EncodeOperationPrepareSubstage::kLayoutOrFrame;
    return true;
  }
  if (state->prepare_substage ==
      EncodeOperationPrepareSubstage::kLayoutOrFrame) {
    if (ultra::EncodePayloadToFrame(state->raw_payload_bytes,
                                    &state->frame_bytes) != ErrorCode::kOk) {
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
    }
    state->prepare_completed_units = std::max<std::uint64_t>(
        state->prepare_completed_units + 1,
        std::min<std::uint64_t>(
            work_plan->preparing_input_work_units,
            std::max<std::size_t>(state->frame_bytes.size(), 1)));
    host->UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                              state->prepare_completed_units);
    state->prepare_substage = EncodeOperationPrepareSubstage::kSymbols;
    return true;
  }
  if (state->prepare_substage == EncodeOperationPrepareSubstage::kSymbols) {
    if (ultra::EncodePayloadToSymbols(
            state->frame_bytes, &state->render_symbols) != ErrorCode::kOk) {
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
    }
    state->ultra_config = ultra::MakeMfsk16Config(config);
    state->stage_pcm.assign(state->render_symbols.size() *
                                static_cast<std::size_t>(std::max(
                                    state->ultra_config.symbol_samples, 0)),
                            0);
    state->prepare_completed_units = work_plan->preparing_input_work_units;
    host->UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                              state->prepare_completed_units);
    state->prepare_substage = EncodeOperationPrepareSubstage::kDone;
    state->stage = EncodeOperationInternalStage::kRenderingPcm;
    return true;
  }
  return false;
}

bool PumpPrepareMini(const CoreConfig& config, const std::string& text,
                     const EncodeWorkPlan& work_plan,
                     EncodeOperationStepState* state,
                     IEncodeOperationStepHost* host) {
  if (state->prepare_substage != EncodeOperationPrepareSubstage::kPayload) {
    return false;
  }
  if (mini_mode::EncodeTextToPayload(text, &state->raw_payload_bytes) !=
      ErrorCode::kOk) {
    host->MarkFailed(ErrorCode::kInvalidArgument);
    return false;
  }
  state->mini_config = mini_mode::MakeMorseToneConfig(config);
  state->stage_pcm.clear();
  state->stage_pcm.reserve(work_plan.estimated_pcm_sample_count);
  state->prepare_completed_units = work_plan.preparing_input_work_units;
  host->UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                            state->prepare_completed_units);
  state->prepare_substage = EncodeOperationPrepareSubstage::kDone;
  state->stage = EncodeOperationInternalStage::kRenderingPcm;
  return true;
}

bool PumpPrepareStep(const CoreConfig& config, const std::string& text,
                     EncodeWorkPlan* work_plan, EncodeOperationStepState* state,
                     IEncodeOperationStepHost* host) {
  host->UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                            host->PhaseCompletedWorkUnits());
  switch (config.mode) {
    case TransportMode::kFlash:
      return PumpPrepareFlash(config, text, work_plan, state, host);
    case TransportMode::kPro:
      return PumpPreparePro(config, text, work_plan, state, host);
    case TransportMode::kUltra:
      return PumpPrepareUltra(config, text, work_plan, state, host);
    case TransportMode::kMini:
      return PumpPrepareMini(config, text, *work_plan, state, host);
    default:
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
  }
}

bool PumpRenderFlash(const EncodeWorkPlan& work_plan,
                     EncodeOperationStepState* state,
                     IEncodeOperationStepHost* host) {
  if (state->render_index >= state->flash_payload_layout.chunks.size()) {
    host->UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                              work_plan.rendering_pcm_work_units);
    state->stage = EncodeOperationInternalStage::kPostprocessing;
    state->flash_chunk_renderer.reset();
    return true;
  }
  const auto& chunk = state->flash_payload_layout.chunks[state->render_index];
  if (state->flash_chunk_renderer == nullptr) {
    state->flash_chunk_renderer = std::make_unique<flash::FlashChunkRenderer>(
        chunk, state->flash_signal_config, &state->stage_pcm);
  }
  const flash::FlashChunkRenderProgress progress =
      state->flash_chunk_renderer->Pump(kRenderPumpWorkBudget);
  state->render_completed_units = std::min<std::uint64_t>(
      work_plan.rendering_pcm_work_units,
      static_cast<std::uint64_t>(chunk.sample_offset +
                                 progress.completed_work));
  host->UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                            state->render_completed_units);
  if (progress.finished || state->flash_chunk_renderer->Finished()) {
    state->flash_chunk_renderer.reset();
    ++state->render_index;
  }
  return true;
}

bool PumpRenderPro(const EncodeWorkPlan& work_plan,
                   EncodeOperationStepState* state,
                   IEncodeOperationStepHost* host) {
  if (state->render_index >= state->render_symbols.size()) {
    host->UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                              work_plan.rendering_pcm_work_units);
    state->stage = EncodeOperationInternalStage::kFinalizing;
    state->pro_symbol_renderer.reset();
    return true;
  }
  const std::uint8_t symbol = state->render_symbols[state->render_index];
  const std::size_t write_offset =
      state->render_index *
      static_cast<std::size_t>(state->pro_config.symbol_samples);
  if (state->pro_symbol_renderer == nullptr) {
    try {
      state->pro_symbol_renderer = std::make_unique<pro::SymbolRenderer>(
          symbol, write_offset, state->pro_config, &state->stage_pcm);
    } catch (const std::invalid_argument&) {
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
    }
  }
  const pro::SymbolRenderProgress progress =
      state->pro_symbol_renderer->Pump(kRenderPumpWorkBudget);
  state->render_completed_units = std::min<std::uint64_t>(
      work_plan.rendering_pcm_work_units,
      static_cast<std::uint64_t>(write_offset + progress.completed_work));
  host->UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                            state->render_completed_units);
  if (progress.finished || state->pro_symbol_renderer->Finished()) {
    state->pro_symbol_renderer.reset();
    ++state->render_index;
  }
  return true;
}

bool PumpRenderUltra(const EncodeWorkPlan& work_plan,
                     EncodeOperationStepState* state,
                     IEncodeOperationStepHost* host) {
  if (state->render_index >= state->render_symbols.size()) {
    host->UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                              work_plan.rendering_pcm_work_units);
    state->stage = EncodeOperationInternalStage::kFinalizing;
    state->ultra_symbol_renderer.reset();
    return true;
  }
  const std::uint8_t symbol = state->render_symbols[state->render_index];
  const std::size_t write_offset =
      state->render_index *
      static_cast<std::size_t>(state->ultra_config.symbol_samples);
  if (state->ultra_symbol_renderer == nullptr) {
    try {
      state->ultra_symbol_renderer = std::make_unique<ultra::SymbolRenderer>(
          symbol, write_offset, state->ultra_config, &state->stage_pcm);
    } catch (const std::invalid_argument&) {
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
    }
  }
  const ultra::SymbolRenderProgress progress =
      state->ultra_symbol_renderer->Pump(kRenderPumpWorkBudget);
  state->render_completed_units = std::min<std::uint64_t>(
      work_plan.rendering_pcm_work_units,
      static_cast<std::uint64_t>(write_offset + progress.completed_work));
  host->UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                            state->render_completed_units);
  if (progress.finished || state->ultra_symbol_renderer->Finished()) {
    state->ultra_symbol_renderer.reset();
    ++state->render_index;
  }
  return true;
}

bool PumpRenderMini(const EncodeWorkPlan& work_plan,
                    EncodeOperationStepState* state,
                    IEncodeOperationStepHost* host) {
  if (state->render_index >= state->raw_payload_bytes.size()) {
    host->UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                              work_plan.rendering_pcm_work_units);
    state->stage = EncodeOperationInternalStage::kFinalizing;
    state->mini_tone_renderer.reset();
    return true;
  }
  if (state->mini_tone_renderer == nullptr) {
    try {
      state->mini_tone_renderer = std::make_unique<mini_mode::ToneUnitRenderer>(
          state->raw_payload_bytes, state->render_index, state->mini_config,
          &state->stage_pcm);
    } catch (const std::invalid_argument&) {
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
    }
  }
  const std::size_t chunk_start_offset =
      state->stage_pcm.size() - state->mini_tone_renderer->TotalWork();
  const mini_mode::ToneUnitRenderProgress progress =
      state->mini_tone_renderer->Pump(kRenderPumpWorkBudget);
  state->render_completed_units = std::min<std::uint64_t>(
      work_plan.rendering_pcm_work_units,
      static_cast<std::uint64_t>(chunk_start_offset + progress.completed_work));
  host->UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                            state->render_completed_units);
  if (progress.finished || state->mini_tone_renderer->Finished()) {
    state->mini_tone_renderer.reset();
    ++state->render_index;
  }
  return true;
}

bool PumpRenderStep(const CoreConfig& config, const EncodeWorkPlan& work_plan,
                    EncodeOperationStepState* state,
                    IEncodeOperationStepHost* host) {
  switch (config.mode) {
    case TransportMode::kFlash:
      return PumpRenderFlash(work_plan, state, host);
    case TransportMode::kPro:
      return PumpRenderPro(work_plan, state, host);
    case TransportMode::kUltra:
      return PumpRenderUltra(work_plan, state, host);
    case TransportMode::kMini:
      return PumpRenderMini(work_plan, state, host);
    default:
      host->MarkFailed(ErrorCode::kInvalidArgument);
      return false;
  }
}

bool PumpPostprocessStep(const CoreConfig& config,
                         const EncodeWorkPlan& work_plan,
                         EncodedPcmFollowResult* result,
                         EncodeOperationStepState* state,
                         IEncodeOperationStepHost* host) {
  if (config.mode != TransportMode::kFlash) {
    state->stage = EncodeOperationInternalStage::kFinalizing;
    return true;
  }
  if (state->flash_voicing_stepper == nullptr) {
    state->flash_voicing_stepper = std::make_unique<flash::FlashVoicingStepper>(
        state->stage_pcm, state->flash_payload_layout,
        state->flash_voicing_flavor, state->flash_voicing_config,
        config.enable_diagnostics);
  }
  const flash::FlashVoicingStepProgress progress =
      state->flash_voicing_stepper->Pump(kRenderPumpWorkBudget);
  state->postprocess_completed_units = std::min<std::uint64_t>(
      work_plan.postprocessing_work_units, progress.completed_work);
  host->UpdatePhaseProgress(EncodeProgressPhase::kPostprocessing,
                            state->postprocess_completed_units);
  if (progress.finished || state->flash_voicing_stepper->Finished()) {
    result->pcm = state->flash_voicing_stepper->Result().pcm;
    if (config.enable_diagnostics) {
      host->SetFlashVoicingDiagnostics(
          state->flash_voicing_stepper->Diagnostics());
    }
    state->stage = EncodeOperationInternalStage::kFinalizing;
  }
  return true;
}

bool PumpFinalizeStep(const CoreConfig& config, const std::string& text,
                      const EncodeWorkPlan& work_plan,
                      EncodedPcmFollowResult* result,
                      EncodeOperationStepState* state,
                      IEncodeOperationStepHost* host) {
  result->follow_data =
      BuildPayloadFollowData(config, state->raw_payload_bytes);
  result->text_follow_data = BuildTextFollowData(
      result->follow_data,
      TextFollowSourceForPayload(config.mode, text, state->raw_payload_bytes));
  if (config.mode != TransportMode::kFlash) {
    result->pcm = state->stage_pcm;
  }
  host->UpdatePhaseProgress(EncodeProgressPhase::kFinalizing,
                            work_plan.finalizing_work_units);
  host->MarkSucceeded();
  state->stage = EncodeOperationInternalStage::kDone;
  return true;
}

}  // namespace

bool PumpEncodeOperationStep(const CoreConfig& config, const std::string& text,
                             EncodeWorkPlan* work_plan,
                             EncodedPcmFollowResult* result,
                             EncodeOperationStepState* state,
                             IEncodeOperationStepHost* host) {
  if (work_plan == nullptr || result == nullptr || state == nullptr ||
      host == nullptr) {
    return false;
  }
  switch (state->stage) {
    case EncodeOperationInternalStage::kPreparingInput:
      return PumpPrepareStep(config, text, work_plan, state, host);
    case EncodeOperationInternalStage::kRenderingPcm:
      return PumpRenderStep(config, *work_plan, state, host);
    case EncodeOperationInternalStage::kPostprocessing:
      return PumpPostprocessStep(config, *work_plan, result, state, host);
    case EncodeOperationInternalStage::kFinalizing:
      return PumpFinalizeStep(config, text, *work_plan, result, state, host);
    case EncodeOperationInternalStage::kDone:
    default:
      return false;
  }
}

}  // namespace bag
