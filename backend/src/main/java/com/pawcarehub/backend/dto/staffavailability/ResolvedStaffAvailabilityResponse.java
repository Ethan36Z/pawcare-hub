package com.pawcarehub.backend.dto.staffavailability;

public record ResolvedStaffAvailabilityResponse(
    Long staffId,
    String date,
    String startTime,
    String endTime,
    String source
) {
}
