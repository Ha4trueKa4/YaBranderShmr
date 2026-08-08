import { Link } from 'react-router-dom'
import { Button } from '@shared'
import styles from './Header.module.css'

interface HeaderProps {
  brandName?: string
  onGetStarted?: () => void
}

export function Header({
  brandName = 'Brandkit AI',
  onGetStarted,
}: HeaderProps) {
  return (
    <header className={styles.header}>
      <span className={styles.brand}>{brandName}</span>
      <nav className={styles.actions}>
        <Link className={styles.signIn} to="/auth">
          Sign in
        </Link>
        <Button onClick={onGetStarted} size="sm">
          Get Started
        </Button>
      </nav>
    </header>
  )
}
