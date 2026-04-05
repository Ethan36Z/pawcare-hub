import http from './http'

export const petsApi = {
  me() {
    return http.get('/pets/me')
  },
  detail(id) {
    return http.get(`/pets/${id}`)
  },
  list() {
    return http.get('/pets')
  },
  create(payload) {
    return http.post('/pets', payload)
  },
  update(id, payload) {
    return http.patch(`/pets/${id}`, payload)
  },
  addMedicalNote(id, payload) {
    return http.post(`/pets/${id}/medical-notes`, payload)
  },
  remove(id) {
    return http.delete(`/pets/${id}`)
  },
}
