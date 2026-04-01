import http from './http'

export const servicesApi = {
  list() {
    return http.get('/services')
  },
  detail(id) {
    return http.get(`/services/${id}`)
  },
}
