package com.pawcarehub.backend.dto.admin;

public record UpsertStaffAvailabilityRequest(
    String dayOfWeek,
    String startTime,
    String endTime,
    Boolean active
) {
}
