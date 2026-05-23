import { sanitizeModeText } from "./request-form.js";

const EMOJI_OPTIONS = ["⚙️", "🕯️", "🧪", "📜", "🧫", "💾", "📟", "🖥️", "0️⃣", "1️⃣"];
const EMOJI_MODES = new Set(["flash", "ultra"]);

function shuffleEmojiOptions() {
  const shuffled = [...EMOJI_OPTIONS];
  for (let index = shuffled.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(Math.random() * (index + 1));
    [shuffled[index], shuffled[swapIndex]] = [shuffled[swapIndex], shuffled[index]];
  }
  return shuffled;
}

export class SampleController {
  constructor({ elements, ui, sampleTextService, sampleView }) {
    this.elements = elements;
    this.ui = ui;
    this.sampleTextService = sampleTextService;
    this.sampleView = sampleView;
    this.isReady = false;
  }

  bindEvents() {
    this.elements.randomSampleButton.addEventListener("click", () => {
      void this.randomize();
    });

    this.elements.sampleLengthSelect.addEventListener("change", () => {
      void this.refreshSampleText();
    });

    this.elements.sampleAddEmojiToggle.addEventListener("change", () => {
      void this.refreshSampleText();
    });
  }

  async warmup() {
    try {
      await this.sampleTextService.ready();
      this.isReady = true;
    } catch (error) {
      console.error(error);
      this.isReady = false;
    }
    return this.isReady;
  }

  setInteractiveEnabled(enabled) {
    this.sampleView.setControlsEnabled(this.isReady && enabled);
  }

  syncMode(mode) {
    this.sampleView.setAddEmojiVisibility(this.supportsEmojiMode(mode));
  }

  supportsEmojiMode(mode) {
    return EMOJI_MODES.has(mode);
  }

  formatSampleText(sampleText, mode) {
    if (!this.supportsEmojiMode(mode) || !this.elements.sampleAddEmojiToggle.checked) {
      return sampleText;
    }

    const [emoji] = shuffleEmojiOptions();
    return `${emoji} ${sampleText}`;
  }

  async randomize() {
    try {
      const mode = this.elements.modeSelect.value;
      const sample = await this.sampleTextService.getRandomSample({
        locale: this.ui.getLocale(),
        mode,
        length: this.elements.sampleLengthSelect.value,
      });
      this.elements.inputText.value = this.formatSampleText(sample.text, mode);
      if (sanitizeModeText(this.elements)) {
        this.ui.setStatusKey("validation.miniAsciiOnly");
        return;
      }
      this.ui.setStatusKey("sample.randomized");
    } catch (error) {
      console.error(error);
      this.ui.setStatusKey("sample.unavailable", {
        message: error instanceof Error ? error.message : String(error),
      });
    }
  }

  async refreshSampleText() {
    if (!this.isReady) {
      return;
    }
    await this.randomize();
  }
}
