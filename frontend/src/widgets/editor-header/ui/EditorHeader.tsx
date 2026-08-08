import { Button } from '@shared'
import styles from './EditorHeader.module.css'

interface EditorHeaderProps {
  brandName?: string
  onGetStarted?: () => void
  onSignIn?: () => void
}

export function EditorHeader({
  brandName = 'Brandkit AI',
  onGetStarted,
  onSignIn,
}: EditorHeaderProps) {
  return (
    <header className={styles.header}>
      <span className={styles.brand}>{brandName}</span>
      <nav className={styles.actions}>
        <button className={styles.signIn} onClick={onSignIn} type="button">
          Sign in
        </button>
        <Button onClick={onGetStarted} size="sm">
          Get Started
        </Button>
      </nav>
    </header>
  )
}
