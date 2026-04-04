package com.pawcarehub.backend.dto.booking;

public record RescheduleBookingRequest(
    String date,
    String time,
    String staff
) {
}
