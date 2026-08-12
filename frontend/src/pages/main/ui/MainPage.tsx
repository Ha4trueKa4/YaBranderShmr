import { Navigate } from 'react-router-dom'

export function MainPage() {
  return <Navigate replace to="/auth" />
}
