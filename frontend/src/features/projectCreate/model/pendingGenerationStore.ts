import { makeAutoObservable } from 'mobx'

const pendingGenerationPromptStorageKey = 'brandkit:pending-generation-prompt'

class PendingGenerationStore {
  prompt: string | null = localStorage.getItem(
    pendingGenerationPromptStorageKey,
  )

  constructor() {
    makeAutoObservable(this, {}, { autoBind: true })
  }

  save(prompt: string) {
    this.prompt = prompt
    localStorage.setItem(pendingGenerationPromptStorageKey, prompt)
  }

  clear() {
    this.prompt = null
    localStorage.removeItem(pendingGenerationPromptStorageKey)
  }
}

export const pendingGenerationStore = new PendingGenerationStore()
