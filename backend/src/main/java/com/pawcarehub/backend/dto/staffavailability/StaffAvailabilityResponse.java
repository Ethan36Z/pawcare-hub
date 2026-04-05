package com.pawcarehub.backend.dto.staffavailability;

public record StaffAvailabilityResponse(
    Long id,
    Long staffId,
    String dayOfWeek,
    String startTime,
    String endTime,
    boolean active
) {
}
