import { Navigate, type RouteObject } from 'react-router-dom'

import { AuthPage } from '@pages/auth'
import { MainPage } from '@pages/main'
import { EditorCreatePage } from '@pages/editorCreate'
import { EditorProjectPage } from '@pages/editorProject'
import { FeedPage } from '@pages/feed'
import { ProfilePage } from '@pages/profile'

export const AppRoute = {
  EditorCreate: '/',
  EditorProject: '/projects/:projectId',
  Main: '/main',
  Profile: '/profile',
  Auth: '/auth',
  Feed: '/feed',
} as const

const publicRoutes: RouteObject[] = [
  { path: AppRoute.EditorCreate, element: <EditorCreatePage /> },
  { path: AppRoute.EditorProject, element: <EditorProjectPage /> },
  { path: AppRoute.Auth, element: <AuthPage /> },
  { path: AppRoute.Main, element: <MainPage /> },
  { path: AppRoute.Profile, element: <ProfilePage /> },
  { path: AppRoute.Feed, element: <FeedPage /> },
]

export const authorizedRoutes: RouteObject[] = [
  ...publicRoutes,
  { path: '*', element: <Navigate to={AppRoute.EditorCreate} replace /> },
]

export const unauthorizedRoutes: RouteObject[] = [
  ...publicRoutes,
  { path: '*', element: <Navigate to={AppRoute.EditorCreate} replace /> },
]

export const getAuthorizedRoutes = () => authorizedRoutes
export const getUnauthorizedRoutes = () => unauthorizedRoutes
