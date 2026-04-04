import http from './http'

export const adminServicesApi = {
  list() {
    return http.get('/admin/services')
  },
  toggle(id) {
    return http.patch(`/admin/services/${id}/toggle`)
  },
}
