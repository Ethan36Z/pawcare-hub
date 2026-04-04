import http from './http'

export const adminServicesApi = {
  list() {
    return http.get('/admin/services')
  },
}
