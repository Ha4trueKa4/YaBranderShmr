import { AuthPage } from '@pages/auth'
import { EditorCreatePage } from '@pages/editorCreate'
import { EditorProjectPage } from '@pages/editorProject'
// import { FeedPage } from '@pages/feed'
// import { MainPage } from '@pages/main'
// import { ProfilePage } from '@pages/profile'
import { Navigate, type RouteObject } from 'react-router-dom'

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
  // { path: AppRoute.Feed, element: <FeedPage /> },
  // { path: AppRoute.Main, element: <MainPage /> },
  // { path: AppRoute.Profile, element: <ProfilePage /> },
]

export const authorizedRoutes: RouteObject[] = [
  ...publicRoutes,
  { path: AppRoute.EditorProject, element: <EditorProjectPage /> },
  { path: '*', element: <Navigate to={AppRoute.EditorCreate} replace /> },
]

export const unauthorizedRoutes: RouteObject[] = [
  ...publicRoutes,
  { path: AppRoute.Auth, element: <AuthPage /> },
  { path: '*', element: <Navigate to={AppRoute.EditorCreate} replace /> },
]

export const getAuthorizedRoutes = () => authorizedRoutes
export const getUnauthorizedRoutes = () => unauthorizedRoutes
