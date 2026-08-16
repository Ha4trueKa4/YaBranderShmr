import { AuthPage } from '@pages/auth'
import { EditorCreatePage } from '@pages/editorCreate'
import { EditorProjectPage } from '@pages/editorProject'
import { FeedPage } from '@pages/feed'
import { ProfilePage } from '@pages/profile'
import { PublicProfilePage } from '@pages/publicProfile'
// import { MainPage } from '@pages/main'
import { Navigate, type RouteObject, useLocation } from 'react-router-dom'

export const AppRoute = {
  EditorCreate: '/',
  EditorProject: '/projects/:projectId',
  Main: '/main',
  Profile: '/profile',
  PublicProfile: '/users/:userId',
  Auth: '/auth',
  Feed: '/feed',
} as const

const publicRoutes: RouteObject[] = [
  { path: AppRoute.EditorCreate, element: <EditorCreatePage /> },
  { path: AppRoute.Feed, element: <FeedPage /> },
  { path: AppRoute.PublicProfile, element: <PublicProfilePage /> },
  // { path: AppRoute.Main, element: <MainPage /> },
  // { path: AppRoute.Profile, element: <ProfilePage /> },
]

function ProjectAuthRedirect() {
  const location = useLocation()
  const from = `${location.pathname}${location.search}${location.hash}`

  return <Navigate replace state={{ from }} to={AppRoute.Auth} />
}

export const authorizedRoutes: RouteObject[] = [
  ...publicRoutes,
  { path: AppRoute.EditorProject, element: <EditorProjectPage /> },
  { path: AppRoute.Profile, element: <ProfilePage /> },
  { path: '*', element: <Navigate to={AppRoute.EditorCreate} replace /> },
]

export const unauthorizedRoutes: RouteObject[] = [
  ...publicRoutes,
  { path: AppRoute.Auth, element: <AuthPage /> },
  { path: AppRoute.EditorProject, element: <ProjectAuthRedirect /> },
  { path: '*', element: <Navigate to={AppRoute.EditorCreate} replace /> },
]

export const getAuthorizedRoutes = () => authorizedRoutes
export const getUnauthorizedRoutes = () => unauthorizedRoutes
