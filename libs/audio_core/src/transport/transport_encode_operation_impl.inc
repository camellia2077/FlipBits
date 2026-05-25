namespace bag {
namespace {

class EncodeOperation final : public IEncodeOperation {
 public:
  EncodeOperation(CoreConfig config, std::string text)
      : config_(config),
        text_(std::move(text)),
        work_plan_(BuildEncodeWorkPlan(config_, text_)) {}

  ~EncodeOperation() override = default;

  ErrorCode Run() override {
    bool did_progress = false;
    while (true) {
      Pump({}, &did_progress);
      const EncodeOperationState state = Snapshot().state;
      if (state == EncodeOperationState::kSucceeded) {
        return ErrorCode::kOk;
      }
      if (state == EncodeOperationState::kCancelled) {
        return ErrorCode::kCancelled;
      }
      if (state == EncodeOperationState::kFailed) {
        return Snapshot().terminal_code;
      }
      if (!did_progress) {
        return ErrorCode::kNotReady;
      }
    }
  }

  ErrorCode Pump(const EncodePumpBudget& budget,
                 bool* out_did_progress) override {
    if (out_did_progress != nullptr) {
      *out_did_progress = false;
    }
    if (work_plan_.total_work_units == 0 &&
        work_plan_.payload_byte_count == 0) {
      return ErrorCode::kInvalidArgument;
    }

    if (IsTerminalState(snapshot_.state.load())) {
      return ErrorCode::kOk;
    }

    const std::uint64_t before_units = snapshot_.completed_work_units.load();
    const std::uint64_t target_units =
        budget.max_work_units > 0
            ? std::min<std::uint64_t>(work_plan_.total_work_units,
                                      before_units + budget.max_work_units)
            : work_plan_.total_work_units;
    const auto deadline =
        budget.max_wall_time_ms > 0
            ? std::chrono::steady_clock::now() +
                  std::chrono::milliseconds(budget.max_wall_time_ms)
            : std::chrono::steady_clock::time_point::max();

    while (true) {
      if (cancel_requested_.load()) {
        MarkTerminal(ErrorCode::kCancelled, EncodeOperationState::kCancelled);
        break;
      }
      if (snapshot_.completed_work_units.load() >= target_units &&
          snapshot_.completed_work_units.load() > before_units) {
        break;
      }
      if (std::chrono::steady_clock::now() >= deadline) {
        break;
      }
      if (IsTerminalState(snapshot_.state.load())) {
        break;
      }
      if (!PumpSingleStep()) {
        break;
      }
    }

    if (out_did_progress != nullptr) {
      *out_did_progress = snapshot_.completed_work_units.load() > before_units;
    }
    return ErrorCode::kOk;
  }

  ErrorCode Cancel() override {
    cancel_requested_.store(true);
    if (snapshot_.state.load() == EncodeOperationState::kQueued) {
      snapshot_.phase.store(EncodeProgressPhase::kPreparingInput);
      MarkTerminal(ErrorCode::kCancelled, EncodeOperationState::kCancelled);
    } else if (!IsTerminalState(snapshot_.state.load())) {
      snapshot_.terminal_code.store(ErrorCode::kCancelled);
    }
    return ErrorCode::kOk;
  }

  EncodeWorkPlan WorkPlan() const override { return work_plan_; }

  EncodeProgressSnapshot Snapshot() const override {
    EncodeProgressSnapshot snapshot{};
    snapshot.state = snapshot_.state.load();
    snapshot.phase = snapshot_.phase.load();
    snapshot.overall_progress_0_to_1 =
        std::clamp(snapshot_.overall_progress_0_to_1.load(), 0.0f, 1.0f);
    snapshot.phase_progress_0_to_1 =
        std::clamp(snapshot_.phase_progress_0_to_1.load(), 0.0f, 1.0f);
    snapshot.completed_work_units = snapshot_.completed_work_units.load();
    snapshot.total_work_units = work_plan_.total_work_units;
    snapshot.phase_completed_work_units =
        snapshot_.phase_completed_work_units.load();
    snapshot.phase_total_work_units = PhaseTotalWork(snapshot.phase);
    snapshot.terminal_code = snapshot_.terminal_code.load();
    snapshot.estimated_pcm_sample_count = work_plan_.estimated_pcm_sample_count;
    if (snapshot.state == EncodeOperationState::kSucceeded) {
      snapshot.estimated_pcm_sample_count = result_.pcm.size();
    }
    snapshot.payload_byte_count = work_plan_.payload_byte_count;
    snapshot.segment_count = work_plan_.segment_count;
    snapshot.current_segment_index = 0;
    return snapshot;
  }

  ErrorCode TakeResult(EncodedPcmFollowResult* out_result) override {
    if (out_result == nullptr) {
      return ErrorCode::kInvalidArgument;
    }
    const EncodeOperationState state = snapshot_.state.load();
    if (state == EncodeOperationState::kSucceeded) {
      *out_result = result_;
      return ErrorCode::kOk;
    }
    *out_result = EncodedPcmFollowResult{};
    if (state == EncodeOperationState::kCancelled) {
      return ErrorCode::kCancelled;
    }
    if (state == EncodeOperationState::kFailed) {
      return snapshot_.terminal_code.load();
    }
    return ErrorCode::kNotReady;
  }

 private:
  struct AtomicSnapshotState {
    std::atomic<EncodeOperationState> state{EncodeOperationState::kQueued};
    std::atomic<EncodeProgressPhase> phase{
        EncodeProgressPhase::kPreparingInput};
    std::atomic<float> overall_progress_0_to_1{0.0f};
    std::atomic<float> phase_progress_0_to_1{0.0f};
    std::atomic<std::uint64_t> completed_work_units{0};
    std::atomic<std::uint64_t> phase_completed_work_units{0};
    std::atomic<ErrorCode> terminal_code{ErrorCode::kNotReady};
  };

  static bool IsTerminalState(EncodeOperationState state) {
    return state == EncodeOperationState::kSucceeded ||
           state == EncodeOperationState::kFailed ||
           state == EncodeOperationState::kCancelled;
  }

  void EnsureStarted() {
    if (snapshot_.state.load() != EncodeOperationState::kQueued) {
      return;
    }
    snapshot_.state.store(EncodeOperationState::kRunning);
    snapshot_.phase.store(EncodeProgressPhase::kPreparingInput);
    snapshot_.overall_progress_0_to_1.store(0.0f);
    snapshot_.phase_progress_0_to_1.store(0.0f);
    snapshot_.completed_work_units.store(0);
    snapshot_.phase_completed_work_units.store(0);
    snapshot_.terminal_code.store(ErrorCode::kNotReady);
  }

  std::uint64_t PhaseBaseWork(EncodeProgressPhase phase) const {
    switch (phase) {
      case EncodeProgressPhase::kPreparingInput:
        return 0;
      case EncodeProgressPhase::kRenderingPcm:
        return work_plan_.preparing_input_work_units;
      case EncodeProgressPhase::kPostprocessing:
        return work_plan_.preparing_input_work_units +
               work_plan_.rendering_pcm_work_units;
      case EncodeProgressPhase::kFinalizing:
      default:
        return work_plan_.preparing_input_work_units +
               work_plan_.rendering_pcm_work_units +
               work_plan_.postprocessing_work_units;
    }
  }

  std::uint64_t PhaseTotalWork(EncodeProgressPhase phase) const {
    switch (phase) {
      case EncodeProgressPhase::kPreparingInput:
        return work_plan_.preparing_input_work_units;
      case EncodeProgressPhase::kRenderingPcm:
        return work_plan_.rendering_pcm_work_units;
      case EncodeProgressPhase::kPostprocessing:
        return work_plan_.postprocessing_work_units;
      case EncodeProgressPhase::kFinalizing:
      default:
        return work_plan_.finalizing_work_units;
    }
  }

  void UpdatePhaseProgress(EncodeProgressPhase phase,
                           std::uint64_t phase_completed_units) {
    const std::uint64_t phase_total = PhaseTotalWork(phase);
    const std::uint64_t clamped_phase_units =
        std::min<std::uint64_t>(phase_total, phase_completed_units);
    const std::uint64_t phase_base = PhaseBaseWork(phase);
    const std::uint64_t total_completed = std::min<std::uint64_t>(
        work_plan_.total_work_units, phase_base + clamped_phase_units);
    const float phase_progress = phase_total == 0
                                     ? 1.0f
                                     : static_cast<float>(clamped_phase_units) /
                                           static_cast<float>(phase_total);
    const float overall_progress =
        work_plan_.total_work_units == 0
            ? 1.0f
            : static_cast<float>(total_completed) /
                  static_cast<float>(work_plan_.total_work_units);

    snapshot_.state.store(EncodeOperationState::kRunning);
    snapshot_.phase.store(phase);
    snapshot_.overall_progress_0_to_1.store(
        std::clamp(overall_progress, 0.0f, 1.0f));
    snapshot_.phase_progress_0_to_1.store(phase_progress);
    snapshot_.phase_completed_work_units.store(clamped_phase_units);
    snapshot_.completed_work_units.store(total_completed);
  }

  void MarkTerminal(ErrorCode code, EncodeOperationState state) {
    snapshot_.terminal_code.store(code);
    snapshot_.state.store(state);
    if (state == EncodeOperationState::kSucceeded) {
      snapshot_.overall_progress_0_to_1.store(1.0f);
      snapshot_.phase_progress_0_to_1.store(1.0f);
      snapshot_.completed_work_units.store(work_plan_.total_work_units);
      snapshot_.phase_completed_work_units.store(
          PhaseTotalWork(EncodeProgressPhase::kFinalizing));
    }
  }

  void MarkFailed(ErrorCode code) {
    result_ = EncodedPcmFollowResult{};
    MarkTerminal(code, EncodeOperationState::kFailed);
    step_state_.stage = EncodeOperationInternalStage::kDone;
  }

  class StepHost final : public IEncodeOperationStepHost {
   public:
    explicit StepHost(EncodeOperation* operation) : operation_(operation) {}

    std::uint64_t PhaseCompletedWorkUnits() const override {
      return operation_->snapshot_.phase_completed_work_units.load();
    }

    void UpdatePhaseProgress(EncodeProgressPhase phase,
                             std::uint64_t phase_completed_units) override {
      operation_->UpdatePhaseProgress(phase, phase_completed_units);
    }

    void MarkFailed(ErrorCode code) override { operation_->MarkFailed(code); }

    void MarkSucceeded() override {
      operation_->MarkTerminal(ErrorCode::kOk,
                               EncodeOperationState::kSucceeded);
    }

   private:
    EncodeOperation* operation_;
  };

  bool PumpSingleStep() {
    EnsureStarted();
    StepHost host(this);
    return PumpEncodeOperationStep(config_, text_, &work_plan_, &result_,
                                   &step_state_, &host);
  }

  CoreConfig config_;
  std::string text_;
  EncodeWorkPlan work_plan_;
  EncodedPcmFollowResult result_;
  AtomicSnapshotState snapshot_{};
  std::atomic<bool> cancel_requested_{false};
  EncodeOperationStepState step_state_{};
};

}  // namespace

std::unique_ptr<IEncodeOperation> CreateEncodeOperation(
    const CoreConfig& config, const std::string& text) {
  if (ValidateEncodeRequest(config, text) != TransportValidationIssue::kOk) {
    return nullptr;
  }
  return std::make_unique<EncodeOperation>(config, text);
}

}  // namespace bag
