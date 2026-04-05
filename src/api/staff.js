import http from './http'

export const staffApi = {
  list() {
    return http.get('/staff')
  },
}
