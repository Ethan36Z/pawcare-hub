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
    hasRefreshedIdentity: false,
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
      this.hasRefreshedIdentity = false
      persistSession(null)
    },
    setSession(payload) {
      this.user = payload.user ?? null
      this.token = payload.token ?? null
      this.role = normalizeRole(payload.role)
      this.isLoggedIn = payload.isLoggedIn ?? Boolean(payload.token)
      this.hasRefreshedIdentity = Boolean(payload.hasRefreshedIdentity)

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
      const nameFromEmail = payload.email?.split('@')[0]?.replace(/[._-]+/g, ' ') ?? 'Pet Owner'
      const normalizedName =
        payload.fullName?.trim() ||
        payload.name?.trim() ||
        nameFromEmail.replace(/\b\w/g, (char) => char.toUpperCase()) ||
        'Pet Owner'

      this.user = {
        id: payload.id ?? null,
        fullName: normalizedName,
        email: payload.email,
      }
      this.token = payload.token ?? this.token
      this.role = normalizeRole(payload.role)
      this.isLoggedIn = true
      this.hasRefreshedIdentity = Boolean(payload.hasRefreshedIdentity)

      persistSession({
        isLoggedIn: this.isLoggedIn,
        user: this.user,
        token: this.token,
        role: this.role,
      })
    },
    async refreshAuthenticatedUser() {
      if (!this.token || !this.isLoggedIn) {
        return
      }

      const { authApi } = await import('@/api/auth')

      try {
        const { data } = await authApi.me()
        this.setAuthenticatedUser({
          email: data.email,
          name: data.name,
          role: data.role,
          token: this.token,
          hasRefreshedIdentity: true,
        })
      } catch (error) {
        this.logout()
        throw error
      }
    },
  },
})
