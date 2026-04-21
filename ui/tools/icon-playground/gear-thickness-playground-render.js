(() => {
  const { GearThicknessGeometry, GearThicknessModel } = window;

  function applyColorMode(elements, mode) {
    if (mode === "aligned") {
      elements.leftBg.setAttribute("fill", GearThicknessModel.COLORS.cream);
      elements.rightBg.setAttribute("fill", GearThicknessModel.COLORS.red);
      elements.dataCoreLeft.setAttribute("color", GearThicknessModel.COLORS.red);
      elements.dataCoreRight.setAttribute("color", GearThicknessModel.COLORS.cream);
    } else {
      elements.leftBg.setAttribute("fill", GearThicknessModel.COLORS.red);
      elements.rightBg.setAttribute("fill", GearThicknessModel.COLORS.cream);
      elements.dataCoreLeft.setAttribute("color", GearThicknessModel.COLORS.cream);
      elements.dataCoreRight.setAttribute("color", GearThicknessModel.COLORS.red);
    }

    elements.gearFillLeft.setAttribute("fill", GearThicknessModel.COLORS.cream);
    elements.gearFillRight.setAttribute("fill", GearThicknessModel.COLORS.red);
  }

  function formatNumber(value) {
    return typeof value === "number" ? value.toFixed(2) : String(value);
  }

  function buildParameterText(state, innerShape) {
    const lines = [
      `innerPatternMode: ${state.innerPatternMode}`,
      `innerPatternLabel: ${GearThicknessModel.formatInnerPatternLabel(state.innerPatternMode)}`,
      `innerRadius: ${formatNumber(state.radius)}`,
      `strokeWidth: ${formatNumber(state.strokeWidth)}`,
      `innerDashMode: ${state.innerDashMode}`,
      `colorMode: ${state.colorMode}`,
      `visiblePathLength: ${innerShape.visiblePath.length}`,
      `embeddedPathLength: ${innerShape.embeddedPath.length}`,
      "",
      "[geometry]",
    ];

    for (const [key, value] of Object.entries(innerShape.details ?? {})) {
      lines.push(`${key}: ${typeof value === "number" ? formatNumber(value) : value}`);
    }

    return lines.join("\n");
  }

  function render(elements, state) {
    const { gearBaseRadius } = GearThicknessGeometry.config;
    const thickness = gearBaseRadius - state.radius;
    const innerShape = GearThicknessGeometry.buildInnerShapeVariants(
      state.innerPatternMode,
      state.radius,
    );

    // These visible layers are rendered at 0 degrees. They must always use the
    // visible path variant, never the rotation-compensated embedded hole path.
    elements.leftBg.setAttribute("d", innerShape.visiblePath);
    elements.rightBg.setAttribute("d", innerShape.visiblePath);
    elements.leftBg.setAttribute("clip-path", "url(#left-wave-clip)");
    elements.rightBg.setAttribute("clip-path", "url(#right-wave-clip)");
    elements.gearShape.setAttribute(
      "d",
      GearThicknessGeometry.buildGearPathWithInnerShape(state.innerPatternMode, state.radius),
    );
    const dashVisibility = state.innerDashMode === "on" ? "visible" : "hidden";
    elements.dataCoreLeft.setAttribute("visibility", dashVisibility);
    elements.dataCoreRight.setAttribute("visibility", dashVisibility);
    elements.innerBrassOutline.setAttribute("d", innerShape.visiblePath);
    elements.innerBrassOutline.setAttribute("stroke-width", state.strokeWidth.toFixed(2));
    elements.gearShape.setAttribute("stroke-width", state.strokeWidth.toFixed(2));

    applyColorMode(elements, state.colorMode);

    elements.innerRadiusValue.textContent = state.radius.toFixed(1);
    elements.strokeWidthValue.textContent = state.strokeWidth.toFixed(2);
    elements.innerPatternModeValue.textContent = GearThicknessModel.formatInnerPatternLabel(state.innerPatternMode);
    elements.gearHoleRadiusValue.textContent = state.radius.toFixed(1);
    elements.ringThicknessValue.textContent = thickness.toFixed(1);
    elements.currentParameters.textContent = buildParameterText(state, innerShape);
  }

  window.GearThicknessRender = {
    render,
  };
})();
