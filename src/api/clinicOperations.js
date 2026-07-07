import http from './http'

export const clinicOperationsApi = {
  listStaffScheduleExceptions(staffId) {
    return http.get(`/admin/operations/staff/${staffId}/schedule-exceptions`)
  },
  createStaffScheduleException(staffId, payload) {
    return http.post(`/admin/operations/staff/${staffId}/schedule-exceptions`, payload)
  },
  resolveCreateStaffScheduleExceptionConflicts(staffId, payload) {
    return http.post(`/admin/operations/staff/${staffId}/schedule-exceptions/resolve-conflicts`, payload)
  },
  updateStaffScheduleException(staffId, exceptionId, payload) {
    return http.patch(`/admin/operations/staff/${staffId}/schedule-exceptions/${exceptionId}`, payload)
  },
  resolveUpdateStaffScheduleExceptionConflicts(staffId, exceptionId, payload) {
    return http.post(`/admin/operations/staff/${staffId}/schedule-exceptions/${exceptionId}/resolve-conflicts`, payload)
  },
  deleteStaffScheduleException(staffId, exceptionId) {
    return http.delete(`/admin/operations/staff/${staffId}/schedule-exceptions/${exceptionId}`)
  },
}
