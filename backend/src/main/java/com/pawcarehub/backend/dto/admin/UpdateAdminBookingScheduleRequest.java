package com.pawcarehub.backend.dto.admin;

public record UpdateAdminBookingScheduleRequest(
    Long staffId,
    String date,
    String time
) {
}
