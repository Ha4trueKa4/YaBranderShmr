import { makeAutoObservable } from 'mobx'

export interface ProjectDraft {
  description: string
  name: string
}

class PendingGenerationStore {
  draft: ProjectDraft | null = null

  constructor() {
    makeAutoObservable(this, {}, { autoBind: true })
  }

  save(draft: ProjectDraft) {
    this.draft = draft
  }

  take() {
    const draft = this.draft

    this.draft = null

    return draft
  }
}

export const pendingGenerationStore = new PendingGenerationStore()
