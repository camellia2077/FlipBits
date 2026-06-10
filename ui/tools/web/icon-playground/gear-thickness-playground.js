(() => {
  const { GearThicknessGeometry, GearThicknessModel, GearThicknessRender } = window;
  const elements = GearThicknessModel.createElements();

  function renderCurrent() {
    GearThicknessRender.render(elements, GearThicknessModel.readState(elements));
  }

  function syncRadiusFromRange() {
    GearThicknessModel.applyRadius(elements, elements.innerRadiusInput.value);
    renderCurrent();
  }

  function syncRadiusFromNumber() {
    GearThicknessModel.applyRadius(elements, elements.innerRadiusNumber.value);
    renderCurrent();
  }

  function syncStrokeFromRange() {
    GearThicknessModel.applyStrokeWidth(elements, elements.strokeWidthInput.value);
    renderCurrent();
  }

  function syncStrokeFromNumber() {
    GearThicknessModel.applyStrokeWidth(elements, elements.strokeWidthNumber.value);
    renderCurrent();
  }

  function setRadius(value) {
    GearThicknessModel.applyRadius(elements, value);
    renderCurrent();
  }

  function setStrokeWidth(value) {
    GearThicknessModel.applyStrokeWidth(elements, value);
    renderCurrent();
  }

  elements.innerRadiusInput.addEventListener("input", syncRadiusFromRange);
  elements.innerRadiusNumber.addEventListener("input", syncRadiusFromNumber);
  elements.strokeWidthInput.addEventListener("input", syncStrokeFromRange);
  elements.strokeWidthNumber.addEventListener("input", syncStrokeFromNumber);
  elements.colorModeInputs.forEach((input) => input.addEventListener("change", renderCurrent));
  elements.innerDashModeInputs.forEach((input) => input.addEventListener("change", renderCurrent));
  elements.innerPatternModeInputs.forEach((input) => input.addEventListener("change", renderCurrent));

  // Design intent:
  // - `gearBaseRadius / 2` is the conceptual binary anchor. It gives the icon a
  //   clean "half-scale" relationship that reinforces the binary design motif.
  // - `gearBaseRadius * 0.65` is the optical adjustment preset. It intentionally
  //   deviates from the strict binary half to keep the center fuller for real
  //   app-icon delivery contexts.
  elements.binaryHalfRadiusButton.addEventListener(
    "click",
    () => setRadius(GearThicknessGeometry.config.gearBaseRadius / 2),
  );
  elements.optical065RadiusButton.addEventListener(
    "click",
    () => setRadius(GearThicknessGeometry.config.gearBaseRadius * 0.65),
  );
  elements.strokePresetButtons.forEach((button) => {
    button.addEventListener("click", () => setStrokeWidth(Number(button.dataset.strokePreset)));
  });
  elements.backgroundPresetButtons.forEach((button) => {
    button.addEventListener("click", () => {
      GearThicknessModel.applyBackgroundPreset(elements, button.dataset.backgroundPreset);
    });
  });

  GearThicknessModel.applyBackgroundPreset(elements, "default");
  renderCurrent();
})();
