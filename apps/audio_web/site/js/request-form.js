import { DEFAULT_SAMPLE_RATE_HZ } from "./constants.js";
import { sanitizeMiniInput, resolveFrameSamples } from "./input-policy.js";

function setFieldVisibility(field, visible) {
  field.hidden = !visible;
  field.style.display = visible ? "" : "none";
}

export function syncModeFields(elements, ui, mode) {
  setFieldVisibility(elements.flashStyleField, mode === "flash");
  setFieldVisibility(elements.miniSpeedField, mode === "mini");
  ui.setInputHint(mode);
}

export function sanitizeModeText(elements, mode) {
  const sanitized = sanitizeMiniInput(
    mode,
    elements.inputText.value,
  );
  if (!sanitized.changed) {
    return false;
  }

  elements.inputText.value = sanitized.text;
  return true;
}

export function readEncodeRequest(elements, mode) {
  return {
    text: elements.inputText.value,
    mode,
    flashStyle: elements.flashStyleSelect.value,
    miniSpeed: elements.miniSpeedSelect.value,
    sampleRateHz: DEFAULT_SAMPLE_RATE_HZ,
    frameSamples: resolveFrameSamples(mode, elements.miniSpeedSelect.value),
  };
}
