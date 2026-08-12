import { authStore } from '@app/providers/auth'
import { AppRoute } from '@app/providers/router/routes'
import brandMark from '@shared/assets/brandmark.svg'
import { Button } from '@shared/ui'
import { observer } from 'mobx-react-lite'
import { useNavigate } from 'react-router-dom'

import styles from './Header.module.css'

export const Header = observer(function Header() {
  const navigate = useNavigate()

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
      {authStore.isAuthorized ? (
        <button
          className={styles.avatar}
          onClick={() => navigate(AppRoute.Profile)}
          type="button"
        >
          OL
        </button>
      ) : (
        <div className={styles.actions}>
          <Button onClick={() => navigate('/auth')} variant="ghost">
            Sign in
          </Button>
          <Button onClick={() => navigate('/auth')}>Get started</Button>
        </div>
      )}
    </header>
  )
})
