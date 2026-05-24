import createFlipBitsRuntime from "./flipbits_web_runtime.js";

const MODE_IDS = {
  mini: 0,
  flash: 1,
  pro: 2,
  ultra: 3,
};

const FLASH_STYLE_IDS = {
  standard: 0,
  litany: 1,
  hostility: 3,
  collapse: 4,
  zeal: 5,
  void: 6,
};

let runtimePromise = null;

function getRuntime() {
  if (!runtimePromise) {
    runtimePromise = createFlipBitsRuntime();
  }
  return runtimePromise;
}

function toModeId(mode) {
  return MODE_IDS[mode] ?? MODE_IDS.flash;
}

function toFlashStyleId(flashStyle) {
  return FLASH_STYLE_IDS[flashStyle] ?? FLASH_STYLE_IDS.standard;
}

export default async function createFlipBitsModule() {
  const runtime = await getRuntime();

  function readCurrentSnapshot() {
    return {
      state: runtime.ccall("flipbits_web_current_operation_state", "number", [], []),
      phase: runtime.ccall("flipbits_web_current_operation_phase", "number", [], []),
      overallProgress: runtime.ccall(
        "flipbits_web_current_operation_overall_progress",
        "number",
        [],
        [],
      ),
      phaseProgress: runtime.ccall(
        "flipbits_web_current_operation_phase_progress",
        "number",
        [],
        [],
      ),
      completedWorkUnits: runtime.ccall(
        "flipbits_web_current_operation_completed_work_units",
        "number",
        [],
        [],
      ),
      totalWorkUnits: runtime.ccall(
        "flipbits_web_current_operation_total_work_units",
        "number",
        [],
        [],
      ),
      phaseCompletedWorkUnits: runtime.ccall(
        "flipbits_web_current_operation_phase_completed_work_units",
        "number",
        [],
        [],
      ),
      phaseTotalWorkUnits: runtime.ccall(
        "flipbits_web_current_operation_phase_total_work_units",
        "number",
        [],
        [],
      ),
      terminalCode: runtime.ccall(
        "flipbits_web_current_operation_terminal_code",
        "number",
        [],
        [],
      ),
      estimatedPcmSampleCount: runtime.ccall(
        "flipbits_web_current_operation_estimated_pcm_sample_count",
        "number",
        [],
        [],
      ),
      payloadByteCount: runtime.ccall(
        "flipbits_web_current_operation_payload_byte_count",
        "number",
        [],
        [],
      ),
      segmentCount: runtime.ccall(
        "flipbits_web_current_operation_segment_count",
        "number",
        [],
        [],
      ),
      currentSegmentIndex: runtime.ccall(
        "flipbits_web_current_operation_current_segment_index",
        "number",
        [],
        [],
      ),
    };
  }

  function readWorkPlan() {
    return {
      preparingInputWorkUnits: runtime.ccall(
        "flipbits_web_current_operation_plan_preparing_work_units",
        "number",
        [],
        [],
      ),
      renderingPcmWorkUnits: runtime.ccall(
        "flipbits_web_current_operation_plan_rendering_work_units",
        "number",
        [],
        [],
      ),
      postprocessingWorkUnits: runtime.ccall(
        "flipbits_web_current_operation_plan_postprocessing_work_units",
        "number",
        [],
        [],
      ),
      finalizingWorkUnits: runtime.ccall(
        "flipbits_web_current_operation_plan_finalizing_work_units",
        "number",
        [],
        [],
      ),
      totalWorkUnits: runtime.ccall(
        "flipbits_web_current_operation_plan_total_work_units",
        "number",
        [],
        [],
      ),
      estimatedPcmSampleCount: runtime.ccall(
        "flipbits_web_current_operation_plan_estimated_pcm_sample_count",
        "number",
        [],
        [],
      ),
      payloadByteCount: runtime.ccall(
        "flipbits_web_current_operation_plan_payload_byte_count",
        "number",
        [],
        [],
      ),
      segmentCount: runtime.ccall(
        "flipbits_web_current_operation_plan_segment_count",
        "number",
        [],
        [],
      ),
    };
  }

  return {
    beginEncodeOperation(request) {
      const modeId = toModeId(request.mode);
      const flashStyleId = toFlashStyleId(request.flashStyle);
      const sampleRateHz = request.sampleRateHz ?? 44100;
      const frameSamples = request.frameSamples ?? Math.max(1, Math.floor(sampleRateHz / 20));
      const success = runtime.ccall(
        "flipbits_web_begin_encode_operation",
        "number",
        ["string", "number", "number", "number", "number"],
        [request.text, modeId, flashStyleId, sampleRateHz, frameSamples],
      );

      if (success !== 1) {
        const message = runtime.ccall(
          "flipbits_web_last_error_message",
          "string",
          [],
          [],
        );
        throw new Error(message || "Encoding failed.");
      }

      return {
        workPlan: readWorkPlan(),
        snapshot: readCurrentSnapshot(),
        sampleRateHz,
        frameSamples,
      };
    },

    pumpEncodeOperation(budget = {}) {
      const pumpCode = runtime.ccall(
        "flipbits_web_pump_encode_operation",
        "number",
        ["number", "number"],
        [budget.maxWorkUnits ?? 0, budget.maxWallTimeMs ?? 0],
      );

      if (pumpCode !== 0) {
        const message = runtime.ccall(
          "flipbits_web_error_message_from_code",
          "string",
          ["number"],
          [pumpCode],
        );
        throw new Error(message || "Encoding failed.");
      }

      return readCurrentSnapshot();
    },

    async takeEncodeOperationResult() {
      const success = runtime.ccall(
        "flipbits_web_take_encode_operation_result",
        "number",
        [],
        [],
      );

      if (success !== 1) {
        const message = runtime.ccall(
          "flipbits_web_last_error_message",
          "string",
          [],
          [],
        );
        throw new Error(message || "Encoding failed.");
      }

      const resolvedSampleRateHz = runtime.ccall(
        "flipbits_web_last_sample_rate_hz",
        "number",
        [],
        [],
      );
      const sampleCount = runtime.ccall(
        "flipbits_web_last_sample_count",
        "number",
        [],
        [],
      );
      const samples = [];
      for (let index = 0; index < sampleCount; index += 1) {
        samples.push(
          runtime.ccall(
            "flipbits_web_last_sample_at",
            "number",
            ["number"],
            [index],
          ),
        );
      }

      return {
        samples,
        sampleRateHz: resolvedSampleRateHz,
      };
    },

    abortEncodeOperation() {
      runtime.ccall("flipbits_web_abort_encode_operation", null, [], []);
    },

    errorMessageFromCode(code) {
      return runtime.ccall(
        "flipbits_web_error_message_from_code",
        "string",
        ["number"],
        [code],
      );
    },
  };
}
