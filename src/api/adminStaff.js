import http from './http'

export const adminStaffApi = {
  list() {
    return http.get('/admin/staff')
  },
  create(payload) {
    return http.post('/admin/staff', payload)
  },
  update(id, payload) {
    return http.patch(`/admin/staff/${id}`, payload)
  },
  toggle(id) {
    return http.patch(`/admin/staff/${id}/toggle`)
  },
}
