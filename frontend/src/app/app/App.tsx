import { observer } from 'mobx-react-lite'
import { AppRouter, authStore } from '@/app/providers'

const AppContent = observer(function AppContent() {
  return <AppRouter isAuthorized={authStore.isAuthorized} />
})

export function App() {
  return <AppContent />
}
