import http from './http'

export const petsApi = {
  me(email) {
    return http.get('/pets/me', {
      headers: {
        'X-User-Email': email,
      },
    })
  },
  detail(email, id) {
    return http.get(`/pets/${id}`, {
      headers: {
        'X-User-Email': email,
      },
    })
  },
  list() {
    return http.get('/pets')
  },
  create(email, payload) {
    return http.post('/pets', payload, {
      headers: {
        'X-User-Email': email,
      },
    })
  },
  update(email, id, payload) {
    return http.patch(`/pets/${id}`, payload, {
      headers: {
        'X-User-Email': email,
      },
    })
  },
  addMedicalNote(email, id, payload) {
    return http.post(`/pets/${id}/medical-notes`, payload, {
      headers: {
        'X-User-Email': email,
      },
    })
  },
  remove(email, id) {
    return http.delete(`/pets/${id}`, {
      headers: {
        'X-User-Email': email,
      },
    })
  },
}
