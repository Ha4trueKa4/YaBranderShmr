import { Avatar } from '@shared/ui'
import type { ReactNode } from 'react'

import styles from './ProfileHeader.module.css'

export interface ProfileHeaderProps {
  actions?: ReactNode
  profile: ProfileHeaderProfile
}

export interface ProfileHeaderProfile {
  bio: string
  followersCount: number
  followingCount: number
  name: string
  projectsCount: number
  username: string
}

export function ProfileHeader({ actions, profile }: ProfileHeaderProps) {
  const initials = profile.name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  return (
    <section className={styles.profile}>
      <div className={styles.cover} />
      <div className={styles.content}>
        <Avatar className={styles.avatar} initials={initials} />
        <div className={styles.identity}>
          <h1>{profile.name}</h1>
          <p className={styles.username}>@{profile.username}</p>
          <p className={styles.bio}>{profile.bio}</p>
        </div>
        <div className={styles.actions}>
          <dl className={styles.stats}>
            <div>
              <dt>{profile.projectsCount}</dt>
              <dd>projects</dd>
            </div>
            <div>
              <dt>{profile.followersCount}</dt>
              <dd>followers</dd>
            </div>
            <div>
              <dt>{profile.followingCount}</dt>
              <dd>following</dd>
            </div>
          </dl>
          {actions && <div className={styles.profileActions}>{actions}</div>}
        </div>
      </div>
    </section>
  )
}
