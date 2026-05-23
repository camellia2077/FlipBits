import createFlipBitsModule from "../wasm/flipbits_web.js";

let encoderPromise = null;

function getEncoder() {
  if (!encoderPromise) {
    encoderPromise = createFlipBitsModule();
  }
  return encoderPromise;
}

globalThis.flipbitsOnEncodeProgress = (phase, progress) => {
  if (globalThis.currentEncodeRequestId == null) {
    return;
  }

  globalThis.postMessage({
    type: "progress",
    id: globalThis.currentEncodeRequestId,
    phase,
    progress,
  });
};

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
    globalThis.currentEncodeRequestId = id;
    const result = await encoder.encodeTextToPcm16(request);
    globalThis.currentEncodeRequestId = null;
    globalThis.postMessage({ type: "result", id, result });
  } catch (error) {
    globalThis.currentEncodeRequestId = null;
    globalThis.postMessage({
      type: "error",
      id,
      error: error instanceof Error ? error.message : String(error),
    });
  }
});
