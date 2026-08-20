import { authStore } from '@app/providers/auth'
import { AuthPage, YandexOAuthCallbackPage } from '@pages/auth'
import { EditorCreatePage } from '@pages/editorCreate'
import { EditorProjectPage } from '@pages/editorProject'
import { FeedPage } from '@pages/feed'
import { ProfilePage } from '@pages/profile'
import { PublicProfilePage } from '@pages/publicProfile'
import { UserSubscribersPage } from '@pages/userSubscribers'
import { UserSubscriptionsPage } from '@pages/userSubscriptions'
import { observer } from 'mobx-react-lite'
import {
  Navigate,
  Outlet,
  type RouteObject,
  useLocation,
} from 'react-router-dom'

export const AppRoute = {
  EditorCreate: '/',
  EditorProject: '/projects/:projectId',
  Main: '/main',
  Profile: '/profile',
  PublicProfile: '/users/:userId',
  UserSubscriptions: '/users/:userId/subscriptions',
  UserSubscribers: '/users/:userId/subscribers',
  Auth: '/auth',
  YandexOAuthCallback: '/auth/yandex/callback',
  Feed: '/feed',
} as const

const publicRoutes: RouteObject[] = [
  { path: AppRoute.EditorCreate, element: <EditorCreatePage /> },
  { path: AppRoute.Feed, element: <FeedPage /> },
  { path: AppRoute.PublicProfile, element: <PublicProfilePage /> },
]

const RequireAuth = observer(function RequireAuth() {
  const location = useLocation()

  if (!authStore.isAuthorized) {
    const from = `${location.pathname}${location.search}${location.hash}`

    return <Navigate replace state={{ from }} to={AppRoute.Auth} />
  }

  return <Outlet />
})

export const routes: RouteObject[] = [
  ...publicRoutes,
  { path: AppRoute.Auth, element: <AuthPage /> },
  { path: AppRoute.YandexOAuthCallback, element: <YandexOAuthCallbackPage /> },
  {
    element: <RequireAuth />,
    children: [
      { path: AppRoute.EditorProject, element: <EditorProjectPage /> },
      { path: AppRoute.Profile, element: <ProfilePage /> },
      {
        path: AppRoute.UserSubscriptions,
        element: <UserSubscriptionsPage />,
      },
      { path: AppRoute.UserSubscribers, element: <UserSubscribersPage /> },
    ],
  },
  { path: '*', element: <Navigate to={AppRoute.EditorCreate} replace /> },
]
