(() => {
  const COLORS = {
    red: "#9E1B1B",
    cream: "#E8E2D0",
  };

  const BACKGROUND_PRESETS = {
    default: { tile: "#1A1412" },
    charcoal: { tile: "#23262A" },
    graphite: { tile: "#30343A" },
    mars: { tile: "#2A1616" },
    relic: { tile: "#40201D" },
    oxidized: { tile: "#31443B" },
    catacomb: { tile: "#20302A" },
    bone: { tile: "#D8CFBC" },
    light: { tile: "#E8E2D0" },
    white: { tile: "#E0E0E0" },
  };

  const INNER_PATTERN_LABELS = {
    circle:  "Circle",
    a6:      "A · 6 teeth",
    a8:      "A · 8 teeth",
    b6:      "B · 6 teeth",
    b8:      "B · 8 teeth",
    c6:      "C · 6 teeth",
    c8:      "C · 8 teeth",
    outer12: "Scaled 12",
  };

  function formatInnerPatternLabel(mode) {
    return INNER_PATTERN_LABELS[mode] ?? mode;
  }

  function createElements(doc = document) {
    return {
      innerRadiusInput: doc.getElementById("innerRadius"),
      innerRadiusNumber: doc.getElementById("innerRadiusNumber"),
      strokeWidthInput: doc.getElementById("strokeWidth"),
      strokeWidthNumber: doc.getElementById("strokeWidthNumber"),
      innerRadiusValue: doc.getElementById("innerRadiusValue"),
      strokeWidthValue: doc.getElementById("strokeWidthValue"),
      innerPatternModeValue: doc.getElementById("innerPatternModeValue"),
      currentParameters: doc.getElementById("currentParameters"),
      gearHoleRadiusValue: doc.getElementById("gearHoleRadiusValue"),
      ringThicknessValue: doc.getElementById("ringThicknessValue"),
      leftBg: doc.getElementById("leftBg"),
      rightBg: doc.getElementById("rightBg"),
      dataCoreLeft: doc.getElementById("dataCoreLeft"),
      dataCoreRight: doc.getElementById("dataCoreRight"),
      innerBrassOutline: doc.getElementById("innerBrassOutline"),
      gearFillLeft: doc.getElementById("gearFillLeft"),
      gearFillRight: doc.getElementById("gearFillRight"),
      gearShape: doc.getElementById("gear-shape"),
      previewCard: doc.getElementById("previewCard"),
      colorModeInputs: doc.querySelectorAll('input[name="colorMode"]'),
      innerDashModeInputs: doc.querySelectorAll('input[name="innerDashMode"]'),
      innerPatternModeInputs: doc.querySelectorAll('input[name="innerPatternMode"]'),
      binaryHalfRadiusButton: doc.getElementById("binaryHalfRadius"),
      optical065RadiusButton: doc.getElementById("optical065Radius"),
      strokePresetButtons: doc.querySelectorAll("[data-stroke-preset]"),
      backgroundPresetButtons: doc.querySelectorAll("[data-background-preset]"),
    };
  }

  function clampRadius(value) {
    return Math.min(96, Math.max(0, Number(value || 0)));
  }

  function clampStrokeWidth(value) {
    return Math.min(12, Math.max(0, Number(value || 0)));
  }

  function currentRadioValue(name) {
    return document.querySelector(`input[name="${name}"]:checked`).value;
  }

  function readState(elements) {
    return {
      radius: Number(elements.innerRadiusInput.value),
      strokeWidth: Number(elements.strokeWidthInput.value),
      innerPatternMode: currentRadioValue("innerPatternMode"),
      innerDashMode: currentRadioValue("innerDashMode"),
      colorMode: currentRadioValue("colorMode"),
    };
  }

  function applyRadius(elements, value) {
    const clamped = clampRadius(value);
    elements.innerRadiusInput.value = clamped;
    elements.innerRadiusNumber.value = clamped;
  }

  function applyStrokeWidth(elements, value) {
    const clamped = clampStrokeWidth(value);
    elements.strokeWidthInput.value = clamped;
    elements.strokeWidthNumber.value = clamped;
  }

  function applyBackgroundPreset(elements, name) {
    const preset = BACKGROUND_PRESETS[name] ?? BACKGROUND_PRESETS.default;
    elements.previewCard.style.setProperty("--tile-bg", preset.tile);
  }

  window.GearThicknessModel = {
    BACKGROUND_PRESETS,
    COLORS,
    applyBackgroundPreset,
    applyRadius,
    applyStrokeWidth,
    clampRadius,
    clampStrokeWidth,
    createElements,
    formatInnerPatternLabel,
    readState,
  };
})();
