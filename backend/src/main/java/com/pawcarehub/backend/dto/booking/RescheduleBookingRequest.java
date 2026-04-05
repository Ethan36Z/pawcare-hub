package com.pawcarehub.backend.dto.booking;

public record RescheduleBookingRequest(
    String date,
    String time,
    Long staffId,
    String staff
) {
}
