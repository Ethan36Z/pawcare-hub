package com.pawcarehub.backend.dto.booking;

public record CreateBookingRequest(
    String petName,
    Long serviceId,
    String service,
    String date,
    String time,
    String status,
    String clinic,
    String staff
) {
}
