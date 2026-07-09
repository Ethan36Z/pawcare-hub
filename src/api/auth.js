import http from './http'

export const authApi = {
  login(payload) {
    return http.post('/auth/login', payload)
  },
  register(payload) {
    return http.post('/auth/register', payload)
  },
  me() {
    return http.get('/auth/me')
  },
  profile() {
    return http.get('/auth/profile')
  },
  updateProfile(payload) {
    return http.patch('/auth/profile', payload)
  },
  changePassword(payload) {
    return http.patch('/auth/change-password', payload)
  },
  changeEmail(payload) {
    return http.patch('/auth/change-email', payload)
  },
  deleteAccount() {
    return http.patch('/auth/delete-account')
  },
}
