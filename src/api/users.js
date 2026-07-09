import http from './http'

export const usersApi = {
  list(params = {}) {
    return http.get('/admin/users', { params })
  },
  getById(id) {
    return http.get(`/admin/users/${id}`)
  },
  deactivate(id) {
    return http.patch(`/admin/users/${id}/deactivate`)
  },
  reactivate(id) {
    return http.patch(`/admin/users/${id}/reactivate`)
  },
  updateRole(id, role) {
    return http.patch(`/admin/users/${id}/role`, { role })
  },
}
