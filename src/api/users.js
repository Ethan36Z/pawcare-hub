import http from './http'

export const usersApi = {
  list(params = {}) {
    return http.get('/admin/users', { params })
  },
  getById(id) {
    return http.get(`/admin/users/${id}`)
  },
}
