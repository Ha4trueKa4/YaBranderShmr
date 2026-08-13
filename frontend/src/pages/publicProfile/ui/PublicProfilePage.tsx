import { Button } from '@shared/ui'
import {
  ProfileHeader,
  type ProfileHeaderProfile,
  PublicProjectsSection,
} from '@widgets'
import { Header } from '@widgets/header'

import styles from './PublicProfilePage.module.css'

const profile: ProfileHeaderProfile = {
  bio: 'Independent art director and identity designer. Making brands feel human.',
  followersCount: 1200,
  followingCount: 146,
  name: 'Julia Morales',
  projectsCount: 8,
  username: 'juliamorales',
}

const projects = [
  {
    description: 'Creative direction · 2 days ago',
    id: 'nova-studio',
    name: 'Nova Studio',
    previewUrl: null,
  },
  {
    description: 'Financial identity · 4 days ago',
    id: 'kite-finance',
    name: 'Kite Finance',
    previewUrl: null,
  },
  {
    description: 'Retail system · 1 week ago',
    id: 'echo-market',
    name: 'Echo Market',
    previewUrl: null,
  },
  {
    description: 'Product studio · 2 weeks ago',
    id: 'form-and-field',
    name: 'Form & Field',
    previewUrl: null,
  },
]

export function PublicProfilePage() {
  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.content}>
        <ProfileHeader actions={<Button>Follow</Button>} profile={profile} />
        <PublicProjectsSection projects={projects} />
      </main>
    </div>
  )
}
