import createFlipBitsModule from "../wasm/flipbits_web.js";
import { ENCODE_OPERATION_STATES } from "./constants.js";
import { elapsedMs, nowMs } from "./perf-diagnostics.js";
let encoderPromise = null;
const DEFAULT_PUMP_BUDGET = {
  maxWorkUnits: 65536,
  maxWallTimeMs: 16,
};
const PROGRESS_POST_INTERVAL_MS = 50;

function isTerminalSnapshot(snapshot) {
  return snapshot.state !== ENCODE_OPERATION_STATES.queued &&
    snapshot.state !== ENCODE_OPERATION_STATES.running;
}

function emptyPhaseDiagnostics() {
  return {
    preparing: { pumpCount: 0, pumpMs: 0 },
    rendering: { pumpCount: 0, pumpMs: 0 },
    postprocessing: { pumpCount: 0, pumpMs: 0 },
    finalizing: { pumpCount: 0, pumpMs: 0 },
    unknown: { pumpCount: 0, pumpMs: 0 },
  };
}

function emptyPostprocessSubphaseDiagnostics() {
  return {
    preamble: { pumpCount: 0, pumpMs: 0, workUnits: 0 },
    payload: { pumpCount: 0, pumpMs: 0, workUnits: 0 },
    epilogue: { pumpCount: 0, pumpMs: 0, workUnits: 0 },
    spectralTexture: { pumpCount: 0, pumpMs: 0, workUnits: 0 },
    unknown: { pumpCount: 0, pumpMs: 0, workUnits: 0 },
  };
}

function phaseDiagnosticsKey(phase) {
  switch (phase) {
    case 0:
      return "preparing";
    case 1:
      return "rendering";
    case 2:
      return "postprocessing";
    case 3:
      return "finalizing";
    default:
      return "unknown";
  }
}

function flashShellSampleCounts(request, workPlan, phaseTotalWorkUnits) {
  const sampleRateHz = request.sampleRateHz ?? 44100;
  const frameSamples = request.frameSamples ?? Math.max(1, Math.floor(sampleRateHz / 20));
  const style = request.flashStyle ?? "standard";
  const hasSpectralTexture = style !== null;
  const voicedTotal = hasSpectralTexture
    ? Math.floor(phaseTotalWorkUnits / 2)
    : phaseTotalWorkUnits;
  const preamble = style === "litany"
    ? Math.round(sampleRateHz * 1.35)
    : frameSamples * 3;
  const epilogue = style === "litany"
    ? Math.round(sampleRateHz * 1.15)
    : frameSamples * 3;
  const fallbackPayload = Math.max(0, voicedTotal - preamble - epilogue);
  const payload = Math.max(
    0,
    Math.min(workPlan?.renderingPcmWorkUnits ?? fallbackPayload, fallbackPayload),
  );

  return {
    preamble,
    payload,
    epilogue,
    spectralTexture: hasSpectralTexture ? voicedTotal : 0,
  };
}

function addSubphaseSlice(stats, key, elapsedMsValue, workUnits, totalDelta) {
  if (workUnits <= 0 || totalDelta <= 0) {
    return;
  }
  const entry = stats[key] ?? stats.unknown;
  entry.pumpCount += 1;
  entry.pumpMs += elapsedMsValue * (workUnits / totalDelta);
  entry.workUnits += workUnits;
}

function recordPostprocessSubphaseDiagnostics(
  stats,
  request,
  previousSnapshot,
  snapshot,
  workPlan,
  pumpElapsedMs,
) {
  if (request.mode !== "flash" || snapshot.phase !== 2) {
    return;
  }
  const phaseTotalWorkUnits = snapshot.phaseTotalWorkUnits ||
    workPlan?.postprocessingWorkUnits ||
    0;
  if (phaseTotalWorkUnits <= 1) {
    return;
  }
  const previousCompleted = previousSnapshot?.phase === 2
    ? previousSnapshot.phaseCompletedWorkUnits
    : 0;
  const currentCompleted = snapshot.phaseCompletedWorkUnits ?? 0;
  const delta = Math.max(0, currentCompleted - previousCompleted);
  if (delta <= 0) {
    return;
  }

  const shell = flashShellSampleCounts(request, workPlan, phaseTotalWorkUnits);
  const ranges = [
    ["preamble", 0, shell.preamble],
    ["payload", shell.preamble, shell.preamble + shell.payload],
    [
      "epilogue",
      shell.preamble + shell.payload,
      shell.preamble + shell.payload + shell.epilogue,
    ],
    [
      "spectralTexture",
      shell.preamble + shell.payload + shell.epilogue,
      shell.preamble + shell.payload + shell.epilogue + shell.spectralTexture,
    ],
  ];
  let knownWorkUnits = 0;
  for (const [key, begin, end] of ranges) {
    const overlap = Math.max(
      0,
      Math.min(currentCompleted, end) - Math.max(previousCompleted, begin),
    );
    knownWorkUnits += overlap;
    addSubphaseSlice(stats, key, pumpElapsedMs, overlap, delta);
  }
  addSubphaseSlice(stats, "unknown", pumpElapsedMs, delta - knownWorkUnits, delta);
}

function getEncoder() {
  if (!encoderPromise) {
    encoderPromise = createFlipBitsModule();
  }
  return encoderPromise;
}

getEncoder()
  .then(() => {
    globalThis.postMessage({ type: "ready" });
  })
  .catch((error) => {
    globalThis.postMessage({
      type: "ready-error",
      error: error instanceof Error ? error.message : String(error),
    });
  });

async function handleEncode(id, request) {
  try {
    const enableDiagnostics = request.enableDiagnostics === true;
    const totalStartMs = nowMs();
    const encoder = await getEncoder();
    const beginStartMs = nowMs();
    const operation = encoder.beginEncodeOperation({
      ...request,
      enableDiagnostics,
    });
    const beginMs = elapsedMs(beginStartMs);
    globalThis.postMessage({
      type: "progress",
      id,
      snapshot: operation.snapshot,
      workPlan: operation.workPlan,
    });

    let snapshot = operation.snapshot;
    let pumpCount = 0;
    let pumpMs = 0;
    let progressPostCount = 1;
    let lastProgressPostMs = nowMs();
    const phaseDiagnostics = emptyPhaseDiagnostics();
    const postprocessSubphaseDiagnostics = emptyPostprocessSubphaseDiagnostics();
    let currentWorkPlan = operation.workPlan;
    while (
      snapshot.state === ENCODE_OPERATION_STATES.queued ||
      snapshot.state === ENCODE_OPERATION_STATES.running
    ) {
      const previousSnapshot = snapshot;
      const pumpStartMs = nowMs();
      const pumpResult = encoder.pumpEncodeOperation(DEFAULT_PUMP_BUDGET);
      const pumpElapsedMs = elapsedMs(pumpStartMs);
      snapshot = pumpResult?.snapshot ?? pumpResult;
      currentWorkPlan = pumpResult?.workPlan ?? currentWorkPlan;
      pumpMs += pumpElapsedMs;
      pumpCount += 1;
      const phaseStats = phaseDiagnostics[phaseDiagnosticsKey(snapshot.phase)];
      phaseStats.pumpCount += 1;
      phaseStats.pumpMs += pumpElapsedMs;
      recordPostprocessSubphaseDiagnostics(
        postprocessSubphaseDiagnostics,
        request,
        previousSnapshot,
        snapshot,
        currentWorkPlan,
        pumpElapsedMs,
      );
      const currentMs = nowMs();
      if (
        isTerminalSnapshot(snapshot) ||
        currentMs - lastProgressPostMs >= PROGRESS_POST_INTERVAL_MS
      ) {
        globalThis.postMessage({
          type: "progress",
          id,
          snapshot,
          workPlan: currentWorkPlan,
        });
        progressPostCount += 1;
        lastProgressPostMs = currentMs;
      }
      await Promise.resolve();
    }

    if (snapshot.state !== ENCODE_OPERATION_STATES.succeeded) {
      throw new Error(
        encoder.errorMessageFromCode?.(snapshot.terminalCode) ??
          `Encoding failed with terminal code ${snapshot.terminalCode}.`,
      );
    }

    const takeStartMs = nowMs();
    const result = await encoder.takeEncodeOperationResult();
    const takeResultMs = elapsedMs(takeStartMs);
    const nativeDiagnostics = result?.diagnostics ?? operation.diagnostics ?? null;
    globalThis.postMessage({
      type: "result",
      id,
      result,
      diagnostics: {
        beginMs,
        pumpCount,
        pumpMs,
        progressPostCount,
        phaseDiagnostics,
        postprocessSubphaseDiagnostics,
        averagePumpMs: pumpCount > 0 ? pumpMs / pumpCount : 0,
        takeResultMs,
        totalWorkerMs: elapsedMs(totalStartMs),
        workPlan: currentWorkPlan,
        nativeDiagnostics,
      },
    });
  } catch (error) {
    try {
      const encoder = await getEncoder();
      encoder.abortEncodeOperation();
    } catch {
      // Best effort cleanup for abandoned operations in the worker.
    }
    globalThis.postMessage({
      type: "error",
      id,
      error: error instanceof Error ? error.message : String(error),
    });
  }
}

async function handleVoiceFx(id, request) {
  try {
    const totalStartMs = nowMs();
    const encoder = await getEncoder();
    const samples = request.samples instanceof Int16Array
      ? request.samples
      : Int16Array.from(request.samples ?? []);
    if (!samples.length) {
      throw new Error("Audio input is empty.");
    }

    const byteSliceStartMs = nowMs();
    const bytes = new Uint8Array(samples.buffer.slice(samples.byteOffset, samples.byteOffset + samples.byteLength));
    const byteSliceMs = elapsedMs(byteSliceStartMs);
    const applyStartMs = nowMs();
    const result = encoder.applyVoiceFxPcmBytes({
      preset: request.preset,
      subvoiceStyle: request.subvoiceStyle,
      sampleRateHz: request.sampleRateHz,
      pcmBytes: bytes,
    });
    const applyTotalMs = elapsedMs(applyStartMs);
    globalThis.postMessage({
      type: "result",
      id,
      result,
      diagnostics: {
        inputSampleCount: samples.length,
        inputByteLength: bytes.byteLength,
        byteSliceMs,
        applyTotalMs,
        totalWorkerMs: elapsedMs(totalStartMs),
        wasm: result.diagnostics ?? null,
      },
    });
  } catch (error) {
    try {
      const encoder = await getEncoder();
      encoder.abortVoiceFx?.();
    } catch {
      // Best effort cleanup for abandoned operations in the worker.
    }
    globalThis.postMessage({
      type: "error",
      id,
      error: error instanceof Error ? error.message : String(error),
    });
  }
}

globalThis.addEventListener("message", (event) => {
  const { type, id, request } = event.data ?? {};
  if (type === "encode") {
    void handleEncode(id, request);
    return;
  }

  if (type === "voice-fx") {
    void handleVoiceFx(id, request);
  }
});
