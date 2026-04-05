import http from './http'

export const bookingsApi = {
  me() {
    return http.get('/bookings/me')
  },
  detail(id) {
    return http.get(`/bookings/${id}`)
  },
  list() {
    return http.get('/bookings')
  },
  create(payload) {
    return http.post('/bookings', payload)
  },
  cancel(id) {
    return http.patch(`/bookings/${id}/cancel`)
  },
  reschedule(id, payload) {
    return http.patch(`/bookings/${id}/reschedule`, payload)
  },
}
