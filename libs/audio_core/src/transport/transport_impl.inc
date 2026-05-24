namespace bag {

using std::int16_t;

namespace {

std::string TextFollowSourceForPayload(TransportMode mode,
                                       const std::string& text,
                                       const std::vector<std::uint8_t>& payload);

class EncodeOperation final : public IEncodeOperation {
 public:
  EncodeOperation(CoreConfig config, std::string text)
      : config_(config), text_(std::move(text)), work_plan_(BuildWorkPlan()) {}

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
    if (work_plan_.total_work_units == 0 && work_plan_.payload_byte_count == 0) {
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
  enum class InternalStage {
    kPreparingInput = 0,
    kRenderingPcm = 1,
    kPostprocessing = 2,
    kFinalizing = 3,
    kDone = 4,
  };

  enum class PrepareSubstage {
    kPayload = 0,
    kLayoutOrFrame = 1,
    kSymbols = 2,
    kDone = 3,
  };

  struct AtomicSnapshotState {
    std::atomic<EncodeOperationState> state{EncodeOperationState::kQueued};
    std::atomic<EncodeProgressPhase> phase{EncodeProgressPhase::kPreparingInput};
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

  static std::size_t SafeFrameSamples(const CoreConfig& config) {
    return config.frame_samples > 0
               ? static_cast<std::size_t>(config.frame_samples)
               : static_cast<std::size_t>(0);
  }

  static std::size_t CountMiniUnits(const std::string& text) {
    auto morse_code_point_size = [](unsigned char lead) -> std::size_t {
      if ((lead & 0x80U) == 0) {
        return 1;
      }
      if ((lead & 0xE0U) == 0xC0U) {
        return 2;
      }
      if ((lead & 0xF0U) == 0xE0U) {
        return 3;
      }
      if ((lead & 0xF8U) == 0xF0U) {
        return 4;
      }
      return 1;
    };

    auto upper_ascii = [](char ch) {
      return (ch >= 'a' && ch <= 'z') ? static_cast<char>('A' + (ch - 'a')) : ch;
    };

    auto symbol_units = [&](char ch) -> std::size_t {
      switch (upper_ascii(ch)) {
        case 'A': return 8;
        case 'B': return 12;
        case 'C': return 14;
        case 'D': return 10;
        case 'E': return 4;
        case 'F': return 12;
        case 'G': return 12;
        case 'H': return 10;
        case 'I': return 6;
        case 'J': return 16;
        case 'K': return 12;
        case 'L': return 12;
        case 'M': return 10;
        case 'N': return 8;
        case 'O': return 14;
        case 'P': return 14;
        case 'Q': return 16;
        case 'R': return 10;
        case 'S': return 8;
        case 'T': return 6;
        case 'U': return 10;
        case 'V': return 12;
        case 'W': return 12;
        case 'X': return 14;
        case 'Y': return 16;
        case 'Z': return 14;
        case '0': return 22;
        case '1': return 20;
        case '2': return 18;
        case '3': return 16;
        case '4': return 14;
        case '5': return 12;
        case '6': return 14;
        case '7': return 16;
        case '8': return 18;
        case '9': return 20;
        case '.':
        case ',':
        case '?':
        case '\'':
        case '!':
        case '/':
        case '(':
        case ')':
        case '&':
        case ':':
        case ';':
        case '=':
        case '+':
        case '-':
        case '_':
        case '"':
        case '$':
        case '@':
          return 20;
        case ' ':
          return 7;
        default:
          return 4;
      }
    };

    std::size_t units = 0;
    for (std::size_t index = 0; index < text.size();) {
      const unsigned char lead = static_cast<unsigned char>(text[index]);
      const std::size_t code_point_size = morse_code_point_size(lead);
      units += symbol_units(text[index]);
      index += std::min(code_point_size, text.size() - index);
    }
    return units;
  }

  static FlashSignalProfile NormalizeFormalSignalProfile(
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

  static FlashVoicingFlavor NormalizeFormalVoicingFlavor(
      FlashVoicingFlavor flavor) {
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

  EncodeWorkPlan BuildWorkPlan() const {
    EncodeWorkPlan plan{};
    plan.payload_byte_count = text_.size();
    plan.segment_count = 1;

    switch (config_.mode) {
      case TransportMode::kFlash: {
        const std::vector<std::uint8_t> bytes(text_.begin(), text_.end());
        const std::size_t frame_samples = SafeFrameSamples(config_);
        const std::size_t prepare_text_work = std::max<std::size_t>(bytes.size(), 1);
        const std::size_t prepare_layout_work =
            std::max<std::size_t>(bytes.size() * 2, 1);
        const std::size_t render_work =
            std::max<std::size_t>(bytes.size() * 8 * std::max<std::size_t>(frame_samples, 1), 1);
        plan.preparing_input_work_units =
            prepare_text_work + prepare_layout_work;
        plan.rendering_pcm_work_units = render_work;
        plan.postprocessing_work_units = 1;
        plan.finalizing_work_units = 1;
        plan.total_work_units = plan.preparing_input_work_units +
                                plan.rendering_pcm_work_units +
                                plan.postprocessing_work_units + plan.finalizing_work_units;
        plan.estimated_pcm_sample_count = render_work;
        break;
      }
      case TransportMode::kPro: {
        const std::size_t payload_size = text_.size();
        const std::size_t symbol_count = payload_size * 2;
        const std::size_t render_work =
            std::max<std::size_t>(symbol_count * std::max<std::size_t>(SafeFrameSamples(config_), 1), 1);
        plan.preparing_input_work_units =
            std::max<std::size_t>(payload_size, 1) + std::max<std::size_t>(symbol_count, 1);
        plan.rendering_pcm_work_units = render_work;
        plan.postprocessing_work_units = 0;
        plan.finalizing_work_units = 1;
        plan.total_work_units = plan.preparing_input_work_units +
                                plan.rendering_pcm_work_units +
                                plan.finalizing_work_units;
        plan.estimated_pcm_sample_count = render_work;
        break;
      }
      case TransportMode::kUltra: {
        const std::size_t payload_size = text_.size();
        const std::size_t frame_size = payload_size + 4;
        const std::size_t symbol_count = frame_size * 2;
        const std::size_t render_work =
            std::max<std::size_t>(symbol_count * std::max<std::size_t>(SafeFrameSamples(config_), 1), 1);
        plan.preparing_input_work_units =
            std::max<std::size_t>(payload_size, 1) +
            std::max<std::size_t>(frame_size + symbol_count, 1);
        plan.rendering_pcm_work_units = render_work;
        plan.postprocessing_work_units = 0;
        plan.finalizing_work_units = 1;
        plan.total_work_units = plan.preparing_input_work_units +
                                plan.rendering_pcm_work_units +
                                plan.finalizing_work_units;
        plan.estimated_pcm_sample_count = render_work;
        break;
      }
      case TransportMode::kMini: {
        const std::size_t units = CountMiniUnits(text_);
        const std::size_t render_work =
            std::max<std::size_t>(units * std::max<std::size_t>(SafeFrameSamples(config_), 1), 1);
        plan.preparing_input_work_units = std::max<std::size_t>(text_.size(), 1);
        plan.rendering_pcm_work_units = render_work;
        plan.postprocessing_work_units = 0;
        plan.finalizing_work_units = 1;
        plan.total_work_units = plan.preparing_input_work_units +
                                plan.rendering_pcm_work_units +
                                plan.finalizing_work_units;
        plan.estimated_pcm_sample_count = render_work;
        break;
      }
      default:
        plan.finalizing_work_units = 1;
        plan.total_work_units = 1;
        break;
    }
    return plan;
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
    const std::uint64_t total_completed =
        std::min<std::uint64_t>(work_plan_.total_work_units,
                                phase_base + clamped_phase_units);
    const float phase_progress =
        phase_total == 0
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
    stage_ = InternalStage::kDone;
  }

  bool PumpSingleStep() {
    EnsureStarted();
    switch (stage_) {
      case InternalStage::kPreparingInput:
        return PumpPrepareStep();
      case InternalStage::kRenderingPcm:
        return PumpRenderStep();
      case InternalStage::kPostprocessing:
        return PumpPostprocessStep();
      case InternalStage::kFinalizing:
        return PumpFinalizeStep();
      case InternalStage::kDone:
      default:
        return false;
    }
  }

  bool PumpPrepareStep() {
    UpdatePhaseProgress(
        EncodeProgressPhase::kPreparingInput,
        snapshot_.phase_completed_work_units.load());
    switch (config_.mode) {
      case TransportMode::kFlash:
        return PumpPrepareFlash();
      case TransportMode::kPro:
        return PumpPreparePro();
      case TransportMode::kUltra:
        return PumpPrepareUltra();
      case TransportMode::kMini:
        return PumpPrepareMini();
      default:
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
    }
  }

  bool PumpPrepareFlash() {
    if (prepare_substage_ == PrepareSubstage::kPayload) {
      if (flash::EncodeTextToBytes(text_, &raw_payload_bytes_) != ErrorCode::kOk) {
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
      }
      flash_signal_profile_ =
          NormalizeFormalSignalProfile(config_.flash_signal_profile);
      flash_voicing_flavor_ =
          NormalizeFormalVoicingFlavor(config_.flash_voicing_flavor);
      prepare_completed_units_ = std::min<std::uint64_t>(
          work_plan_.preparing_input_work_units,
          std::max<std::uint64_t>(1, work_plan_.preparing_input_work_units / 2));
      UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                          prepare_completed_units_);
      prepare_substage_ = PrepareSubstage::kLayoutOrFrame;
      return true;
    }
    if (prepare_substage_ == PrepareSubstage::kLayoutOrFrame) {
      flash_signal_config_ =
          flash::MakeBfskConfigForSignalProfile(config_, flash_signal_profile_);
      flash_voicing_config_ =
          flash::MakeFormalVoicingConfigForFlavor(config_, flash_voicing_flavor_);
      flash_payload_layout_ = flash::BuildPayloadLayoutForVoicing(
          raw_payload_bytes_, flash_signal_config_, flash_voicing_flavor_);
      stage_pcm_.assign(flash_payload_layout_.payload_sample_count, 0);
      work_plan_.rendering_pcm_work_units = std::max<std::uint64_t>(
          flash_payload_layout_.payload_sample_count, 1);
      flash_voicing_stepper_ = std::make_unique<flash::FlashVoicingStepper>(
          stage_pcm_, flash_payload_layout_, flash_voicing_flavor_,
          flash_voicing_config_);
      work_plan_.postprocessing_work_units = std::max<std::uint64_t>(
          flash_voicing_stepper_->TotalWork(), 1);
      work_plan_.total_work_units =
          work_plan_.preparing_input_work_units +
          work_plan_.rendering_pcm_work_units +
          work_plan_.postprocessing_work_units +
          work_plan_.finalizing_work_units;
      work_plan_.estimated_pcm_sample_count =
          flash_voicing_stepper_->Result().pcm.size();
      prepare_completed_units_ = work_plan_.preparing_input_work_units;
      UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                          prepare_completed_units_);
      prepare_substage_ = PrepareSubstage::kDone;
      stage_ = InternalStage::kRenderingPcm;
      return true;
    }
    return false;
  }

  bool PumpPreparePro() {
    if (prepare_substage_ == PrepareSubstage::kPayload) {
      if (pro::EncodeTextToPayload(text_, &raw_payload_bytes_) != ErrorCode::kOk) {
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
      }
      prepare_completed_units_ = std::max<std::uint64_t>(
          1, std::min<std::uint64_t>(work_plan_.preparing_input_work_units,
                                     raw_payload_bytes_.size()));
      UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                          prepare_completed_units_);
      prepare_substage_ = PrepareSubstage::kSymbols;
      return true;
    }
    if (prepare_substage_ == PrepareSubstage::kSymbols) {
      if (pro::EncodePayloadToSymbols(raw_payload_bytes_, &render_symbols_) !=
          ErrorCode::kOk) {
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
      }
      pro_config_ = pro::MakeDualToneConfig(config_);
      stage_pcm_.assign(render_symbols_.size() *
                            static_cast<std::size_t>(std::max(pro_config_.symbol_samples, 0)),
                        0);
      prepare_completed_units_ = work_plan_.preparing_input_work_units;
      UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                          prepare_completed_units_);
      prepare_substage_ = PrepareSubstage::kDone;
      stage_ = InternalStage::kRenderingPcm;
      return true;
    }
    return false;
  }

  bool PumpPrepareUltra() {
    if (prepare_substage_ == PrepareSubstage::kPayload) {
      if (ultra::EncodeTextToPayload(text_, &raw_payload_bytes_) !=
          ErrorCode::kOk) {
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
      }
      prepare_completed_units_ = std::max<std::uint64_t>(
          1, std::min<std::uint64_t>(work_plan_.preparing_input_work_units,
                                     raw_payload_bytes_.size()));
      UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                          prepare_completed_units_);
      prepare_substage_ = PrepareSubstage::kLayoutOrFrame;
      return true;
    }
    if (prepare_substage_ == PrepareSubstage::kLayoutOrFrame) {
      if (ultra::EncodePayloadToFrame(raw_payload_bytes_, &frame_bytes_) !=
          ErrorCode::kOk) {
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
      }
      prepare_completed_units_ = std::max<std::uint64_t>(
          prepare_completed_units_ + 1,
          std::min<std::uint64_t>(work_plan_.preparing_input_work_units,
                                  std::max<std::size_t>(frame_bytes_.size(), 1)));
      UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                          prepare_completed_units_);
      prepare_substage_ = PrepareSubstage::kSymbols;
      return true;
    }
    if (prepare_substage_ == PrepareSubstage::kSymbols) {
      if (ultra::EncodePayloadToSymbols(frame_bytes_, &render_symbols_) !=
          ErrorCode::kOk) {
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
      }
      ultra_config_ = ultra::MakeMfsk16Config(config_);
      stage_pcm_.assign(render_symbols_.size() *
                            static_cast<std::size_t>(std::max(ultra_config_.symbol_samples, 0)),
                        0);
      prepare_completed_units_ = work_plan_.preparing_input_work_units;
      UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                          prepare_completed_units_);
      prepare_substage_ = PrepareSubstage::kDone;
      stage_ = InternalStage::kRenderingPcm;
      return true;
    }
    return false;
  }

  bool PumpPrepareMini() {
    if (prepare_substage_ != PrepareSubstage::kPayload) {
      return false;
    }
    if (mini_mode::EncodeTextToPayload(text_, &raw_payload_bytes_) !=
        ErrorCode::kOk) {
      MarkFailed(ErrorCode::kInvalidArgument);
      return false;
    }
    mini_config_ = mini_mode::MakeMorseToneConfig(config_);
    stage_pcm_.clear();
    stage_pcm_.reserve(work_plan_.estimated_pcm_sample_count);
    prepare_completed_units_ = work_plan_.preparing_input_work_units;
    UpdatePhaseProgress(EncodeProgressPhase::kPreparingInput,
                        prepare_completed_units_);
    prepare_substage_ = PrepareSubstage::kDone;
    stage_ = InternalStage::kRenderingPcm;
    return true;
  }

  static int16_t ClampPcm16(double sample) {
    const double clamped = std::clamp(sample, -1.0, 1.0);
    return static_cast<int16_t>(std::lround(clamped * 32767.0));
  }

  bool PumpRenderStep() {
    switch (config_.mode) {
      case TransportMode::kFlash:
        return PumpRenderFlash();
      case TransportMode::kPro:
        return PumpRenderPro();
      case TransportMode::kUltra:
        return PumpRenderUltra();
      case TransportMode::kMini:
        return PumpRenderMini();
      default:
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
    }
  }

  bool PumpRenderFlash() {
    if (render_index_ >= flash_payload_layout_.chunks.size()) {
      UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                          work_plan_.rendering_pcm_work_units);
      stage_ = InternalStage::kPostprocessing;
      flash_chunk_renderer_.reset();
      return true;
    }
    const auto& chunk = flash_payload_layout_.chunks[render_index_];
    if (flash_chunk_renderer_ == nullptr) {
      flash_chunk_renderer_ = std::make_unique<flash::FlashChunkRenderer>(
          chunk, flash_signal_config_, &stage_pcm_);
    }
    const flash::FlashChunkRenderProgress progress =
        flash_chunk_renderer_->Pump(256);
    render_completed_units_ = std::min<std::uint64_t>(
        work_plan_.rendering_pcm_work_units,
        static_cast<std::uint64_t>(chunk.sample_offset + progress.completed_work));
    UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                        render_completed_units_);
    if (progress.finished || flash_chunk_renderer_->Finished()) {
      flash_chunk_renderer_.reset();
      ++render_index_;
    }
    return true;
  }

  bool PumpRenderPro() {
    if (render_index_ >= render_symbols_.size()) {
      UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                          work_plan_.rendering_pcm_work_units);
      stage_ = InternalStage::kFinalizing;
      pro_symbol_renderer_.reset();
      return true;
    }
    const std::uint8_t symbol = render_symbols_[render_index_];
    const std::size_t write_offset =
        render_index_ * static_cast<std::size_t>(pro_config_.symbol_samples);
    if (pro_symbol_renderer_ == nullptr) {
      try {
        pro_symbol_renderer_ = std::make_unique<pro::SymbolRenderer>(
            symbol, write_offset, pro_config_, &stage_pcm_);
      } catch (const std::invalid_argument&) {
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
      }
    }
    const pro::SymbolRenderProgress progress = pro_symbol_renderer_->Pump(256);
    render_completed_units_ = std::min<std::uint64_t>(
        work_plan_.rendering_pcm_work_units,
        static_cast<std::uint64_t>(write_offset + progress.completed_work));
    UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                        render_completed_units_);
    if (progress.finished || pro_symbol_renderer_->Finished()) {
      pro_symbol_renderer_.reset();
      ++render_index_;
    }
    return true;
  }

  bool PumpRenderUltra() {
    if (render_index_ >= render_symbols_.size()) {
      UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                          work_plan_.rendering_pcm_work_units);
      stage_ = InternalStage::kFinalizing;
      ultra_symbol_renderer_.reset();
      return true;
    }
    const std::uint8_t symbol = render_symbols_[render_index_];
    const std::size_t write_offset =
        render_index_ * static_cast<std::size_t>(ultra_config_.symbol_samples);
    if (ultra_symbol_renderer_ == nullptr) {
      try {
        ultra_symbol_renderer_ = std::make_unique<ultra::SymbolRenderer>(
            symbol, write_offset, ultra_config_, &stage_pcm_);
      } catch (const std::invalid_argument&) {
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
      }
    }
    const ultra::SymbolRenderProgress progress =
        ultra_symbol_renderer_->Pump(256);
    render_completed_units_ = std::min<std::uint64_t>(
        work_plan_.rendering_pcm_work_units,
        static_cast<std::uint64_t>(write_offset + progress.completed_work));
    UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                        render_completed_units_);
    if (progress.finished || ultra_symbol_renderer_->Finished()) {
      ultra_symbol_renderer_.reset();
      ++render_index_;
    }
    return true;
  }

  bool PumpRenderMini() {
    if (render_index_ >= raw_payload_bytes_.size()) {
      UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                          work_plan_.rendering_pcm_work_units);
      stage_ = InternalStage::kFinalizing;
      mini_tone_renderer_.reset();
      return true;
    }
    if (mini_tone_renderer_ == nullptr) {
      try {
        mini_tone_renderer_ = std::make_unique<mini_mode::ToneUnitRenderer>(
            raw_payload_bytes_, render_index_, mini_config_, &stage_pcm_);
      } catch (const std::invalid_argument&) {
        MarkFailed(ErrorCode::kInvalidArgument);
        return false;
      }
    }
    const std::size_t chunk_start_offset =
        stage_pcm_.size() - mini_tone_renderer_->TotalWork();
    const mini_mode::ToneUnitRenderProgress progress =
        mini_tone_renderer_->Pump(256);
    render_completed_units_ = std::min<std::uint64_t>(
        work_plan_.rendering_pcm_work_units,
        static_cast<std::uint64_t>(chunk_start_offset + progress.completed_work));
    UpdatePhaseProgress(EncodeProgressPhase::kRenderingPcm,
                        render_completed_units_);
    if (progress.finished || mini_tone_renderer_->Finished()) {
      mini_tone_renderer_.reset();
      ++render_index_;
    }
    return true;
  }

  bool PumpPostprocessStep() {
    if (config_.mode != TransportMode::kFlash) {
      stage_ = InternalStage::kFinalizing;
      return true;
    }
    if (flash_voicing_stepper_ == nullptr) {
      flash_voicing_stepper_ = std::make_unique<flash::FlashVoicingStepper>(
          stage_pcm_, flash_payload_layout_, flash_voicing_flavor_,
          flash_voicing_config_);
    }
    const flash::FlashVoicingStepProgress progress =
        flash_voicing_stepper_->Pump(256);
    postprocess_completed_units_ = std::min<std::uint64_t>(
        work_plan_.postprocessing_work_units, progress.completed_work);
    UpdatePhaseProgress(EncodeProgressPhase::kPostprocessing,
                        postprocess_completed_units_);
    if (progress.finished || flash_voicing_stepper_->Finished()) {
      result_.pcm = flash_voicing_stepper_->Result().pcm;
      stage_ = InternalStage::kFinalizing;
    }
    return true;
  }

  bool PumpFinalizeStep() {
    result_.follow_data = BuildPayloadFollowData(config_, raw_payload_bytes_);
    result_.text_follow_data = BuildTextFollowData(
        result_.follow_data,
        TextFollowSourceForPayload(config_.mode, text_, raw_payload_bytes_));
    if (config_.mode != TransportMode::kFlash) {
      result_.pcm = stage_pcm_;
    }
    UpdatePhaseProgress(EncodeProgressPhase::kFinalizing,
                        work_plan_.finalizing_work_units);
    MarkTerminal(ErrorCode::kOk, EncodeOperationState::kSucceeded);
    stage_ = InternalStage::kDone;
    return true;
  }

  CoreConfig config_;
  std::string text_;
  EncodeWorkPlan work_plan_;
  EncodedPcmFollowResult result_;
  AtomicSnapshotState snapshot_{};
  std::atomic<bool> cancel_requested_{false};
  InternalStage stage_ = InternalStage::kPreparingInput;
  PrepareSubstage prepare_substage_ = PrepareSubstage::kPayload;
  std::uint64_t prepare_completed_units_ = 0;
  std::uint64_t render_completed_units_ = 0;
  std::uint64_t postprocess_completed_units_ = 0;
  std::vector<std::uint8_t> raw_payload_bytes_;
  std::vector<std::uint8_t> frame_bytes_;
  std::vector<std::uint8_t> render_symbols_;
  std::vector<int16_t> stage_pcm_;
  std::size_t render_index_ = 0;
  FlashSignalProfile flash_signal_profile_ =
      FlashSignalProfile::kStandard;
  FlashVoicingFlavor flash_voicing_flavor_ =
      FlashVoicingFlavor::kStandard;
  flash::BfskConfig flash_signal_config_{};
  flash::FlashVoicingConfig flash_voicing_config_{};
  flash::FlashPayloadLayout flash_payload_layout_{};
  std::unique_ptr<flash::FlashChunkRenderer> flash_chunk_renderer_;
  std::unique_ptr<flash::FlashVoicingStepper> flash_voicing_stepper_;
  std::unique_ptr<pro::SymbolRenderer> pro_symbol_renderer_;
  std::unique_ptr<ultra::SymbolRenderer> ultra_symbol_renderer_;
  std::unique_ptr<mini_mode::ToneUnitRenderer> mini_tone_renderer_;
  pro::DualToneConfig pro_config_{};
  ultra::Mfsk16Config ultra_config_{};
  mini_mode::MorseToneConfig mini_config_{};
};

bool IsAsciiText(std::string_view text) {
  for (unsigned char ch : text) {
    if (ch > 0x7F) {
      return false;
    }
  }
  return true;
}

bool IsMiniMorseText(std::string_view text) {
  return bag::mini_mode::IsMorseText(text);
}

std::string TextFollowSourceForPayload(TransportMode mode,
                                       const std::string& text,
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

TransportValidationIssue ValidateCommonConfig(const CoreConfig& config) {
  if (config.sample_rate_hz <= 0) {
    return TransportValidationIssue::kInvalidSampleRate;
  }
  if (config.frame_samples <= 0) {
    return TransportValidationIssue::kInvalidFrameSamples;
  }
  if (!IsValidTransportMode(config.mode)) {
    return TransportValidationIssue::kInvalidMode;
  }
  if (!IsValidFlashSignalProfile(config.flash_signal_profile)) {
    return TransportValidationIssue::kInvalidFlashSignalProfile;
  }
  if (!IsValidFlashVoicingFlavor(config.flash_voicing_flavor)) {
    return TransportValidationIssue::kInvalidFlashVoicingFlavor;
  }
  return TransportValidationIssue::kOk;
}

}  // namespace

TransportValidationIssue ValidateEncodeRequest(const CoreConfig& config,
                                               std::string_view text) {
  const TransportValidationIssue config_issue = ValidateCommonConfig(config);
  if (config_issue != TransportValidationIssue::kOk) {
    return config_issue;
  }
  if (text.empty()) {
    return TransportValidationIssue::kEmptyText;
  }

  if (config.mode == TransportMode::kFlash) {
    return TransportValidationIssue::kOk;
  }

  if (config.mode == TransportMode::kPro) {
    return IsAsciiText(text) ? TransportValidationIssue::kOk
                             : TransportValidationIssue::kProAsciiOnly;
  }

  if (config.mode == TransportMode::kMini) {
    return IsMiniMorseText(text) ? TransportValidationIssue::kOk
                                : TransportValidationIssue::kMiniMorseOnly;
  }

  return TransportValidationIssue::kOk;
}

TransportValidationIssue ValidateDecoderConfig(const CoreConfig& config) {
  return ValidateCommonConfig(config);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm) {
  return EncodeTextToPcm16(config, text, out_pcm, nullptr);
}

ErrorCode EncodeTextToPcm16(const CoreConfig& config, const std::string& text,
                            std::vector<int16_t>* out_pcm,
                            const EncodeProgressSink* progress_sink) {
  if (out_pcm == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (ValidateEncodeRequest(config, text) != TransportValidationIssue::kOk) {
    return ErrorCode::kInvalidArgument;
  }

  switch (config.mode) {
    case TransportMode::kFlash:
      return bag::flash::EncodeTextToPcm16(config, text, out_pcm,
                                           progress_sink);
    case TransportMode::kPro:
      return bag::pro::EncodeTextToPcm16(config, text, out_pcm, progress_sink);
    case TransportMode::kUltra:
      return bag::ultra::EncodeTextToPcm16(config, text, out_pcm,
                                           progress_sink);
    case TransportMode::kMini:
      return bag::mini_mode::EncodeTextToPcm16(config, text, out_pcm,
                                              progress_sink);
    default:
      return ErrorCode::kInvalidArgument;
  }
}

ErrorCode EncodeTextToPcm16WithFollowData(const CoreConfig& config,
                                          const std::string& text,
                                          EncodedPcmFollowResult* out_result) {
  return EncodeTextToPcm16WithFollowData(config, text, out_result, nullptr);
}

ErrorCode EncodeTextToPcm16WithFollowData(
    const CoreConfig& config, const std::string& text,
    EncodedPcmFollowResult* out_result,
    const EncodeProgressSink* progress_sink) {
  if (out_result == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (ValidateEncodeRequest(config, text) != TransportValidationIssue::kOk) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<std::uint8_t> raw_payload_bytes;
  ErrorCode payload_code = ErrorCode::kInvalidArgument;
  switch (config.mode) {
    case TransportMode::kFlash:
      payload_code = bag::flash::EncodeTextToBytes(text, &raw_payload_bytes);
      break;
    case TransportMode::kPro:
      payload_code = bag::pro::EncodeTextToPayload(text, &raw_payload_bytes);
      break;
    case TransportMode::kUltra:
      payload_code = bag::ultra::EncodeTextToPayload(text, &raw_payload_bytes);
      break;
    case TransportMode::kMini:
      payload_code =
          bag::mini_mode::EncodeTextToPayload(text, &raw_payload_bytes);
      break;
    default:
      return ErrorCode::kInvalidArgument;
  }
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }

  out_result->follow_data = BuildPayloadFollowData(config, raw_payload_bytes);
  out_result->text_follow_data = BuildTextFollowData(
      out_result->follow_data,
      TextFollowSourceForPayload(config.mode, text, raw_payload_bytes));
  return EncodeTextToPcm16(config, text, &out_result->pcm, progress_sink);
}

ErrorCode BuildEncodeFollowData(const CoreConfig& config,
                                const std::string& text,
                                PayloadFollowData* out_follow_data,
                                TextFollowData* out_text_follow_data) {
  if (out_follow_data == nullptr || out_text_follow_data == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  if (ValidateEncodeRequest(config, text) != TransportValidationIssue::kOk) {
    return ErrorCode::kInvalidArgument;
  }

  std::vector<std::uint8_t> raw_payload_bytes;
  ErrorCode payload_code = ErrorCode::kInvalidArgument;
  switch (config.mode) {
    case TransportMode::kFlash:
      payload_code = bag::flash::EncodeTextToBytes(text, &raw_payload_bytes);
      break;
    case TransportMode::kPro:
      payload_code = bag::pro::EncodeTextToPayload(text, &raw_payload_bytes);
      break;
    case TransportMode::kUltra:
      payload_code = bag::ultra::EncodeTextToPayload(text, &raw_payload_bytes);
      break;
    case TransportMode::kMini:
      payload_code =
          bag::mini_mode::EncodeTextToPayload(text, &raw_payload_bytes);
      break;
    default:
      return ErrorCode::kInvalidArgument;
  }
  if (payload_code != ErrorCode::kOk) {
    return payload_code;
  }

  *out_follow_data = BuildPayloadFollowData(config, raw_payload_bytes);
  *out_text_follow_data = BuildTextFollowData(
      *out_follow_data,
      TextFollowSourceForPayload(config.mode, text, raw_payload_bytes));
  return ErrorCode::kOk;
}

std::unique_ptr<IEncodeOperation> CreateEncodeOperation(
    const CoreConfig& config, const std::string& text) {
  if (ValidateEncodeRequest(config, text) != TransportValidationIssue::kOk) {
    return nullptr;
  }
  return std::make_unique<EncodeOperation>(config, text);
}

std::unique_ptr<ITransportDecoder> CreateTransportDecoder(
    const CoreConfig& config) {
  if (ValidateDecoderConfig(config) != TransportValidationIssue::kOk) {
    return nullptr;
  }

  switch (config.mode) {
    case TransportMode::kFlash:
      return bag::flash::CreateDecoder(config);
    case TransportMode::kPro:
      return bag::pro::CreateDecoder(config);
    case TransportMode::kUltra:
      return bag::ultra::CreateDecoder(config);
    case TransportMode::kMini:
      return bag::mini_mode::CreateDecoder(config);
    default:
      return nullptr;
  }
}

}  // namespace bag
