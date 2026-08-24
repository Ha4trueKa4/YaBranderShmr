import { makeAutoObservable } from 'mobx'

export const maxProjectReferences = 2

export interface ProjectReference {
  id: string
  name: string
}

class ProjectReferencesStore {
  referencesById = new Map<string, ProjectReference>()

  constructor() {
    makeAutoObservable(this, {}, { autoBind: true })
  }

  get references() {
    return [...this.referencesById.values()]
  }

  get ids() {
    return [...this.referencesById.keys()]
  }

  get canAddReference() {
    return this.referencesById.size < maxProjectReferences
  }

  has(id: string) {
    return this.referencesById.has(id)
  }

  add(reference: ProjectReference) {
    if (!this.canAddReference || this.has(reference.id)) {
      return
    }

    this.referencesById.set(reference.id, reference)
  }

  remove(id: string) {
    this.referencesById.delete(id)
  }

  toggle(reference: ProjectReference) {
    if (this.has(reference.id)) {
      this.remove(reference.id)
      return
    }

    this.add(reference)
  }
}

export const projectReferencesStore = new ProjectReferencesStore()
