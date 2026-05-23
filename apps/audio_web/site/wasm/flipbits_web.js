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

  return {
    async encodeTextToPcm16(request) {
      const modeId = toModeId(request.mode);
      const flashStyleId = toFlashStyleId(request.flashStyle);
      const sampleRateHz = request.sampleRateHz ?? 44100;
      const frameSamples = request.frameSamples ?? Math.max(1, Math.floor(sampleRateHz / 20));
      const success = runtime.ccall(
        "flipbits_web_encode_text",
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
        frameSamples,
      };
    },
  };
}
