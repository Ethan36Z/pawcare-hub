package com.pawcarehub.backend.dto.scheduling;

import java.util.List;

public record ScheduleBookingConflictResponse(
    String code,
    String message,
    List<ScheduleBookingConflictItemResponse> conflicts
) {
}
