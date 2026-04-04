import http from './http'

export const adminBookingsApi = {
  list() {
    return http.get('/admin/bookings')
  },
  confirm(id) {
    return http.patch(`/admin/bookings/${id}/confirm`)
  },
  cancel(id) {
    return http.patch(`/admin/bookings/${id}/cancel`)
  },
}
