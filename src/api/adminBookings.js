import http from './http'

export const adminBookingsApi = {
  list(params = {}) {
    return http.get('/admin/bookings', { params })
  },
  confirm(id) {
    return http.patch(`/admin/bookings/${id}/confirm`)
  },
  cancel(id) {
    return http.patch(`/admin/bookings/${id}/cancel`)
  },
  complete(id, payload) {
    return http.patch(`/admin/bookings/${id}/complete`, payload)
  },
  updateSchedule(id, payload) {
    return http.patch(`/admin/bookings/${id}/schedule`, payload)
  },
}
