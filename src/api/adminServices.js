import http from './http'

export const adminServicesApi = {
  list(params = {}) {
    return http.get('/admin/services', { params })
  },
  create(payload) {
    return http.post('/admin/services', payload)
  },
  update(id, payload) {
    return http.patch(`/admin/services/${id}`, payload)
  },
  toggle(id) {
    return http.patch(`/admin/services/${id}/toggle`)
  },
}
