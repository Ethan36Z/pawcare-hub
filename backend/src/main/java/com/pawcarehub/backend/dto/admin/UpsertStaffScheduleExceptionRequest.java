package com.pawcarehub.backend.dto.admin;

public record UpsertStaffScheduleExceptionRequest(
    String date,
    Boolean available,
    String customStartTime,
    String customEndTime
) {
}
