package com.pawcarehub.backend.dto.scheduling;

import java.util.List;

public record ScheduleBookingConflictItemResponse(
    Long bookingId,
    String date,
    String startTime,
    String endTime,
    String petName,
    String ownerName,
    String ownerEmail,
    String serviceName,
    Long currentStaffId,
    String currentStaffName,
    String status,
    List<EligibleReplacementStaffResponse> eligibleReplacements
) {
}
