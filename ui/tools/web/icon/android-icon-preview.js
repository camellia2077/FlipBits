const foregroundSource = "../../icon-foreground.svg";
const maskConfigs = [
  {
    id: "rounded-square",
    label: "Rounded square",
    clipId: "mask-rounded-square",
    note: "Close to many Pixel-style launcher treatments and a good baseline for overall balance."
  },
  {
    id: "circle",
    label: "Circle",
    clipId: "mask-circle",
    note: "Useful for checking whether the gear still reads clearly when the corners disappear entirely."
  },
  {
    id: "squircle",
    label: "Squircle",
    clipId: "mask-squircle",
    note: "A softer, more inflated mask that can make the icon feel fuller near the corners."
  },
  {
    id: "teardrop",
    label: "Teardrop",
    clipId: "mask-teardrop",
    note: "Less common now, but still worth checking because it stresses top and bottom balance differently."
  }
];

const sizePreviews = [96, 72, 48];
const VIEWPORT_SIZE = 108;
const BASE_ICON_AREA_DIAMETER = 84;
const BASE_ICON_AREA_RADIUS = BASE_ICON_AREA_DIAMETER / 2;
const BASE_ICON_AREA_OFFSET = (VIEWPORT_SIZE - BASE_ICON_AREA_DIAMETER) / 2;
const BASE_ICON_ROUNDED_SQUARE_RADIUS = 18;
const GUIDE_REFERENCE_DIAMETER = 72;
const FOREGROUND_ART_VISIBLE_RATIO = 220 / 256;
const scaleInput = document.getElementById("foregroundScale");
const scaleNumber = document.getElementById("foregroundScaleNumber");
const backgroundPicker = document.getElementById("backgroundPicker");
const backgroundHex = document.getElementById("backgroundHex");
const statusBlock = document.getElementById("statusBlock");
const deviceGrid = document.getElementById("deviceGrid");
const maskGrid = document.getElementById("maskGrid");
const deviceConfigs = [
  {
    id: "pixel",
    label: "Pixel (circle)",
    clipId: "mask-circle",
    note: "Uses the shared base icon area, then resolves that same base to a circular launcher result.",
    size: 220,
    launcherShape: "circle"
  },
  {
    id: "cn-android",
    label: "CN Android (rounded square)",
    clipId: "mask-rounded-square",
    note: "Uses the same shared base icon area as Pixel, but keeps it as a rounded-square launcher result.",
    size: 220,
    launcherShape: "rounded-square"
  }
];

function normalizeHex(value) {
  const cleaned = value.trim().replace(/^#?/, "#");
  if (!/^#[0-9a-fA-F]{6}$/.test(cleaned)) {
    return null;
  }

  return cleaned.toUpperCase();
}

function buildMaskCard(config) {
  const article = document.createElement("article");
  article.className = "mask-card";
  article.innerHTML = `
    <div>
      <h3>${config.label}</h3>
      <p>${config.note}</p>
    </div>
    <div class="big-preview" data-preview-host></div>
    <div class="sizes" data-sizes-host></div>
  `;

  const previewHost = article.querySelector("[data-preview-host]");
  const sizesHost = article.querySelector("[data-sizes-host]");
  previewHost.appendChild(createPreviewSvg(config.clipId, 212, true));

  for (const size of sizePreviews) {
    const chip = document.createElement("div");
    chip.className = "size-chip";
    chip.innerHTML = `<div>${size}px</div>`;
    chip.appendChild(createPreviewSvg(config.clipId, size, false));
    sizesHost.appendChild(chip);
  }

  return article;
}

function buildDeviceCard(config) {
  const article = document.createElement("article");
  article.className = "device-card";
  article.innerHTML = `
    <div>
      <h3>${config.label}</h3>
      <p>${config.note}</p>
    </div>
    <div class="device-preview">
      <div class="device-preview-frame" data-device-host></div>
    </div>
  `;

  const host = article.querySelector("[data-device-host]");
  host.appendChild(createPreviewSvg(config.clipId, config.size, true, {
    devicePreview: true,
    launcherShape: config.launcherShape,
  }));

  const label = document.createElement("div");
  label.className = "device-label";
  label.textContent = `${config.size}px preview`;
  host.appendChild(label);

  return article;
}

function createPreviewSvg(clipId, sizePx, large, options = {}) {
  const svgNs = "http://www.w3.org/2000/svg";
  const devicePreview = options.devicePreview === true;
  const launcherShape = options.launcherShape ?? "mask";
  const svg = document.createElementNS(svgNs, "svg");
  svg.setAttribute("class", "preview-svg");
  svg.setAttribute("viewBox", "0 0 108 108");
  svg.setAttribute("width", String(sizePx));
  svg.setAttribute("height", String(sizePx));
  svg.dataset.clipId = clipId;
  svg.dataset.devicePreview = devicePreview ? "true" : "false";
  svg.dataset.launcherShape = launcherShape;

  const background = document.createElementNS(svgNs, "rect");
  if (devicePreview) {
    background.setAttribute("x", String(BASE_ICON_AREA_OFFSET));
    background.setAttribute("y", String(BASE_ICON_AREA_OFFSET));
    background.setAttribute("width", String(BASE_ICON_AREA_DIAMETER));
    background.setAttribute("height", String(BASE_ICON_AREA_DIAMETER));
    if (launcherShape === "circle") {
      background.setAttribute("rx", String(BASE_ICON_AREA_RADIUS));
      background.setAttribute("ry", String(BASE_ICON_AREA_RADIUS));
    } else if (launcherShape === "rounded-square") {
      background.setAttribute("rx", String(BASE_ICON_ROUNDED_SQUARE_RADIUS));
      background.setAttribute("ry", String(BASE_ICON_ROUNDED_SQUARE_RADIUS));
    }
  } else {
    background.setAttribute("x", "0");
    background.setAttribute("y", "0");
    background.setAttribute("width", "108");
    background.setAttribute("height", "108");
    background.setAttribute("clip-path", `url(#${clipId})`);
  }
  background.setAttribute("fill", getBackgroundHex());
  background.dataset.role = "background";
  svg.appendChild(background);

  const image = document.createElementNS(svgNs, "image");
  image.setAttributeNS("http://www.w3.org/1999/xlink", "href", foregroundSource);
  if (!devicePreview) {
    image.setAttribute("clip-path", `url(#${clipId})`);
  }
  image.setAttribute("preserveAspectRatio", "xMidYMid meet");
  image.dataset.role = "foreground";
  svg.appendChild(image);
  updatePreviewSvg(svg);
  return svg;
}

function getScaleValue() {
  return Number(scaleInput.value);
}

function getBackgroundHex() {
  return normalizeHex(backgroundHex.value) || "#E8E2D0";
}

function updatePreviewSvg(svg) {
  const scale = getScaleValue() / 100;
  const devicePreview = svg.dataset.devicePreview === "true";
  const background = svg.querySelector('[data-role="background"]');
  const foreground = svg.querySelector('[data-role="foreground"]');

  // Device previews use one shared base icon area. Pixel and CN Android differ
  // in launcher shape, not in icon base size. The outer launcher boundary is
  // intentionally not drawn here because it made the previews read as if the
  // base were "too small". We keep only the shared background base plus the
  // optional guide overlay so the preview stays focused on the icon itself.
  const baseDiameter = devicePreview ? BASE_ICON_AREA_DIAMETER : VIEWPORT_SIZE;
  const baseOffset = devicePreview ? BASE_ICON_AREA_OFFSET : 0;

  // The browser preview renders the formal foreground as an <image>, which
  // includes transparent margin around the visible gear artwork. Android scales
  // VectorDrawable path geometry directly, so applying the same numeric scale
  // to the raw <image> box makes the foreground look much smaller than on a
  // real device. To match runtime perception more closely, interpret the UI
  // scale against a guide/reference diameter for the *visible* artwork, then
  // expand back to the image box using the artwork's occupied ratio.
  const desiredVisibleArtDiameter = GUIDE_REFERENCE_DIAMETER * scale;
  const imageBoxSize = desiredVisibleArtDiameter / FOREGROUND_ART_VISIBLE_RATIO;
  const offset = (VIEWPORT_SIZE - imageBoxSize) / 2;

  background.setAttribute("fill", getBackgroundHex());
  foreground.setAttribute("x", offset.toFixed(2));
  foreground.setAttribute("y", offset.toFixed(2));
  foreground.setAttribute("width", imageBoxSize.toFixed(2));
  foreground.setAttribute("height", imageBoxSize.toFixed(2));
}

function syncScaleInputs(nextValue) {
  scaleInput.value = String(nextValue);
  scaleNumber.value = String(nextValue);
}

function syncBackgroundInputs(nextValue) {
  backgroundPicker.value = nextValue;
  backgroundHex.value = nextValue;
}

function renderStatus() {
  statusBlock.textContent = [
    `Foreground source : ${foregroundSource}`,
    `Foreground scale  : ${getScaleValue()}%`,
    `Adaptive bg       : ${getBackgroundHex()}`,
    "",
    "Notes:",
    "- This page is a fast browser-side style preview, not a strict launcher simulation.",
    "- Use it to compare circle vs rounded-square styling, background color, and whether the icon feels too empty or too full.",
    "- Pixel and CN Android differ here by launcher shape, not by background size.",
    "- Foreground scale is compensated for visible artwork, but it still should not be treated as a device-exact number."
  ].join("\n");
}

function rerender() {
  document.querySelectorAll(".preview-svg").forEach(updatePreviewSvg);
  renderStatus();
}

function handleScaleChange(value) {
  const parsed = Number(value);
  const numeric = Number.isFinite(parsed) ? Math.min(100, Math.max(0, parsed)) : 100;
  syncScaleInputs(numeric);
  rerender();
}

scaleInput.addEventListener("input", (event) => handleScaleChange(event.target.value));
scaleNumber.addEventListener("input", (event) => handleScaleChange(event.target.value));

backgroundPicker.addEventListener("input", (event) => {
  const value = normalizeHex(event.target.value) || "#E8E2D0";
  syncBackgroundInputs(value);
  rerender();
});

backgroundHex.addEventListener("change", (event) => {
  const value = normalizeHex(event.target.value) || "#E8E2D0";
  syncBackgroundInputs(value);
  rerender();
});

for (const config of maskConfigs) {
  maskGrid.appendChild(buildMaskCard(config));
}

for (const config of deviceConfigs) {
  deviceGrid.appendChild(buildDeviceCard(config));
}

renderStatus();
