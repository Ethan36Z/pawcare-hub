import http from './http'

export const usersApi = {
  list() {
    return http.get('/admin/users')
  },
  getById(id) {
    return http.get(`/admin/users/${id}`)
  },
}
