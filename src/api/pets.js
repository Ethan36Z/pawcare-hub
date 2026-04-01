import http from './http'

export const petsApi = {
  list() {
    return http.get('/pets')
  },
  create(payload) {
    return http.post('/pets', payload)
  },
}
