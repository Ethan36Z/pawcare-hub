import http from './http'

export const adminBookingsApi = {
  list() {
    return http.get('/admin/bookings')
  },
}
