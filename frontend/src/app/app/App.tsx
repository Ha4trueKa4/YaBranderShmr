import { AppRouter, authStore, QueryProvider } from '@app/providers'
import { ToastProvider } from '@shared/ui'
import { observer } from 'mobx-react-lite'

const AppContent = observer(function AppContent() {
  return <AppRouter isAuthorized={authStore.isAuthorized} />
})

export function App() {
  return (
    <QueryProvider>
      <ToastProvider>
        <AppContent />
      </ToastProvider>
    </QueryProvider>
  )
}
