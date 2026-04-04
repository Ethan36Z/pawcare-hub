import http from './http'

export const adminServicesApi = {
  list() {
    return http.get('/admin/services')
  },
  create(payload) {
    return http.post('/admin/services', payload)
  },
  toggle(id) {
    return http.patch(`/admin/services/${id}/toggle`)
  },
}
