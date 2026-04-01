import { defineStore } from 'pinia'

const STORAGE_KEY = 'pawcarehub-auth'

function loadStoredSession() {
  if (typeof window === 'undefined') {
    return null
  }

  const raw = window.localStorage.getItem(STORAGE_KEY)

  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch {
    window.localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

function persistSession(session) {
  if (typeof window === 'undefined') {
    return
  }

  if (!session?.token) {
    window.localStorage.removeItem(STORAGE_KEY)
    return
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

const storedSession = loadStoredSession()

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isLoggedIn: Boolean(storedSession?.token),
    user: storedSession?.user ?? null,
    token: storedSession?.token ?? null,
    role: storedSession?.role ?? 'guest',
  }),
  getters: {
    isAuthenticated: (state) => state.isLoggedIn,
    isAdmin: (state) => state.role === 'admin',
  },
  actions: {
    login(payload) {
      const nameFromEmail = payload.email?.split('@')[0]?.replace(/[._-]+/g, ' ') ?? 'Pet Owner'
      const normalizedName =
        payload.fullName?.trim() ||
        nameFromEmail.replace(/\b\w/g, (char) => char.toUpperCase()) ||
        'Pet Owner'

      this.user = {
        id: 'mock-user-1',
        fullName: normalizedName,
        email: payload.email,
      }
      this.token = 'mock-token-pawcarehub'
      this.role = 'customer'
      this.isLoggedIn = true

      persistSession({
        user: this.user,
        token: this.token,
        role: this.role,
      })
    },
    logout() {
      this.user = null
      this.token = null
      this.role = 'guest'
      this.isLoggedIn = false
      persistSession(null)
    },
    setSession(payload) {
      this.user = payload.user
      this.token = payload.token
      this.role = payload.role ?? 'customer'
      this.isLoggedIn = Boolean(payload.token)

      persistSession({
        user: this.user,
        token: this.token,
        role: this.role,
      })
    },
    clearSession() {
      this.logout()
    },
  },
})
