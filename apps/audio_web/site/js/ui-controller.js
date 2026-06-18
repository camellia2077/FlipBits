import { ENCODE_PROGRESS_PHASES } from "./constants.js";
import { TRANSLATIONS, getTranslation } from "./i18n.js";

function appendAboutParagraphContent(element, paragraph) {
  const normalized = paragraph.replaceAll("`Flash`", "Flash");
  const segments = normalized.split(/(Flash)/g);

  for (const segment of segments) {
    if (!segment) {
      continue;
    }
    if (segment === "Flash") {
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
    this.currentWorkflow = "data";
    this.statusState = { key: "loading.pending" };
    this.voiceStatusState = { key: "loading.pending" };
    this.voiceRecordStatusState = { key: "voiceFx.recordNoInput" };
    this.isVoiceRecording = false;
    this.currentProgress = null;
    this.currentResultSummary = null;
    this.currentVoiceResultSummary = null;
  }

  setLocale(locale) {
    this.currentLocale = TRANSLATIONS[locale] ? locale : "en";
    const copy = TRANSLATIONS[this.currentLocale];
    document.documentElement.lang = copy.htmlLang;
    this.elements.languageSelect.value = this.currentLocale;
    this.elements.languageLabel.textContent = copy.languageLabel;
    this.elements.heroCopy.textContent = getTranslation(this.currentLocale, "hero.copy");
    this.elements.aboutTitle.textContent = getTranslation(this.currentLocale, "about.title");
    this.renderAboutCopy();
    this.elements.workflowTabData.textContent = getTranslation(this.currentLocale, "workflow.data");
    this.elements.workflowTabVoice.textContent = getTranslation(this.currentLocale, "workflow.voice");
    this.elements.modeSummaryTitle.textContent = getTranslation(this.currentLocale, "data.title");
    this.elements.dataWorkflowCopy.textContent = getTranslation(this.currentLocale, "data.workflow.copy");
    this.elements.inputTextLabel.textContent = copy.inputTextLabel;
    this.elements.inputText.placeholder = copy.inputPlaceholder;
    this.elements.flashStyleLabel.textContent = copy.flashStyleLabel;
    this.elements.flashStyleDescriptionLabel.textContent = getTranslation(
      this.currentLocale,
      "flashStyleDescriptionLabel",
    );
    this.elements.miniSpeedLabel.textContent = copy.miniSpeedLabel;
    this.elements.generateButton.textContent = copy.generateButton;
    this.elements.dataPreviewTitle.textContent = getTranslation(this.currentLocale, "data.previewTitle");
    this.elements.voiceFxTitle.textContent = getTranslation(this.currentLocale, "voiceFx.title");
    this.elements.voiceFxCopy.textContent = getTranslation(this.currentLocale, "voiceFx.copy");
    this.elements.voiceFxFileLabel.textContent = getTranslation(this.currentLocale, "voiceFx.fileLabel");
    this.elements.voiceFxFileHint.textContent = getTranslation(this.currentLocale, "voiceFx.fileHint");
    this.elements.voiceRecordLabel.textContent = getTranslation(this.currentLocale, "voiceFx.recordLabel");
    this.elements.voiceRecordHint.textContent = getTranslation(this.currentLocale, "voiceFx.recordHint");
    this.elements.voiceTrackLabel.textContent = getTranslation(this.currentLocale, "voiceFx.trackLabel");
    this.elements.voiceTrackSingleLabel.textContent = getTranslation(this.currentLocale, "voiceFx.track.single");
    this.elements.voiceTrackDualLabel.textContent = getTranslation(this.currentLocale, "voiceFx.track.dual");
    this.elements.voiceFxPresetLabel.textContent = getTranslation(this.currentLocale, "voiceFx.presetLabel");
    this.elements.voiceFxStyleLabel.textContent = getTranslation(this.currentLocale, "voiceFx.styleLabel");
    this.elements.voiceFxProcessButton.textContent = getTranslation(this.currentLocale, "voiceFx.processButton");
    this.setVoiceFxFileName(this.elements.voiceFxFile.files?.[0]?.name ?? "");
    this.elements.downloadLink.textContent = copy.downloadLink;
    this.elements.voiceDownloadLink.textContent = copy.downloadLink;
    this.elements.progressLabel.textContent = copy.progressLabel;
    this.elements.resultSummaryTitle.textContent = getTranslation(this.currentLocale, "result.summaryTitle");
    this.elements.resultModeLabel.textContent = getTranslation(this.currentLocale, "result.mode");
    this.elements.resultProfileLabel.textContent = getTranslation(this.currentLocale, "result.profile");
    this.elements.resultDurationLabel.textContent = getTranslation(this.currentLocale, "result.duration");
    this.elements.resultSampleRateLabel.textContent = getTranslation(this.currentLocale, "result.sampleRate");
    this.elements.voicePreviewTitle.textContent = getTranslation(this.currentLocale, "voiceFx.previewTitle");
    this.elements.voiceResultSummaryTitle.textContent = getTranslation(this.currentLocale, "result.summaryTitle");
    this.elements.voiceResultModeLabel.textContent = getTranslation(this.currentLocale, "result.mode");
    this.elements.voiceResultProfileLabel.textContent = getTranslation(this.currentLocale, "result.profile");
    this.elements.voiceResultDurationLabel.textContent = getTranslation(this.currentLocale, "result.duration");
    this.elements.voiceResultSampleRateLabel.textContent = getTranslation(this.currentLocale, "result.sampleRate");
    this.renderModeCards();
    this.renderStyleOptionLabels();
    this.renderFlashStyleDescription();
    this.renderWorkflowTabs();
    this.renderStatus();
    this.renderVoiceStatus();
    this.renderVoiceRecordStatus();
    this.setVoiceRecordingState(this.isVoiceRecording);
    this.renderProgress();
    this.renderResultSummary();
    this.renderVoiceResultSummary();
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

  setWorkflow(workflow) {
    this.currentWorkflow = workflow === "voice" ? "voice" : "data";
    this.renderWorkflowTabs();
  }

  renderWorkflowTabs() {
    for (const tab of this.elements.workflowTabs) {
      const active = tab.dataset.workflowTab === this.currentWorkflow;
      tab.classList.toggle("is-active", active);
      tab.setAttribute("aria-selected", active ? "true" : "false");
    }
    for (const panel of this.elements.workflowPanels) {
      const active = panel.dataset.workflowPanel === this.currentWorkflow;
      panel.classList.toggle("is-active", active);
      panel.hidden = !active;
    }
  }

  setVoiceTrackMode(trackMode) {
    const normalizedTrackMode = trackMode === "dual" ? "dual" : "single";
    for (const tile of this.elements.voiceFxPresetTiles) {
      const visible = tile.dataset.voiceTrack === normalizedTrackMode;
      tile.hidden = !visible;
      tile.style.display = visible ? "" : "none";
      const input = tile.querySelector("input");
      if (input) {
        input.disabled = !visible;
      }
    }

    const checkedPreset = this.elements.voiceFxPresetInputs.find((input) => input.checked);
    if (!checkedPreset || checkedPreset.disabled) {
      const fallbackPreset = this.elements.voiceFxPresetInputs.find((input) => !input.disabled);
      if (fallbackPreset) {
        fallbackPreset.checked = true;
      }
    }

    const showStyle = normalizedTrackMode === "dual";
    this.elements.voiceFxStyleField.hidden = !showStyle;
    this.elements.voiceFxStyleField.style.display = showStyle ? "" : "none";
    for (const input of this.elements.voiceFxStyleInputs) {
      input.disabled = !showStyle;
    }
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
    this.renderModeCards(mode);
  }

  renderModeCards(activeMode = "flash") {
    const mode = activeMode;
    this.elements.modeCards?.setAttribute("data-active-mode", mode);
    this.elements.modeCardCopyMini.textContent = getTranslation(
      this.currentLocale,
      "mode.summary.mini",
    );
    this.elements.modeCardCopyFlash.textContent = getTranslation(
      this.currentLocale,
      "mode.summary.flash",
    );
    this.elements.modeCardCopyPro.textContent = getTranslation(
      this.currentLocale,
      "mode.summary.pro",
    );
    this.elements.modeCardCopyUltra.textContent = getTranslation(
      this.currentLocale,
      "mode.summary.ultra",
    );

    this.elements.modeCardMini.classList.toggle("is-active", mode === "mini");
    this.elements.modeCardFlash.classList.toggle("is-active", mode === "flash");
    this.elements.modeCardPro.classList.toggle("is-active", mode === "pro");
    this.elements.modeCardUltra.classList.toggle("is-active", mode === "ultra");
  }

  renderFlashStyleDescription() {
    const selectedStyle =
      this.elements.flashStyleInputs.find((input) => input.checked)?.value ?? "standard";
    this.elements.flashStyleDescription.textContent = getTranslation(
      this.currentLocale,
      `flashStyle.description.${selectedStyle}`,
    );
  }

  renderStyleOptionLabels() {
    for (const input of this.elements.flashStyleInputs) {
      const label = input.nextElementSibling;
      if (label) {
        label.textContent = getTranslation(
          this.currentLocale,
          `flashStyle.option.${input.value}`,
        );
      }
    }

    for (const input of this.elements.voiceFxStyleInputs) {
      const label = input.nextElementSibling;
      if (label) {
        label.textContent = getTranslation(
          this.currentLocale,
          `flashStyle.option.${input.value}`,
        );
      }
    }
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

  setVoiceStatusKey(key, params = {}) {
    this.voiceStatusState = { key, params };
    this.renderVoiceStatus();
  }

  renderVoiceStatus() {
    this.elements.voiceStatus.textContent = getTranslation(
      this.currentLocale,
      this.voiceStatusState.key,
      this.voiceStatusState.params,
    );
  }

  setGenerating(isGenerating) {
    this.elements.generateButton.disabled = isGenerating;
    this.elements.voiceFxProcessButton.disabled = isGenerating;
    this.elements.voiceFxFile.disabled = isGenerating;
    this.elements.voiceRecordButton.disabled = isGenerating;
    for (const input of this.elements.voiceTrackInputs) {
      input.disabled = isGenerating;
    }
  }

  setVoiceRecordingState(isRecording) {
    this.isVoiceRecording = isRecording;
    this.elements.voiceRecordButton.textContent = getTranslation(
      this.currentLocale,
      isRecording ? "voiceFx.recordStopButton" : "voiceFx.recordStartButton",
    );
    this.elements.voiceRecordButton.classList.toggle("is-recording", isRecording);
  }

  setVoiceFxFileName(fileName) {
    this.elements.voiceFxFileName.textContent = fileName || getTranslation(
      this.currentLocale,
      "voiceFx.noFile",
    );
  }

  setVoiceRecordStatusKey(key, params = {}) {
    this.voiceRecordStatusState = { key, params };
    this.renderVoiceRecordStatus();
  }

  renderVoiceRecordStatus() {
    this.elements.voiceRecordStatus.textContent = getTranslation(
      this.currentLocale,
      this.voiceRecordStatusState.key,
      this.voiceRecordStatusState.params,
    );
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

  resetVoiceDownloadState() {
    this.elements.voiceDownloadLink.removeAttribute("href");
    this.elements.voiceDownloadLink.classList.add("is-disabled");
    this.elements.voicePlayer.removeAttribute("src");
  }

  setVoiceDownloadUrl(url) {
    this.elements.voicePlayer.src = url;
    this.elements.voiceDownloadLink.href = url;
    this.elements.voiceDownloadLink.classList.remove("is-disabled");
  }

  setProgress(snapshot, workPlan) {
    const overallRatio = Number.isFinite(snapshot?.overallProgress)
      ? Math.max(0, Math.min(1, snapshot.overallProgress))
      : 0;
    this.currentProgress = {
      phase: typeof snapshot?.phase === "string"
        ? snapshot.phase
        : ENCODE_PROGRESS_PHASES[snapshot?.phase] ?? "preparing",
      ratio: overallRatio,
      phaseRatio: Number.isFinite(snapshot?.phaseProgress)
        ? Math.max(0, Math.min(1, snapshot.phaseProgress))
        : 0,
      completedWorkUnits: Math.max(0, snapshot?.completedWorkUnits ?? 0),
      totalWorkUnits: Math.max(0, snapshot?.totalWorkUnits ?? 0),
      phaseCompletedWorkUnits: Math.max(
        0,
        snapshot?.phaseCompletedWorkUnits ?? 0,
      ),
      phaseTotalWorkUnits: Math.max(0, snapshot?.phaseTotalWorkUnits ?? 0),
      state: snapshot?.state ?? 0,
      workPlan: workPlan ?? null,
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

  clearVoiceResultSummary() {
    this.currentVoiceResultSummary = null;
    this.renderVoiceResultSummary();
  }

  setVoiceResultSummary(summary) {
    this.currentVoiceResultSummary = summary;
    this.renderVoiceResultSummary();
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

  renderVoiceResultSummary() {
    if (!this.currentVoiceResultSummary) {
      this.elements.voiceResultSummary.hidden = true;
      this.elements.voiceResultModeValue.textContent = "";
      this.elements.voiceResultProfileValue.textContent = "";
      this.elements.voiceResultDurationValue.textContent = "";
      this.elements.voiceResultSampleRateValue.textContent = "";
      return;
    }

    this.elements.voiceResultSummary.hidden = false;
    this.elements.voiceResultModeValue.textContent = this.currentVoiceResultSummary.modeLabel;
    this.elements.voiceResultProfileValue.textContent = this.currentVoiceResultSummary.profileLabel;
    this.elements.voiceResultDurationValue.textContent = this.currentVoiceResultSummary.durationLabel;
    this.elements.voiceResultSampleRateValue.textContent = this.currentVoiceResultSummary.sampleRateLabel;
  }
}
