import { TRANSLATIONS } from "./i18n.js";

const DEFAULT_LOCALE = "en";
const LOCALE_ALIASES = new Map([
  ["pt-BR", "pt-rBR"],
  ["zh", "zh-CN"],
  ["zh-Hans", "zh-CN"],
  ["zh-Hans-CN", "zh-CN"],
  ["zh-CN", "zh-CN"],
  ["zh-Hant", "zh-rTW"],
  ["zh-Hant-TW", "zh-rTW"],
  ["zh-TW", "zh-rTW"],
  ["zh-HK", "zh-rTW"],
]);

function normalizeBrowserLocale(locale) {
  if (!locale || typeof locale !== "string") {
    return null;
  }
  const normalized = locale.replace("_", "-");
  return LOCALE_ALIASES.get(normalized) ?? normalized.split("-")[0];
}

export function resolveInitialLocale(navigatorLike = globalThis.navigator) {
  const candidates = [
    ...(Array.isArray(navigatorLike?.languages) ? navigatorLike.languages : []),
    navigatorLike?.language,
  ];

  for (const candidate of candidates) {
    const locale = normalizeBrowserLocale(candidate);
    if (locale && TRANSLATIONS[locale]) {
      return locale;
    }
  }

  return DEFAULT_LOCALE;
}
