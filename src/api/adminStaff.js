import http from './http'

export const adminStaffApi = {
  list() {
    return http.get('/admin/staff')
  },
  toggle(id) {
    return http.patch(`/admin/staff/${id}/toggle`)
  },
}
