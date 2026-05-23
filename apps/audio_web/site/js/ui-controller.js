import { ENCODE_PROGRESS_PHASES } from "./constants.js";
import { TRANSLATIONS, getTranslation } from "./i18n.js";

function appendAboutParagraphContent(element, paragraph) {
  const normalized = paragraph.replaceAll("`flash`", "flash");
  const segments = normalized.split(/(flash)/g);

  for (const segment of segments) {
    if (!segment) {
      continue;
    }
    if (segment === "flash") {
      const token = document.createElement("strong");
      token.className = "about-mode-token";
      token.textContent = segment;
      element.append(token);
      continue;
    }
    element.append(document.createTextNode(segment));
  }
}

export class UiController {
  constructor(elements) {
    this.elements = elements;
    this.currentLocale = "en";
    this.statusState = { key: "loading.pending" };
    this.currentProgress = null;
    this.currentResultSummary = null;
  }

  setLocale(locale) {
    this.currentLocale = TRANSLATIONS[locale] ? locale : "en";
    const copy = TRANSLATIONS[this.currentLocale];
    document.documentElement.lang = copy.htmlLang;
    this.elements.languageSelect.value = this.currentLocale;
    this.elements.languageLabel.textContent = copy.languageLabel;
    this.elements.aboutTitle.textContent = getTranslation(this.currentLocale, "about.title");
    this.renderAboutCopy();
    this.elements.inputTextLabel.textContent = copy.inputTextLabel;
    this.elements.inputText.placeholder = copy.inputPlaceholder;
    this.elements.modeLabel.textContent = copy.modeLabel;
    this.elements.modeSummaryTitle.textContent = getTranslation(this.currentLocale, "mode.summaryTitle");
    this.elements.flashStyleLabel.textContent = copy.flashStyleLabel;
    this.elements.miniSpeedLabel.textContent = copy.miniSpeedLabel;
    this.elements.generateButton.textContent = copy.generateButton;
    this.elements.downloadLink.textContent = copy.downloadLink;
    this.elements.progressLabel.textContent = copy.progressLabel;
    this.elements.resultSummaryTitle.textContent = getTranslation(this.currentLocale, "result.summaryTitle");
    this.elements.resultModeLabel.textContent = getTranslation(this.currentLocale, "result.mode");
    this.elements.resultProfileLabel.textContent = getTranslation(this.currentLocale, "result.profile");
    this.elements.resultDurationLabel.textContent = getTranslation(this.currentLocale, "result.duration");
    this.elements.resultSampleRateLabel.textContent = getTranslation(this.currentLocale, "result.sampleRate");
    this.renderStatus();
    this.renderProgress();
    this.renderResultSummary();
  }

  renderAboutCopy() {
    const aboutCopy = getTranslation(this.currentLocale, "about.copy");
    const paragraphs = Array.isArray(aboutCopy) ? aboutCopy : [aboutCopy];
    this.elements.aboutCopy.replaceChildren(
      ...paragraphs.map((paragraph) => {
        const element = document.createElement("p");
        appendAboutParagraphContent(element, paragraph);
        return element;
      }),
    );
  }

  getLocale() {
    return this.currentLocale;
  }

  getInputHint(mode) {
    const copy = TRANSLATIONS[this.currentLocale] ?? TRANSLATIONS.en;
    return copy.inputHintByMode?.[mode] ?? "";
  }

  setInputHint(mode) {
    this.elements.inputHint.textContent = this.getInputHint(mode);
    this.elements.modeSummaryCopy.textContent = getTranslation(
      this.currentLocale,
      `mode.summary.${mode}`,
    );
  }

  setStatusKey(key, params = {}) {
    this.statusState = { key, params };
    this.renderStatus();
  }

  renderStatus() {
    this.elements.status.textContent = getTranslation(
      this.currentLocale,
      this.statusState.key,
      this.statusState.params,
    );
  }

  setGenerating(isGenerating) {
    this.elements.generateButton.disabled = isGenerating;
  }

  resetDownloadState() {
    this.elements.downloadLink.removeAttribute("href");
    this.elements.downloadLink.classList.add("is-disabled");
    this.elements.player.removeAttribute("src");
  }

  setDownloadUrl(url) {
    this.elements.player.src = url;
    this.elements.downloadLink.href = url;
    this.elements.downloadLink.classList.remove("is-disabled");
  }

  setProgress(phaseCode, progressValue) {
    this.currentProgress = {
      phase: ENCODE_PROGRESS_PHASES[phaseCode] ?? "preparing",
      ratio: Math.max(0, Math.min(1, progressValue)),
    };
    this.renderProgress();
  }

  clearProgress() {
    this.currentProgress = null;
    this.renderProgress();
  }

  renderProgress() {
    if (!this.currentProgress) {
      this.elements.progressSection.hidden = true;
      this.elements.progressBar.style.width = "0%";
      this.elements.progressValue.textContent = "";
      return;
    }

    const phaseTranslations =
      TRANSLATIONS[this.currentLocale]?.progressPhase ??
      TRANSLATIONS.en.progressPhase;
    const percent = Math.round(this.currentProgress.ratio * 100);
    const phaseLabel =
      phaseTranslations[this.currentProgress.phase] ?? this.currentProgress.phase;

    this.elements.progressSection.hidden = false;
    this.elements.progressBar.style.width = `${percent}%`;
    this.elements.progressValue.textContent = getTranslation(
      this.currentLocale,
      "progress.display",
      { phaseLabel, percent },
    );
  }

  clearResultSummary() {
    this.currentResultSummary = null;
    this.renderResultSummary();
  }

  setResultSummary(summary) {
    this.currentResultSummary = summary;
    this.renderResultSummary();
  }

  renderResultSummary() {
    if (!this.currentResultSummary) {
      this.elements.resultSummary.hidden = true;
      this.elements.resultModeValue.textContent = "";
      this.elements.resultProfileValue.textContent = "";
      this.elements.resultDurationValue.textContent = "";
      this.elements.resultSampleRateValue.textContent = "";
      return;
    }

    this.elements.resultSummary.hidden = false;
    this.elements.resultModeValue.textContent = this.currentResultSummary.modeLabel;
    this.elements.resultProfileValue.textContent = this.currentResultSummary.profileLabel;
    this.elements.resultDurationValue.textContent = this.currentResultSummary.durationLabel;
    this.elements.resultSampleRateValue.textContent = this.currentResultSummary.sampleRateLabel;
  }
}
