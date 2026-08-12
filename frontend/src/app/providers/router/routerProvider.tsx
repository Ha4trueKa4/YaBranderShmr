import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { getAuthorizedRoutes, getUnauthorizedRoutes } from './routes'

type AppRouterProps = {
  isAuthorized?: boolean
}

export function AppRouter({ isAuthorized = false }: AppRouterProps) {
  const routes = isAuthorized ? getAuthorizedRoutes() : getUnauthorizedRoutes()
  // Если будут какие-то роли потом, то тут можно передавать роль

  return <RouterProvider router={createBrowserRouter(routes)} />
}
