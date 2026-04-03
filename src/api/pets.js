import http from './http'

export const petsApi = {
  me(email) {
    return http.get('/pets/me', {
      headers: {
        'X-User-Email': email,
      },
    })
  },
  list() {
    return http.get('/pets')
  },
  create(payload) {
    return http.post('/pets', payload)
  },
}
