package com.pawcarehub.backend.dto.staffavailability;

public record StaffScheduleExceptionResponse(
    Long id,
    Long staffId,
    String date,
    boolean available,
    String customStartTime,
    String customEndTime
) {
}
