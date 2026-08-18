import { getAvatarInitials } from '@shared/lib'
import { Avatar, Button } from '@shared/ui'
import type { ReactNode } from 'react'

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
  return (
    <section className={styles.profile}>
      <div className={styles.cover} />
      <div className={styles.content}>
        <Avatar
          className={styles.avatar}
          initials={getAvatarInitials(profile.name)}
        />
        <div className={styles.identity}>
          <h1>{profile.name}</h1>
          <p className={styles.username}>@{profile.username}</p>
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
              {profile.projectsCount} projects
            </Button>
            <Button
              className={styles.statButton}
              disabled={!onFollowersClick}
              onClick={onFollowersClick}
              variant={onFollowersClick ? 'ghost' : 'ghost'}
            >
              {profile.followersCount} followers
            </Button>
            <Button
              className={styles.statButton}
              disabled={!onFollowingClick}
              onClick={onFollowingClick}
              variant={onFollowersClick ? 'ghost' : 'ghost'}
            >
              {profile.followingCount} following
            </Button>
          </dl>
          {actions && <div className={styles.profileActions}>{actions}</div>}
        </div>
      </div>
    </section>
  )
}
