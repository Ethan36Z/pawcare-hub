import http from './http'

export const bookingsApi = {
  me(email) {
    return http.get('/bookings/me', {
      headers: {
        'X-User-Email': email,
      },
    })
  },
  list() {
    return http.get('/bookings')
  },
  create(email, payload) {
    return http.post('/bookings', payload, {
      headers: {
        'X-User-Email': email,
      },
    })
  },
  cancel(email, id) {
    return http.patch(`/bookings/${id}/cancel`, null, {
      headers: {
        'X-User-Email': email,
      },
    })
  },
}
