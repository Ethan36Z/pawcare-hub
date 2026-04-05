import http from './http'

export const adminStaffApi = {
  list() {
    return http.get('/admin/staff')
  },
  listForOperations() {
    return http.get('/admin/staff/operations-list')
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
  listAvailability(staffId) {
    return http.get(`/admin/staff/${staffId}/availability`)
  },
  createAvailability(staffId, payload) {
    return http.post(`/admin/staff/${staffId}/availability`, payload)
  },
  updateAvailability(staffId, availabilityId, payload) {
    return http.patch(`/admin/staff/${staffId}/availability/${availabilityId}`, payload)
  },
  toggleAvailability(staffId, availabilityId) {
    return http.patch(`/admin/staff/${staffId}/availability/${availabilityId}/toggle`)
  },
  deleteAvailability(staffId, availabilityId) {
    return http.delete(`/admin/staff/${staffId}/availability/${availabilityId}`)
  },
}
