import http from './http'

export const staffApi = {
  list() {
    return http.get('/staff')
  },
  availability(staffId, date) {
    return http.get(`/staff/${staffId}/availability`, {
      params: {
        date,
      },
    })
  },
}
