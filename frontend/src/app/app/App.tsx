import { AppRouter, QueryProvider } from '@app/providers'
import { ToastProvider } from '@shared/ui'

export function App() {
  return (
    <QueryProvider>
      <ToastProvider>
        <AppRouter />
      </ToastProvider>
    </QueryProvider>
  )
}
