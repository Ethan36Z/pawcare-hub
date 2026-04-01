import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: null,
    role: 'guest',
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdmin: (state) => state.role === 'admin',
  },
  actions: {
    setSession(payload) {
      this.user = payload.user
      this.token = payload.token
      this.role = payload.role ?? 'customer'
    },
    clearSession() {
      this.user = null
      this.token = null
      this.role = 'guest'
    },
  },
})
