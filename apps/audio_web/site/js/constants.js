export const DEFAULT_SAMPLE_RATE_HZ = 44100;
export const DEFAULT_FRAME_SECONDS = 0.05;

export const MINI_SPEED_FRAME_SECONDS = {
  wpm10: 0.12,
  wpm15: 0.08,
  wpm20: 0.06,
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

export const WORKFLOW_OPTIONS = {
  data: "data",
  voice: "voice",
};

export const VOICE_TRACK_MODE_OPTIONS = {
  single: "single",
  dual: "dual",
};

export const VOICE_FX_PRESET_OPTIONS = [
  { value: "machine_voice", label: "Machine Voice", trackMode: "single" },
  { value: "signal_cant", label: "Signal Cant", trackMode: "single" },
  { value: "robot_vox", label: "Robot Vox", trackMode: "single" },
  { value: "binaric_cant", label: "Binaric Cant", trackMode: "dual" },
  { value: "voice_trigger", label: "Voice Trigger", trackMode: "dual" },
  { value: "raw_constant", label: "Raw Constant", trackMode: "dual" },
];

export const VOICE_FX_SUBVOICE_STYLE_OPTIONS = [
  { value: "standard", label: "standard" },
  { value: "litany", label: "litany" },
  { value: "hostility", label: "hostility" },
  { value: "collapse", label: "collapse" },
  { value: "zeal", label: "zeal" },
  { value: "void", label: "void" },
];

export const ENCODE_PROGRESS_PHASES = {
  0: "preparing",
  1: "rendering",
  2: "postprocessing",
  3: "finalizing",
};

export const LINEAR_PROGRESS_PHASES = {
  voiceProcessing: "voiceProcessing",
};

export const ENCODE_OPERATION_STATES = {
  queued: 0,
  running: 1,
  succeeded: 2,
  failed: 3,
  cancelled: 4,
};
