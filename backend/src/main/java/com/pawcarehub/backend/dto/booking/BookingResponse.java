package com.pawcarehub.backend.dto.booking;

public record BookingResponse(
    Long id,
    String petName,
    Long serviceId,
    String service,
    String date,
    String time,
    String status,
    String clinic,
    Long staffId,
    String staff,
    String ownerEmail
) {
}
