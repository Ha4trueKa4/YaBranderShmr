import { authStore } from '@app/providers/auth'
import { queryClient } from '@app/providers/query'
import { setUnauthorizedHandler } from '@shared/api/api'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { routes } from './routes'

export const router = createBrowserRouter(routes)

let isHandlingUnauthorized = false

setUnauthorizedHandler(() => {
  if (isHandlingUnauthorized) {
    return
  }

  isHandlingUnauthorized = true
  const { hash, pathname, search } = router.state.location

  authStore.signOut()
  queryClient.clear()
  void router
    .navigate('/auth', {
      replace: true,
      state: { from: `${pathname}${search}${hash}` },
    })
    .finally(() => {
      isHandlingUnauthorized = false
    })
})

export function AppRouter() {
  return <RouterProvider router={router} />
}
