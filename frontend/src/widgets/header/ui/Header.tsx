import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import brandMark from '@shared/assets/brandmark.svg'
import { cn } from '@shared/lib'
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

  return (
    <header className={styles.header}>
      <button
        className={styles.brand}
        onClick={() => navigate('/')}
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
        >
          Generate
          <span className={styles.generateIcon} />
        </NavLink>
        <NavLink
          className={({ isActive }) =>
            cn(styles.navigationLink, isActive && styles.navigationLinkActive)
          }
          to={AppRoute.Feed}
        >
          Feed
        </NavLink>
        {isAuthorized && (
          <NavLink
            className={({ isActive }) =>
              cn(styles.navigationLink, isActive && styles.navigationLinkActive)
            }
            to={AppRoute.Profile}
          >
            My projects
          </NavLink>
        )}
      </nav>
      {isAuthorized ? (
        <button
          className={styles.brand}
          onClick={() => navigate('/')}
          type="button"
        >
          <Avatar initials="OL" />
        </button>
        <nav className={styles.navigation}>
          <NavLink
            className={({ isActive }) =>
              cn(styles.navigationLink, isActive && styles.navigationLinkActive)
            }
            to={AppRoute.EditorCreate}
          >
            Generate
            <span className={styles.generateIcon} />
          </NavLink>
          <NavLink
            className={({ isActive }) =>
              cn(styles.navigationLink, isActive && styles.navigationLinkActive)
            }
            to={AppRoute.Feed}
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
            >
              My projects
            </NavLink>
          )}
        </nav>
        {isAuthorized ? (
          <button
            className={styles.avatar}
            onClick={() => navigate(AppRoute.Profile)}
            type="button"
          >
            <Avatar initials="OL" />
          </button>
        ) : (
          <div className={styles.actions}>
            <Button onClick={() => navigate('/auth')} variant="ghost">
              Sign in
            </Button>
            <Button onClick={() => navigate('/auth')}>Get started</Button>
          </div>
        )}
      </div>
    </header>
  )
})
