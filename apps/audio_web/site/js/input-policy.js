import { DEFAULT_FRAME_SECONDS, MINI_SPEED_FRAME_SECONDS } from "./constants.js";

export function stripNonAscii(text) {
  return Array.from(text).filter((char) => char.charCodeAt(0) <= 0x7f).join("");
}

export function sanitizeMiniInput(mode, text) {
  if (mode !== "mini") {
    return { text, changed: false };
  }

  const sanitizedText = stripNonAscii(text);
  return {
    text: sanitizedText,
    changed: sanitizedText !== text,
  };
}

function secondsToSamples(seconds, sampleRateHz) {
  return Math.max(1, Math.round(seconds * sampleRateHz));
}

export function resolveFrameSamples(mode, miniSpeed, ultraSpeed, sampleRateHz) {
  if (mode === "mini") {
    return secondsToSamples(
      MINI_SPEED_FRAME_SECONDS[miniSpeed] ?? MINI_SPEED_FRAME_SECONDS.wpm15,
      sampleRateHz,
    );
  }

  if (mode === "ultra" && ultraSpeed === "bd15_625") {
    return secondsToSamples(1 / 15.625, sampleRateHz);
  }

  return secondsToSamples(DEFAULT_FRAME_SECONDS, sampleRateHz);
}
