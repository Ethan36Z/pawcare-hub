package com.pawcarehub.backend.dto.scheduling;

public record BookingReassignmentRequest(
    Long bookingId,
    Long replacementStaffId
) {
}
