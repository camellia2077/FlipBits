export const DEFAULT_SAMPLE_RATE_HZ = 44100;
export const DEFAULT_FRAME_SAMPLES = 2205;

export const MINI_SPEED_FRAME_SAMPLES = {
  wpm10: 5292,
  wpm15: 3528,
  wpm20: 2646,
};

export const MODE_OPTIONS = [
  { value: "mini", label: "mini: Morse code" },
  { value: "flash", label: "flash: Bit-by-bit BFSK" },
  { value: "pro", label: "pro: DTMF-like dual-tone mapping" },
  { value: "ultra", label: "ultra: 16-FSK" },
];

export const FLASH_STYLE_OPTIONS = [
  { value: "standard", label: "standard" },
  { value: "litany", label: "litany" },
  { value: "hostility", label: "hostility" },
  { value: "collapse", label: "collapse" },
  { value: "zeal", label: "zeal" },
  { value: "void", label: "void" },
];

export const MINI_SPEED_OPTIONS = [
  { value: "wpm10", label: "10 WPM" },
  { value: "wpm15", label: "15 WPM" },
  { value: "wpm20", label: "20 WPM" },
];

export const SAMPLE_LENGTH_OPTIONS = [
  { value: "short", label: "Short" },
  { value: "long", label: "Long" },
];

export const ENCODE_PROGRESS_PHASES = {
  0: "preparing",
  1: "rendering",
  2: "postprocessing",
  3: "finalizing",
};

export const ENCODE_OPERATION_STATES = {
  queued: 0,
  running: 1,
  succeeded: 2,
  failed: 3,
  cancelled: 4,
};
