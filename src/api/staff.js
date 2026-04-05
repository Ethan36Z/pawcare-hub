import http from './http'

export const staffApi = {
  list() {
    return http.get('/staff')
  },
  listHomepage() {
    return http.get('/staff/homepage')
  },
  availability(staffId, date) {
    return http.get(`/staff/${staffId}/availability`, {
      params: {
        date,
      },
    })
  },
}
