import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import { cn, getAvatarInitials } from '@shared/lib'
import { Avatar, Button } from '@shared/ui'
import { LogIn } from 'lucide-react'
import { observer } from 'mobx-react-lite'
import { useTranslation } from 'react-i18next'
import { NavLink, useNavigate } from 'react-router-dom'

import mobileLogo from './assets/logo.svg'
import darkLogo from './assets/logo-dark.svg'
import lightLogo from './assets/logo-light.svg'
import styles from './Header.module.css'

interface HeaderProps {
  fullWidth?: boolean
}

export const Header = observer(function Header({
  fullWidth = false,
}: HeaderProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const isAuthorized = authStore.isAuthorized
  const session = authStore.getSession

  function handleSignInClick() {
    document.documentElement.dataset.navigationTransition = 'auth'
    navigate('/auth', { viewTransition: true })

    requestAnimationFrame(() => {
      const transition = document.activeViewTransition

      if (!transition) {
        delete document.documentElement.dataset.navigationTransition

        return
      }

      void transition.finished.then(
        () => delete document.documentElement.dataset.navigationTransition,
        () => delete document.documentElement.dataset.navigationTransition,
      )
    })
  }

  return (
    <header className={styles.header}>
      <div className={cn(styles.content, fullWidth && styles.contentFullWidth)}>
        <button
          className={styles.brand}
          onClick={() => navigate('/', { viewTransition: true })}
          type="button"
        >
          <span className={styles.fullLogo}>
            <img
              alt=""
              className={cn(styles.logo, styles.logoDark)}
              src={darkLogo}
            />
            <img
              alt=""
              className={cn(styles.logo, styles.logoLight)}
              src={lightLogo}
            />
          </span>
          <img alt="" className={styles.mobileLogo} src={mobileLogo} />
        </button>
        <nav className={styles.navigation}>
          <NavLink
            className={({ isActive }) =>
              cn(styles.navigationLink, isActive && styles.navigationLinkActive)
            }
            to={AppRoute.EditorCreate}
            viewTransition
          >
            {t('Generate')}
            <span className={styles.generateIcon} />
          </NavLink>
          <NavLink
            className={({ isActive }) =>
              cn(styles.navigationLink, isActive && styles.navigationLinkActive)
            }
            to={AppRoute.Feed}
            viewTransition
          >
            {t('Feed')}
          </NavLink>
          {isAuthorized && (
            <NavLink
              className={({ isActive }) =>
                cn(
                  styles.navigationLink,
                  isActive && styles.navigationLinkActive,
                )
              }
              to={AppRoute.Profile}
              viewTransition
            >
              <span className={styles.projectsLabel}>{t('My projects')}</span>
            </NavLink>
          )}
        </nav>
        {isAuthorized && session ? (
          <button
            className={styles.avatar}
            onClick={() => navigate(AppRoute.Profile, { viewTransition: true })}
            type="button"
          >
            <span>@{session.user.nickname}</span>
            <Avatar initials={getAvatarInitials(session.user.nickname)} />
          </button>
        ) : (
          <div className={styles.actions}>
            <Button className={styles.signInButton} onClick={handleSignInClick}>
              <LogIn size={18} />
              <span>{t('Sign in')}</span>
            </Button>
          </div>
        )}
      </div>
    </header>
  )
})
