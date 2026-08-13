import type { UserProfile } from '@shared/api/types'
import { Avatar, Button } from '@shared/ui'

import styles from './ProfileHeader.module.css'

export interface ProfileHeaderProps {
  onEditClick?: () => void
  user: UserProfile
}

export function ProfileHeader({ user, onEditClick }: ProfileHeaderProps) {
  const name = user.name ?? 'Oliver Lee'
  const initials = name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  // TODO: Replace temporary profile details when the API returns them.
  const profileDetails = {
    bio: 'Building ideas into brands. Product designer and creative technologist.',
    followersCount: 340,
    followingCount: 28,
    projectsCount: 12,
    username: 'oliverlee',
  }

  return (
    <section className={styles.profile}>
      <div className={styles.cover} />
      <div className={styles.content}>
        <Avatar className={styles.avatar} initials={initials} />
        <div className={styles.identity}>
          <h1>{name}</h1>
          <p className={styles.username}>@{profileDetails.username}</p>
          <p className={styles.bio}>{profileDetails.bio}</p>
        </div>
        <div className={styles.actions}>
          <dl className={styles.stats}>
            <div>
              <dt>{profileDetails.projectsCount}</dt>
              <dd>projects</dd>
            </div>
            <div>
              <dt>{profileDetails.followersCount}</dt>
              <dd>followers</dd>
            </div>
            <div>
              <dt>{profileDetails.followingCount}</dt>
              <dd>following</dd>
            </div>
          </dl>
          <Button
            className={styles.editButton}
            onClick={onEditClick}
            variant="secondary"
          >
            Edit profile
          </Button>
        </div>
      </div>
    </section>
  )
}
