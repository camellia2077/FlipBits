import { DEFAULT_FRAME_SAMPLES, MINI_SPEED_FRAME_SAMPLES } from "./constants.js";

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

export function resolveFrameSamples(mode, miniSpeed) {
  if (mode === "mini") {
    return MINI_SPEED_FRAME_SAMPLES[miniSpeed] ?? MINI_SPEED_FRAME_SAMPLES.wpm15;
  }

  return DEFAULT_FRAME_SAMPLES;
}
