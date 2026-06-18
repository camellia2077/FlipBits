import { audioFileToMonoPcm16AtSampleRate, pcm16ToWavBlob } from "./audio-utils.js";
import { resolveInitialLocale } from "./browser-locale.js";
import { DEFAULT_SAMPLE_RATE_HZ, ENCODE_OPERATION_STATES } from "./constants.js";
import { readEncodeRequest, readVoiceFxRequest, sanitizeModeText, syncModeFields } from "./request-form.js";
import {
  buildResultSummaryViewModel,
  buildVoiceFxResultSummaryViewModel,
} from "./result-summary-view-model.js";
import { elapsedMs, logPerf, nowMs } from "./perf-diagnostics.js";

const DEFAULT_MODE = "flash";
const DATA_PROGRESS_RENDER_INTERVAL_MS = 100;

export class AppController {
  constructor({ elements, ui, encoderClient, sampleController, sampleView }) {
    this.elements = elements;
    this.ui = ui;
    this.encoderClient = encoderClient;
    this.sampleController = sampleController;
    this.sampleView = sampleView;
    this.currentDataAudioUrl = null;
    this.currentVoiceAudioUrl = null;
    this.recordedVoiceBlob = null;
    this.recordedVoiceName = "";
    this.voiceMediaRecorder = null;
    this.voiceRecordingChunks = [];
    this.voiceRecordingStream = null;
    this.audioContext = null;
    this.currentMode = DEFAULT_MODE;
    this.currentWorkflow = "data";
    this.isGenerating = false;
    this.isVoiceRecording = false;
  }

  initialize() {
    this.bindEvents();
    const initialLocale = resolveInitialLocale();
    this.ui.setLocale(initialLocale);
    this.sampleView.setLocale(initialLocale);
    this.ui.setWorkflow(this.currentWorkflow);
    void this.applyMode(this.currentMode);
    this.ui.setStatusKey("loading.pending");
    this.ui.setVoiceStatusKey("loading.pending");
    this.sampleView.setControlsEnabled(false);
    this.sampleController.bindEvents();
    void this.warmupSampleController();
    void this.warmupEncoder();
    this.ui.setVoiceTrackMode(this.getSelectedVoiceTrackMode());
  }

  bindEvents() {
    this.elements.languageSelect.addEventListener("change", (event) => {
      this.ui.setLocale(event.target.value);
      this.ui.setVoiceRecordingState(this.isVoiceRecording);
      this.sampleView.setLocale(event.target.value);
      this.ui.setInputHint(this.currentMode);
      void this.sampleController.refreshSampleText();
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

    this.elements.workflowTabs?.forEach((tab) => {
      tab.addEventListener("click", () => {
        void this.applyWorkflow(tab.dataset.workflowTab);
      });
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
      if (sanitizeModeText(this.elements, this.currentMode)) {
        this.ui.setStatusKey("validation.miniAsciiOnly");
      }
    });

    for (const input of this.elements.flashStyleInputs) {
      input.addEventListener("change", () => {
        this.ui.renderFlashStyleDescription();
      });
    }

    this.elements.generateButton.addEventListener("click", () => {
      void this.generateAudio();
    });

    this.elements.voiceTrackInputs.forEach((input) => {
      input.addEventListener("change", () => {
        this.ui.setVoiceTrackMode(this.getSelectedVoiceTrackMode());
      });
    });

    this.elements.voiceFxFile.addEventListener("change", () => {
      this.clearRecordedVoiceInput();
      this.ui.setVoiceFxFileName(this.elements.voiceFxFile.files?.[0]?.name ?? "");
    });

    this.elements.voiceRecordButton.addEventListener("click", () => {
      void this.toggleVoiceRecording();
    });

    this.elements.voiceFxProcessButton.addEventListener("click", () => {
      void this.processVoiceFxAudio();
    });
  }

  async warmupEncoder() {
    try {
      await this.encoderClient.ready();
      this.ui.setStatusKey("loading.ready");
      this.ui.setVoiceStatusKey("loading.ready");
    } catch (error) {
      console.error(error);
      this.ui.setStatusKey("loading.missing");
      this.ui.setVoiceStatusKey("loading.missing");
    }
  }

  async warmupSampleController() {
    await this.sampleController.warmup();
    this.syncSampleControlState();
  }

  syncSampleControlState() {
    this.sampleController.setInteractiveEnabled(!this.isGenerating);
  }

  getSelectedVoiceTrackMode() {
    return this.elements.voiceTrackInputs.find((input) => input.checked)?.value ?? "single";
  }

  getSelectedVoicePreset() {
    return this.elements.voiceFxPresetInputs.find((input) => input.checked)?.value ?? "machine_voice";
  }

  syncVoicePresetSelection() {
    const trackMode = this.getSelectedVoiceTrackMode();
    const allowedValues = trackMode === "dual"
      ? new Set(["binaric_cant", "voice_trigger", "raw_constant"])
      : new Set(["machine_voice", "signal_cant", "robot_vox"]);
    const selected = this.elements.voiceFxPresetInputs.find((input) => input.checked && allowedValues.has(input.value));
    if (selected) {
      return;
    }
    const fallback = this.elements.voiceFxPresetInputs.find((input) => allowedValues.has(input.value));
    if (fallback) {
      fallback.checked = true;
    }
  }

  getCurrentMode() {
    return this.currentMode;
  }

  releaseCurrentAudioUrl() {
    if (!this.currentDataAudioUrl) {
      return;
    }

    URL.revokeObjectURL(this.currentDataAudioUrl);
    this.currentDataAudioUrl = null;
  }

  releaseCurrentVoiceAudioUrl() {
    if (!this.currentVoiceAudioUrl) {
      return;
    }

    URL.revokeObjectURL(this.currentVoiceAudioUrl);
    this.currentVoiceAudioUrl = null;
  }

  clearRecordedVoiceInput() {
    this.recordedVoiceBlob = null;
    this.recordedVoiceName = "";
    this.ui.setVoiceRecordStatusKey("voiceFx.recordNoInput");
  }

  clearSelectedVoiceFile() {
    this.elements.voiceFxFile.value = "";
    this.ui.setVoiceFxFileName("");
  }

  stopVoiceRecordingStream() {
    for (const track of this.voiceRecordingStream?.getTracks?.() ?? []) {
      track.stop();
    }
    this.voiceRecordingStream = null;
  }

  async toggleVoiceRecording() {
    if (this.isVoiceRecording) {
      this.voiceMediaRecorder?.stop();
      return;
    }
    await this.startVoiceRecording();
  }

  async startVoiceRecording() {
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") {
      this.ui.setVoiceStatusKey("voiceFx.recordUnsupported");
      return;
    }

    try {
      this.clearSelectedVoiceFile();
      this.recordedVoiceBlob = null;
      this.recordedVoiceName = "";
      this.voiceRecordingChunks = [];
      this.voiceRecordingStream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mimeType = this.resolveVoiceRecordingMimeType();
      this.voiceMediaRecorder = mimeType
        ? new MediaRecorder(this.voiceRecordingStream, { mimeType })
        : new MediaRecorder(this.voiceRecordingStream);
      this.voiceMediaRecorder.addEventListener("dataavailable", (event) => {
        if (event.data?.size > 0) {
          this.voiceRecordingChunks.push(event.data);
        }
      });
      this.voiceMediaRecorder.addEventListener("stop", () => {
        this.finishVoiceRecording();
      }, { once: true });
      this.voiceMediaRecorder.start();
      this.isVoiceRecording = true;
      this.ui.setVoiceRecordingState(true);
      this.ui.setVoiceRecordStatusKey("voiceFx.recording");
      this.ui.setVoiceStatusKey("voiceFx.recording");
    } catch (error) {
      console.error(error);
      this.stopVoiceRecordingStream();
      this.isVoiceRecording = false;
      this.ui.setVoiceRecordingState(false);
      this.ui.setVoiceStatusKey("voiceFx.recordFailure", {
        message: error instanceof Error ? error.message : String(error),
      });
    }
  }

  resolveVoiceRecordingMimeType() {
    const candidates = [
      "audio/webm;codecs=opus",
      "audio/webm",
      "audio/mp4",
      "audio/ogg;codecs=opus",
    ];
    return candidates.find((candidate) => MediaRecorder.isTypeSupported?.(candidate)) ?? "";
  }

  finishVoiceRecording() {
    const mimeType = this.voiceMediaRecorder?.mimeType || "audio/webm";
    this.stopVoiceRecordingStream();
    this.voiceMediaRecorder = null;
    this.isVoiceRecording = false;
    this.ui.setVoiceRecordingState(false);

    if (!this.voiceRecordingChunks.length) {
      this.recordedVoiceBlob = null;
      this.recordedVoiceName = "";
      this.ui.setVoiceRecordStatusKey("voiceFx.recordEmpty");
      this.ui.setVoiceStatusKey("voiceFx.recordEmpty");
      return;
    }

    this.recordedVoiceBlob = new Blob(this.voiceRecordingChunks, { type: mimeType });
    this.recordedVoiceName = `recording-${new Date().toISOString().replaceAll(":", "-")}.webm`;
    this.voiceRecordingChunks = [];
    this.ui.setVoiceRecordStatusKey("voiceFx.recordReady", {
      fileName: this.recordedVoiceName,
    });
    this.ui.setVoiceStatusKey("voiceFx.recordReady", {
      fileName: this.recordedVoiceName,
    });
  }

  revealPlayerArea(target = this.elements.player) {
    target.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  resolvePlaybackSampleRateHz() {
    const AudioContextClass = globalThis.AudioContext ?? globalThis.webkitAudioContext;
    if (!AudioContextClass) {
      return DEFAULT_SAMPLE_RATE_HZ;
    }
    this.audioContext ??= new AudioContextClass();
    return this.audioContext.sampleRate || DEFAULT_SAMPLE_RATE_HZ;
  }

  async applyMode(mode) {
    if (!mode) {
      return;
    }

    this.currentMode = mode;
    syncModeFields(this.elements, this.ui, mode);
    this.sampleController.syncMode(mode);
    await this.sampleController.refreshSampleText();
    if (sanitizeModeText(this.elements, mode)) {
      this.ui.setStatusKey("validation.miniAsciiOnly");
    }
  }

  async applyWorkflow(workflow) {
    this.currentWorkflow = workflow === "voice" ? "voice" : "data";
    this.ui.setWorkflow(this.currentWorkflow);
  }

  async generateAudio() {
    const totalStartMs = nowMs();
    const sampleRateHz = this.resolvePlaybackSampleRateHz();
    const requestStartMs = nowMs();
    const request = readEncodeRequest(this.elements, this.currentMode, sampleRateHz);
    request.enableDiagnostics = window.__flipbitsPerfEnableDiagnostics === true;
    const requestBuildMs = elapsedMs(requestStartMs);
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
      let progressEventCount = 0;
      let progressRenderMs = 0;
      let progressRenderCount = 0;
      let lastProgressRenderMs = 0;
      const workerStartMs = nowMs();
      const result = await this.encoderClient.encode(request, {
        onProgress: (snapshot, workPlan) => {
          progressEventCount += 1;
          const currentMs = nowMs();
          const shouldRenderProgress = snapshot.state !== ENCODE_OPERATION_STATES.running ||
            currentMs - lastProgressRenderMs >= DATA_PROGRESS_RENDER_INTERVAL_MS;
          if (!shouldRenderProgress) {
            return;
          }
          const progressStartMs = nowMs();
          this.ui.setProgress(snapshot, workPlan);
          progressRenderMs += elapsedMs(progressStartMs);
          progressRenderCount += 1;
          lastProgressRenderMs = currentMs;
        },
      });
      const workerRoundtripMs = elapsedMs(workerStartMs);
      const wavStartMs = nowMs();
      const samples = Int16Array.from(result.samples);
      const wavBlob = pcm16ToWavBlob(
        samples,
        result.sampleRateHz ?? request.sampleRateHz,
      );
      const wavBlobMs = elapsedMs(wavStartMs);
      this.currentDataAudioUrl = URL.createObjectURL(wavBlob);
      this.ui.setDownloadUrl(this.currentDataAudioUrl);
      this.ui.setResultSummary(
        buildResultSummaryViewModel(request, result, samples.length),
      );
      this.ui.setStatusKey("encoding.success", { sampleCount: samples.length });
      this.revealPlayerArea();
      logPerf("data.generate", {
        mode: request.mode,
        textLength: request.text.length,
        sampleRateHz: request.sampleRateHz,
        sampleCount: samples.length,
        requestBuildMs,
        workerRoundtripMs,
        wavBlobMs,
        progressEventCount,
        progressRenderCount,
        progressRenderMs,
        totalMs: elapsedMs(totalStartMs),
        worker: result.diagnostics ?? null,
      });
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

  async processVoiceFxAudio() {
    if (this.isVoiceRecording) {
      this.ui.setVoiceStatusKey("voiceFx.recording");
      return;
    }

    const file = this.elements.voiceFxFile.files?.[0] ?? null;
    const inputAudio = file ?? this.recordedVoiceBlob;
    if (!inputAudio) {
      this.ui.setVoiceStatusKey("voiceFx.missingInput");
      return;
    }

    const totalStartMs = nowMs();
    this.isGenerating = true;
    this.ui.setGenerating(true);
    this.syncSampleControlState();
    this.ui.clearVoiceResultSummary();
    this.ui.setVoiceStatusKey("voiceFx.decoding");
    this.ui.resetVoiceDownloadState();
    this.releaseCurrentVoiceAudioUrl();

    try {
      const decodeStartMs = nowMs();
      const decoded = await audioFileToMonoPcm16AtSampleRate(inputAudio, 48000);
      const decodeAndResampleMs = elapsedMs(decodeStartMs);
      this.ui.setVoiceStatusKey("voiceFx.processing");
      this.syncVoicePresetSelection();
      const requestStartMs = nowMs();
      const request = readVoiceFxRequest(
        this.elements,
        decoded.samples,
        decoded.sampleRateHz,
      );
      const requestBuildMs = elapsedMs(requestStartMs);
      const workerStartMs = nowMs();
      const result = await this.encoderClient.processVoiceFx(request);
      const workerRoundtripMs = elapsedMs(workerStartMs);
      const wavStartMs = nowMs();
      const samples = Int16Array.from(result.samples);
      const wavBlob = pcm16ToWavBlob(
        samples,
        result.sampleRateHz ?? request.sampleRateHz,
      );
      const wavBlobMs = elapsedMs(wavStartMs);
      this.currentVoiceAudioUrl = URL.createObjectURL(wavBlob);
      this.ui.setVoiceDownloadUrl(this.currentVoiceAudioUrl);
      this.ui.setVoiceResultSummary(
        buildVoiceFxResultSummaryViewModel(request, result, samples.length),
      );
      this.ui.setVoiceStatusKey("voiceFx.success", { sampleCount: samples.length });
      this.revealPlayerArea(this.elements.voicePlayer);
      logPerf("voice.process", {
        source: file ? "file" : "recording",
        inputByteLength: inputAudio.size ?? null,
        trackMode: request.trackMode,
        preset: request.preset,
        subvoiceStyle: request.subvoiceStyle,
        decodedSampleRateHz: decoded.sampleRateHz,
        decodedSampleCount: decoded.samples.length,
        outputSampleCount: samples.length,
        decodeAndResampleMs,
        requestBuildMs,
        workerRoundtripMs,
        wavBlobMs,
        totalMs: elapsedMs(totalStartMs),
        worker: result.diagnostics ?? null,
      });
    } catch (error) {
      console.error(error);
      this.ui.clearVoiceResultSummary();
      this.ui.setVoiceStatusKey("voiceFx.failure", {
        message: error instanceof Error ? error.message : String(error),
      });
    } finally {
      this.isGenerating = false;
      this.ui.setGenerating(false);
      this.syncSampleControlState();
    }
  }
}
