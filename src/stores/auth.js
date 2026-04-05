import { defineStore } from 'pinia'

const STORAGE_KEY = 'pawcarehub-auth'

function normalizeRole(role) {
  if (role === 'admin') {
    return 'admin'
  }

  if (role === 'front_desk') {
    return 'front_desk'
  }

  if (role === 'doctor') {
    return 'doctor'
  }

  if (role === 'user' || role === 'customer') {
    return 'user'
  }

  return null
}

export function resolveMockRole(email = '') {
  if (/(doctor|dr\.|vet)/i.test(email)) {
    return 'doctor'
  }

  if (/(frontdesk|front-desk|reception|receptionist)/i.test(email)) {
    return 'front_desk'
  }

  if (/(admin|clinic|team|manager)/i.test(email)) {
    return 'admin'
  }

  return 'user'
}

export function getDefaultRouteForRole(role) {
  return role === 'admin' || role === 'front_desk' || role === 'doctor' ? '/admin' : '/pets'
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
    isFrontDesk: (state) => state.role === 'front_desk',
    isDoctor: (state) => state.role === 'doctor',
    isClinicStaff: (state) => ['admin', 'front_desk', 'doctor'].includes(state.role),
  },
  actions: {
    login(payload) {
      this.setAuthenticatedUser(payload)
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
    setAuthenticatedUser(payload) {
      const role = resolveMockRole(payload.email)
      const nameFromEmail = payload.email?.split('@')[0]?.replace(/[._-]+/g, ' ') ?? 'Pet Owner'
      const normalizedName =
        payload.fullName?.trim() ||
        payload.name?.trim() ||
        nameFromEmail.replace(/\b\w/g, (char) => char.toUpperCase()) ||
        'Pet Owner'

      this.user = {
        id: role === 'admin' ? 'mock-admin-1' : 'mock-user-1',
        fullName: normalizedName,
        email: payload.email,
      }
      this.token = payload.token ?? `session-${payload.email}`
      this.role = normalizeRole(payload.role ?? role)
      this.isLoggedIn = true

      persistSession({
        isLoggedIn: this.isLoggedIn,
        user: this.user,
        token: this.token,
        role: this.role,
      })
    },
  },
})
