import { DEFAULT_SAMPLE_RATE_HZ } from "./constants.js";
import { sanitizeMiniInput, resolveFrameSamples } from "./input-policy.js";

function setFieldVisibility(field, visible) {
  field.hidden = !visible;
  field.style.display = visible ? "" : "none";
}

function checkedValue(inputs, fallbackValue = "") {
  return inputs.find((input) => input.checked)?.value ?? fallbackValue;
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

export function readEncodeRequest(elements, mode, sampleRateHz = DEFAULT_SAMPLE_RATE_HZ) {
  const miniSpeed = checkedValue(elements.miniSpeedInputs, "wpm15");
  return {
    text: elements.inputText.value,
    mode,
    flashStyle: checkedValue(elements.flashStyleInputs, "standard"),
    miniSpeed,
    sampleRateHz,
    frameSamples: resolveFrameSamples(mode, miniSpeed, sampleRateHz),
  };
}
