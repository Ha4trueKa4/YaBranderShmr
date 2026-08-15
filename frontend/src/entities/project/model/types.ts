export interface FeedProject {
  author: {
    avatarUrl: string | null
    id: string
    name: string
  }
  id: string
  name: string
  previewUrl: string | null
  publishedAt: string
}

export interface PublicProfileProject {
  description: string
  id: string
  name: string
  previewUrl: string | null
}
