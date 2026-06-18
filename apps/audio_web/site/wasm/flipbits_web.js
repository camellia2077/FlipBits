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

const VOICE_FX_PRESET_IDS = {
  machine_voice: 0,
  binaric_cant: 1,
  signal_cant: 2,
  robot_vox: 3,
  raw_constant: 4,
  voice_trigger: 5,
};

const VOICE_FX_SUBVOICE_STYLE_IDS = {
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

function toVoiceFxPresetId(preset) {
  return VOICE_FX_PRESET_IDS[preset] ?? VOICE_FX_PRESET_IDS.machine_voice;
}

function toVoiceFxSubvoiceStyleId(subvoiceStyle) {
  return VOICE_FX_SUBVOICE_STYLE_IDS[subvoiceStyle] ?? VOICE_FX_SUBVOICE_STYLE_IDS.standard;
}

export default async function createFlipBitsModule() {
  const runtime = await getRuntime();

  function readLastSampleResult() {
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
    const samplePointer = runtime.ccall(
      "flipbits_web_last_samples_ptr",
      "number",
      [],
      [],
    );

    if (samplePointer && runtime.HEAP16) {
      const begin = samplePointer >> 1;
      const end = begin + sampleCount;
      return {
        samples: runtime.HEAP16.slice(begin, end),
        sampleRateHz: resolvedSampleRateHz,
      };
    }

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
  }

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

  function readDiagnostics() {
    return {
      flashPayloadPrepareMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_prepare_ms", "number", [], []),
      flashPayloadSampleSetupMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_sample_setup_ms", "number", [], []),
      flashPayloadEnvelopeMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_envelope_ms", "number", [], []),
      flashPayloadArticulationMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_articulation_ms", "number", [], []),
      flashPayloadHarmonicMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_harmonic_ms", "number", [], []),
      flashPayloadMetallicMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_metallic_ms", "number", [], []),
      flashPayloadChantResonanceMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_chant_resonance_ms", "number", [], []),
      flashPayloadChantDroneMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_chant_drone_ms", "number", [], []),
      flashPayloadMechanicalThroatMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_mechanical_throat_ms", "number", [], []),
      flashPayloadStandardLowVoiceMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_standard_low_voice_ms", "number", [], []),
      flashPayloadHostilityEdgeMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_hostility_edge_ms", "number", [], []),
      flashPayloadBoundaryClickMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_boundary_click_ms", "number", [], []),
      flashPayloadModulationMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_modulation_ms", "number", [], []),
      flashPayloadMixShapeStoreMs: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_mix_shape_store_ms", "number", [], []),
      flashPayloadVoicedSamples: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_voiced_samples", "number", [], []),
      flashPayloadSilenceSamples: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_silence_samples", "number", [], []),
      flashPayloadProfiledSamples: runtime.ccall("flipbits_web_current_operation_diagnostics_flash_payload_profiled_samples", "number", [], []),
    };
  }

  function toHeapByteArray(bytes) {
    const startMs = performance?.now?.() ?? Date.now();
    const heap = runtime.HEAPU8;
    if (!heap) {
      throw new Error("WASM heap is not ready.");
    }
    const byteArray = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
    const pointer = runtime.ccall("malloc", "number", ["number"], [byteArray.byteLength]);
    if (!pointer) {
      throw new Error("Failed to allocate WASM memory.");
    }
    heap.set(byteArray, pointer);
    return {
      pointer,
      byteLength: byteArray.byteLength,
      heapCopyMs: (performance?.now?.() ?? Date.now()) - startMs,
    };
  }

  function freeHeapByteArray(pointer) {
    if (!pointer) {
      return;
    }
    runtime.ccall("free", null, ["number"], [pointer]);
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
        ["string", "number", "number", "number", "number", "number"],
        [
          request.text,
          modeId,
          flashStyleId,
          sampleRateHz,
          frameSamples,
          request.enableDiagnostics ? 1 : 0,
        ],
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
        diagnostics: readDiagnostics(),
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

      return {
        snapshot: readCurrentSnapshot(),
        workPlan: readWorkPlan(),
        diagnostics: readDiagnostics(),
      };
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

      const result = readLastSampleResult();
      result.diagnostics = readDiagnostics();
      return result;
    },

    beginVoiceFx(request) {
      const success = runtime.ccall(
        "flipbits_web_begin_voice_fx",
        "number",
        ["number", "number", "number"],
        [
          toVoiceFxPresetId(request.preset),
          toVoiceFxSubvoiceStyleId(request.subvoiceStyle),
          request.sampleRateHz ?? 44100,
        ],
      );

      if (success !== 1) {
        const message = runtime.ccall(
          "flipbits_web_last_error_message",
          "string",
          [],
          [],
        );
        throw new Error(message || "Voice processing failed.");
      }
    },

    applyVoiceFxPcmBytes(request) {
      const totalStartMs = performance?.now?.() ?? Date.now();
      const { pointer, byteLength, heapCopyMs } = toHeapByteArray(request.pcmBytes);
      try {
        const applyStartMs = performance?.now?.() ?? Date.now();
        const success = runtime.ccall(
          "flipbits_web_apply_voice_fx_pcm_bytes",
          "number",
          ["number", "number", "number", "number", "number"],
          [
            toVoiceFxPresetId(request.preset),
            toVoiceFxSubvoiceStyleId(request.subvoiceStyle),
            request.sampleRateHz ?? 44100,
            pointer,
            byteLength,
          ],
        );
        const wasmApplyMs = (performance?.now?.() ?? Date.now()) - applyStartMs;

        if (success !== 1) {
          const message = runtime.ccall(
            "flipbits_web_last_error_message",
            "string",
            [],
            [],
          );
          throw new Error(message || "Voice processing failed.");
        }

        const readStartMs = performance?.now?.() ?? Date.now();
        const result = readLastSampleResult();
        const readResultMs = (performance?.now?.() ?? Date.now()) - readStartMs;
        result.diagnostics = {
          heapCopyMs,
          wasmApplyMs,
          readResultMs,
          totalMs: (performance?.now?.() ?? Date.now()) - totalStartMs,
          inputByteLength: byteLength,
        };
        return result;
      } finally {
        freeHeapByteArray(pointer);
      }
    },

    processVoiceFxPcmBytes(pcmBytes) {
      const success = runtime.ccall(
        "flipbits_web_process_voice_fx_pcm_bytes",
        "number",
        ["array", "number"],
        [pcmBytes, pcmBytes.byteLength],
      );

      if (success !== 1) {
        const message = runtime.ccall(
          "flipbits_web_last_error_message",
          "string",
          [],
          [],
        );
        throw new Error(message || "Voice processing failed.");
      }
    },

    finishVoiceFx() {
      const success = runtime.ccall(
        "flipbits_web_finish_voice_fx",
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
        throw new Error(message || "Voice processing failed.");
      }

      return readLastSampleResult();
    },

    abortEncodeOperation() {
      runtime.ccall("flipbits_web_abort_encode_operation", null, [], []);
    },

    abortVoiceFx() {
      runtime.ccall("flipbits_web_abort_voice_fx", null, [], []);
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
