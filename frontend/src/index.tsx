import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './i18n'
import './index.css'
import { App } from '@app/app/App.tsx'
import { changeTheme, getInitialTheme } from '@shared/lib/theme'

const rootElement = document.getElementById('root')

if (!rootElement) {
  throw new Error('Root element was not found')
}

changeTheme(getInitialTheme())

createRoot(rootElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
