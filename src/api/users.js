import http from './http'

export const usersApi = {
  list() {
    return http.get('/users')
  },
}
