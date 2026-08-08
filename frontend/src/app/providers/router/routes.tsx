import { Navigate, type RouteObject } from 'react-router-dom'

import { AuthPage } from '@pages/auth'
import { MainPage } from '@pages/main'
import { EditorPage } from '@pages/editor'
import { FeedPage } from '@pages/feed'
import { ProfilePage } from '@pages/profile'

export const AppRoute = {
  Main: '/',
  Profile: '/profile',
  Auth: '/auth',
  Feed: '/feed',
  Editor: '/editor',
} as const

const publicRoutes: RouteObject[] = [
  { path: AppRoute.Auth, element: <AuthPage /> },
  { path: AppRoute.Main, element: <MainPage /> },
  { path: AppRoute.Profile, element: <ProfilePage /> },
  { path: AppRoute.Feed, element: <FeedPage /> },
]

export const authorizedRoutes: RouteObject[] = [
  ...publicRoutes,
  { path: AppRoute.Editor, element: <EditorPage /> },
  { path: '*', element: <Navigate to={AppRoute.Main} replace /> },
]

export const unauthorizedRoutes: RouteObject[] = [
  ...publicRoutes,
  { path: AppRoute.Editor, element: <Navigate to={AppRoute.Auth} replace /> },
  { path: '*', element: <Navigate to={AppRoute.Main} replace /> },
]

export const getAuthorizedRoutes = () => authorizedRoutes
export const getUnauthorizedRoutes = () => unauthorizedRoutes
