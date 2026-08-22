import { makeAutoObservable } from 'mobx'

export interface GenerationDraft {
  description: string
  model: string
}

type GenerationProcessStatus = 'idle' | 'starting' | 'pending'

class GenerationProcessStore {
  draft: GenerationDraft | null = null
  jobId: string | null = null
  projectId: string | null = null
  status: GenerationProcessStatus = 'idle'

  constructor() {
    makeAutoObservable(this, {}, { autoBind: true })
  }

  get isInProgress() {
    return this.status !== 'idle'
  }

  begin(draft: GenerationDraft) {
    this.draft = draft
    this.jobId = null
    this.projectId = null
    this.status = 'starting'
  }

  trackJob(projectId: string, jobId: string) {
    this.projectId = projectId
    this.jobId = jobId
    this.status = 'pending'
  }

  reset() {
    this.draft = null
    this.jobId = null
    this.projectId = null
    this.status = 'idle'
  }
}

export const generationProcessStore = new GenerationProcessStore()
