function normalizeLocale(locale) {
  if (locale === "zh-CN") {
    return "zh-CN";
  }
  if (locale === "zh-rTW") {
    return "zh-rTW";
  }
  return locale;
}

export class SampleTextService {
  constructor(dataUrl = new URL("../data/sample-texts.json", import.meta.url)) {
    this.dataUrl = dataUrl;
    this.dataPromise = null;
  }

  async ready() {
    if (!this.dataPromise) {
      this.dataPromise = fetch(this.dataUrl).then(async (response) => {
        if (!response.ok) {
          throw new Error(`Sample data request failed: ${response.status}`);
        }
        return response.json();
      });
    }
    return this.dataPromise;
  }

  async getRandomSample({ locale, mode, length }) {
    const data = await this.ready();
    const normalizedLength = length === "long" ? "long" : "short";

    if (mode === "mini" || mode === "pro") {
      const entries = data.ascii_shared?.en?.[normalizedLength] ?? [];
      if (entries.length === 0) {
        throw new Error("No ASCII shared samples are available.");
      }
      return entries[Math.floor(Math.random() * entries.length)];
    }

    const normalizedLocale = normalizeLocale(locale);
    const localizedEntries =
      data.sacred_machine?.[normalizedLocale]?.[normalizedLength]
      ?? data.sacred_machine?.en?.[normalizedLength]
      ?? [];

    if (localizedEntries.length === 0) {
      throw new Error("No localized sacred_machine samples are available.");
    }

    return localizedEntries[Math.floor(Math.random() * localizedEntries.length)];
  }
}
