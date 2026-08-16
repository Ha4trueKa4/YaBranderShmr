import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import brandMark from '@shared/assets/brandmark.svg'
import { cn, getAvatarInitials } from '@shared/lib'
import { Avatar, Button } from '@shared/ui'
import { observer } from 'mobx-react-lite'
import { NavLink, useNavigate } from 'react-router-dom'

import styles from './Header.module.css'

interface HeaderProps {
  fullWidth?: boolean
}

export const Header = observer(function Header({
  fullWidth = false,
}: HeaderProps) {
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
          <img alt="" src={brandMark} />
          <span>Brandkit AI</span>
        </button>
        <nav className={styles.navigation}>
          <NavLink
            className={({ isActive }) =>
              cn(styles.navigationLink, isActive && styles.navigationLinkActive)
            }
            to={AppRoute.EditorCreate}
            viewTransition
          >
            Generate
            <span className={styles.generateIcon} />
          </NavLink>
          <NavLink
            className={({ isActive }) =>
              cn(styles.navigationLink, isActive && styles.navigationLinkActive)
            }
            to={AppRoute.Feed}
            viewTransition
          >
            Feed
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
              My projects
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
            <Button onClick={handleSignInClick} variant="ghost">
              Sign in
            </Button>
            <Button onClick={() => navigate('/auth', { viewTransition: true })}>
              Get started
            </Button>
          </div>
        )}
      </div>
    </header>
  )
})
