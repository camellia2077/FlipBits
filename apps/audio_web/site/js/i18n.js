import { MESSAGES as de } from "./i18n/de.js";
import { MESSAGES as en } from "./i18n/en.js";
import { MESSAGES as es } from "./i18n/es.js";
import { MESSAGES as fr } from "./i18n/fr.js";
import { MESSAGES as it } from "./i18n/it.js";
import { MESSAGES as ja } from "./i18n/ja.js";
import { MESSAGES as ko } from "./i18n/ko.js";
import { MESSAGES as la } from "./i18n/la.js";
import { MESSAGES as pl } from "./i18n/pl.js";
import { MESSAGES as ptBR } from "./i18n/pt-rBR.js";
import { MESSAGES as ru } from "./i18n/ru.js";
import { MESSAGES as uk } from "./i18n/uk.js";
import { MESSAGES as zhCN } from "./i18n/zh-CN.js";
import { MESSAGES as zhTW } from "./i18n/zh-rTW.js";

export const TRANSLATIONS = {
  "zh-CN": zhCN,
  en,
  de,
  es,
  fr,
  it,
  ja,
  ko,
  la,
  pl,
  "pt-rBR": ptBR,
  ru,
  uk,
  "zh-rTW": zhTW,
};

export function getTranslation(locale, key, params = {}) {
  const localeTable = TRANSLATIONS[locale] ?? TRANSLATIONS.en;
  const value = localeTable[key]
    ?? TRANSLATIONS.en?.[key]
    ?? TRANSLATIONS["zh-CN"]?.[key];
  if (typeof value === "function") {
    return value(params);
  }
  return value ?? key;
}
