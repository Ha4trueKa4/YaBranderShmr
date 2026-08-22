import { getAvatarInitials } from '@shared/lib'
import { Avatar, Button } from '@shared/ui'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

import styles from './ProfileHeader.module.css'

export interface ProfileHeaderProps {
  actions?: ReactNode
  onFollowersClick?: () => void
  onFollowingClick?: () => void
  onProjectsClick?: () => void
  profile: ProfileHeaderProfile
}

export interface ProfileHeaderProfile {
  followersCount: number
  followingCount: number
  name: string
  projectsCount: number
  username: string
}

export function ProfileHeader({
  actions,
  onFollowersClick,
  onFollowingClick,
  onProjectsClick,
  profile,
}: ProfileHeaderProps) {
  const { t } = useTranslation()

  return (
    <section className={styles.profile}>
      <div className={styles.cover} />
      <div className={styles.content}>
        <Avatar
          className={styles.avatar}
          initials={getAvatarInitials(profile.name)}
        />
        <div className={styles.identity}>
          <h1 className={styles.username}>@{profile.name}</h1>
          {/*<p className={styles.username}>@{profile.username}</p>*/}
          {/* <p className={styles.bio}>{profile.bio}</p> */}
        </div>
        <div className={styles.actions}>
          <dl className={styles.stats}>
            <Button
              className={styles.statButton}
              disabled={!onProjectsClick}
              onClick={onProjectsClick}
              variant={onFollowersClick ? 'ghost' : 'ghost'}
            >
              {t('{{count}} projects', { count: profile.projectsCount })}
            </Button>
            <Button
              className={styles.statButton}
              disabled={!onFollowersClick}
              onClick={onFollowersClick}
              variant={onFollowersClick ? 'ghost' : 'ghost'}
            >
              {t('{{count}} followers', { count: profile.followersCount })}
            </Button>
            <Button
              className={styles.statButton}
              disabled={!onFollowingClick}
              onClick={onFollowingClick}
              variant={onFollowersClick ? 'ghost' : 'ghost'}
            >
              {t('{{count}} following', { count: profile.followingCount })}
            </Button>
          </dl>
          {actions && <div className={styles.profileActions}>{actions}</div>}
        </div>
      </div>
    </section>
  )
}
