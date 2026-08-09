import { configure, makeAutoObservable } from 'mobx'

import type { AuthSession } from './session'
import {
  getStoredSession,
  removeStoredSession,
  setStoredSession,
} from './session'

configure({ enforceActions: 'always' })

class AuthStore {
  session: AuthSession | null = getStoredSession()

  constructor() {
    makeAutoObservable(this, {}, { autoBind: true })
  }

  get isAuthorized() {
    return Boolean(this.session)
  }

  signIn(session: AuthSession) {
    setStoredSession(session)
    this.session = session
  }

  signOut() {
    removeStoredSession()
    this.session = null
  }
}

export const authStore = new AuthStore()
