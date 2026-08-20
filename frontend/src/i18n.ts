import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

import auth from './locales/ru/auth.json'
import common from './locales/ru/common.json'
import editor from './locales/ru/editor.json'
import errors from './locales/ru/errors.json'
import projects from './locales/ru/projects.json'

export const supportedLanguages = ['en', 'ru'] as const
export type SupportedLanguage = (typeof supportedLanguages)[number]

const languageStorageKey = 'brandkit-language'
const defaultLanguage: SupportedLanguage = 'en'

function getInitialLanguage(): SupportedLanguage {
  try {
    const storedLanguage = localStorage.getItem(languageStorageKey)

    if (storedLanguage === 'en' || storedLanguage === 'ru') {
      return storedLanguage
    }
  } catch {
    // The app still works when browser storage is unavailable.
  }

  return defaultLanguage
}

void i18n.use(initReactI18next).init({
  resources: {
    ru: {
      translation: {
        ...common,
        ...auth,
        ...projects,
        ...editor,
        ...errors,
      },
    },
  },
  lng: getInitialLanguage(),
  fallbackLng: defaultLanguage,
  interpolation: {
    escapeValue: false,
  },
  returnNull: false,
})

export function changeLanguage(language: SupportedLanguage) {
  try {
    localStorage.setItem(languageStorageKey, language)
  } catch {
    // The language can still be changed for the current session.
  }

  return i18n.changeLanguage(language)
}

export default i18n
