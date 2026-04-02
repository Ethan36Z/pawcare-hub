import { defineStore } from 'pinia'

const STORAGE_KEY = 'pawcarehub-auth'
const ADMIN_EMAIL_PATTERN = /(admin|staff|clinic|team|manager)/i

function normalizeRole(role) {
  if (role === 'admin') {
    return 'admin'
  }

  if (role === 'user' || role === 'customer') {
    return 'user'
  }

  return null
}

export function resolveMockRole(email = '') {
  return ADMIN_EMAIL_PATTERN.test(email) ? 'admin' : 'user'
}

export function getDefaultRouteForRole(role) {
  return role === 'admin' ? '/admin' : '/pets'
}

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
    role: normalizeRole(storedSession?.role),
  }),
  getters: {
    isAuthenticated: (state) => state.isLoggedIn,
    isUser: (state) => state.role === 'user',
    isAdmin: (state) => state.role === 'admin',
  },
  actions: {
    login(payload) {
      const role = resolveMockRole(payload.email)
      const nameFromEmail = payload.email?.split('@')[0]?.replace(/[._-]+/g, ' ') ?? 'Pet Owner'
      const normalizedName =
        payload.fullName?.trim() ||
        nameFromEmail.replace(/\b\w/g, (char) => char.toUpperCase()) ||
        'Pet Owner'

      this.user = {
        id: role === 'admin' ? 'mock-admin-1' : 'mock-user-1',
        fullName: normalizedName,
        email: payload.email,
      }
      this.token = `mock-token-${role}-pawcarehub`
      this.role = normalizeRole(role)
      this.isLoggedIn = true

      persistSession({
        isLoggedIn: this.isLoggedIn,
        user: this.user,
        token: this.token,
        role: this.role,
      })
    },
    logout() {
      this.user = null
      this.token = null
      this.role = null
      this.isLoggedIn = false
      persistSession(null)
    },
    setSession(payload) {
      this.user = payload.user ?? null
      this.token = payload.token ?? null
      this.role = normalizeRole(payload.role)
      this.isLoggedIn = payload.isLoggedIn ?? Boolean(payload.token)

      persistSession({
        isLoggedIn: this.isLoggedIn,
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
