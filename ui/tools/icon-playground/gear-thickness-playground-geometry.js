(() => {
  const cx = 128;
  const cy = 128;
  const gearBaseRadius = 110;
  const outerGearTipRadius = 115;

  // Outer gear proportions from generate_v10_colors.py
  const outerRTip = 115;
  const outerRBase = 110;

  // The gear-rotated <use> applies this rotation to gear-shape.
  // Inner holes embedded in gear-shape must pre-rotate by the negative
  // of this so they end up at 0° after the transform, matching
  // leftBg/rightBg and innerBrassOutline which are NOT rotated.
  const GEAR_ROTATION_DEG = 15;

  /* ------------------------------------------------------------------ */
  /*  Coordinate helpers                                                 */
  /* ------------------------------------------------------------------ */

  function rotatePoint(x, y, angleDeg) {
    const rad = angleDeg * Math.PI / 180;
    const dx = x - cx;
    const dy = y - cy;
    return [
      cx + dx * Math.cos(rad) - dy * Math.sin(rad),
      cy + dx * Math.sin(rad) + dy * Math.cos(rad),
    ];
  }

  function scaleValueAroundCenter(value, center, scale) {
    return center + ((value - center) * scale);
  }

  /* ------------------------------------------------------------------ */
  /*  Parametric gear-path generator (matches generate_v10_colors.py)    */
  /* ------------------------------------------------------------------ */

  /**
   * @param {number} rTip         – Radius to the tip of each tooth
   * @param {number} rBase        – Radius of the base arc between teeth
   * @param {number} tipWidthDeg  – Angular half-width of the tooth tip (degrees)
   * @param {number} baseWidthDeg – Angular half-width of the base arc gap (degrees)
   * @param {number} teeth        – Number of teeth
   * @param {number} [angleOffset=0] – Global rotation offset (degrees)
   * @returns {string} SVG path d-attribute (outline only, no hole)
   */
  function generateGearPath(rTip, rBase, tipWidthDeg, baseWidthDeg, teeth, angleOffset = 0) {
    const cmds = [];
    const angleStep = 360 / teeth;

    for (let i = 0; i < teeth; i++) {
      const baseAngle = i * angleStep + angleOffset;

      const xTip1 = cx + rTip * Math.cos((baseAngle - tipWidthDeg / 2) * Math.PI / 180);
      const yTip1 = cy + rTip * Math.sin((baseAngle - tipWidthDeg / 2) * Math.PI / 180);
      const xTip2 = cx + rTip * Math.cos((baseAngle + tipWidthDeg / 2) * Math.PI / 180);
      const yTip2 = cy + rTip * Math.sin((baseAngle + tipWidthDeg / 2) * Math.PI / 180);

      const xBase1 = cx + rBase * Math.cos((baseAngle + baseWidthDeg / 2) * Math.PI / 180);
      const yBase1 = cy + rBase * Math.sin((baseAngle + baseWidthDeg / 2) * Math.PI / 180);

      const nextBaseAngle = (i + 1) * angleStep + angleOffset;
      const xBase2 = cx + rBase * Math.cos((nextBaseAngle - baseWidthDeg / 2) * Math.PI / 180);
      const yBase2 = cy + rBase * Math.sin((nextBaseAngle - baseWidthDeg / 2) * Math.PI / 180);

      if (i === 0) {
        cmds.push(`M ${xTip1.toFixed(2)} ${yTip1.toFixed(2)}`);
      } else {
        cmds.push(`L ${xTip1.toFixed(2)} ${yTip1.toFixed(2)}`);
      }

      cmds.push(`L ${xTip2.toFixed(2)} ${yTip2.toFixed(2)}`);
      cmds.push(`L ${xBase1.toFixed(2)} ${yBase1.toFixed(2)}`);
      cmds.push(`A ${rBase} ${rBase} 0 0 1 ${xBase2.toFixed(2)} ${yBase2.toFixed(2)}`);
    }

    cmds.push("Z");
    return cmds.join(" ");
  }

  /* ------------------------------------------------------------------ */
  /*  Inner gear builders                                                 */
  /*                                                                      */
  /*  Strategy A  – Proportional angles, uniform tooth height              */
  /*  Strategy B  – Fixed outer-gear angles (15°/20°), boosted height      */
  /*  Strategy C  – Proportional angles + boosted height                   */
  /* ------------------------------------------------------------------ */

  // Outer gear ratios: tip/step = 15/30 = 0.5, base/step = 20/30 = 2/3
  const OUTER_TIP_RATIO = 15 / (360 / 12);   // 0.5
  const OUTER_BASE_RATIO = 20 / (360 / 12);  // 2/3

  // Outer gear absolute angular widths
  const OUTER_TIP_WIDTH = 15;
  const OUTER_BASE_WIDTH = 20;

  // Outer gear tooth height = rTip - rBase = 115 - 110 = 5
  // Height boost factors for fewer teeth (to maintain visual presence)
  const HEIGHT_BOOST = { 6: 2.0, 8: 1.5 };

  /**
   * Strategy A: Proportional angles, uniform tooth height.
   * Tooth/gap ratio matches the outer gear exactly.
   */
  function buildInnerGearA(radius, teeth, angleOffset = 0) {
    if (radius <= 0) return `M ${cx} ${cy} L ${cx} ${cy}`;
    const scale = radius / outerRTip;
    const rTip = outerRTip * scale;
    const rBase = outerRBase * scale;
    const angleStep = 360 / teeth;
    return generateGearPath(
      rTip, rBase,
      angleStep * OUTER_TIP_RATIO,
      angleStep * OUTER_BASE_RATIO,
      teeth, angleOffset,
    );
  }

  /**
   * Strategy B: Fixed outer-gear angles (15°/20°), boosted tooth height.
   * Teeth have the same angular width as the outer gear but are taller.
   */
  function buildInnerGearB(radius, teeth, angleOffset = 0) {
    if (radius <= 0) return `M ${cx} ${cy} L ${cx} ${cy}`;
    const scale = radius / outerRTip;
    const baseHeight = (outerRTip - outerRBase) * scale;   // proportional 5px
    const boost = HEIGHT_BOOST[teeth] ?? 1;
    const rTip = radius;                                    // tip stays at radius
    const rBase = radius - baseHeight * boost;              // base moves inward
    return generateGearPath(
      rTip, rBase,
      OUTER_TIP_WIDTH, OUTER_BASE_WIDTH,
      teeth, angleOffset,
    );
  }

  /**
   * Strategy C: Proportional angles + boosted tooth height.
   * Combines A's balanced tooth/gap ratio with B's taller teeth.
   */
  function buildInnerGearC(radius, teeth, angleOffset = 0) {
    if (radius <= 0) return `M ${cx} ${cy} L ${cx} ${cy}`;
    const scale = radius / outerRTip;
    const baseHeight = (outerRTip - outerRBase) * scale;
    const boost = HEIGHT_BOOST[teeth] ?? 1;
    const rTip = radius;
    const rBase = radius - baseHeight * boost;
    const angleStep = 360 / teeth;
    return generateGearPath(
      rTip, rBase,
      angleStep * OUTER_TIP_RATIO,
      angleStep * OUTER_BASE_RATIO,
      teeth, angleOffset,
    );
  }

  /* ------------------------------------------------------------------ */
  /*  Outer-12 scaled gear (parse-and-transform the literal path)        */
  /* ------------------------------------------------------------------ */

  const outerGearPath =
    "M 242.02 112.99 L 242.02 143.01 L 236.33 147.10 A 110 110 0 0 1 231.37 165.62 L 234.25 172.01 L 219.24 198.01 L 212.26 198.71 A 110 110 0 0 1 198.71 212.26 L 198.01 219.24 L 172.01 234.25 L 165.62 231.37 A 110 110 0 0 1 147.10 236.33 L 143.01 242.02 L 112.99 242.02 L 108.90 236.33 A 110 110 0 0 1 90.38 231.37 L 83.99 234.25 L 57.99 219.24 L 57.29 212.26 A 110 110 0 0 1 43.74 198.71 L 36.76 198.01 L 21.75 172.01 L 24.63 165.62 A 110 110 0 0 1 19.67 147.10 L 13.98 143.01 L 13.98 112.99 L 19.67 108.90 A 110 110 0 0 1 24.63 90.38 L 21.75 83.99 L 36.76 57.99 L 43.74 57.29 A 110 110 0 0 1 57.29 43.74 L 57.99 36.76 L 83.99 21.75 L 90.38 24.63 A 110 110 0 0 1 108.90 19.67 L 112.99 13.98 L 143.01 13.98 L 147.10 19.67 A 110 110 0 0 1 165.62 24.63 L 172.01 21.75 L 198.01 36.76 L 198.71 43.74 A 110 110 0 0 1 212.26 57.29 L 219.24 57.99 L 234.25 83.99 L 231.37 90.38 A 110 110 0 0 1 236.33 108.90 Z";

  /**
   * Scale (and optionally rotate) the literal outer gear path around center.
   * @param {number} scale      – uniform scale factor
   * @param {number} [rotateDeg=0] – additional rotation (degrees)
   */
  function transformOuterGearPath(scale, rotateDeg = 0) {
    if (scale <= 0) {
      return `M ${cx} ${cy} L ${cx} ${cy}`;
    }

    const needsRotation = rotateDeg !== 0;
    const tokens = outerGearPath.match(/[MLAZ]|-?\d+(?:\.\d+)?/g) ?? [];
    const output = [];
    let index = 0;

    while (index < tokens.length) {
      const token = tokens[index];
      index += 1;

      if (token === "M" || token === "L") {
        let x = scaleValueAroundCenter(Number(tokens[index]), cx, scale);
        let y = scaleValueAroundCenter(Number(tokens[index + 1]), cy, scale);
        if (needsRotation) [x, y] = rotatePoint(x, y, rotateDeg);
        output.push(token, x.toFixed(2), y.toFixed(2));
        index += 2;
        continue;
      }

      if (token === "A") {
        const rx = Number(tokens[index]);
        const ry = Number(tokens[index + 1]);
        const rotation = tokens[index + 2];
        const largeArcFlag = tokens[index + 3];
        const sweepFlag = tokens[index + 4];
        let x = scaleValueAroundCenter(Number(tokens[index + 5]), cx, scale);
        let y = scaleValueAroundCenter(Number(tokens[index + 6]), cy, scale);
        if (needsRotation) [x, y] = rotatePoint(x, y, rotateDeg);
        output.push(
          token,
          (rx * scale).toFixed(2),
          (ry * scale).toFixed(2),
          rotation,
          largeArcFlag,
          sweepFlag,
          x.toFixed(2),
          y.toFixed(2),
        );
        index += 7;
        continue;
      }

      output.push(token);
    }

    return output.join(" ");
  }

  /* ------------------------------------------------------------------ */
  /*  Simple shapes                                                      */
  /* ------------------------------------------------------------------ */

  function buildCircleOutlinePath(radius) {
    if (radius <= 0) {
      return `M ${cx} ${cy} L ${cx} ${cy}`;
    }
    const topY = cy - radius;
    const bottomY = cy + radius;
    return `M ${cx} ${topY} A ${radius} ${radius} 0 0 1 ${cx} ${bottomY} A ${radius} ${radius} 0 0 1 ${cx} ${topY} Z`;
  }

  function buildGearHolePath(radius) {
    if (radius <= 0) {
      return "";
    }
    const topY = cy - radius;
    const bottomY = cy + radius;
    return ` M ${cx} ${topY} A ${radius} ${radius} 0 0 0 ${cx} ${bottomY} A ${radius} ${radius} 0 0 0 ${cx} ${topY} Z`;
  }

  /* ------------------------------------------------------------------ */
  /*  Public builders                                                    */
  /* ------------------------------------------------------------------ */

  /** Map mode string → { builder, teeth } for parametric modes */
  const PARAMETRIC_MODES = {
    // Strategy A: proportional angles, uniform height
    a6:  { builder: buildInnerGearA, teeth: 6 },
    a8:  { builder: buildInnerGearA, teeth: 8 },
    // Strategy B: fixed angles, boosted height
    b6:  { builder: buildInnerGearB, teeth: 6 },
    b8:  { builder: buildInnerGearB, teeth: 8 },
    // Strategy C: proportional angles, boosted height
    c6:  { builder: buildInnerGearC, teeth: 6 },
    c8:  { builder: buildInnerGearC, teeth: 8 },
  };

  function buildInnerShapeVariants(mode, radius) {
    if (mode === "circle") {
      return {
        details: {
          family: "circle",
          radius,
          embeddedRotationCompensationDeg: 0,
        },
        visiblePath: buildCircleOutlinePath(radius),
        embeddedPath: buildGearHolePath(radius),
      };
    }

    if (mode === "outer12") {
      const scale = radius / outerGearTipRadius;
      return {
        details: {
          family: "scaled_outer",
          source: "literal_outer_path",
          scale,
          teeth: 12,
          angleOffsetDeg: 0,
          embeddedRotationCompensationDeg: -GEAR_ROTATION_DEG,
        },
        visiblePath: transformOuterGearPath(radius / outerGearTipRadius, 0),
        embeddedPath: ` ${transformOuterGearPath(radius / outerGearTipRadius, -GEAR_ROTATION_DEG)}`,
      };
    }

    // Parametric modes (a6, a8, b6, b8, c6, c8)
    const entry = PARAMETRIC_MODES[mode];
    if (entry) {
      const strategy = mode.charAt(0).toUpperCase();
      const scale = radius / outerRTip;
      const baseHeight = (outerRTip - outerRBase) * scale;
      const boost = HEIGHT_BOOST[entry.teeth] ?? 1;
      const usesProportionalAngles = strategy === "A" || strategy === "C";
      const usesBoostedHeight = strategy === "B" || strategy === "C";
      const angleStep = 360 / entry.teeth;
      const tipWidthDeg = usesProportionalAngles ? angleStep * OUTER_TIP_RATIO : OUTER_TIP_WIDTH;
      const baseWidthDeg = usesProportionalAngles ? angleStep * OUTER_BASE_RATIO : OUTER_BASE_WIDTH;
      const rTip = radius;
      const rBase = strategy === "A" ? outerRBase * scale : radius - (baseHeight * boost);
      return {
        details: {
          family: "parametric",
          strategy,
          teeth: entry.teeth,
          rTip,
          rBase,
          tipWidthDeg,
          baseWidthDeg,
          usesProportionalAngles,
          usesBoostedHeight,
          embeddedRotationCompensationDeg: -GEAR_ROTATION_DEG,
        },
        visiblePath: entry.builder(radius, entry.teeth, 0),
        embeddedPath: ` ${entry.builder(radius, entry.teeth, -GEAR_ROTATION_DEG)}`,
      };
    }

    // Fallback to circle
    return {
      details: {
        family: "circle_fallback",
        radius,
        embeddedRotationCompensationDeg: 0,
      },
      visiblePath: buildCircleOutlinePath(radius),
      embeddedPath: buildGearHolePath(radius),
    };
  }

  /**
   * Build the full gear-shape path: outer gear boundary + inner hole.
   *
   * IMPORTANT: `gear-shape` is consumed through `gear-rotated`, which applies
   * `rotate(GEAR_ROTATION_DEG)`. Any non-circular embedded inner hole must be
   * pre-rotated by `-GEAR_ROTATION_DEG`, otherwise it will drift away from the
   * visible 0° inner fill/stroke paths and produce seams / double outlines.
   */
  function buildGearPathWithInnerShape(mode, radius) {
    const { embeddedPath } = buildInnerShapeVariants(mode, radius);
    return `${outerGearPath}${embeddedPath}`;
  }

  window.GearThicknessGeometry = {
    config: {
      cx,
      cy,
      gearBaseRadius,
      outerGearPath,
    },
    buildGearPathWithInnerShape,
    buildInnerShapeVariants,
  };
})();
