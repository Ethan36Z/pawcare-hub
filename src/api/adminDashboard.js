import http from './http'

export const adminDashboardApi = {
  stats() {
    return http.get('/admin/dashboard/stats')
  },
}
