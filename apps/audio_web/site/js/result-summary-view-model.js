import {
  FLASH_STYLE_OPTIONS,
  MINI_SPEED_OPTIONS,
  MODE_OPTIONS,
  VOICE_FX_PRESET_OPTIONS,
  VOICE_FX_SUBVOICE_STYLE_OPTIONS,
} from "./constants.js";
import { formatDuration } from "./result-formatters.js";

function findOptionLabel(options, value, fallback) {
  return options.find((option) => option.value === value)?.label ?? fallback;
}

function resolveModeLabel(mode) {
  return findOptionLabel(MODE_OPTIONS, mode, mode);
}

function resolveProfileLabel(request) {
  if (request.mode === "flash") {
    return findOptionLabel(
      FLASH_STYLE_OPTIONS,
      request.flashStyle,
      request.flashStyle,
    );
  }

  if (request.mode === "mini") {
    return findOptionLabel(
      MINI_SPEED_OPTIONS,
      request.miniSpeed,
      request.miniSpeed,
    );
  }

  return "default";
}

export function buildResultSummaryViewModel(request, result, sampleCount) {
  const sampleRateHz = result.sampleRateHz ?? request.sampleRateHz;
  return {
    modeLabel: resolveModeLabel(request.mode),
    profileLabel: resolveProfileLabel(request),
    durationLabel: formatDuration(sampleCount, sampleRateHz),
    sampleRateLabel: `${sampleRateHz} Hz`,
  };
}

export function buildVoiceFxResultSummaryViewModel(request, result, sampleCount) {
  const sampleRateHz = result.sampleRateHz ?? request.sampleRateHz;
  return {
    modeLabel: "Voice FX",
    profileLabel: `${findOptionLabel(
      VOICE_FX_PRESET_OPTIONS,
      request.preset,
      request.preset,
    )} / ${findOptionLabel(
      VOICE_FX_SUBVOICE_STYLE_OPTIONS,
      request.subvoiceStyle,
      request.subvoiceStyle,
    )}`,
    durationLabel: formatDuration(sampleCount, sampleRateHz),
    sampleRateLabel: `${sampleRateHz} Hz`,
  };
}
