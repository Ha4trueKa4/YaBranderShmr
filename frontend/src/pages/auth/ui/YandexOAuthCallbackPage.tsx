import { authStore } from '@app/providers/auth'
import {
  getYandexAuthErrorMessage,
  getYandexOAuthReturnPath,
  useYandexLoginMutation,
  validateYandexOAuthState,
} from '@features/auth'
import { useToast } from '@shared/ui'
import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'

export function YandexOAuthCallbackPage() {
  const hasStarted = useRef(false)
  const location = useLocation()
  const navigate = useNavigate()
  const toast = useToast()
  const { t } = useTranslation()
  const { mutateAsync: loginWithYandex } = useYandexLoginMutation()

  useEffect(() => {
    if (hasStarted.current) {
      return
    }

    hasStarted.current = true

    const searchParams = new URLSearchParams(location.search)
    const code = searchParams.get('code')
    const state = searchParams.get('state')

    if (!code || !validateYandexOAuthState(state)) {
      toast.error(t('Unable to complete Yandex ID sign-in. Please try again.'))
      navigate('/auth', { replace: true, viewTransition: true })

      return
    }

    const returnPath = getYandexOAuthReturnPath()

    void loginWithYandex({ code })
      .then(({ access_token, user }) => {
        authStore.signIn({ accessToken: access_token, user })
        toast.info(t('You are signed in.'))
        navigate(returnPath, { replace: true, viewTransition: true })
      })
      .catch((error: unknown) => {
        toast.error(getYandexAuthErrorMessage(error))
        navigate('/auth', { replace: true, viewTransition: true })
      })
  }, [location.search, loginWithYandex, navigate, t, toast])

  return null
}
