import http from './http'

export const bookingsApi = {
  list() {
    return http.get('/bookings')
  },
  create(payload) {
    return http.post('/bookings', payload)
  },
}
