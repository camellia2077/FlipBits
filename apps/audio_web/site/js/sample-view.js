import { getTranslation } from "./i18n.js";

export class SampleView {
  constructor(elements) {
    this.elements = elements;
  }

  setLocale(locale) {
    this.elements.sampleLengthLabel.textContent = getTranslation(locale, "sample.lengthLabel");
    this.elements.sampleLengthShortLabel.textContent = getTranslation(locale, "sample.length.short");
    this.elements.sampleLengthLongLabel.textContent = getTranslation(locale, "sample.length.long");
    this.elements.sampleAddEmojiLabel.textContent = getTranslation(locale, "sample.addEmoji");
    const randomLabel = getTranslation(locale, "sample.randomButton");
    this.elements.randomSampleButtonText.textContent = randomLabel;
    this.elements.randomSampleButton.setAttribute("aria-label", randomLabel);
  }

  setControlsEnabled(enabled) {
    for (const input of this.elements.sampleLengthInputs) {
      input.disabled = !enabled;
    }
    this.elements.sampleAddEmojiToggle.disabled = !enabled;
    this.elements.randomSampleButton.disabled = !enabled;
  }

  setAddEmojiVisibility(visible) {
    this.elements.sampleAddEmojiField.hidden = !visible;
  }
}
