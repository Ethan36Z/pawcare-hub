import http from './http'

export const authApi = {
  login(payload) {
    return http.post('/auth/login', payload)
  },
  register(payload) {
    return http.post('/auth/register', payload)
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
  deleteAccount() {
    return http.patch('/auth/delete-account')
  },
}
