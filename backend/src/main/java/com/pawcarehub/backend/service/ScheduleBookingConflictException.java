package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.scheduling.ScheduleBookingConflictResponse;

public class ScheduleBookingConflictException extends RuntimeException {
    private final ScheduleBookingConflictResponse response;

    public ScheduleBookingConflictException(ScheduleBookingConflictResponse response) {
        super(response.message());
        this.response = response;
    }

    public ScheduleBookingConflictResponse getResponse() {
        return response;
    }
}
