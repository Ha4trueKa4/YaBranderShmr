export const themeOptions = [
  'yandex',
  'system',
  'dark',
  'light',
  'sephia',
] as const
export type Theme = (typeof themeOptions)[number]

const themeStorageKey = 'brandkit-theme'

export function getInitialTheme(): Theme {
  try {
    const storedTheme = localStorage.getItem(themeStorageKey)

    if (
      storedTheme === 'system' ||
      storedTheme === 'dark' ||
      storedTheme === 'light' ||
      storedTheme === 'sephia' ||
      storedTheme === 'yandex'
    ) {
      return storedTheme
    }
  } catch {
    // The app still works when browser storage is unavailable.
  }

  return 'yandex'
}

export function changeTheme(theme: Theme) {
  document.documentElement.dataset.theme = theme === 'system' ? '' : theme

  try {
    localStorage.setItem(themeStorageKey, theme)
  } catch {
    // The theme can still be changed for the current session.
  }
}
