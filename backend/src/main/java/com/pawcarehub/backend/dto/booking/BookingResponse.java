package com.pawcarehub.backend.dto.booking;

public record BookingResponse(
    String petName,
    String service,
    String date,
    String time,
    String status,
    String clinic,
    String staff,
    String ownerEmail
) {
}
