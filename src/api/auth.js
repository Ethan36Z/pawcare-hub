import http from './http'

export const authApi = {
  login(payload) {
    return http.post('/auth/login', payload)
  },
  register(payload) {
    return http.post('/auth/register', payload)
  },
  profile(email) {
    return http.get('/auth/profile', {
      headers: {
        'X-User-Email': email,
      },
    })
  },
  updateProfile(email, payload) {
    return http.patch('/auth/profile', payload, {
      headers: {
        'X-User-Email': email,
      },
    })
  },
  changePassword(email, payload) {
    return http.patch('/auth/change-password', payload, {
      headers: {
        'X-User-Email': email,
      },
    })
  },
}
