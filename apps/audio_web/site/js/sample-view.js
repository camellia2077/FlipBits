import { getTranslation } from "./i18n.js";

export class SampleView {
  constructor(elements) {
    this.elements = elements;
  }

  setLocale(locale) {
    this.elements.sampleLengthLabel.textContent = getTranslation(locale, "sample.lengthLabel");
    this.elements.sampleLengthSelect.options[0].textContent = getTranslation(locale, "sample.length.short");
    this.elements.sampleLengthSelect.options[1].textContent = getTranslation(locale, "sample.length.long");
    this.elements.sampleAddEmojiLabel.textContent = getTranslation(locale, "sample.addEmoji");
    const randomLabel = getTranslation(locale, "sample.randomButton");
    this.elements.randomSampleButtonText.textContent = randomLabel;
    this.elements.randomSampleButton.setAttribute("aria-label", randomLabel);
  }

  setControlsEnabled(enabled) {
    this.elements.sampleLengthSelect.disabled = !enabled;
    this.elements.sampleAddEmojiToggle.disabled = !enabled;
    this.elements.randomSampleButton.disabled = !enabled;
  }

  setAddEmojiVisibility(visible) {
    this.elements.sampleAddEmojiField.hidden = !visible;
  }
}
