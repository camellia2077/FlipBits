import createFlipBitsModule from "../wasm/flipbits_web.js";
import { ENCODE_OPERATION_STATES } from "./constants.js";

let encoderPromise = null;
const DEFAULT_PUMP_BUDGET = {
  maxWorkUnits: 2048,
  maxWallTimeMs: 8,
};

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

globalThis.addEventListener("message", async (event) => {
  const { type, id, request } = event.data ?? {};
  if (type !== "encode") {
    return;
  }

  try {
    const encoder = await getEncoder();
    const operation = encoder.beginEncodeOperation(request);
    globalThis.postMessage({
      type: "progress",
      id,
      snapshot: operation.snapshot,
      workPlan: operation.workPlan,
    });

    let snapshot = operation.snapshot;
    while (
      snapshot.state === ENCODE_OPERATION_STATES.queued ||
      snapshot.state === ENCODE_OPERATION_STATES.running
    ) {
      snapshot = encoder.pumpEncodeOperation(DEFAULT_PUMP_BUDGET);
      globalThis.postMessage({
        type: "progress",
        id,
        snapshot,
        workPlan: operation.workPlan,
      });
      await Promise.resolve();
    }

    if (snapshot.state !== ENCODE_OPERATION_STATES.succeeded) {
      throw new Error(
        encoder.errorMessageFromCode?.(snapshot.terminalCode) ??
          `Encoding failed with terminal code ${snapshot.terminalCode}.`,
      );
    }

    const result = await encoder.takeEncodeOperationResult();
    globalThis.postMessage({ type: "result", id, result });
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
});
