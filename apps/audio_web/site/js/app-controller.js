import { pcm16ToWavBlob } from "./audio-utils.js";
import { readEncodeRequest, sanitizeModeText, syncModeFields } from "./request-form.js";
import { buildResultSummaryViewModel } from "./result-summary-view-model.js";

export class AppController {
  constructor({ elements, ui, encoderClient, sampleController, sampleView }) {
    this.elements = elements;
    this.ui = ui;
    this.encoderClient = encoderClient;
    this.sampleController = sampleController;
    this.sampleView = sampleView;
    this.currentAudioUrl = null;
    this.isGenerating = false;
  }

  initialize() {
    this.bindEvents();
    this.ui.setLocale("en");
    this.sampleView.setLocale("en");
    void this.applyMode(this.elements.modeSelect.value);
    this.ui.setStatusKey("loading.pending");
    this.sampleView.setControlsEnabled(false);
    this.sampleController.bindEvents();
    void this.warmupSampleController();
    void this.warmupEncoder();
  }

  bindEvents() {
    this.elements.languageSelect.addEventListener("change", (event) => {
      this.ui.setLocale(event.target.value);
      this.sampleView.setLocale(event.target.value);
      this.ui.setInputHint(this.elements.modeSelect.value);
      void this.sampleController.refreshSampleText();
    });

    this.elements.modeSelect.addEventListener("change", () => {
      void this.applyMode(this.elements.modeSelect.value);
    });

    this.elements.modeCards?.addEventListener("click", (event) => {
      const origin = event.target instanceof Node
        ? event.target
        : null;
      const card = origin?.nodeType === Node.ELEMENT_NODE
        ? origin.closest("[data-mode-card]")
        : origin?.parentElement?.closest("[data-mode-card]");
      if (!(card instanceof HTMLElement)) {
        return;
      }
      void this.applyMode(card.dataset.modeCard);
    });

    this.elements.modeCards?.addEventListener("keydown", (event) => {
      if (event.key !== "Enter" && event.key !== " ") {
        return;
      }
      const origin = event.target instanceof Node
        ? event.target
        : null;
      const card = origin?.nodeType === Node.ELEMENT_NODE
        ? origin.closest("[data-mode-card]")
        : origin?.parentElement?.closest("[data-mode-card]");
      if (!(card instanceof HTMLElement)) {
        return;
      }
      event.preventDefault();
      void this.applyMode(card.dataset.modeCard);
    });

    this.elements.inputText.addEventListener("input", () => {
      if (sanitizeModeText(this.elements)) {
        this.ui.setStatusKey("validation.miniAsciiOnly");
      }
    });

    this.elements.generateButton.addEventListener("click", () => {
      void this.generateAudio();
    });
  }

  async warmupEncoder() {
    try {
      await this.encoderClient.ready();
      this.ui.setStatusKey("loading.ready");
    } catch (error) {
      console.error(error);
      this.ui.setStatusKey("loading.missing");
    }
  }

  async warmupSampleController() {
    await this.sampleController.warmup();
    this.syncSampleControlState();
  }

  syncSampleControlState() {
    this.sampleController.setInteractiveEnabled(!this.isGenerating);
  }

  releaseCurrentAudioUrl() {
    if (!this.currentAudioUrl) {
      return;
    }

    URL.revokeObjectURL(this.currentAudioUrl);
    this.currentAudioUrl = null;
  }

  revealPlayerArea() {
    this.elements.player.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  async applyMode(mode) {
    if (!mode) {
      return;
    }

    if (this.elements.modeSelect.value !== mode) {
      this.elements.modeSelect.value = mode;
    }

    syncModeFields(this.elements, this.ui, mode);
    this.sampleController.syncMode(mode);
    await this.sampleController.refreshSampleText();
    if (sanitizeModeText(this.elements)) {
      this.ui.setStatusKey("validation.miniAsciiOnly");
    }
  }

  async generateAudio() {
    const request = readEncodeRequest(this.elements);
    if (!request.text.trim()) {
      this.ui.setStatusKey("validation.emptyText");
      return;
    }

    this.isGenerating = true;
    this.ui.setGenerating(true);
    this.syncSampleControlState();
    this.ui.clearProgress();
    this.ui.clearResultSummary();
    this.ui.setStatusKey("encoding.inProgress");
    this.ui.resetDownloadState();
    this.releaseCurrentAudioUrl();

    try {
      const result = await this.encoderClient.encode(request, {
        onProgress: (snapshot, workPlan) => {
          this.ui.setProgress(snapshot, workPlan);
        },
      });
      const samples = Int16Array.from(result.samples);
      const wavBlob = pcm16ToWavBlob(
        samples,
        result.sampleRateHz ?? request.sampleRateHz,
      );
      this.currentAudioUrl = URL.createObjectURL(wavBlob);
      this.ui.setDownloadUrl(this.currentAudioUrl);
      this.ui.setResultSummary(
        buildResultSummaryViewModel(request, result, samples.length),
      );
      this.ui.setStatusKey("encoding.success", { sampleCount: samples.length });
      this.revealPlayerArea();
    } catch (error) {
      console.error(error);
      this.ui.clearProgress();
      this.ui.clearResultSummary();
      this.ui.setStatusKey("encoding.failure", {
        message: error instanceof Error ? error.message : String(error),
      });
    } finally {
      this.isGenerating = false;
      this.ui.setGenerating(false);
      this.syncSampleControlState();
    }
  }
}
