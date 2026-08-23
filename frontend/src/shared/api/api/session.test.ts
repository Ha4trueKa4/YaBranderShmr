import { afterEach, describe, expect, it } from 'vitest'
import {
  getStoredSession,
  removeStoredSession,
  setStoredSession,
} from './session'

const sessionStorageKey = 'brandkit:session'
const session = {
  accessToken: 'access-token',
  user: {
    email: 'user@example.com',
    id: 'user-id',
    nickname: 'user',
  },
}

afterEach(() => {
  localStorage.clear()
})

describe('auth session storage', () => {
  it('stores the session in localStorage', () => {
    setStoredSession(session)

    expect(getStoredSession()).toEqual(session)
    expect(localStorage.getItem(sessionStorageKey)).toBe(
      JSON.stringify(session),
    )
  })

  it('removes the session from localStorage on sign out', () => {
    localStorage.setItem(sessionStorageKey, JSON.stringify(session))

    removeStoredSession()

    expect(localStorage.getItem(sessionStorageKey)).toBeNull()
  })
})
